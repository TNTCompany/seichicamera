package com.tnt.seichicamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SeichiCamera";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";

    // 状态保存的 Key
    private static final String STATE_IMAGE_URI = "state_image_uri";
    private static final String STATE_ALPHA = "state_alpha";
    private static final String STATE_TRANSLATION_X = "state_translation_x";
    private static final String STATE_TRANSLATION_Y = "state_translation_y";
    private static final String STATE_SCALE = "state_scale";
    private static final String STATE_ROTATION = "state_rotation";

    // 权限
    private static final String[] REQUIRED_PERMISSIONS;
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS = new String[]{ Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES };
        } else {
            REQUIRED_PERMISSIONS = new String[]{ Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE };
        }
    }

    // UI 组件
    private PreviewView cameraPreview;
    private ImageView overlayImageView;
    private SeekBar transparencySlider;
    private Button loadImageButton;
    private ImageButton captureButton;
    private ImageButton settingsButton;
    private Button mirrorButton;
    private Button resetButton;
    private Button gridButton;
    private GridView gridView;

    // CameraX
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    // Launchers
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;
    private ActivityResultLauncher<String> pickContentLauncher;

    // 手势检测
    private ScaleGestureDetector scaleGestureDetector;
    private float mScaleFactor = 1.0f;
    private float mRotation = 0.0f; // 注意：旋转功能在此简化版中未完全实现
    private float mTranslationX = 0.0f;
    private float mTranslationY = 0.0f;
    private float mLastTouchX, mLastTouchY;
    private int mActivePointerId = MotionEvent.INVALID_POINTER_ID;

    private Uri currentImageUri;

    // 将 OnImageSavedCallback 提取为成员变量
    private final ImageCapture.OnImageSavedCallback onImageSavedCallback =
            new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    String msg = getString(R.string.photo_saved);
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, msg + ": " + outputFileResults.getSavedUri());
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Log.e(TAG, "拍照失败: " + exception.getMessage(), exception);
                    Toast.makeText(MainActivity.this, getString(R.string.photo_failed), Toast.LENGTH_SHORT).show();
                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        cameraExecutor = Executors.newSingleThreadExecutor();
        registerActivityResults();
        checkPermissionsAndStartCamera();
        setupListeners();
        setupGestureDetectors();

        // 恢复状态
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }
    }

    private void initViews() {
        cameraPreview = findViewById(R.id.camera_preview);
        overlayImageView = findViewById(R.id.overlay_image_view);
        transparencySlider = findViewById(R.id.transparency_slider);
        loadImageButton = findViewById(R.id.load_image_button);
        captureButton = findViewById(R.id.capture_button);
        settingsButton = findViewById(R.id.settings_button);
        mirrorButton = findViewById(R.id.mirror_button);
        resetButton = findViewById(R.id.reset_button);
        gridButton = findViewById(R.id.grid_button);
        gridView = findViewById(R.id.grid_view);
    }

    private void registerActivityResults() {
        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    if (allPermissionsGranted(permissions)) {
                        startCamera();
                    } else {
                        Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_LONG).show();
                        finish();
                    }
                });

        pickMediaLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        loadOverlayImage(uri);
                    }
                });

        pickContentLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        loadOverlayImage(uri);
                    }
                });
    }

    private boolean allPermissionsGranted(Map<String, Boolean> permissions) {
        // 检查 Map 中的所有值是否都为 true
        for (boolean granted : permissions.values()) {
            if (!granted) return false;
        }
        return true;
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getBaseContext(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void checkPermissionsAndStartCamera() {
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissionsLauncher.launch(REQUIRED_PERMISSIONS);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupListeners() {
        // 拍摄
        captureButton.setOnClickListener(v -> takePhoto());

        // 加载图片
        loadImageButton.setOnClickListener(v -> {
            if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(this)) {
                pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            } else {
                pickContentLauncher.launch("image/*");
            }
        });

        // 设置
        settingsButton.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        // 透明度
        transparencySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                overlayImageView.setAlpha(progress / 100f);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 镜像
        mirrorButton.setOnClickListener(v -> {
            overlayImageView.setScaleX(overlayImageView.getScaleX() * -1);
        });

        // 重置
        resetButton.setOnClickListener(v -> {
            resetOverlayTransform();
        });

        // 网格
        gridButton.setOnClickListener(v -> {
            boolean isVisible = gridView.getVisibility() == View.VISIBLE;
            // [修复] 将 ViewView.INVISIBLE 改为 View.INVISIBLE
            gridView.setVisibility(isVisible ? View.GONE : View.INVISIBLE);
        });
    }

    // 核心：手势处理
    @SuppressLint("ClickableViewAccessibility")
    private void setupGestureDetectors() {
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                mScaleFactor *= detector.getScaleFactor();
                // 限制缩放范围
                mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f));
                overlayImageView.setScaleX(mScaleFactor * (overlayImageView.getScaleX() > 0 ? 1 : -1)); // 保持镜像状态
                overlayImageView.setScaleY(mScaleFactor);
                return true;
            }
        });

        // 我们将使用 OnTouchListener 来统一处理拖动、缩放和旋转
        overlayImageView.setOnTouchListener((view, event) -> {
            // 优先让 ScaleGestureDetector 处理
            scaleGestureDetector.onTouchEvent(event);

            final int action = event.getActionMasked();

            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    // 处理单指拖动
                    final int pointerIndex = event.getActionIndex();
                    mLastTouchX = event.getX(pointerIndex);
                    mLastTouchY = event.getY(pointerIndex);
                    mActivePointerId = event.getPointerId(0);
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    // 处理拖动
                    if (event.getPointerCount() == 1 && mActivePointerId != MotionEvent.INVALID_POINTER_ID) {
                        final int pointerIndex = event.findPointerIndex(mActivePointerId);
                        if (pointerIndex == -1) break; // 防止索引越界
                        final float x = event.getX(pointerIndex);
                        final float y = event.getY(pointerIndex);

                        final float dx = x - mLastTouchX;
                        final float dy = y - mLastTouchY;

                        mTranslationX += dx;
                        mTranslationY += dy;

                        overlayImageView.setTranslationX(mTranslationX);
                        overlayImageView.setTranslationY(mTranslationY);

                        mLastTouchX = x;
                        mLastTouchY = y;
                    }
                    // (旋转逻辑可以在这里添加)
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                    break;
                }
                case MotionEvent.ACTION_POINTER_UP: {
                    // 处理多点触控抬起
                    final int pointerIndex = event.getActionIndex();
                    final int pointerId = event.getPointerId(pointerIndex);

                    if (pointerId == mActivePointerId) {
                        // 如果抬起的是主手指，切换到另一个手指
                        final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                        mLastTouchX = event.getX(newPointerIndex);
                        mLastTouchY = event.getY(newPointerIndex);
                        mActivePointerId = event.getPointerId(newPointerIndex);
                    }
                    break;
                }
            }
            return true; // 消费此事件
        });
    }

    private void loadOverlayImage(Uri uri) {
        currentImageUri = uri; // 保存 URI
        overlayImageView.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(uri)
                .fitCenter()
                .into(overlayImageView);
        resetOverlayTransform();
    }

    private void resetOverlayTransform() {
        mScaleFactor = 1.0f;
        mRotation = 0.0f;
        mTranslationX = 0.0f;
        mTranslationY = 0.0f;

        overlayImageView.setTranslationX(mTranslationX);
        overlayImageView.setTranslationY(mTranslationY);
        overlayImageView.setScaleX(mScaleFactor);
        overlayImageView.setScaleY(mScaleFactor);
        overlayImageView.setRotation(mRotation);

        transparencySlider.setProgress(50);
        overlayImageView.setAlpha(0.5f);
    }


    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        // 关键：设置目标旋转为当前显示的方向
                        .setTargetRotation(cameraPreview.getDisplay().getRotation())
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "相机启动失败", e);
                Toast.makeText(this, getString(R.string.camera_start_failed), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        // 关键：在拍照前，再次更新旋转，防止中途旋转导致照片方向错误
        imageCapture.setTargetRotation(cameraPreview.getDisplay().getRotation());

        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SeichiCamera");
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();

        // 使用之前定义的 onImageSavedCallback 变量
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                onImageSavedCallback
        );
    }

    // 核心：保存状态 (用于屏幕旋转)
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentImageUri != null) {
            outState.putString(STATE_IMAGE_URI, currentImageUri.toString());
            outState.putFloat(STATE_ALPHA, overlayImageView.getAlpha());
            outState.putFloat(STATE_TRANSLATION_X, mTranslationX);
            outState.putFloat(STATE_TRANSLATION_Y, mTranslationY);
            outState.putFloat(STATE_SCALE, mScaleFactor);
            outState.putFloat(STATE_ROTATION, overlayImageView.getRotation());
            // 保存镜像状态
            outState.putFloat("state_scale_x_sign", Math.signum(overlayImageView.getScaleX()));
        }
    }

    // 核心：恢复状态
    private void restoreState(@NonNull Bundle savedInstanceState) {
        String uriString = savedInstanceState.getString(STATE_IMAGE_URI);
        if (uriString != null) {
            currentImageUri = Uri.parse(uriString);

            mTranslationX = savedInstanceState.getFloat(STATE_TRANSLATION_X);
            mTranslationY = savedInstanceState.getFloat(STATE_TRANSLATION_Y);
            mScaleFactor = savedInstanceState.getFloat(STATE_SCALE);
            mRotation = savedInstanceState.getFloat(STATE_ROTATION);
            float scaleXSign = savedInstanceState.getFloat("state_scale_x_sign", 1.0f);

            // 必须先加载图片，Glide 加载是异步的
            // 我们在 Glide 的回调中应用变换
            overlayImageView.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(currentImageUri)
                    .fitCenter()
                    // 使用导入的类名并实现监听器
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            // 图片加载完成后，立即应用保存的变换
                            overlayImageView.setAlpha(savedInstanceState.getFloat(STATE_ALPHA));
                            overlayImageView.setTranslationX(mTranslationX);

                            overlayImageView.setTranslationY(mTranslationY);
                            overlayImageView.setScaleX(mScaleFactor * scaleXSign);
                            overlayImageView.setScaleY(mScaleFactor);
                            overlayImageView.setRotation(mRotation);
                            transparencySlider.setProgress((int)(overlayImageView.getAlpha() * 100));
                            return false;
                        }
                    })
                    .into(overlayImageView);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
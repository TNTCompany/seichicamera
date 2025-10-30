package com.tnt.seichicamera;

// 导入
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout; // [新增] 导入
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
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private static final String STATE_CAMERA_LENS = "state_camera_lens";

    // 权限 (不变)
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
    private ImageButton gridButton;
    private GridView gridView;
    private ImageButton galleryButton, flipCameraButton, flashButton;

    // [新增] 变换框UI
    private FrameLayout overlayContainer;
    private View overlayBorder;
    private ImageView handleTopLeft, handleTopRight, handleBottomLeft, handleBottomRight, handleRotate;
    private List<View> transformControls;
    private boolean isEditingOverlay = false;

    // CameraX (不变)
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private CameraSelector cameraSelector;
    private ImageCapture imageCapture;
    private Preview preview;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private boolean hasFlash;
    private int currentFlashMode = ImageCapture.FLASH_MODE_OFF;

    // Launchers (不变)
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;
    private ActivityResultLauncher<String> pickContentLauncher;

    // [修改] 变换变量
    private float mScaleFactor = 1.0f;
    private float mRotationDegrees = 0.0f;
    private float mTranslationX = 0.0f;
    private float mTranslationY = 0.0f;
    private float mLastTouchX, mLastTouchY;
    private int mActivePointerId = MotionEvent.INVALID_POINTER_ID;

    private Uri currentImageUri;

    // 旋转监听 (不变)
    private OrientationEventListener orientationEventListener;
    private int currentRotation = 0;
    private List<View> rotatableViews;

    // 拍照回调 (不变)
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

        if (savedInstanceState != null) {
            lensFacing = savedInstanceState.getInt(STATE_CAMERA_LENS, CameraSelector.LENS_FACING_BACK);
        }
        cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        checkPermissionsAndStartCamera();
        setupListeners();
        setupNewTouchListeners(); // [修改]
        setupTapToFocus();
        setupOrientationListener();

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            loadGridPreference();
        }
    }

    private void initViews() {
        cameraPreview = findViewById(R.id.camera_preview);

        // [修改] 初始化所有新控件
        overlayContainer = findViewById(R.id.overlay_container);
        overlayImageView = findViewById(R.id.overlay_image_view);
        overlayBorder = findViewById(R.id.overlay_border);
        handleTopLeft = findViewById(R.id.handle_top_left);
        handleTopRight = findViewById(R.id.handle_top_right);
        handleBottomLeft = findViewById(R.id.handle_bottom_left);
        handleBottomRight = findViewById(R.id.handle_bottom_right);
        handleRotate = findViewById(R.id.handle_rotate);

        // [新增] 将控件分组
        transformControls = new ArrayList<>();
        transformControls.add(overlayBorder);
        transformControls.add(handleTopLeft);
        transformControls.add(handleTopRight);
        transformControls.add(handleBottomLeft);
        transformControls.add(handleBottomRight);
        transformControls.add(handleRotate);

        transparencySlider = findViewById(R.id.transparency_slider);
        loadImageButton = findViewById(R.id.load_image_button);
        captureButton = findViewById(R.id.capture_button);
        settingsButton = findViewById(R.id.settings_button);
        mirrorButton = findViewById(R.id.mirror_button);
        resetButton = findViewById(R.id.reset_button);
        gridButton = findViewById(R.id.grid_button);
        gridView = findViewById(R.id.grid_view);
        galleryButton = findViewById(R.id.gallery_button);
        flipCameraButton = findViewById(R.id.flip_camera_button);
        flashButton = findViewById(R.id.flash_button);

        // [功能1] 初始化所有需要旋转的视图
        rotatableViews = new ArrayList<>();
        rotatableViews.add(settingsButton);
        rotatableViews.add(flashButton);
        rotatableViews.add(gridButton);
        rotatableViews.add(galleryButton);
        rotatableViews.add(flipCameraButton);
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
            // [修改] 变换容器
            overlayContainer.setScaleX(overlayContainer.getScaleX() * -1);
        });

        // 重置
        resetButton.setOnClickListener(v -> {
            resetOverlayTransform();
        });

        // 网格
        gridButton.setOnClickListener(v -> {
            boolean isVisible = gridView.getVisibility() == View.VISIBLE;
            int newVisibility = isVisible ? View.GONE : View.VISIBLE;
            gridView.setVisibility(newVisibility);
            saveGridPreference(newVisibility);
        });

        // [功能3] 相册按钮
        galleryButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            // 为 intent 添加一个 flag，以便在新的任务中打开相册
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "无法打开相册", e);
                Toast.makeText(this, "Unable to open gallery", Toast.LENGTH_SHORT).show();
            }
        });

        // [功能3] 翻转摄像头
        flipCameraButton.setOnClickListener(v -> flipCamera());

        // [功能3] 闪光灯
        flashButton.setOnClickListener(v -> cycleFlashMode());
    }

    // [删除] 旧的 setupGestureDetectors() 方法

    // [新增] 新的触摸逻辑
    @SuppressLint("ClickableViewAccessibility")
    private void setupNewTouchListeners() {
        // 1. 点击截图，进入编辑模式
        overlayImageView.setOnClickListener(v -> {
            if (currentImageUri != null) {
                setEditMode(true);
            }
        });

        // (步骤 2: 将在此处为平移、缩放和旋转添加监听器)
    }

    // [新增] 切换编辑模式
    private void setEditMode(boolean enable) {
        isEditingOverlay = enable;
        int visibility = enable ? View.VISIBLE : View.GONE;
        for (View control : transformControls) {
            control.setVisibility(visibility);
        }
    }

    // [修改] 点击对焦
    @SuppressLint("ClickableViewAccessibility")
    private void setupTapToFocus() {
        cameraPreview.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // [新增] 如果点击了预览，则退出编辑模式
                if (isEditingOverlay) {
                    setEditMode(false);
                    // 返回 true，消费掉事件，防止触发对焦
                    return true;
                }

                if (camera == null) return false;

                // 1. 创建 MeteringPoint
                MeteringPointFactory factory = cameraPreview.getMeteringPointFactory();
                MeteringPoint point = factory.createPoint(event.getX(), event.getY());

                // 2. 创建对焦动作
                FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                        .setAutoCancelDuration(3, TimeUnit.SECONDS) // 3秒后自动取消
                        .build();

                // 3. 执行对焦
                camera.getCameraControl().startFocusAndMetering(action);

                // 4. (可选) 显示对焦提示
                // Toast.makeText(this, getString(R.string.tap_to_focus), Toast.LENGTH_SHORT).show();
                return true;
            }
            // 返回 false 以便 overlayImageView 也能接收到触摸事件
            return false;
        });
    }

    // [功能1] 旋转监听 (不变)
    private void setupOrientationListener() {
        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return;
                }

                int rotation;
                if (orientation >= 45 && orientation < 135) {
                    rotation = Surface.ROTATION_270; // 横屏 (左)
                } else if (orientation >= 135 && orientation < 225) {
                    rotation = Surface.ROTATION_180; // 倒置
                } else if (orientation >= 225 && orientation < 315) {
                    rotation = Surface.ROTATION_90;  // 横屏 (右)
                } else {
                    rotation = Surface.ROTATION_0;   // 竖屏
                }

                if (rotation != currentRotation) {
                    currentRotation = rotation;
                    rotateUi(rotation);
                }
            }
        };

        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        }
    }

    // [功能1] 旋转UI (不变)
    private void rotateUi(int rotation) {
        float targetAngle;
        switch (rotation) {
            case Surface.ROTATION_90:
                targetAngle = 270;
                break;
            case Surface.ROTATION_180:
                targetAngle = 180;
                break;
            case Surface.ROTATION_270:
                targetAngle = 90;
                break;
            case Surface.ROTATION_0:
            default:
                targetAngle = 0;
                break;
        }

        for (View view : rotatableViews) {
            view.animate().rotation(targetAngle).setDuration(300).start();
        }

        // 旋转 ImageCapture
        if (imageCapture != null) {
            imageCapture.setTargetRotation(rotation);
        }
    }

    // [新增] 恢复的方法
    private void loadOverlayImage(Uri uri) {
        currentImageUri = uri; // 保存 URI
        overlayImageView.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(uri)
                .fitCenter()
                .into(overlayImageView);

        // 关键：加载新图片时，重置所有变换
        resetOverlayTransform();
    }


    // [修改] 应用变换
    // 我们现在变换的是 overlayContainer，而不是 overlayImageView
    private void applyOverlayTransform() {
        overlayContainer.setRotation(mRotationDegrees);
        overlayContainer.setScaleX(mScaleFactor * (overlayContainer.getScaleX() > 0 ? 1 : -1)); // 保持镜像
        overlayContainer.setScaleY(mScaleFactor);
        overlayContainer.setTranslationX(mTranslationX);
        overlayContainer.setTranslationY(mTranslationY);
    }

    // [修改] 重置变换
    private void resetOverlayTransform() {
        mScaleFactor = 1.0f;
        mRotationDegrees = 0.0f;
        mTranslationX = 0.0f;
        mTranslationY = 0.0f;
        applyOverlayTransform(); // 应用重置
        overlayContainer.setScaleX(1.0f); // 重置镜像

        // 隐藏控件
        setEditMode(false);

        // 重置透明度
        transparencySlider.setProgress(50);
        overlayImageView.setAlpha(0.5f);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get(); // [修改] 存为成员变量

                preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .build();

                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .setTargetRotation(currentRotation)
                        .build();

                bindCameraUseCases();

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "相机启动失败", e);
                Toast.makeText(this, getString(R.string.camera_start_failed), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // [新增] 绑定相机用例 (不变)
    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        cameraProvider.unbindAll();
        try {
            camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture);

            updateFlashState();
        } catch (Exception e) {
            Log.e(TAG, "绑定失败", e);
        }
    }

    // [功能3] 翻转摄像头 (不变)
    private void flipCamera() {
        lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK) ?
                CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
        cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
        bindCameraUseCases();
    }

    // [功能3] 更新闪光灯状态 (不变)
    private void updateFlashState() {
        if (camera != null) {
            CameraInfo cameraInfo = camera.getCameraInfo();
            hasFlash = cameraInfo.hasFlashUnit();
            flashButton.setVisibility(hasFlash ? View.VISIBLE : View.GONE);

            if (currentFlashMode == ImageCapture.FLASH_MODE_ON) {
                flashButton.setImageResource(R.drawable.ic_flash_on_24);
            } else {
                flashButton.setImageResource(R.drawable.ic_flash_off_24);
            }
            imageCapture.setFlashMode(currentFlashMode);
        }
    }

    // [功能3] 循环切换闪光灯 (不变)
    private void cycleFlashMode() {
        if (!hasFlash) return;

        if (currentFlashMode == ImageCapture.FLASH_MODE_OFF) {
            currentFlashMode = ImageCapture.FLASH_MODE_ON;
            Toast.makeText(this, getString(R.string.flash_on), Toast.LENGTH_SHORT).show();
        } else {
            currentFlashMode = ImageCapture.FLASH_MODE_OFF;
            Toast.makeText(this, getString(R.string.flash_off), Toast.LENGTH_SHORT).show();
        }
        updateFlashState();
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        // 旋转已在 rotateUi() 中设置
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

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                onImageSavedCallback
        );
    }

    // [功能4] 保存/加载网格线偏好 (不变)
    private void saveGridPreference(int visibility) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean("save_grid_state", visibility == View.VISIBLE).apply();
    }

    private void loadGridPreference() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showGrid = prefs.getBoolean("save_grid_state", false);
        gridView.setVisibility(showGrid ? View.VISIBLE : View.GONE);
    }

    // 核心：保存状态 (不变)
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CAMERA_LENS, lensFacing);
        if (currentImageUri != null) {
            outState.putString(STATE_IMAGE_URI, currentImageUri.toString());
            outState.putFloat(STATE_ALPHA, overlayImageView.getAlpha());
            outState.putFloat(STATE_TRANSLATION_X, mTranslationX);
            outState.putFloat(STATE_TRANSLATION_Y, mTranslationY);
            outState.putFloat(STATE_SCALE, mScaleFactor);
            outState.putFloat(STATE_ROTATION, mRotationDegrees);
            outState.putFloat("state_scale_x_sign", Math.signum(overlayContainer.getScaleX()));
        }
    }

    // 核心：恢复状态 (不变)
    private void restoreState(@NonNull Bundle savedInstanceState) {
        String uriString = savedInstanceState.getString(STATE_IMAGE_URI);
        if (uriString != null) {
            currentImageUri = Uri.parse(uriString);

            mTranslationX = savedInstanceState.getFloat(STATE_TRANSLATION_X);
            mTranslationY = savedInstanceState.getFloat(STATE_TRANSLATION_Y);
            mScaleFactor = savedInstanceState.getFloat(STATE_SCALE);
            mRotationDegrees = savedInstanceState.getFloat(STATE_ROTATION);
            float scaleXSign = savedInstanceState.getFloat("state_scale_x_sign", 1.0f);

            overlayImageView.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(currentImageUri)
                    .fitCenter()
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) { return false; }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            overlayImageView.setAlpha(savedInstanceState.getFloat(STATE_ALPHA));
                            applyOverlayTransform();
                            overlayContainer.setScaleX(mScaleFactor * scaleXSign);
                            transparencySlider.setProgress((int)(overlayImageView.getAlpha() * 100));

                            // 默认不进入编辑模式
                            setEditMode(false);
                            return false;
                        }
                    })
                    .into(overlayImageView);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (orientationEventListener != null && orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (orientationEventListener != null) {
            orientationEventListener.disable();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (orientationEventListener != null) {
            orientationEventListener.disable();
        }
    }
}


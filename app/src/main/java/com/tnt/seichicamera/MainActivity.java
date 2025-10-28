package com.tnt.seichicamera;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
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

    // 动态确定所需的权限列表
    private static final String[] REQUIRED_PERMISSIONS;

    static {
        // Android 13 (API 33) 及以上需要更加细分的媒体权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            // Android 12 及以下使用旧的存储权限
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }
    }

    // UI 组件
    private PreviewView cameraPreview;
    private ImageView overlayImageView;
    private SeekBar transparencySlider;
    private Button loadImageButton;
    private ImageButton captureButton;

    // CameraX 核心组件
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    // Activity Result Launchers (用于权限请求和图片选择)
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;
    // 备用图片选择器 (兼容旧版)
    private ActivityResultLauncher<String> pickContentLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 初始化视图
        initViews();

        // 2. 初始化后台相机线程
        cameraExecutor = Executors.newSingleThreadExecutor();

        // 3. 注册 Activity Result 回调
        registerActivityResults();

        // 4. 检查并请求权限，成功则启动相机
        checkPermissionsAndStartCamera();

        // 5. 设置按钮监听器
        setupListeners();
    }

    private void initViews() {
        cameraPreview = findViewById(R.id.camera_preview);
        overlayImageView = findViewById(R.id.overlay_image_view);
        transparencySlider = findViewById(R.id.transparency_slider);
        loadImageButton = findViewById(R.id.load_image_button);
        captureButton = findViewById(R.id.capture_button);
    }

    private void registerActivityResults() {
        // 注册权限请求回调
        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    // 检查是否所有请求的权限都被授予
                    boolean allGranted = true;
                    for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                        if (!entry.getValue()) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted) {
                        startCamera();
                    } else {
                        Toast.makeText(this, "需要相机和存储权限才能正常工作", Toast.LENGTH_LONG).show();
                        finish(); // 权限被拒绝，退出应用
                    }
                });

        // 注册新版图片选择器 (Android 13+ 推荐)
        pickMediaLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        loadOverlayImage(uri);
                    } else {
                        Log.d(TAG, "未选择图片");
                    }
                });

        // 注册旧版内容选择器 (作为兼容备份)
        pickContentLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        loadOverlayImage(uri);
                    }
                });
    }

    private void checkPermissionsAndStartCamera() {
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissionsLauncher.launch(REQUIRED_PERMISSIONS);
        }
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

    private void setupListeners() {
        // 拍摄按钮
        captureButton.setOnClickListener(v -> takePhoto());

        // 加载图片按钮
        loadImageButton.setOnClickListener(v -> {
            // 优先使用新版照片选择器
            if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(this)) {
                pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            } else {
                // 旧设备使用通用内容选择器
                pickContentLauncher.launch("image/*");
            }
        });

        // 透明度滑块
        transparencySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 将 0-100 的进度转换为 0.0f - 1.0f 的 alpha 值
                float alpha = progress / 100f;
                overlayImageView.setAlpha(alpha);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    // 使用 Glide 加载选中的图片到 ImageView
    private void loadOverlayImage(android.net.Uri uri) {
        Log.d(TAG, "加载叠加图: " + uri.toString());
        overlayImageView.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(uri)
                .fitCenter() // 确保完整显示
                .into(overlayImageView);

        // 重置透明度到 50%
        transparencySlider.setProgress(50);
        overlayImageView.setAlpha(0.5f);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 预览用例
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                // 拍照用例
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // 选择后置摄像头
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // 解绑可能存在的旧用例，并重新绑定
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "相机启动失败", e);
                Toast.makeText(this, "相机启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        // 创建文件名
        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.CHINA)
                .format(System.currentTimeMillis());

        // 配置存入 MediaStore 的元数据
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        // Android Q (10) 以上可以指定相对路径到 Pictures/SeichiCamera
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SeichiCamera");
        }

        // 创建输出选项
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();

        // 播放快门动画提示（可选，简单的视觉反馈）
        cameraPreview.postDelayed(() -> {
            cameraPreview.setForeground(new android.graphics.drawable.ColorDrawable(0xCCFFFFFF));
            cameraPreview.postDelayed(() -> cameraPreview.setForeground(null), 50);
        }, 100);


        // 执行拍照
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        String msg = "巡礼照片已保存";
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, msg + ": " + outputFileResults.getSavedUri());
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "拍照失败: " + exception.getMessage(), exception);
                        Toast.makeText(MainActivity.this, "拍照失败", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
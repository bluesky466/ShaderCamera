package me.islinjw.shadercamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.media.MediaRecorder;
import android.net.Uri;
import android.opengl.EGLSurface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import me.islinjw.shadercamera.gl.GLCore;
import me.islinjw.shadercamera.gl.SurfaceRender;
import me.islinjw.shadercamera.gl.shader.DecolorShader;
import me.islinjw.shadercamera.gl.shader.IShader;
import me.islinjw.shadercamera.gl.shader.NormalShader;
import me.islinjw.shadercamera.gl.shader.NostalgiaShader;
import me.islinjw.shadercamera.gl.shader.ReversalShader;

public class MainActivity extends AppCompatActivity implements
        TextureView.SurfaceTextureListener,
        CameraCapturer.CaptureListener, ShaderAdaptor.OnSelectShaderListener {

    private static final int VIDEO_BIT_RATE = 1024 * 1024 * 1024;
    private static final int VIDEO_FRAME_RATE = 30;
    private static final int AUDIO_BIT_RATE = 44800;

    @Bind(R.id.preview)
    TextureView mPreview;

    @Bind(R.id.record)
    CheckBox mRecord;

    @Bind(R.id.play_video)
    Button mPlayVideo;

    @Bind(R.id.shader_selector)
    RecyclerView mShaderSelector;

    @Bind(R.id.select_shader)
    Button mSelectShader;

    private boolean mOpenCamera = false;
    private CameraCapturer mCameraCapturer;
    private SurfaceTexture mCameraTexutre;
    private int mCameraFacing = CameraCharacteristics.LENS_FACING_BACK;

    private GLCore mGLCore = new GLCore();
    private SurfaceRender mSurfaceRender;
    private float[] mTransformMatrix = new float[16];

    private HandlerThread mRenderThread = new HandlerThread("render");
    private Handler mRenderHandler;

    private MediaRecorder mMediaRecorder;
    private EGLSurface mRecordSurface;

    private RxPermissions mRxPermissions = new RxPermissions(this);

    private File mLastVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        mPreview.setSurfaceTextureListener(this);

        List<ShaderAdaptor.ShaderInfo> shaders = new ArrayList<ShaderAdaptor.ShaderInfo>() {{
            add(new ShaderAdaptor.ShaderInfo(R.string.shader_normal, NormalShader.class));
            add(new ShaderAdaptor.ShaderInfo(R.string.shader_decolor, DecolorShader.class));
            add(new ShaderAdaptor.ShaderInfo(R.string.shader_reversal, ReversalShader.class));
            add(new ShaderAdaptor.ShaderInfo(R.string.shader_nostalgia, NostalgiaShader.class));
        }};
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        ShaderAdaptor adaptor = new ShaderAdaptor(mShaderSelector, shaders);
        adaptor.setOnSelectShaderListener(this);
        mShaderSelector.setLayoutManager(layoutManager);
        mShaderSelector.setAdapter(adaptor);

        mRenderThread.start();
        mRenderHandler = new Handler(mRenderThread.getLooper());
    }

    @OnCheckedChanged(R.id.record)
    public void record(boolean start) {
        if (start) {
            requestPermissionAndStartRecord();
        } else {
            if (mMediaRecorder != null) {
                stopRecord();
            }
        }
    }

    @OnClick(R.id.select_shader)
    public void selectShader() {
        mShaderSelector.setVisibility(View.VISIBLE);
        mSelectShader.setVisibility(View.GONE);
    }

    private String genFileName() {
        return "video_" + System.currentTimeMillis() + ".mp4";
    }

    public void requestPermissionAndStartRecord() {
        mRxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
                .observeOn(AndroidSchedulers.from(mRenderThread.getLooper()))
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean granted) {
                        if (granted) {
                            mRecord.setChecked(true);
                            mRecordSurface = startRecord(genFileName());
                        } else {
                            mRecord.setChecked(false);
                        }
                    }
                });
    }

    @OnClick(R.id.switch_camera)
    public void switchCamera() {
        mCameraFacing = mCameraFacing == CameraCharacteristics.LENS_FACING_BACK
                ? CameraCharacteristics.LENS_FACING_FRONT : CameraCharacteristics.LENS_FACING_BACK;

        closeCamera();
        openCamera();
    }

    @OnClick(R.id.play_video)
    public void playVideo() {
        Uri uri = FileProvider.getUriForFile(
                this,
                BuildConfig.APPLICATION_ID + ".fileprovider",
                mLastVideo);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(uri, "video/*");
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        makeFullscreen();

        if (mCameraCapturer != null && !mOpenCamera) {
            openCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mMediaRecorder != null) {
            stopRecord();
        }

        closeCamera();
        mRecord.setChecked(false);
    }

    private void makeFullscreen() {
        getWindow().getDecorView()
                .setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
    }

    SurfaceTexture mSurface;

    @SuppressLint("CheckResult")
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mSurface = surface;
        Observable.just(new Surface(surface))
                .subscribeOn(AndroidSchedulers.from(mRenderHandler.getLooper()))
                .map(new Function<Surface, String>() {
                    @Override
                    public String apply(Surface surface) {
                        mGLCore.init();
                        mSurfaceRender = new SurfaceRender(
                                mGLCore,
                                surface,
                                mPreview.getWidth(),
                                mPreview.getHeight());
                        mSurfaceRender.setShader(MainActivity.this, new NormalShader());
                        mCameraTexutre = new SurfaceTexture(mGLCore.getTexture());
                        return Manifest.permission.CAMERA;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(new Function<String, ObservableSource<Boolean>>() {
                    @Override
                    public ObservableSource<Boolean> apply(String permission) {
                        return mRxPermissions.request(permission);
                    }
                })
                .observeOn(AndroidSchedulers.from(mRenderHandler.getLooper()))
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean granted) {
                        if (granted) {
                            mCameraCapturer = new CameraCapturer(MainActivity.this);
                            openCamera();

                        }
                    }
                });
    }

    private void openCamera() {
        mOpenCamera = true;

        mCameraCapturer.openCamera(
                mCameraTexutre,
                mCameraFacing,
                mPreview.getWidth(),
                mPreview.getHeight(),
                mRenderHandler,
                this);
    }

    private void closeCamera() {
        mOpenCamera = false;
        mCameraCapturer.closeCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onCaptureCompleted() {
        mCameraTexutre.updateTexImage();
        mCameraTexutre.getTransformMatrix(mTransformMatrix);
        mSurfaceRender.render(mTransformMatrix);
        if (mRecordSurface != null) {
            mSurfaceRender.render(mTransformMatrix, mRecordSurface);
            mGLCore.setPresentationTime(mRecordSurface, mSurface.getTimestamp());
        }
    }

    private EGLSurface startRecord(String filename) {
        mLastVideo = new File(Environment.getExternalStorageDirectory(), filename);

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mLastVideo.getPath());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setVideoEncodingBitRate(VIDEO_BIT_RATE);
        mMediaRecorder.setVideoSize(mPreview.getWidth(), mPreview.getHeight());
        mMediaRecorder.setVideoFrameRate(VIDEO_FRAME_RATE);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setAudioEncodingBitRate(AUDIO_BIT_RATE);
        mMediaRecorder.setOrientationHint(0);

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Toast.makeText(this, "MediaRecorder failed on prepare()", Toast.LENGTH_LONG).show();
        }

        mMediaRecorder.start();
        return mGLCore.createEGLSurface(mMediaRecorder.getSurface());
    }

    private void stopRecord() {
        mPlayVideo.setVisibility(View.VISIBLE);

        mMediaRecorder.stop();
        mMediaRecorder.release();
        mMediaRecorder = null;

        mGLCore.destroyEGLSurface(mRecordSurface);
        mRecordSurface = null;
    }

    @Override
    public void onSelectShader(final IShader shader) {
        mRenderHandler.post(new Runnable() {
            @Override
            public void run() {
                mSurfaceRender.setShader(MainActivity.this, shader);
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mShaderSelector.getVisibility() == View.VISIBLE
                && event.getAction() == MotionEvent.ACTION_UP) {
            mShaderSelector.setVisibility(View.GONE);
            mSelectShader.setVisibility(View.VISIBLE);
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (mShaderSelector.getVisibility() == View.VISIBLE) {
            mShaderSelector.setVisibility(View.GONE);
            mSelectShader.setVisibility(View.VISIBLE);
        } else {
            super.onBackPressed();
        }
    }
}

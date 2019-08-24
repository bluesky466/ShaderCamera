package me.islinjw.shadercamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.media.MediaRecorder;
import android.opengl.EGLSurface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.IOException;

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
import me.islinjw.shadercamera.gl.DecolorShader;
import me.islinjw.shadercamera.gl.SurfaceRender;

public class MainActivity extends AppCompatActivity implements
        TextureView.SurfaceTextureListener,
        CameraCapturer.CaptureListener {

    private static final int VIDEO_BIT_RATE = 1024 * 1024 * 1024;
    private static final int VIDEO_FRAME_RATE = 30;
    private static final int AUDIO_BIT_RATE = 44800;

    @Bind(R.id.preview)
    TextureView mPreview;

    @Bind(R.id.record)
    CheckBox mRecord;

    @Bind(R.id.switch_camera)
    Button mSwitchCamera;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        mPreview.setSurfaceTextureListener(this);

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
                        mSurfaceRender.setShader(MainActivity.this, new DecolorShader());
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
        File outputFile = new File(Environment.getExternalStorageDirectory(), filename);

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(outputFile.getPath());
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
        mMediaRecorder.stop();
        mMediaRecorder.release();
        mMediaRecorder = null;

        mGLCore.destroyEGLSurface(mRecordSurface);
        mRecordSurface = null;
    }
}

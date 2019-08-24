package me.islinjw.shadercamera;

import android.Manifest;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import com.tbruyelle.rxpermissions2.RxPermissions;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    @Bind(R.id.preview)
    TextureView mPreview;

    @Bind(R.id.record)
    CheckBox mRecord;

    @Bind(R.id.switch_camera)
    Button mSwitchCamera;

    private CameraCapturer mCameraCapturer;

    private SurfaceTexture mSurfaceTexture;

    private boolean mOpenCamera = false;
    private int mCameraFacing = CameraCharacteristics.LENS_FACING_BACK;

    private RxPermissions mRxPermissions = new RxPermissions(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        mPreview.setSurfaceTextureListener(this);
    }

    @OnCheckedChanged(R.id.record)
    public void record(boolean start) {
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
        closeCamera();
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

    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height) {
        mSurfaceTexture = surface;
        mRxPermissions
                .request(Manifest.permission.CAMERA)
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
                mSurfaceTexture,
                mCameraFacing,
                getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels,
                null);
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
}

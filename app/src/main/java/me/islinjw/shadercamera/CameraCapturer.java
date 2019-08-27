package me.islinjw.shadercamera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.util.Arrays;
import java.util.List;

public class CameraCapturer {
    private static final String TAG = "CameraCapturer";

    private Context mContext;
    private Handler mHandler;

    private CameraDevice mCameraDevice;
    private Surface mPreviewSurface;

    private CaptureListener mListener;

    private CameraDevice.StateCallback mOpenCameraCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    openCameraSession(camera);
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                }
            };

    private CameraCaptureSession.StateCallback mCreateSessionCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    requestPreview(session);
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            };

    private CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(
                        CameraCaptureSession session,
                        CaptureRequest request,
                        TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);

                    if (mListener != null) {
                        mListener.onCaptureCompleted();
                    }
                }
            };

    public CameraCapturer(Context context) {
        mContext = context.getApplicationContext();
    }

    @SuppressLint("MissingPermission")
    public void openCamera(
            SurfaceTexture preview,
            int facing,
            int width,
            int height,
            Handler handler,
            CaptureListener listener) {
        mListener = listener;
        mPreviewSurface = new Surface(preview);
        mHandler = handler;

        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

        try {
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics cc = manager.getCameraCharacteristics(id);
                if (cc.get(CameraCharacteristics.LENS_FACING) == facing) {
                    Size[] sizes = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                            .getOutputSizes(SurfaceTexture.class);
                    Size size = getMostSuitableSize(sizes, width, height);
                    preview.setDefaultBufferSize(size.getWidth(), size.getHeight());
                    manager.openCamera(id, mOpenCameraCallback, handler);
                    break;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "can not open camera", e);
        }
    }

    public void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private Size getMostSuitableSize(
            Size[] sizes,
            float width,
            float height) {

        float targetRatio = height / width;
        Size result = null;
        for (Size size : sizes) {
            if (result == null || isMoreSuitable(result, size, targetRatio)) {
                result = size;
            }
        }
        return result;
    }

    private boolean isMoreSuitable(Size current, Size target, float targetRatio) {
        if (current == null) {
            return true;
        }
        float dRatioTarget = Math.abs(targetRatio - getRatio(target));
        float dRatioCurrent = Math.abs(targetRatio - getRatio(current));
        return dRatioTarget < dRatioCurrent
                || (dRatioTarget == dRatioCurrent && getArea(target) > getArea(current));
    }

    private int getArea(Size size) {
        return size.getWidth() * size.getHeight();
    }

    private float getRatio(Size size) {
        return ((float) size.getWidth()) / size.getHeight();
    }

    private void openCameraSession(CameraDevice camera) {
        mCameraDevice = camera;
        try {
            List<Surface> outputs = Arrays.asList(mPreviewSurface);
            camera.createCaptureSession(outputs, mCreateSessionCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "createCaptureSession failed", e);
        }
    }

    private void requestPreview(CameraCaptureSession session) {
        if (mCameraDevice == null) {
            return;
        }
        try {
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(mPreviewSurface);
            session.setRepeatingRequest(builder.build(), mCaptureCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "requestPreview failed", e);
        }
    }

    public interface CaptureListener {
        void onCaptureCompleted();
    }
}

package com.hyperai.example.lpr3_demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.hyperai.hyperlpr3.HyperLPR3;
import com.hyperai.hyperlpr3.bean.Plate;
import com.hyperai.example.lpr3_demo.camera.CameraViewModel;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.List;

public class CameraPreviews extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "CameraPreview";
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private Paint mPaint;
    private float oldDist = 1f;
    private boolean isStopReg;

    private Context mContext;
    private CameraViewModel viewModel;

    public CameraPreviews(Context context, CameraViewModel viewModel) {
        super(context);
        mContext = context;
        mHolder = getHolder();
        mHolder.addCallback(this);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(2);
        mPaint.setStyle(Paint.Style.STROKE);
        this.viewModel = viewModel;
    }

    public Camera getCameraInstance() {
        if (mCamera == null) {
            try {
                CameraHandlerThread mThread = new CameraHandlerThread("camera thread");
                synchronized (mThread) {
                    mThread.openCamera();
                }
            } catch (Exception e) {
                Log.e(TAG, "camera is not available: " + e.getMessage());
            }
        }
        return mCamera;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera = getCameraInstance();
            mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            setPreviewFocus(mCamera);
        } catch (Exception e) {
            Log.e(TAG, "surfaceCreated error: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {
            int rotation = getDisplayOrientation();
            mCamera.setDisplayOrientation(rotation);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setRotation(rotation);
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            Log.e(TAG, "surfaceChanged error: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            mHolder.removeCallback(this);
            if (mCamera != null) {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "surfaceDestroyed error: " + e.getMessage());
        }
    }

    public int getDisplayOrientation() {
        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        int result = (info.orientation - degrees + 360) % 360;
        return result;
    }

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        try {
            Camera.Size previewSize = camera.getParameters().getPreviewSize();
            Plate[] plates = HyperLPR3.getInstance().plateRecognition(data, previewSize.height, previewSize.width, HyperLPR3.CAMERA_ROTATION_270, HyperLPR3.STREAM_YUV_NV21);

            // 获取一帧Bitmap用于保存
            if (viewModel != null && data != null) {
                Bitmap bitmap = com.hyperai.example.lpr3_demo.utils.BitmapUtils.nv21ToBitmap(data, previewSize.width, previewSize.height, getDisplayOrientation());
                viewModel.setLastFrameBitmap(bitmap);
            }

            for (Plate plate : plates) {
                Log.i(TAG, "" + plate.toString());
            }

            if (!isStopReg && plates.length > 0) {
                sendPlate(plates);
            }
        } catch (Exception e) {
            Log.e(TAG, "onPreviewFrame error: " + e.getMessage());
        }
    }

    private void sendPlate(Plate[] plates) {
        EventBus.getDefault().post(plates);
    }

    private void openCameraOriginal() {
        try {
            mCamera = Camera.open();
        } catch (Exception e) {
            Log.e(TAG, "camera is not available: " + e.getMessage());
        }
    }

    private class CameraHandlerThread extends HandlerThread {
        Handler handler;
        public CameraHandlerThread(String name) {
            super(name);
            start();
            handler = new Handler(getLooper());
        }

        synchronized void notifyCameraOpened() {
            notify();
        }

        void openCamera() {
            handler.post(() -> {
                openCameraOriginal();
                notifyCameraOpened();
            });
            try {
                wait();
            } catch (Exception e) {
                Log.e(TAG, "wait was interrupted: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 2) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = getFingerSpacing(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float newDist = getFingerSpacing(event);
                    if (newDist > oldDist) {
                        handleZoom(true, mCamera);
                    } else if (newDist < oldDist) {
                        handleZoom(false, mCamera);
                    }
                    oldDist = newDist;
                    break;
            }
        }
        return true;
    }

    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void handleZoom(boolean isZoomIn, Camera camera) {
        try {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters.isZoomSupported()) {
                int maxZoom = parameters.getMaxZoom();
                int zoom = parameters.getZoom();
                if (isZoomIn && zoom < maxZoom) {
                    zoom++;
                } else if (zoom > 0) {
                    zoom--;
                }
                parameters.setZoom(zoom);
                camera.setParameters(parameters);
            } else {
                Log.e(TAG, "handleZoom: not supported");
            }
        } catch (Exception e) {
            Log.e(TAG, "handleZoom error: " + e.getMessage());
        }
    }

    private void setPreviewFocus(Camera camera) {
        try {
            Camera.Parameters parameters = camera.getParameters();
            List<String> focusList = parameters.getSupportedFocusModes();
            if (focusList.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            camera.setParameters(parameters);
        } catch (Exception e) {
            Log.e(TAG, "setPreviewFocus error: " + e.getMessage());
        }
    }
}
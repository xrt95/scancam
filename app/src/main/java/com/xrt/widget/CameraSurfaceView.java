package com.xrt.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.xrt.camera.CameraProxy;

public class CameraSurfaceView extends SurfaceView {
    private static String TAG = "mCameraProxy";
    private CameraProxy cameraProxy;
    private Surface tempSurface;
    private SurfaceHolder.Callback holderCallback = new SurfaceHolder.Callback(){
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder){
            Log.d(TAG, "surface was created");
            Surface surface = surfaceHolder.getSurface();
            tempSurface = surface;
            cameraProxy.addPotentialSurface(surface);
            cameraProxy.addPreviewSurface(surface);
            cameraProxy.openCamera();

        }
        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height){
            //Log.d(TAG, "surface was changed");
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder){
            //Log.d(TAG, "surface was destroyed");
            if (tempSurface != null){
                cameraProxy.removePotentialSurface(tempSurface);
                cameraProxy.removePreviewSurface(tempSurface);
            }

        }
    };
    public CameraSurfaceView(Context context, AttributeSet attr){
        super(context, attr);
        this.getHolder().addCallback(holderCallback);
    }
    public void addCameraProxy(CameraProxy cameraProxy){
        this.cameraProxy = cameraProxy;
    }
}

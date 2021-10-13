package com.xrt.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.xrt.camera.CameraProxy;

public class CameraTextureView extends TextureView implements View.OnTouchListener{
    private CameraProxy cameraProxy;
    private TextureView.SurfaceTextureListener textureViewListener = new TextureView.SurfaceTextureListener(){
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height){
            //Log.d(TAG, "SurfaceTexture was created");
            if (cameraProxy != null){
                Surface surface = new Surface(surfaceTexture);
                cameraProxy.addPotentialSurface(surface);
                cameraProxy.addPreviewSurface(surface);
                if (!cameraProxy.isConnected()){
                    cameraProxy.openCamera();
                }
            }
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height){}
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture){return false;}
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture){}
    };
    public CameraTextureView(Context context, AttributeSet attr){
        super(context, attr);
        setSurfaceTextureListener(textureViewListener);
        setOnTouchListener(this);
    }
    public void addCameraProxy(CameraProxy cameraProxy){
        this.cameraProxy = cameraProxy;
    }
    @Override
    public boolean onTouch(View view, MotionEvent event){
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                cameraProxy.focus((int)event.getX(), (int)event.getY(), getWidth(), getHeight());
                break;
        }
        return false;
    }

}

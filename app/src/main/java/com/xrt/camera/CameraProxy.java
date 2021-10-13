package com.xrt.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import com.xrt.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;

public class CameraProxy {
    private static final String TAG = "mCameraProxy";
    private Context mContext;
    private String cameraId;
    private boolean isConnected = false;
    private CameraManager cameraManager;
    private CameraCharacteristics cameraCharacteristics;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CameraCaptureSession.CaptureCallback mPreviewCallback;
    private CaptureRequest.Builder previewRequestBuilder;
    private CaptureRequest.Builder captureRequestBuilder;
    private List<Surface> potentialSurfaceList = new ArrayList<>();
    private List<Surface> previewSurfaceList = new ArrayList<>();
    private List<?> previewKeyList = new ArrayList<>();
    private List previewValueList = new ArrayList<>();
    private List<Surface> captureSurfaceList = new ArrayList<>();
    private List<?> captureKeyList = new ArrayList<>();
    private List captureValueList = new ArrayList<>();
    private boolean isFlashOn = false;
    private static final int ORIENTATION_0 = 0;
    private static final int ORIENTATION_90 = 90;
    private static final int ORIENTATION_180 = 180;
    private static final int ORIENTATION_270 = 270;
    private CameraCaptureSession.CaptureCallback focusDoneListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            //Log.d("mxrt", "completed");
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            Log.d("mxrt", "progressed");
        }

        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
            //Log.d("mxrt", "started");
        }
        @Override
        public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
            Log.d("mxrt", "se completed");
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.d("mxrt", "capture failed");
        }

        @Override
        public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
            super.onCaptureSequenceAborted(session, sequenceId);
            Log.d("mxrt", "se abort");
        }
    };
    private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback(){
        @Override
        public void onOpened(CameraDevice cameraDevice){
            CameraProxy.this.cameraDevice = cameraDevice;
            initPreviewRequestBuilder();
            initCaptureRequestBuilder();
            createCaptureSession();
            isConnected = true;
            Log.d(TAG, ": cameraStateCallback: camera Opened");
        }
        @Override
        public void onDisconnected(CameraDevice cameraDevice){
            cameraDevice.close();
            CameraProxy.this.cameraDevice = null;
            isConnected = false;
            Log.d(TAG, ": cameraStateCallback: camera Disconnected");
        }
        @Override
        public void onError(CameraDevice cameraDevice, int errorCode){
            cameraDevice.close();
            CameraProxy.this.cameraDevice = null;
            isConnected = false;
            switch (errorCode){
                case CameraDevice.StateCallback.ERROR_CAMERA_IN_USE:
                    Log.d(TAG, "cameraStateCallback: error:" + "camera in use");
                    break;
                case CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE:
                    Log.d(TAG, "cameraStateCallback: error:" + "max camera in use");
                    break;
                case CameraDevice.StateCallback.ERROR_CAMERA_DISABLED:
                    Log.d(TAG, "cameraStateCallback: error:" + "camera disabled");
                    break;
                case CameraDevice.StateCallback.ERROR_CAMERA_DEVICE:
                    Log.d(TAG, "cameraStateCallback: error:" + "camera device");
                    break;
                case CameraDevice.StateCallback.ERROR_CAMERA_SERVICE:
                    Log.d(TAG, "cameraStateCallback: error:" + "camera service");
                    break;
            }
        }
    };

    public CameraProxy(Context context, String cameraType){
        mContext = context;
        this.cameraManager = getCameraManager(context);
        setCameraId(cameraType);
        initCameraCharacteristics();
    }
    private CameraManager getCameraManager(Context context){
        Log.d(TAG, "CameraProxy: getCameraManager executed");
        return (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
    }
    private void setCameraId(String cameraType){
        switch(cameraType){
            case "front":
                cameraId = String.valueOf(CameraCharacteristics.LENS_FACING_BACK);
                break;
            case "back":
                cameraId = String.valueOf(CameraCharacteristics.LENS_FACING_FRONT);
                break;
            default:
                cameraId = String.valueOf(CameraCharacteristics.LENS_FACING_FRONT);
                break;
        }
    }
    private void initCameraCharacteristics(){
        try{
            cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
            Range<Integer> maxFpsRange = getMaxFpsRange();
            setPreviewArgs(Arrays.asList(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE), Arrays.asList(maxFpsRange));
        }
        catch(Exception e){
            Log.d(TAG, "initCameraCharacteritics: " +e.getMessage());
        }
    }
    public void openFlash(){
        if (!isFlashOn){
            previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            preview();
            isFlashOn = true;
        }else{
            previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            preview();
            isFlashOn = false;
        }
    }
    public void addPotentialSurface(Surface surface){
        this.potentialSurfaceList.add(surface);
    }
    public void addPreviewSurface(Surface surface){
        this.previewSurfaceList.add(surface);
    }
    public void addCaptureSurface(Surface surface){
        this.captureSurfaceList.add(surface);
    }
    public void removePotentialSurface(Surface surface){
        potentialSurfaceList.remove(surface);
    }
    public void removePreviewSurface(Surface surface){
        previewSurfaceList.remove(surface);
    }
    public void setPreviewArgs(List<?> previewKeyList, List previewValueList){
        this.previewKeyList = previewKeyList;
        this.previewValueList = previewValueList;
    }
    public void setCaptureArgs(List<?> captureKeyList, List captureValueList){
        this.captureKeyList = captureKeyList;
        this.captureValueList = captureValueList;
    }
    public void openCamera(){
        try{
            if (checkPreviewSurfaceList()){
                cameraManager.openCamera(cameraId, cameraStateCallback, null);
            }
        }
        catch(Exception e){Log.d(TAG, "openCamera: " + e.getMessage());}
    }
    /*
     * 检测是否已经配置好预览用的Surface。
     * @return 返回true表示预览用的Surface已经准备好。false反之。
     */
    private boolean checkPreviewSurfaceList(){
        for (int i = 0; i < previewSurfaceList.size(); i++){
            for (int j = 0; j < potentialSurfaceList.size(); j++){
                Surface previewSurface = previewSurfaceList.get(i);
                Surface potentialSurface = potentialSurfaceList.get(j);
                if (previewSurface.hashCode() == potentialSurface.hashCode()){
                    return true;
                }
            }
        }
        return false;
    }
    /*
     * 获取相机的最高帧数范围。
     */
    public Range<Integer> getMaxFpsRange(){
        Range<Integer>[] fpsRange = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        //Log.d(TAG, "FpsRange:" + Arrays.toString(fpsRange));
        int maxRangeIndex = 0;
        int maxLowerLimit = 0;
        for (int i = 0; i < fpsRange.length; i++){
            int lower = fpsRange[i].getLower();
            if (lower > maxLowerLimit){
                maxLowerLimit = lower;
                maxRangeIndex = i;
            }
        }
        return fpsRange[maxRangeIndex];
    }
    /*
     * 获取相机的最高输出像素。
     */
    public Size getMaxOutputSize(){
        //camera Width:4032 height:3024
        StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        List<Float> rateList = new ArrayList<>();
        for (Size size : map.getOutputSizes(ImageFormat.JPEG)){
            rateList.add(size.getWidth() / (float)size.getHeight());
        }
        //Log.d("mxrt", Arrays.toString(map.getOutputSizes(ImageFormat.JPEG)));
        //Log.d("mxrt", Arrays.toString(rateList.toArray()));
        return Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
            @Override
            public int compare(Size size1, Size size2) {
                return Integer.signum(size1.getWidth() * size1.getHeight() - size2.getWidth() * size2.getHeight());
            }
        });
    }
    private static class CompareSize implements Comparator<Size> {
        @Override
        public int compare(Size size1, Size size2){
            return Integer.signum(size1.getWidth() * size1.getHeight() - size2.getWidth() * size2.getHeight());
        }
    }
    /*
     * 创建与相机的会话。会话创建成功后即可开始相机的预览。
     */
    private void createCaptureSession(){
        try{
            cameraDevice.createCaptureSession(potentialSurfaceList, new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession){
                    try {
                        CameraProxy.this.cameraCaptureSession = cameraCaptureSession;
                        preview();
                    }
                    catch(Exception e){Log.d(TAG, e.getMessage());}
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession){
                    Log.d(TAG, "createCameraCaptureSession: cameraCaptureSession configured failed");
                }
            }, null);
        }
        catch(Exception e){Log.d(TAG, "createCaptureSession: " + e.getMessage());}
    }
    public void focus(int x, int y, int previewWidth, int previewHeight){
        Rect area = getFocusArea(x, y, previewWidth, previewHeight);
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{new MeteringRectangle(area, 100)});
        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{new MeteringRectangle(area, 100)});
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        focusExcute();
    }
    private Rect getFocusArea(int x, int y, int previewWidth, int previewHeight){
        Size size = getMaxOutputSize();//最大输出分辨率
        int picWidth = size.getHeight();//因为相机输出的照片是横的
        int picHeight = size.getWidth();//因为相机输出的照片是横的
        //Rect region = previewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);//相机裁剪区域
        Rect region = new Rect(0, 0, picHeight, picWidth);
        //Log.d("mxrt", "size width:" + size.getWidth());
        //Log.d("mxrt", "size height:" + size.getHeight());
        //Log.d("mxrt", "region width:" + region.width());
        //Log.d("mxrt", "region height:" + region.height());
        int dy = Math.abs(picHeight - previewHeight) / 2;//裁剪区域相对于照片输出区域的偏移
        int dx = Math.abs(picWidth - previewWidth) / 2;
        int focusX = picWidth - picWidth / previewWidth * x;//显示区域相比于照片输出区域的缩放
        int focusY = picHeight / previewHeight * y;//显示区域相比于照片输出区域的缩放
        //Log.d("mxrt" , "focusX:" + focusX + " focusY:" + focusY);
        int focusAreaWidth = 100;
        int focusAreaHeight = 100;
        //int focusAreaWidth = mContext.getResources().getDimensionPixelSize(R.dimen.hintpoint_pointdownhintview_width);
        //int focusAreaHeight = mContext.getResources().getDimensionPixelSize(R.dimen.hintpoint_pointdownhintview_width);
        return new Rect(focusY - focusAreaHeight / 2, focusX - focusAreaHeight / 2, focusY + focusAreaHeight / 2, focusX + focusAreaWidth / 2);
    }
    private void focusExcute(){
        CaptureRequest previewRequest = getPreviewRequest();

        try{
            //cameraCaptureSession.capture(previewRequest, focusDoneListener, null);
            cameraCaptureSession.setRepeatingRequest(previewRequest, focusDoneListener, null);
            //previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    /*
     * 启动相机预览
     */
    private void preview(){
        CaptureRequest previewRequest = getPreviewRequest();
        try{
            cameraCaptureSession.setRepeatingRequest(previewRequest, focusDoneListener, null);
        }
        catch(Exception e){
            Log.d(TAG, "preview: " + e.getMessage());
        }
    }

    /*
     * 相机拍照。先停止相机预览，完成拍照后再恢复相机预览。
     */
    public void capture(){
        //captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0);
        CaptureRequest captureRequest = getCaptureRequest();
        CaptureRequest previewRequest = getPreviewRequest();
        try{
            cameraCaptureSession.stopRepeating();
            cameraCaptureSession.capture(captureRequest, null, null);
            cameraCaptureSession.setRepeatingRequest(previewRequest, null, null);
        }
        catch(Exception e){
            Log.d(TAG, "capture: " + e.getMessage());
        }
    }
    public void setPreviewCallbackListener(CameraCaptureSession.CaptureCallback captureCallback){
        mPreviewCallback = captureCallback;
    }
    /*
     * Surface和参数设置后就完成CaptureRequest.Builder对象的创建。
     */
    private CaptureRequest getPreviewRequest(){
        setUpPreviewSurface();
        return previewRequestBuilder.build();
    }
    /*
     * Surface和参数设置后就完成CaptureRequest.Builder对象的创建。
     */
    private CaptureRequest getCaptureRequest(){
        setUpCaptureSurface();
        return captureRequestBuilder.build();
    }
    /*
     * 预览时传入Surface。调用时保证已获得CaptureRequest.Builder对象。
     */
    private void setUpPreviewSurface(){
        addSurfaceToBuilder(previewRequestBuilder, previewSurfaceList);
    }
    /*
     * 拍照时传入Surface。调用时保证已获得CaptureRequest.Builder对象。
     */
    private void setUpCaptureSurface(){
        addSurfaceToBuilder(captureRequestBuilder, captureSurfaceList);
    }
    /**
     * @param builder 预览或者是拍照时的CaptureRequest.Builder
     * @param surfaceList Surface列表
     * <p>批量设置预览或者拍照时的Surface。设计此函数主要原因是 CaptureRequest.Builder只有在相机启动后才能通过回调获得，
     * 如果单独在任意时刻在外部传入Surface的话容易引发空指针错误。所以解决办法是先将Surface通过列表存储起来，在获得
     * CaptureRequest.Builder后再批量进行参数的设置。</p>
     */
    private void addSurfaceToBuilder(CaptureRequest.Builder builder, List<Surface> surfaceList){
        try{
            for (int index = 0; index < surfaceList.size(); index++){
                builder.addTarget(surfaceList.get(index));
            }
        }
        catch (Exception e){
            Log.d(TAG, "addSurfaceToBuilder: " + e.getMessage());
        }
    }
    /*
     * 创建预览时的CaptureRequest.Builder对象。要获取了CameraDevice对象后才能创建。CameraDevice对象的获得需要在相机打开成功通过回调才能获得。
     */
    private void initPreviewRequestBuilder(){
        try{
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        }
        catch(Exception e){
            Log.d(TAG, "initPreviewRequestBuilder: " + e.getMessage());
        }
    }
    /*
     * 创建拍照时的CaptureRequest.Builder对象。要获取了CameraDevice对象后才能创建。CameraDevice对象的获得需要在相机打开成功通过回调才能获得。
     */
    private void initCaptureRequestBuilder(){
        try{
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        }
        catch(Exception e){
            Log.d(TAG, "initCaptureRequestBuilder: " + e.getMessage());
        }
    }
    /**
     * 返回相机是否处于连接状态。
     * 如果CameraDevice.StateCallback中的onOpened函数被回调，isConnected标志位会被设置为true。
     * 如果CameraDevice.StateCallback中的onDisconnected或者是onError函数被回调，isConnected标志位会被设置为false。
     * @return 返回true表示相机处于连接状态，返回false表示相机处于断开状态。
     */
    public boolean isConnected(){
        return isConnected;
    }
    public void close(){
        if (cameraCaptureSession != null){
            cameraCaptureSession.close();
        }
        if (cameraDevice != null){
            cameraDevice.close();
        }
    }
    public int getSensorOrientation(){
        return cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
    }

}

package com.xrt.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.transition.ChangeTransform;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.xrt.R;
import com.xrt.authority.AuthorityController;
import com.xrt.authority.AuthorityCtrlFactory;
import com.xrt.camera.CameraProxy;
import com.xrt.thirdpartylib.cv.CvUtils;
import com.xrt.accesser.originpic.OriginPicAccesserFactory;
import com.xrt.accesser.originpic.OriginPicAccesser;
import com.xrt.tools.IOUtils;
import com.xrt.constant.CommonExtraTag;
import com.xrt.tools.UiTools;
import com.xrt.widget.CameraTextureView;
import com.xrt.widget.FreeSelectImageView;
import com.xrt.widget.RadioButtonGroup;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

//相机拍照页
public class CameraActivity extends AppCompatActivity{
    private CameraProxy cameraProxy;
    private ImageReader imageReader;
    private String mDir;
    private String mDirFromOtherActivity;
    private DisplayMetrics mDisplayMetrics;
    private int mPicCount = 0;
    private String mCapturedStorePath;
    private SharedPreferences mPreference;
    private SharedPreferences.Editor mPreferenceEditor;
    private Bitmap mCacheForRedoBitmap;
    private Bitmap mOriginPic;
    private Bitmap mBlackRedProcessedPic;
    private Bitmap mBlackWhiteProcessedPic;
    private Bitmap mLighterProcessedPic;
    private Bitmap mProcessedPic;
    private Point[] mLastFourPoints;
    private SensorManager mSensorManager;
    private float[] mAcValues = new float[3];
    private float[] mMagValuse = new float[3];
    private ChangeTransform mChangeTransform;
    private OriginPicAccesser mOriginPicAccesser;
    private OrientationEventListener mOrientListener;
    private AuthorityController mAuthorityCtr;

    //权限请求常量
    private static final int CAMERA_REQUEST_CODE = 0x123;//相机权限请求
    //页面类型常量
    public static final int TYPE_NO_PREVIEW = 1;
    public static final int TYPE_PREVIEW = 2;
    //源Activity常量
    public static final int START_FROM_MAIN_ACTIVITY = 1;
    public static final int START_FROM_PICVIEW_ACTIVITY = 2;
    //START_FROM_MAIN_ACTIVITY对应的EXTRA
    public static final String FROM_PICVIEW_ACTIVITY_PIC_COUNT = "from_main_activity_pic_count";
    public static final String FROM_PICVIEW_ACTIVITY_PIC = "from_main_activity_pic";
    //StartActivityForResult方法启动其他Activity的常量
    public static final int CONNECT_PICVIEW_ACTIVITY = 0x10;//启动PicViewActivity的requestCode
    public static final int OPTION_RESET_PREVIEW_INFO = 0X11;//启动PicViewActivity的resultCode
    public static final int OPTION_FINISH_ACTIVITY = 0X12;//启动PicViewActivity的resultCode
    public static final int OPTION_CLEAN_PREVIEW_INFO = 0X13;//启动PicViewActivity的resultCode
    public static final String INTENT_Extra_FLAG_PIC_COUNT = "pic_count";//PicViewActivity返回的Extra
    public static final String INTENT_Extra_FLAG_PREVIEW_PIC = "preview_pic";//PicViewActivity返回的Extra
    //其他常量
    private static final int PREVIEW_IMAGE_INSAMPLE_SIZE = 8;//预览图的采样率
    //图片处理方法常量
    private static final int ORIGIN_PROCESS = 1;
    private static final int REDBLACK_PROCESS = 2;
    private static final int BLACKWHITE_PROCESS = 3;
    private static final int LIGHTER_PROCESS = 4;

    //view fields
    private ConstraintLayout mRootView;
    private ConstraintLayout mToolbar;
    private CameraTextureView mCameraTextureView;
    private ImageView mPreviewImageView;
    private TextView mPicCountTextView;
    private Button mShutterButton;
    private ProgressBar mWaitingProgressBar;
    private PopupWindow mPopWindow;
    private View mPopWindowRootView;
    private Button mPopWindowOkButton;
    private Button mPopWindowCanceButton;
    private Button mPopWindowRedoButton;
    private Button mPopWindowSelectedButton;
    private RadioButtonGroup mPopWindowProcessSelectBox;
    private Button mOpenFlashButton;
    private FreeSelectImageView mFreeSelectImageView;
    private ProgressBar mWaitingProcessProgressbar;
    //flag
    private int mType;
    private int mSourceActivity;
    private int mPicProcessType = ORIGIN_PROCESS;
    private int mOrientation = ORIENTATION_0;
    private int mLastOrientation = mOrientation;
    private static final int ORIENTATION_0 = 0;
    private static final int ORIENTATION_90 = 90;
    private static final int ORIENTATION_180 = 180;
    private static final int ORIENTATION_270 = 270;
    //权限相关
    private static final int REQUEST_CAMERA_AUTHORITY = 0x10;

    //listener
    private CameraCaptureSession.CaptureCallback mPreviewCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            if (!cameraProxy.isConnected()){
                return;
            }
        }
    };
    private SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            int sensorType = event.sensor.getType();
            switch (sensorType){
                case Sensor.TYPE_ACCELEROMETER:
                    mAcValues = event.values;
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mMagValuse = event.values;
                    break;
            }
            calOrientation();
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private void calOrientation(){
        float[] resultVal = new float[3];
        float[] R = new float[9];
        SensorManager.getRotationMatrix(R, null, mAcValues, mMagValuse);
        SensorManager.getOrientation(R, resultVal);
        float zArc = resultVal[0];
        //Log.d("mxrt", "z:" + zArc);
        if (0f < zArc && zArc < 0.2f){
            mOrientation = ORIENTATION_90;
        }
        if (zArc > 2.8f){
            mOrientation = ORIENTATION_270;
        }
        if (zArc > 1.2f && zArc <1.8f){
            mOrientation = ORIENTATION_0;
        }
        if (mOrientation != mLastOrientation){
            switch (mOrientation){
                case ORIENTATION_0:
                    changeLogo(ORIENTATION_0);
                    break;
                case ORIENTATION_90:
                    changeLogo(ORIENTATION_90);
                    break;
                case ORIENTATION_270:
                    changeLogo(ORIENTATION_270);
                    break;
            }
            mLastOrientation = mOrientation;
        }
    }
    private void changeLogo(int screenOrient){
        TransitionManager.beginDelayedTransition(mToolbar, mChangeTransform);
        int rotateDegree;
        switch (screenOrient){
            case ORIENTATION_90:
                rotateDegree = ORIENTATION_270;
                break;
            case ORIENTATION_270:
                rotateDegree = ORIENTATION_90;
                break;
            case ORIENTATION_0:
            case ORIENTATION_180:
            default:
                rotateDegree = ORIENTATION_0;
                break;
        }
        mOpenFlashButton.setRotation(rotateDegree);
    }
    private int getPicRotationDegree(int screenOrient){
        int sensorOrient = cameraProxy.getSensorOrientation();
        switch (screenOrient){
            case ORIENTATION_270:
                return ORIENTATION_270 + sensorOrient;
            case ORIENTATION_90:
                return ORIENTATION_90 + sensorOrient;
            case ORIENTATION_0:
            case ORIENTATION_180:
            default:
                return ORIENTATION_0 + sensorOrient;
        }
    }
    private class OrientListener extends OrientationEventListener{

        public OrientListener(Context context) {
            super(context);
        }
        @Override
        public void onOrientationChanged(int orientation) {
            //Log.d("mxrt", "orientation:" + orientation);
            if (orientation >= 350 || (orientation >= 0 && orientation <= 10)){
                mOrientation = ORIENTATION_0;
            }else if (orientation >= 80 && orientation <= 100){
                mOrientation = ORIENTATION_90;
            }else if (orientation >= 170 && orientation <= 190){
                mOrientation = ORIENTATION_180;
            }else if (orientation >= 260 && orientation <= 280){
                mOrientation = ORIENTATION_270;
            }
            if (mOrientation != mLastOrientation){
                mLastOrientation = mOrientation;
                changeLogo(mOrientation);
            }
        }
    }
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //Log.d("mxrt", "ratation:" + ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        OpenCVLoader.initDebug();
        mDir = MainActivity.SCAM_TAG + System.currentTimeMillis();
        initNormalVar();
        initView();
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        cameraProxy = new CameraProxy(this, "back");
        cameraProxy.setPreviewCallbackListener(mPreviewCallback);
        mCameraTextureView.addCameraProxy(cameraProxy);
        initImageReader();
        cameraProxy.addPotentialSurface(imageReader.getSurface());
        cameraProxy.addCaptureSurface(imageReader.getSurface());
        setViewsListener();
        getIntentExtraOnCreate();
        String dir = getDir();
        Thread thread = new Thread(() -> {
           mPreference = getSharedPreferences(dir, Context.MODE_PRIVATE);
           mPreferenceEditor = mPreference.edit();
        });
        thread.start();
        mOriginPicAccesser = OriginPicAccesserFactory.createOriginImgAccesser(this);
        mAuthorityCtr.validPermissionsWithHint(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_AUTHORITY);
    }
    private void initNormalVar(){
        mDisplayMetrics = UiTools.getScreenMetrics(this);
        mChangeTransform = new ChangeTransform();
        mChangeTransform.setDuration(200);
        mOrientListener = new OrientListener(this);
        mOrientListener.enable();
        mAuthorityCtr = AuthorityCtrlFactory.createAuthorityCtrl(this);
    }
    /*
     * 初始化View
     */
    private void initView(){
        mRootView = findViewById(R.id.rootview_activity_camera);
        mToolbar = findViewById(R.id.toolbar_activity_camera);
        mCameraTextureView = findViewById(R.id.textureview_activity_camera);
        mShutterButton = findViewById(R.id.button_shutter_activity_camera);
        mPicCountTextView = findViewById(R.id.textview_piccount_activity_camera);
        mPreviewImageView = findViewById(R.id.imageview_preview_activity_camera);
        mWaitingProgressBar = findViewById(R.id.progressbar_waiting_activity_camera);
        mPopWindowRootView = getLayoutInflater().inflate(R.layout.popwindow_activity_camera, null);
        mPopWindow = new PopupWindow(mPopWindowRootView, mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels);
        mPopWindowOkButton = mPopWindowRootView.findViewById(R.id.button_final_ok_popwindow_activity_camera);
        mPopWindowCanceButton = mPopWindowRootView.findViewById(R.id.button_cancel_popwindow_activity_camera);
        mPopWindowRedoButton = mPopWindowRootView.findViewById(R.id.button_redo_popwindow_activity_camera);
        mPopWindowSelectedButton = mPopWindowRootView.findViewById(R.id.button_select_ok_popwindow_activity_camera);
        mOpenFlashButton = findViewById(R.id.button_flash_activity_camera);
        initPopWindowProcessSelectBox();
        mFreeSelectImageView = mPopWindowRootView.findViewById(R.id.freeselectimageview_popwindow_activity_camera);
        mWaitingProcessProgressbar = mPopWindowRootView.findViewById(R.id.progressbar_waiting_process_activity_camera);
    }
    private void initPopWindowProcessSelectBox(){
        mPopWindowProcessSelectBox = mPopWindowRootView.findViewById(R.id.radiobuttongroup_popwindow_activity_camera);
        mPopWindowProcessSelectBox.setLayoutResId(R.layout.item_radiobutton_processbox_activity_camera);
        mPopWindowProcessSelectBox.setSpecFirstClickPosition(0);
        mPopWindowProcessSelectBox.setItemCount(4);
        String[] labels = new String[]{"原图", "白底黑字红章", "白底黑字", "増亮"};
        mPopWindowProcessSelectBox.setBindDataListener((rootView, position) -> {
            TextView textView = rootView.findViewById(R.id.textview_label_item_process_selectbox_activity_camera);
            textView.setText(labels[position]);
        });
        mPopWindowProcessSelectBox.setClickListeners(Arrays.asList(
                () -> {
                    if (mOriginPic != null){
                        mPicProcessType = ORIGIN_PROCESS;
                        mFreeSelectImageView.setCropedImg(mOriginPic);
                    }
                },
                () -> {
                    if (mBlackRedProcessedPic == null){
                        mBlackRedProcessedPic = redBlackProcess(mOriginPic);
                    }
                    mPicProcessType = REDBLACK_PROCESS;
                    mProcessedPic = mBlackRedProcessedPic;
                    mFreeSelectImageView.setCropedImg(mBlackRedProcessedPic);
                },
                () -> {
                    if (mBlackWhiteProcessedPic == null){
                        mBlackWhiteProcessedPic = blackWhiteProcess(mOriginPic);
                    }
                    mPicProcessType = BLACKWHITE_PROCESS;
                    mProcessedPic = mBlackWhiteProcessedPic;
                    mFreeSelectImageView.setCropedImg(mBlackWhiteProcessedPic);
                },
                () -> {
                    if (mLighterProcessedPic == null){
                        mLighterProcessedPic = lighterProcess(mOriginPic);
                    }
                    mPicProcessType = LIGHTER_PROCESS;
                    mProcessedPic = mLighterProcessedPic;
                    mFreeSelectImageView.setCropedImg(mLighterProcessedPic);
                }
        ));
        mPopWindowProcessSelectBox.setClickAffect(
                (rootView) -> {
                    TextView textView = rootView.findViewById(R.id.textview_label_item_process_selectbox_activity_camera);
                    textView.setBackgroundResource(R.color.lightBlue);
                }
        );
        mPopWindowProcessSelectBox.setResetAffect(
                (rootView) -> {
                    TextView textView = rootView.findViewById(R.id.textview_label_item_process_selectbox_activity_camera);
                    textView.setBackgroundColor(Color.WHITE);
                }
        );
    }
    /*
     * 设置View事件监听
     */
    private void setViewsListener(){
        setPopPreviewWindowClickListener();
        mShutterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraProxy.capture();//此处的代码非阻塞，执行完但是不代表已经会获得mCaptureBitmap对象。
                mWaitingProgressBar.setVisibility(View.VISIBLE);
                mShutterButton.setClickable(false);
            }
        });
        mPreviewImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPicviewActivity();
            }
        });
        mOpenFlashButton.setOnClickListener((v) -> {
            cameraProxy.openFlash();
        });
    }
    /*
     * 启动PicviewActivity。
     */
    private void startPicviewActivity(){
        Intent intent = new Intent(CommonExtraTag.START_PICVIEW_ACTIVITY_ACTION);
        String dir = getDir();
        intent.putExtra(CommonExtraTag.PICVIEW_ACTIVITY_FILEPATH_EXTRA, getExternalFilesDir(dir).getPath());
        intent.putExtra(CommonExtraTag.PICVIEW_ACTIVITY_FILE_TITLE, "");
        intent.putExtra(CommonExtraTag.PICVIEW_ACTIVITY_TYPE, PictureViewActivity.TYPE_START_FROM_CAMERA);
        startActivityForResult(intent, CONNECT_PICVIEW_ACTIVITY);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        //Log.d("mxrt", "CameraActivity requestCode:" + requestCode);
        //Log.d("mxrt", "CameraActivity resultCode:" + resultCode);
        if (requestCode == CONNECT_PICVIEW_ACTIVITY){
            switch (resultCode){
                case OPTION_RESET_PREVIEW_INFO:
                    mPicCount = intent.getIntExtra(INTENT_Extra_FLAG_PIC_COUNT, mPicCount);
                    String previewFileName = intent.getStringExtra(INTENT_Extra_FLAG_PREVIEW_PIC);
                    Thread thread = new Thread(() -> {
                        Bitmap previewPic = IOUtils.getAbridgeBitmap(previewFileName, PREVIEW_IMAGE_INSAMPLE_SIZE);
                        mPreviewImageView.post(() -> {
                            updatePreviewPic(previewPic);
                        });
                    });
                    thread.start();
                    updatePreviewPicCount();
                    break;
                case OPTION_FINISH_ACTIVITY:
                    finish();
                    break;
                case OPTION_CLEAN_PREVIEW_INFO:
                    mPicCount = 0;
                    mPicCountTextView.setVisibility(View.INVISIBLE);
                    mPreviewImageView.setVisibility(View.INVISIBLE);
                    break;
            }
        }
    }
    @Override
    protected void onResume(){
        super.onResume();
        if (!cameraProxy.isConnected()){
            cameraProxy.openCamera();
        }
        bindSensor();
    }
    private void bindSensor(){
        //mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        //mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        //mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
    }
    private void unBindSensor(){
        //mSensorManager.unregisterListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        //mSensorManager.unregisterListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    }
    /*
     * 获取Extra。
     */
    private void getIntentExtraOnCreate(){
        Intent intent = getIntent();
        mType = intent.getIntExtra(CommonExtraTag.CAMERA_ACTIVITY_TYPE, TYPE_PREVIEW);
        mSourceActivity = intent.getIntExtra(CommonExtraTag.SOURCE_ACTIVITY, -1);
        mDirFromOtherActivity = intent.getStringExtra(CommonExtraTag.CAMERA_ACTIVITY_PATH);//照片目录，非完整路径。"scam_xxxxx"
        switch(mSourceActivity){
            case CameraActivity.START_FROM_MAIN_ACTIVITY:
                break;
            case CameraActivity.START_FROM_PICVIEW_ACTIVITY:
                switch(mType){
                    case TYPE_PREVIEW:
                        mPicCount = intent.getIntExtra(CameraActivity.FROM_PICVIEW_ACTIVITY_PIC_COUNT, mPicCount);
                        String previewPicAbsPath = intent.getStringExtra(CameraActivity.FROM_PICVIEW_ACTIVITY_PIC);
                        Thread thread = new Thread(() -> {
                            Bitmap pic = IOUtils.getAbridgeBitmap(previewPicAbsPath, PREVIEW_IMAGE_INSAMPLE_SIZE);
                            mPicCountTextView.post(() -> {
                                mPicCountTextView.setVisibility(View.VISIBLE);
                                updatePreviewPicCount();
                            });
                            mPreviewImageView.post(() -> {
                                mPreviewImageView.setVisibility(View.VISIBLE);
                                updatePreviewPic(pic);
                            });
                        });
                        thread.start();
                        break;
                }
                break;
        }
        //Log.d("mxrt", ":" + mDirFromOtherActivity);
    }
    /*
     * 配置ImageReader。
     */
    private void initImageReader(){
        Size largestSize = cameraProxy.getMaxOutputSize();
        imageReader = ImageReader.newInstance(largestSize.getWidth(), largestSize.getHeight(), ImageFormat.JPEG, 1);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                String dirName = getDir();
                String picName = System.currentTimeMillis() + ".jpg";
                mCapturedStorePath = getExternalFilesDir(dirName).getPath() + File.separator + picName;
                long t1 = System.currentTimeMillis();
                Bitmap captureBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                long t2 = System.currentTimeMillis();
                Log.d("mxrt", "time:" + (t2 - t1));
                Matrix matrix = new Matrix();
                //把图旋正，这里不应该硬编码，需要根据取得的参数来实施。
                int rotateDegree = getPicRotationDegree(mOrientation);
                matrix.postRotate(rotateDegree);
                long t3 = System.currentTimeMillis();
                //Bitmap rotatedBitmap = UiTools.rotateBitmap(captureBitmap ,getPicRotationDegree(mOrientation));
                Bitmap rotatedBitmap = Bitmap.createBitmap(captureBitmap, 0, 0, captureBitmap.getWidth(), captureBitmap.getHeight(), matrix, true);
                long t4 = System.currentTimeMillis();
                Log.d("mxrt", "time2:" + (t4 - t3));
                String relativePath = File.separator + dirName + File.separator + picName;
                long t5 = System.currentTimeMillis();
                mOriginPicAccesser.storeOriginPic(relativePath, rotatedBitmap, 20);
                long t6 = System.currentTimeMillis();
                Log.d("mxrt", "time3:" + (t6 - t5));
                popPreviewWindow(rotatedBitmap);
                image.close();
            }
        }, null);
    }
    private Bitmap redBlackProcess(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        Mat resultMat = CvUtils.redBlackProcess(srcMat);
        return CvUtils.matToBitmap(resultMat);
    }
    private Bitmap blackWhiteProcess(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        Mat resultMat = CvUtils.blackWhiteProcess(srcMat);
        return CvUtils.matToBitmap(resultMat);
    }
    private Bitmap lighterProcess(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        Mat resultMat = CvUtils.lighterProcess(srcMat);
        return CvUtils.matToBitmap(resultMat);
    }
    private int[] getScanedCoo(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        //srcMat = CvLib.cvtColor(srcMat, Imgproc.COLOR_BGR2GRAY);
        Mat edgeMat = CvUtils.dilate(CvUtils.canny(srcMat));
        Mat resultMat = CvUtils.drawContours(edgeMat, srcMat);
        int[] coo = CvUtils.getLTRBCoo(resultMat);
        //int[] coo = CvUtils.getMinAreaCoo4(edgeMat, srcMat);
        return coo;
    }
    /*
     * 以拍照生成的图片的完整储存路径为key值，将顺序值记录到SharedPreference中。
     */
    private void recordPicOrder(){
        if (mType == TYPE_PREVIEW) {
            mPreferenceEditor.putInt(mCapturedStorePath, mPicCount);
            mPreferenceEditor.commit();
        }
    }
    private void setPopPreviewWindowClickListener(){
        mPopWindowOkButton.setOnClickListener((View v) -> {
            Bitmap bitmapToSave = null;
            switch (mPicProcessType){
                case ORIGIN_PROCESS:
                    bitmapToSave = mOriginPic;
                    break;
                case REDBLACK_PROCESS:
                    bitmapToSave = mProcessedPic;
                    break;
                case BLACKWHITE_PROCESS:
                    bitmapToSave = mProcessedPic;
                    break;
                case LIGHTER_PROCESS:
                    bitmapToSave = mProcessedPic;
                    break;
            }
            clearProcessCache();
            if (bitmapToSave == null){
                bitmapToSave = mFreeSelectImageView.getCropedImg();
            }
            IOUtils.saveImgFileWithJpg(mCapturedStorePath, bitmapToSave, 20);//保存到本地
            recordPicOrder();
            updatePreviewPic(bitmapToSave);
            mPreviewImageView.setVisibility(View.VISIBLE);
            mPicCount += 1;
            updatePreviewPicCount();
            mPopWindow.dismiss();
            mShutterButton.setClickable(true);
            mPicCountTextView.setVisibility(View.VISIBLE);
            switch(mType){
                case TYPE_NO_PREVIEW:
                    mPicCountTextView.setVisibility(View.INVISIBLE);
                    mPreviewImageView.setVisibility(View.INVISIBLE);
                    finish();
                    break;
            }
            mPopWindowSelectedButton.setVisibility(View.VISIBLE);
            mPopWindowOkButton.setVisibility(View.INVISIBLE);
            mPopWindowRedoButton.setVisibility(View.INVISIBLE);
            mPopWindowProcessSelectBox.setVisibility(View.INVISIBLE);
            mPicProcessType = ORIGIN_PROCESS;
        });
        mPopWindowCanceButton.setOnClickListener((View v) -> {
            /*
            IOUtils.deleteDirectoryOrFile(mCapturedStorePath);
            if (mType == TYPE_PREVIEW){
                mPreferenceEditor.remove(mCapturedStorePath);
                mPreferenceEditor.commit();
            }
             */
            mPopWindow.dismiss();
            mShutterButton.setClickable(true);
        });
        mPopWindowRedoButton.setOnClickListener((view) -> {
            clearProcessCache();
            mPopWindowOkButton.setVisibility(View.INVISIBLE);
            mPopWindowSelectedButton.setVisibility(View.VISIBLE);
            mPopWindowRedoButton.setVisibility(View.INVISIBLE);
            mPopWindowCanceButton.setVisibility(View.VISIBLE);
            mPopWindowProcessSelectBox.setVisibility(View.INVISIBLE);
            mFreeSelectImageView.setImgWithSelectBoxReback(mCacheForRedoBitmap);
        });
        mPopWindowSelectedButton.setOnClickListener((view) -> {
            mPopWindowSelectedButton.setVisibility(View.GONE);
            mPopWindowOkButton.setVisibility(View.VISIBLE);
            mPopWindowRedoButton.setVisibility(View.VISIBLE);
            mPopWindowProcessSelectBox.setVisibility(View.VISIBLE);
            mOriginPic = mFreeSelectImageView.getCropedImg();
            mFreeSelectImageView.setCropedImg(mOriginPic);
            mPopWindowProcessSelectBox.reset();
        });
    }
    /*
     * 弹出预览窗口
     */
    private void popPreviewWindow(Bitmap capturedBitmap){
        mCacheForRedoBitmap = capturedBitmap;
        int[] coo = getScanedCoo(capturedBitmap);
        mFreeSelectImageView.setImgWithSelectBox(capturedBitmap, coo);
        mPopWindow.showAtLocation(mShutterButton, Gravity.CENTER, 0, 0);
        mWaitingProgressBar.setVisibility(View.INVISIBLE);
    }
    private void clearProcessCache(){
        mBlackRedProcessedPic = null;
        mBlackWhiteProcessedPic = null;
        mLighterProcessedPic = null;
    }
    /*
     * 刷新图片数
     */
    private void updatePreviewPicCount(){
        mPicCountTextView.setText("" + mPicCount);
    }
    /*
     * 刷新预览图
     */
    private void updatePreviewPic(Bitmap pic){
        mPreviewImageView.setImageBitmap(pic);
    }
    /*
     * 获取照片的储存目录，非完整路径。目录格式"scam_xxxx"
     */
    private String getDir(){
        switch(mSourceActivity){
            case CameraActivity.START_FROM_PICVIEW_ACTIVITY:
                return mDirFromOtherActivity;

            case CameraActivity.START_FROM_MAIN_ACTIVITY:
            default:
                return mDir;
        }
    }
    @Override
    protected void onPause(){
        super.onPause();
        unBindSensor();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (mPicCount == 0){
            String preferencePath = "/data/data" + "/" + getPackageName() + "/" + "shared_prefs" +  "/" + mDir + ".xml";
            IOUtils.deleteDirectoryOrFile(preferencePath);
        }
        cameraProxy.close();
        unBindSensor();
        mOrientListener.disable();
    }
}

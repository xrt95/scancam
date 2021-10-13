package com.xrt.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.xrt.R;
import com.xrt.thirdpartylib.cv.CvUtils;
import com.xrt.accesser.originpic.OriginPicAccesserFactory;
import com.xrt.accesser.originpic.OriginPicAccesser;
import com.xrt.tools.IOUtils;
import com.xrt.constant.CommonExtraTag;
import com.xrt.tools.ShareTools;
import com.xrt.tools.UiTools;
import com.xrt.tools.Utils;
import com.xrt.widget.FreeSelectImageView;
import com.xrt.widget.HorizonImageTextButtonGroup;
import com.xrt.widget.PicScrollView;
import com.xrt.widget.RadioButtonGroup;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;

//图片浏览页
public class PictureViewActivity extends AppCompatActivity{
    private Handler mHandler = new UiHandler();
    private List<String> mPicAbsPaths;
    private List<String> mOrderAdjustPicPaths;
    private List<String> mLastFileNames = new ArrayList<>();
    private String mTitle;
    private ChangeBounds mChangeBounds;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mPreferenceEditor;
    private String mDir;
    private Map<String, Bitmap> mFileNameToBitmap = new HashMap<>();
    private Bitmap mCurrentBitmap;
    private int mLastCurrentIndex = -1;
    private DisplayMetrics mDisplayMetrics;
    private int mFirstLoadPicIndex = 0;
    private OriginPicAccesser mOriginPicAccesser;
    private ArrayList<String> mNeedToUpdatePicPaths = new ArrayList<>();
    //页面类型常量
    public static final int TYPE_DEFAULT = 1;
    public static final int TYPE_START_FROM_ACTIVITY = 2;
    public static final int TYPE_START_FROM_CAMERA = 3;
    public static final int TYPE_START_FROM_FLATVIEW= 4;
    //功能状态常量
    public static final int STATE_NORMAL = 5;
    public static final int STATE_RIGHT_INSERT = 6;
    public static final int STATE_LEFT_INSERT = 7;
    public static final int STATE_REPLACE = 8;
    //其他常量
    private static final int VIEW_INSAMPLE_SIZE = 2;
    //
    private int mOptionState = OPTION_DO_NOTHING;
    private static final int START_IMGPROCESS_ACTIVITY = 0x10;
    public static final int OPTION_DO_NOTHING = 1;
    public static final int OPTION_UPATE_PROCESS = 2;
    //view fields
    private ConstraintLayout mRootView;
    private TextView mMaskView;
    private TextView mPageTextView;
    private PicScrollView mPicScrollView;
    private Button mPreButton;
    private Button mNextButton;
    private HorizonImageTextButtonGroup mFootbarButtonGroup;
    private TextView mtoolbarTitle;
    private Button mBackButton;
    private Button mShareButton;
    private HorizonImageTextButtonGroup.ImageTextButton mFootbarDeleteButton;
    private HorizonImageTextButtonGroup.ImageTextButton mFootbarReplaceButton;
    private HorizonImageTextButtonGroup.ImageTextButton mLeftInsertButton;
    private HorizonImageTextButtonGroup.ImageTextButton mRightInsertButton;
    private HorizonImageTextButtonGroup.ImageTextButton mContinueAddButton;
    private HorizonImageTextButtonGroup.ImageTextButton mFootbarCompleteButton;
    private ConstraintLayout mToolbar;
    private ConstraintLayout mFootbar;
    private PopupWindow mDeleteHintPopwindow;
    private View mDeleteHintPopwindowRootView;
    private Button mImgProcessButton;
    private FreeSelectImageView mFreeSelectImageView;
    //Intent fields
    private String mLoadPicFilePath;
    //size fields
    private int mToolbarHeight;
    private int mFootbarHeight;
    //flags
    private boolean isHideBar = false;
    private boolean isDelLastPic = false;
    private int mType = TYPE_DEFAULT;
    private int mState = STATE_NORMAL;
    //临时变量
    private Button mMenuButton;
    private PopupMenu mPopMenu;
    private ConstraintLayout mPopwindowRootView;
    private PopupWindow mPopupwindow;
    private Button mPopWindowCanceButton;
    private Button mPopWindowOkButton;
    private Button mPopWindowRedoButton;
    private Button mPopWindowSelectedButton;
    private RadioButtonGroup mPopWindowProcessSelectBox;
    private HorizonImageTextButtonGroup.ImageTextButton mOriginProcessButton;
    private HorizonImageTextButtonGroup.ImageTextButton mScanedProcessButton;
    private Bitmap mCacheForRedoBitmap;
    private Bitmap mOriginBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picview);
        getIntentExtraOnCreate();
        initNormalVar();
        initView();
        initFootbarButtonGroup();
        //initPopWindowProcessSelectBox();
        setActivityTitle();
        setViewsListener();
        mOriginPicAccesser = OriginPicAccesserFactory.createOriginImgAccesser(this);
        mPicAbsPaths = IOUtils.getSpecSufixFilePaths(mLoadPicFilePath, new String[]{"jpg"});
        mPicScrollView.addPicView(mPicAbsPaths.size());
        Thread thread = new Thread(() -> {
            mPreferences = getSharedPreferences(mDir.replace("/", ""), Context.MODE_PRIVATE);
            mPreferenceEditor = mPreferences.edit();
            firstLoad();
        });
        thread.start();
        //mPopWindowFreeBox.setAdaptImageView(mPopWindowImageView);

    }
    @Override
    public boolean onTouchEvent(MotionEvent event){
        //TestTool.judgeEventAction("mxrt", "PicviewActivity ", event);
        return false;
    }
    /**
     * 一般变量的初始化放在这里
     */
    private void initNormalVar(){
        mChangeBounds = new ChangeBounds();
        mChangeBounds.setDuration(100);
        mDisplayMetrics = UiTools.getScreenMetrics(this);
    }
    /**
     * View的初始化
     */
    private void initView(){
        mMaskView = findViewById(R.id.mask_activity_picview);
        mPageTextView = findViewById(R.id.textview_page_activity_picview);
        mPicScrollView = findViewById(R.id.scrollview_activity_picview);
        mPreButton = findViewById(R.id.button_preflip_activity_picview);
        mNextButton = findViewById(R.id.button_nextflip_activity_picview);
        mFootbarButtonGroup = findViewById(R.id.imagetext_button_group_activity_picview);
        mRootView = findViewById(R.id.rootview_activity_picview);
        mtoolbarTitle = findViewById(R.id.title_toolbar_activity_picview);
        mBackButton = findViewById(R.id.button_back_activity_picview);
        mToolbar = findViewById(R.id.toolbar_activity_piciview);
        mFootbar = findViewById(R.id.footbar_activity_picview);
        mShareButton = findViewById(R.id.button_share_activity_picview);
        mMenuButton = findViewById(R.id.button_menu_activity_picview);
        mPopMenu = new PopupMenu(this, mMenuButton);
        mPopMenu.inflate(R.menu.menu_activity_picview);
        mPopwindowRootView = (ConstraintLayout)getLayoutInflater().inflate(R.layout.popwindow_activity_camera, null);
        //mPopWindowImageView = mPopwindowRootView.findViewById(R.id.imageview_popwindow_activity_camera);
        //mPopWindowFreeBox = mPopwindowRootView.findViewById(R.id.freeselectbox_popwindow_activity_camera);
        mPopWindowCanceButton = mPopwindowRootView.findViewById(R.id.button_cancel_popwindow_activity_camera);
        mPopWindowOkButton = mPopwindowRootView.findViewById(R.id.button_final_ok_popwindow_activity_camera);
        mPopWindowRedoButton = mPopwindowRootView.findViewById(R.id.button_redo_popwindow_activity_camera);
        mPopWindowSelectedButton = mPopwindowRootView.findViewById(R.id.button_select_ok_popwindow_activity_camera);
        mPopupwindow = new PopupWindow(mPopwindowRootView, mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels);
        mDeleteHintPopwindowRootView = getLayoutInflater().inflate(R.layout.popwindow_delete_hint, null);
        int deleteHintPopwindowHeight = getResources().getDimensionPixelSize(R.dimen.popwindow_delete_hint_height);
        mDeleteHintPopwindow = new PopupWindow(mDeleteHintPopwindowRootView, mDisplayMetrics.widthPixels, deleteHintPopwindowHeight);
        initPopWindowProcessSelectBox();
        mImgProcessButton = findViewById(R.id.button_img_process_activity_picview);
        mFreeSelectImageView = mPopwindowRootView.findViewById(R.id.freeselectimageview_popwindow_activity_camera);
    }
    private void initPopWindowProcessSelectBox(){
        mPopWindowProcessSelectBox = mPopwindowRootView.findViewById(R.id.radiobuttongroup_popwindow_activity_camera);
        mPopWindowProcessSelectBox.setLayoutResId(R.layout.item_radiobutton_processbox_activity_camera);
        mPopWindowProcessSelectBox.setSpecFirstClickPosition(0);
        mPopWindowProcessSelectBox.setItemCount(3);
        String[] labels = new String[]{"原图", "白底黑字红章", "白底黑字"};
        mPopWindowProcessSelectBox.setBindDataListener((rootView, position) -> {
            TextView textView = rootView.findViewById(R.id.textview_label_item_process_selectbox_activity_camera);
            textView.setText(labels[position]);
        });
        mPopWindowProcessSelectBox.setClickListeners(Arrays.asList(
                () -> {
                    if (mOriginBitmap != null){
                        mFreeSelectImageView.setCropedImg(mOriginBitmap);
                    }
                },
                () -> {
                    Mat srcMat = CvUtils.bitmapToMat(mOriginBitmap);
                    Mat scanedMat = CvUtils.sharp(srcMat);
                    Bitmap scanBitmap = CvUtils.matToBitmap(scanedMat);
                    mFreeSelectImageView.setCropedImg(scanBitmap);
                },
                () -> {}
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
    private void initPopWindowProcessSelectBox(){
        mPopWindowProcessSelectBox.addImageTextButtom(2);
        mOriginProcessButton = mPopWindowProcessSelectBox.getButtonAt(0);
        mScanedProcessButton = mPopWindowProcessSelectBox.getButtonAt(1);
        mPopWindowProcessSelectBox.setAllImageViewGone();
        mPopWindowProcessSelectBox.setAllTextViewConstraintWidthPercent(1f);
        mPopWindowProcessSelectBox.setAllTextViewConstraintHeightPercent(1f);
        mPopWindowProcessSelectBox.setAllImageViewVerticalBias(0f);
        mOriginProcessButton.getTextView().setText("原图");
        mOriginProcessButton.getTextView().setBackgroundResource(R.color.lightBlue);
        mOriginProcessButton.getTextView().setTextColor(Color.parseColor("black"));
        mOriginProcessButton.getTextView().setGravity(Gravity.CENTER);
        mScanedProcessButton.getTextView().setText("锐化");
        mScanedProcessButton.getTextView().setBackgroundResource(R.color.white);
        mScanedProcessButton.getTextView().setTextColor(Color.parseColor("black"));
        mScanedProcessButton.getTextView().setGravity(Gravity.CENTER);
        mOriginProcessButton.setOnClickListener((view) -> {
            mOriginProcessButton.getTextView().setBackgroundResource(R.color.lightBlue);
            mScanedProcessButton.getTextView().setBackgroundResource(R.color.white);
            if (mOriginBitmap != null){
                mPopWindowImageView.setImageBitmap(mOriginBitmap);
            }
        });
        mScanedProcessButton.setOnClickListener((view) -> {
            mScanedProcessButton.getTextView().setBackgroundResource(R.color.lightBlue);
            mOriginProcessButton.getTextView().setBackgroundResource(R.color.white);
            Point[] nowFourPoints = mPopWindowFreeBox.getOriginSelectedFourPoint();
            if ((!Utils.isPointArrayEqual(nowFourPoints, mLastFourPoints) || mScanedBitmap == null) && mOriginBitmap != null){
                mLastFourPoints = nowFourPoints;
                Mat srcMat = CvUtils.bitmapToMat(mOriginBitmap);
                Mat scanedMat = CvUtils.sharp(srcMat);
                mScanedBitmap = CvUtils.matToBitmap(scanedMat);
            }
            if (mScanedBitmap != null){
                mPopWindowImageView.setImageBitmap(mScanedBitmap);
            }
        });
    }
     */
    /**
     * 隐藏顶部和底部的菜单栏
     */
    private void hideBar(){
        TransitionManager.beginDelayedTransition(mRootView, mChangeBounds);
        changeToolbarState(false);
        changeFootbarState(false);
    }
    /**
     * 显示顶部和底部的菜单栏
     */
    private void showBar(){
        TransitionManager.beginDelayedTransition(mRootView, mChangeBounds);
        changeToolbarState(true);
        changeFootbarState(true);
    }
    /**
     * 收展顶部菜单栏
     */
    private void changeToolbarState(boolean isRestore){
        ViewGroup.LayoutParams toolbarLP = mToolbar.getLayoutParams();
        if (isRestore){
            toolbarLP.height = mToolbarHeight;
        }else{
            mToolbarHeight = toolbarLP.height;
            toolbarLP.height = 0;
        }
        mToolbar.setLayoutParams(toolbarLP);
    }
    /**
     * 收展底部菜单栏
     */
    private void changeFootbarState(boolean isRestore){
        ViewGroup.LayoutParams footbarLP = mFootbar.getLayoutParams();
        if (isRestore){
            footbarLP.height = mFootbarHeight;
        }else{
            mFootbarHeight = footbarLP.height;
            footbarLP.height = 0;
        }
        mFootbar.setLayoutParams(footbarLP);
    }
    /**
     * 在onRestart的时候获取Extra
     */
    private void getIntentExtraOnRestart(){
        Intent intent = getIntent();
    }
    /**
     * 在onCreate的时候获取Extra
     */
    private void getIntentExtraOnCreate(){
        Intent intent = getIntent();
        mLoadPicFilePath = intent.getStringExtra(CommonExtraTag.PICVIEW_ACTIVITY_FILEPATH_EXTRA);
        mFirstLoadPicIndex = intent.getIntExtra(CommonExtraTag.PICVIEW_ACTIVITY_FIRST_LOAD_PIC_INDEX, 0);
        mDir = mLoadPicFilePath.replace(getExternalFilesDir("").getPath(), "");
        mTitle = intent.getStringExtra(CommonExtraTag.PICVIEW_ACTIVITY_FILE_TITLE);
        mType = intent.getIntExtra(CommonExtraTag.PICVIEW_ACTIVITY_TYPE, TYPE_DEFAULT);
    }
    /**
     * 设置View的事件监听
     */
    private void setViewsListener(){
        mImgProcessButton.setOnClickListener((v) -> {
            startImgProcessActivity();
        });
        mPicScrollView.setOnPictureChangedListener(new PicScrollView.OnPictureChanged() {
            @Override
            public void onPictureIndexChange(int index, int totalCount) {
                mPageTextView.setText(String.format("%d/%d", index, totalCount));
            }
        });
        mPreButton.setOnClickListener((view) -> {
            mPicScrollView.skipPicBy(-1, true);
        });
        mNextButton.setOnClickListener((view) -> {
            mPicScrollView.skipPicBy(1, true);
        });
        mBackButton.setOnClickListener((view) -> {
            switch(mType){
                case TYPE_START_FROM_CAMERA:
                    setCameraActivityResult();
                    break;
                case TYPE_START_FROM_FLATVIEW:
                    setFlatViewActivityResult();
                    break;
            }
            finish();
        });
        mShareButton.setOnClickListener((view) -> {
            try{
                ShareTools.shareFile(mOrderAdjustPicPaths.get(mPicScrollView.getCurrentIndex()), "image/*", this, "com.xrt.fileprovider", ShareTools.SHARE_MORE);
            }catch (Exception e){
                Toast.makeText(this, R.string.share_error, Toast.LENGTH_SHORT).show();
            }
        });
        mPopWindowCanceButton.setOnClickListener((view) -> {
            mPopupwindow.dismiss();
        });
        mPopWindowOkButton.setOnClickListener((view) -> {
        });
        mPopWindowRedoButton.setOnClickListener((view) -> {
            mPopWindowProcessSelectBox.setVisibility(View.INVISIBLE);
            mPopWindowOkButton.setVisibility(View.INVISIBLE);
            mPopWindowSelectedButton.setVisibility(View.VISIBLE);
            mPopWindowRedoButton.setVisibility(View.INVISIBLE);
            mPopWindowCanceButton.setVisibility(View.VISIBLE);
            mFreeSelectImageView.setImgWithSelectBoxReback(mCacheForRedoBitmap);
        });
        mPopWindowSelectedButton.setOnClickListener((view) -> {
            mPopWindowSelectedButton.setVisibility(View.GONE);
            mPopWindowOkButton.setVisibility(View.VISIBLE);
            mPopWindowRedoButton.setVisibility(View.VISIBLE);
            mPopWindowProcessSelectBox.setVisibility(View.VISIBLE);
            Bitmap pic = ((BitmapDrawable)mPicScrollView.getPicViewAt(mPicScrollView.getCurrentIndex()).getImageView().getDrawable()).getBitmap();
            mCacheForRedoBitmap = pic;
            Bitmap newPic = mFreeSelectImageView.getCropedImg();
            mOriginBitmap = newPic;
            mFreeSelectImageView.setCropedImg(newPic);
            mPopWindowProcessSelectBox.reset();
        });
        mPopMenu.setOnMenuItemClickListener((item) -> {
            Bitmap pic = ((BitmapDrawable)mPicScrollView.getPicViewAt(mPicScrollView.getCurrentIndex()).getImageView().getDrawable()).getBitmap();
            Bitmap newPic;
            switch (item.getItemId()){
                case R.id.option_pop_window_activity_picview:
                    int[] coo = getScanedCoo(pic);
                    mFreeSelectImageView.setImgWithSelectBox(pic, coo);
                    mPopupwindow.showAtLocation(mRootView, Gravity.TOP, 0,0);
                    break;
                case R.id.option_scan_shift_activity_picview:
                    newPic = scanMeanShiftPic(pic);
                    mPicScrollView.getPicViewAt(mPicScrollView.getCurrentIndex()).getImageView().setImageBitmap(newPic);
                    break;
                case R.id.option_scan_edge_activity_picview:
                    newPic = scanEdgePic(pic);
                    mPicScrollView.getPicViewAt(mPicScrollView.getCurrentIndex()).getImageView().setImageBitmap(newPic);
                    break;
                case R.id.option_scan_spec_edge_activity_picview:
                    newPic = scanSpecEdgePic(pic);
                    mPicScrollView.getPicViewAt(mPicScrollView.getCurrentIndex()).getImageView().setImageBitmap(newPic);
                    break;
                case R.id.option_scan_contour_activity_picview:
                    newPic = scanContourPic(pic);
                    mPicScrollView.getPicViewAt(mPicScrollView.getCurrentIndex()).getImageView().setImageBitmap(newPic);
                    break;
                case R.id.option_scan_spec_contour_activity_picview:
                    newPic = scanSpecContourPic(pic);
                    mPicScrollView.getPicViewAt(mPicScrollView.getCurrentIndex()).getImageView().setImageBitmap(newPic);
                    break;
                case R.id.option_scan_completed_activity_picview:
                    newPic = scanCompletePic(pic);
                    mPicScrollView.getPicViewAt(mPicScrollView.getCurrentIndex()).getImageView().setImageBitmap(newPic);
                    break;
                case R.id.option_perspective_change_activity_picview:
                    newPic = perspectiveChangePic(pic);
                    mPicScrollView.getPicViewAt(mPicScrollView.getCurrentIndex()).getImageView().setImageBitmap(newPic);
                    break;
                case R.id.option_scan_crop_activity_picview:
                    newPic = scanCropPic(pic);
                    mPicScrollView.getPicViewAt(mPicScrollView.getCurrentIndex()).getImageView().setImageBitmap(newPic);
                    break;
                case R.id.option_reset_activity_picview:
                    mPicScrollView.getPicViewAt(mPicScrollView.getCurrentIndex()).getImageView().setImageBitmap(mCurrentBitmap);
                    break;
                case R.id.option_scan_step1_activity_picview:
                    newPic = scanStep1(pic);
                    mPicScrollView.getPicViewAt(mPicScrollView.getCurrentIndex()).getImageView().setImageBitmap(newPic);
                    break;
                case R.id.option_scan_step2_activity_picview:
                    newPic = scanStep2(pic);
                    mPicScrollView.getPicViewAt(mPicScrollView.getCurrentIndex()).getImageView().setImageBitmap(newPic);
                    break;
                case R.id.option_scan_step3_activity_picview:
                    newPic = scanStep3(pic);
                    mPicScrollView.getPicViewAt(mPicScrollView.getCurrentIndex()).getImageView().setImageBitmap(newPic);
                    break;
                case R.id.option_scan_step4_activity_picview:
                    newPic = scanStep4(pic);
                    mPicScrollView.getPicViewAt(mPicScrollView.getCurrentIndex()).getImageView().setImageBitmap(newPic);
                    break;
            }
            return true;
        });
        mMenuButton.setOnClickListener((view) -> {
            if (mCurrentBitmap == null || mLastCurrentIndex != mPicScrollView.getCurrentIndex()){
                mCurrentBitmap = ((BitmapDrawable)mPicScrollView.getPicViewAt(mPicScrollView.getCurrentIndex()).getImageView().getDrawable()).getBitmap();
                mLastCurrentIndex = mPicScrollView.getCurrentIndex();
            }
            mPopMenu.show();
        });

        mPicScrollView.setImageViewTapUpListener(() -> {
            if (isHideBar){
                showBar();
                isHideBar = false;
            }else{
                hideBar();
                isHideBar = true;
            }
        });
    }
    /**
     * @param index 在PicScrollView中的索引
     * 删除PicScrollView中指定index的图片。会删除同时图片和PicView控件。
     */
    private void deleteCurrentIndexPic(int index){
        if (mPicScrollView.getPicViewCount() == 1){
            isDelLastPic = true;
            mPicScrollView.removePicViewAt(index);
            deleteOnePic(index);
            switch(mType){
                case TYPE_START_FROM_CAMERA:
                    setResult(CameraActivity.OPTION_CLEAN_PREVIEW_INFO, getIntent());
                    finish();
                    break;
                case TYPE_START_FROM_ACTIVITY:
                    setResult(MainActivity.OPTION_DELETE_ITEM, getIntent());
                    finish();
                    break;
                case TYPE_START_FROM_FLATVIEW:
                    finish();
                    break;
            }
        }else{
            mPicScrollView.removePicViewAt(index);
            deleteOnePic(index);
            storeIndex(mOrderAdjustPicPaths);
            mPicScrollView.skipPicBy(0, false);
        }
    }
    /**
     * @param type CameraActivity的启动类型
     * 以不同的类型启动CamearaActivity
     */
    private void startCameraActivity(int type){
        Intent intent = new Intent(CommonExtraTag.START_CAMERA_ACTIVITY_ACTION);
        intent.putExtra(CommonExtraTag.CAMERA_ACTIVITY_TYPE, type);
        intent.putExtra(CommonExtraTag.CAMERA_ACTIVITY_PATH, mLoadPicFilePath.replace((getExternalFilesDir("").getPath() + "/"), ""));
        intent.putExtra(CommonExtraTag.SOURCE_ACTIVITY, CameraActivity.START_FROM_PICVIEW_ACTIVITY);
        switch(type){
            case CameraActivity.TYPE_NO_PREVIEW:
                break;
            case CameraActivity.TYPE_PREVIEW:
                intent.putExtra(CameraActivity.FROM_PICVIEW_ACTIVITY_PIC_COUNT, mOrderAdjustPicPaths.size());
                intent.putExtra(CameraActivity.FROM_PICVIEW_ACTIVITY_PIC, mOrderAdjustPicPaths.get(mOrderAdjustPicPaths.size() - 1));
                break;
            default:
                break;
        }
        startActivity(intent);
    }
    private void startImgProcessActivity(){
        String picPath = mOrderAdjustPicPaths.get(mPicScrollView.getCurrentIndex());
        String relativePath = picPath.replace(getExternalFilesDir("").getPath(), "");
        if (mOriginPicAccesser.isOriginPicExist(relativePath)){
            Intent intent = new Intent(CommonExtraTag.START_IMGPROCESS_ACTIVITY_ACTION);
            intent.putExtra(CommonExtraTag.IMGPROCESS_PIC_ABS_PATH, picPath);
            startActivityForResult(intent, START_IMGPROCESS_ACTIVITY);
        }else{
            Toast.makeText(this, R.string.no_originpic_error, Toast.LENGTH_SHORT).show();
        }

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == START_IMGPROCESS_ACTIVITY){
            if (resultCode == OPTION_UPATE_PROCESS){
                mOptionState = OPTION_UPATE_PROCESS;
                int currentPicViewIndex = mPicScrollView.getCurrentIndex();
                Thread thread = new Thread(() -> {
                    String picPath = mOrderAdjustPicPaths.get(currentPicViewIndex);
                    mNeedToUpdatePicPaths.add(picPath);
                    Bitmap pic = IOUtils.getAbridgeBitmap(picPath, VIEW_INSAMPLE_SIZE);
                    mPicScrollView.post(() -> {
                        mPicScrollView.getPicViewAt(currentPicViewIndex).getImageView().setImageBitmap(pic);
                    });
                });
                thread.start();
            }
        }
    }
    /**
     * 返回从CameraActivity启动时的结果。用作向CameraActivity返回预览图路径和图片数量，并通知CameraActivity更新相关信息。
     */
    private void setCameraActivityResult(){
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        bundle.putInt(CameraActivity.INTENT_Extra_FLAG_PIC_COUNT, mOrderAdjustPicPaths.size());
        String previewPicPath = mOrderAdjustPicPaths.get(mOrderAdjustPicPaths.size() - 1);
        bundle.putString(CameraActivity.INTENT_Extra_FLAG_PREVIEW_PIC, previewPicPath);
        intent.putExtras(bundle);
        setResult(CameraActivity.OPTION_RESET_PREVIEW_INFO, intent);
        //intent.putExtra(CameraActivity.INTENT_FLAG_PREVIEW_PIC, )
    }
    private void setFlatViewActivityResult(){
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        bundle.putStringArrayList(FlatViewActivity.UPDATE_PIC_PATHS, mNeedToUpdatePicPaths);
        intent.putExtras(bundle);
        setResult(FlatViewActivity.OPTION_UPDATE_PIC, intent);
    }
    /**
     * 主动按返回键时回调
     */
    @Override
    public void onBackPressed(){
        switch (mType){
            case TYPE_START_FROM_CAMERA:
                setCameraActivityResult();
                break;
            case TYPE_START_FROM_FLATVIEW:
                setFlatViewActivityResult();
                break;
        }
        super.onBackPressed();
    }
    @Override
    public void onResume(){
        super.onResume();
        //TestTool.printMapElement("mxrt", mPreferences.getAll());
    }
    @Override
    public void onRestart(){
        super.onRestart();
        getIntentExtraOnRestart();
        if (detectInsert()){
            loadNewAdd();
        }
    }
    /**
     * 设置Activity的标题
     */
    private void setActivityTitle(){
        mtoolbarTitle.setText(mTitle);
    }
    /**
     * 初始化底部菜单栏的按钮组
     */
    private void initFootbarButtonGroup(){
        switch (mType){
            case TYPE_START_FROM_CAMERA:
                mFootbarButtonGroup.addImageTextButtom(5);
                mFootbarButtonGroup.setAllImageViewVerticalBias(0.2f);
                mLeftInsertButton = mFootbarButtonGroup.getButtonAt(0);
                mLeftInsertButton.getImageView().setImageResource(R.drawable.left_insert_button);
                mLeftInsertButton.getTextView().setText("左侧插入");
                mLeftInsertButton.getTextView().setTextColor(Color.parseColor("black"));

                mRightInsertButton = mFootbarButtonGroup.getButtonAt(1);
                mRightInsertButton.getImageView().setImageResource(R.drawable.right_insert_button);
                mRightInsertButton.getTextView().setText("右侧插入");
                mRightInsertButton.getTextView().setTextColor(Color.parseColor("black"));

                mFootbarReplaceButton = mFootbarButtonGroup.getButtonAt(2);
                mFootbarReplaceButton.getImageView().setImageResource(R.drawable.replace_button);
                mFootbarReplaceButton.getTextView().setText("替换");
                mFootbarReplaceButton.getTextView().setTextColor(Color.parseColor("black"));

                mFootbarDeleteButton = mFootbarButtonGroup.getButtonAt(3);
                mFootbarDeleteButton.getImageView().setImageResource(R.drawable.delete_button);
                mFootbarDeleteButton.getTextView().setText("删除");
                mFootbarDeleteButton.getTextView().setTextColor(Color.parseColor("black"));
                //mFootbarButtonGroup.setAllTextViewTextSize(13);

                mFootbarCompleteButton = mFootbarButtonGroup.getButtonAt(4);
                mFootbarCompleteButton.getImageView().setImageResource(R.drawable.complete_button);
                mFootbarCompleteButton.getTextView().setText("完成");
                mFootbarCompleteButton.getTextView().setTextColor(Color.parseColor("black"));
                mFootbarButtonGroup.setAllTextViewTextSize(13);

                mLeftInsertButton.setOnClickListener((view) -> {
                    mState = STATE_LEFT_INSERT;
                    startCameraActivity(CameraActivity.TYPE_NO_PREVIEW);
                });
                mRightInsertButton.setOnClickListener((view) -> {
                    mState = STATE_RIGHT_INSERT;
                    startCameraActivity(CameraActivity.TYPE_NO_PREVIEW);
                });
                mFootbarReplaceButton.setOnClickListener((view) -> {
                    mState = STATE_REPLACE;
                    startCameraActivity(CameraActivity.TYPE_NO_PREVIEW);
                });

                mFootbarCompleteButton.setOnClickListener((view) -> {
                    setResult(CameraActivity.OPTION_FINISH_ACTIVITY, getIntent());
                    finish();
                });
                break;

            default:
                mFootbarButtonGroup.addImageTextButtom(5);
                mFootbarButtonGroup.setAllImageViewVerticalBias(0.2f);
                mLeftInsertButton = mFootbarButtonGroup.getButtonAt(0);
                mLeftInsertButton.getImageView().setImageResource(R.drawable.left_insert_button);
                mLeftInsertButton.getTextView().setText("左侧插入");
                mLeftInsertButton.getTextView().setTextColor(Color.parseColor("black"));
                mRightInsertButton = mFootbarButtonGroup.getButtonAt(1);
                mRightInsertButton.getImageView().setImageResource(R.drawable.right_insert_button);
                mRightInsertButton.getTextView().setText("右侧插入");
                mRightInsertButton.getTextView().setTextColor(Color.parseColor("black"));

                mContinueAddButton = mFootbarButtonGroup.getButtonAt(2);
                mContinueAddButton.getImageView().setImageResource(R.drawable.add_button);
                mContinueAddButton.getTextView().setText("继续添加");
                mContinueAddButton.getTextView().setTextColor(Color.parseColor("black"));

                mFootbarReplaceButton = mFootbarButtonGroup.getButtonAt(3);
                mFootbarReplaceButton.getImageView().setImageResource(R.drawable.replace_button);
                mFootbarReplaceButton.getTextView().setText("替换");
                mFootbarReplaceButton.getTextView().setTextColor(Color.parseColor("black"));
                mFootbarDeleteButton = mFootbarButtonGroup.getButtonAt(4);
                mFootbarDeleteButton.getImageView().setImageResource(R.drawable.delete_button);
                mFootbarDeleteButton.getTextView().setText("删除");
                mFootbarDeleteButton.getTextView().setTextColor(Color.parseColor("black"));
                mFootbarButtonGroup.setAllImageViewConstraintWidthPercent(0.9f);
                mFootbarButtonGroup.setAllTextViewConstraintWidthPercent(0.9f);
                mFootbarButtonGroup.setAllTextViewTextSize(13);

                mLeftInsertButton.setOnClickListener((view) -> {
                    mState = STATE_LEFT_INSERT;
                    startCameraActivity(CameraActivity.TYPE_NO_PREVIEW);
                });
                mRightInsertButton.setOnClickListener((view) -> {
                    mState = STATE_RIGHT_INSERT;
                    startCameraActivity(CameraActivity.TYPE_NO_PREVIEW);
                });
                mContinueAddButton.setOnClickListener((view) -> {
                    startCameraActivity(CameraActivity.TYPE_PREVIEW);
                    setFlatViewActivityResult();
                    finish();
                });
                mFootbarReplaceButton.setOnClickListener((view) -> {
                    mState = STATE_REPLACE;
                    startCameraActivity(CameraActivity.TYPE_NO_PREVIEW);
                });
                break;
        }
        mFootbarDeleteButton.setOnClickListener((view) -> {
            popDeleteHintPopWindow(mMaskView,
                    v -> {deleteCurrentIndexPic(mPicScrollView.getCurrentIndex());
                          mDeleteHintPopwindow.dismiss(); },
                    v -> {mDeleteHintPopwindow.dismiss();});
        });
    }
    /**
     * 检测PicviewActivity对应的图片目录下是否有新增图片
     */
    private boolean detectInsert(){
        mPicAbsPaths = IOUtils.getSpecSufixFilePaths(mLoadPicFilePath, new String[]{"jpg"});
        mPicAbsPaths.removeAll(mLastFileNames);
        if (mPicAbsPaths.isEmpty()){
            return false;
        }else{
            return true;
        }
    }
    /**
     * 加载并显示新增加的图片。
     */
    private void loadNewAdd(){
        String newFileName = mPicAbsPaths.get(0);//设计上保证了如果有新增，那么新增的图片肯定只有一张。detectInsert判断了是否有新增。
        int addIndex;
        switch(mState){
            case STATE_LEFT_INSERT:
                addIndex = mPicScrollView.getCurrentIndex();
                mPicScrollView.addPicView(1);
                mPicScrollView.getPicViewAt(addIndex);
                mPicScrollView.skipPicBy(addIndex - mPicScrollView.getCurrentIndex(), false);
                mOrderAdjustPicPaths.add(addIndex, newFileName);
                //loadAndSetPic(addIndex ,mOrderAdjustFileNames.size() - 1);
                mLastFileNames = Utils.copyList(mOrderAdjustPicPaths);
                storeIndex(mOrderAdjustPicPaths);
                mState = STATE_NORMAL;
                break;

            case STATE_RIGHT_INSERT:
                addIndex = mPicScrollView.getCurrentIndex() + 1;
                mPicScrollView.addPicView(1);
                mPicScrollView.getPicViewAt(addIndex).post(() -> {
                    mPicScrollView.skipPicBy(1, false);
                });
                mOrderAdjustPicPaths.add(addIndex, newFileName);
                //loadAndSetPic(addIndex ,mOrderAdjustFileNames.size() - 1);
                mLastFileNames = Utils.copyList(mOrderAdjustPicPaths);
                storeIndex(mOrderAdjustPicPaths);
                mState = STATE_NORMAL;
                break;

            case STATE_REPLACE:
                addIndex = mPicScrollView.getCurrentIndex();
                mOrderAdjustPicPaths.add(addIndex, newFileName);
                String fileNameDeleted = mOrderAdjustPicPaths.remove(addIndex + 1);
                mPreferenceEditor.remove(fileNameDeleted);
                mPreferenceEditor.commit();
                IOUtils.deleteDirectoryOrFile(fileNameDeleted);
                //loadAndSetPic(addIndex, mOrderAdjustFileNames.size()-1);
                mLastFileNames = Utils.copyList(mOrderAdjustPicPaths);
                storeIndex(mOrderAdjustPicPaths);
                mState = STATE_NORMAL;
                break;

            case STATE_NORMAL:
            default:
                addIndex = mOrderAdjustPicPaths.size() - 1;
                mPicScrollView.addPicView(1);
                mPicScrollView.getPicViewAt(addIndex);
                mPicScrollView.skipPicBy(addIndex - mPicScrollView.getCurrentIndex(), false);
                mOrderAdjustPicPaths.add(addIndex, newFileName);
                //loadAndSetPic(addIndex ,mOrderAdjustFileNames.size() - 1);
                mLastFileNames = Utils.copyList(mOrderAdjustPicPaths);
                storeIndex(mOrderAdjustPicPaths);
                break;
        }
        Thread thread = new Thread(() -> {
            loadAndSetPic(addIndex , mOrderAdjustPicPaths.size() - 1);
        });
        thread.start();
    }
    /**
     * @param picAbsPaths 图片的完整路径列表。列表的索引代表的就是图片的顺序
     * 更新SharedPreferences中保存的图片顺序
     */
    private void storeIndex(List<String> picAbsPaths){
        for (int i = 0; i < picAbsPaths.size(); i++){
            mPreferenceEditor.putInt(picAbsPaths.get(i), i);
            mPreferenceEditor.apply();
        }
        mPreferenceEditor.commit();
    }
    /**
     * 打开PicviewActivity时首次加载并显示所有图片
     */
    private void firstLoad(){
        mOrderAdjustPicPaths = getOrderAdjustedPicPaths(mPicAbsPaths);
        mLastFileNames = Utils.copyList(mPicAbsPaths);
        int firstIndex;
        switch(mType){
            case TYPE_START_FROM_CAMERA:
                firstIndex = mOrderAdjustPicPaths.size() - 1;
                mPicScrollView.getPicViewAt(firstIndex).post(() -> {
                    mPicScrollView.skipPicBy(firstIndex - mPicScrollView.getCurrentIndex(),false);
                });
                loadAndSetPic(firstIndex, mOrderAdjustPicPaths.size() - 1);
                break;

            default:
                firstIndex = mFirstLoadPicIndex;
                mPicScrollView.getPicViewAt(firstIndex).post(() -> {
                    mPicScrollView.skipPicBy(firstIndex - mPicScrollView.getCurrentIndex(),false);
                });
                loadAndSetPic(firstIndex, mOrderAdjustPicPaths.size() - 1);
                break;
        }
    }
    /**
     * @param centerIndex 最先加载显示的图片的索引。其余图片按左右依次的顺序进行加载显示
     * @param maxIndex 图片索引的最大值。
     * 加载并显示图片。可调节加载显示的顺序。
     */
    private void loadAndSetPic(int centerIndex, int maxIndex){
        List<Integer> order = Utils.generateSpecIndexList(centerIndex, maxIndex);
        //TestTool.printStringArray("mxrt", order.toArray());
        for (int i = 0; i < order.size(); i++){
            int orderIndex = order.get(i);
            String fileName = mOrderAdjustPicPaths.get(orderIndex);
            Bitmap pic;
            if (mFileNameToBitmap.containsKey(fileName)){
                pic = mFileNameToBitmap.get(fileName);
            }else{
                pic = IOUtils.getAbridgeBitmap(fileName, VIEW_INSAMPLE_SIZE);
                mFileNameToBitmap.put(fileName, pic);
            }
            setPic(orderIndex, pic);
        }
    }
    /**
     * @param index 在PicScrollView中的索引
     * @param pic 要显示的图片
     * 将图片设置到指定索引的PicView控件上。
     */
    private void setPic(int index, Bitmap pic){
        Message msg = Message.obtain();
        msg.arg1 = index;
        msg.obj = pic;
        mHandler.sendMessage(msg);
    }
    /**
     * @param index 图片在PicScrollView中的索引
     * 删除指定的图片。删除的是图片文件，不包括对控件的操作。
     */
    private void deleteOnePic(int index){
        String filePath = mOrderAdjustPicPaths.get(index);
        String relativePath = filePath.replace(getExternalFilesDir("").getPath(), "");
        mOriginPicAccesser.deleteOriginPic(relativePath);
        IOUtils.deleteDirectoryOrFile(filePath);
        String fileNameRemoved = mOrderAdjustPicPaths.remove(index);
        mPreferenceEditor.remove(fileNameRemoved);
        mPreferenceEditor.commit();
    }
    private void popDeleteHintPopWindow(View maskView, View.OnClickListener okListener, View.OnClickListener cancelListener){
        if (maskView != null){
            maskView.setVisibility(View.VISIBLE);
            mDeleteHintPopwindow.setOnDismissListener(() -> {
                maskView.setVisibility(View.GONE);
            });
        }
        Button okButton = mDeleteHintPopwindowRootView.findViewById(R.id.button_ok_delete_hint_popwindow);
        Button cancelButton = mDeleteHintPopwindowRootView.findViewById(R.id.button_cancel_delete_hint_popwindow);
        okButton.setOnClickListener(okListener);
        cancelButton.setOnClickListener(cancelListener);
        mDeleteHintPopwindow.setFocusable(true);
        mDeleteHintPopwindow.showAtLocation(mRootView, Gravity.BOTTOM, 0, 0);
    }
    /**
     * @param absFileNames PicviewActivity对应图片目录下的图片完整路径列表
     * 获取PicviewActivity对应图片目录下顺序经调整的图片完整路径列表。
     */
    private ArrayList<String> getOrderAdjustedPicPaths(List<String> absFileNames){
        ArrayList<String> resultList = new ArrayList<>();
        String[] orderAdjustedFileNamesArray = new String[absFileNames.size()];
        for (int i = 0; i < absFileNames.size(); i++){
            String absFileName = absFileNames.get(i);
            int order = mPreferences.getInt(absFileName, i);
            orderAdjustedFileNamesArray[order] = absFileName;
        }
        resultList.addAll(Arrays.asList(orderAdjustedFileNamesArray));
        return resultList;
    }

    private class UiHandler extends Handler{
        @Override
        public void handleMessage(Message msg){
            int index = msg.arg1;
            Bitmap pic = (Bitmap)msg.obj;
            mPicScrollView.getPicViewAt(index).post(() -> {
                mPicScrollView.getPicViewAt(index).getImageView().setImageBitmap(pic);
            });

        }
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        storeIndex(mOrderAdjustPicPaths);
        if (isDelLastPic){
            String preferencePath = "/data/data" + "/" + getPackageName() + "/" + "shared_prefs" +  mDir + ".xml";
            IOUtils.deleteDirectoryOrFile(preferencePath);
        }
    }
    private Mat getEdgeMat(Mat srcMat){
        Mat edgeMat = CvUtils.dilate(CvUtils.canny(CvUtils.equalizeHist(srcMat)));
        return edgeMat;
    }
    private Bitmap scanEdgePic(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        Mat grayedSrcMat = CvUtils.cvtColor(srcMat, Imgproc.COLOR_RGB2GRAY);
        Mat edgeMat = CvUtils.dilate(CvUtils.canny(CvUtils.equalizeHist(grayedSrcMat)));
        //Mat resultMat = CvLib.drawContours(edgeMat, srcMat);
        //int[] coo = CvLib.getMatCoor(resultMat);
        //Mat cropedMat = CvLib.sharp(CvLib.cropMat(srcMat, coo[0], coo[1], coo[2], coo[3]));
        //cropedMat = CvLib.cvtColor(cropedMat, Imgproc.COLOR_BGR2RGB);
        return CvUtils.matToBitmap(edgeMat);
    }
    private Bitmap scanSpecEdgePic(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        srcMat = CvUtils.cvtColor(srcMat, Imgproc.COLOR_BGRA2BGR);
        for (int i = 0; i < 1; i++){
            srcMat = CvUtils.meanShiftFilter(srcMat);
        }
        Mat grayedSrcMat = CvUtils.cvtColor(srcMat, Imgproc.COLOR_BGR2GRAY);
        Mat edgeMat = CvUtils.dilate(CvUtils.canny(CvUtils.equalizeHist(grayedSrcMat)));
        //Mat resultMat = CvLib.drawContours(edgeMat, srcMat);
        //int[] coo = CvLib.getMatCoor(resultMat);
        //Mat cropedMat = CvLib.sharp(CvLib.cropMat(srcMat, coo[0], coo[1], coo[2], coo[3]));
        //cropedMat = CvLib.cvtColor(cropedMat, Imgproc.COLOR_BGR2RGB);
        return CvUtils.matToBitmap(edgeMat);
    }
    private Bitmap scanContourPic(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        Mat edgeMat = CvUtils.dilate(CvUtils.canny(srcMat));
        Mat resultMat = CvUtils.drawContours(edgeMat, srcMat);
        //int[] coo = CvLib.getMatCoor(resultMat);
        //Mat cropedMat = CvLib.sharp(CvLib.cropMat(srcMat, coo[0], coo[1], coo[2], coo[3]));
        //cropedMat = CvLib.cvtColor(cropedMat, Imgproc.COLOR_BGR2RGB);
        return CvUtils.matToBitmap(resultMat);
    }
    private Bitmap scanSpecContourPic(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        srcMat = CvUtils.cvtColor(srcMat, Imgproc.COLOR_BGRA2BGR);
        for (int i = 0; i < 1; i++){
            srcMat = CvUtils.meanShiftFilter(srcMat);
        }
        Mat grayedSrcMat = CvUtils.cvtColor(srcMat, Imgproc.COLOR_BGR2GRAY);
        Mat edgeMat = CvUtils.dilate(CvUtils.canny(CvUtils.equalizeHist(grayedSrcMat)));
        Mat resultMat = CvUtils.drawContours(edgeMat, srcMat);
        //int[] coo = CvLib.getMatCoor(resultMat);
        //Mat cropedMat = CvLib.sharp(CvLib.cropMat(srcMat, coo[0], coo[1], coo[2], coo[3]));
        //cropedMat = CvLib.cvtColor(cropedMat, Imgproc.COLOR_BGR2RGB);
        return CvUtils.matToBitmap(resultMat);
    }
    private Bitmap perspectiveChangePic(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        Mat grayedSrcMat = CvUtils.cvtColor(srcMat, Imgproc.COLOR_BGR2GRAY);
        Mat edgeMat = CvUtils.dilate(CvUtils.canny(CvUtils.equalizeHist(grayedSrcMat)));
        edgeMat = getEdgeMat(edgeMat);
        Mat resultMat = CvUtils.drawContours(edgeMat, srcMat);
        int[] coo = CvUtils.getLTRBCoo(resultMat);
        Mat changedMat = CvUtils.perspectiveChange(srcMat, coo);
        //Mat cropedMat = CvLib.sharp(CvLib.cropMat(srcMat, coo[0], coo[1], coo[2], coo[3]));
        //cropedMat = CvLib.cvtColor(cropedMat, Imgproc.COLOR_BGR2RGB);
        return CvUtils.matToBitmap(changedMat);
    }
    private Bitmap scanCropPic(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        Mat edgeMat = CvUtils.dilate(CvUtils.canny(srcMat));
        Mat resultMat = CvUtils.drawContours(edgeMat, srcMat);
        int[] coo = CvUtils.getLTRBCoo(resultMat);
        Mat cropedMat = CvUtils.cropMat(srcMat, coo[0], coo[2], coo[1], coo[3]);
        cropedMat = CvUtils.cvtColor(cropedMat, Imgproc.COLOR_BGR2RGB);
        return CvUtils.matToBitmap(cropedMat);
    }
    private Bitmap scanCompletePic(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        Mat resultMat = CvUtils.sharp(srcMat);
        return CvUtils.matToBitmap(resultMat);
    }
    private Bitmap scanStep1(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        Mat bgrMat = CvUtils.cvtColor(srcMat, Imgproc.COLOR_RGB2BGR);
        Mat resultMat = CvUtils.threshold(CvUtils.light(CvUtils.contrast(CvUtils.gray(bgrMat), 1.2f), 20), 180,"black");
        return CvUtils.matToBitmap(resultMat);
    }
    private Bitmap scanStep2(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        Mat bgrMat = CvUtils.cvtColor(srcMat, Imgproc.COLOR_RGB2BGR);
        //Mat resultMat = CvUtils.contrast(CvUtils.light(CvUtils.gray(bgrMat), 20), 1.2f);
        Mat resultMat = CvUtils.redBlackProcess(srcMat);
        return CvUtils.matToBitmap(resultMat);
    }
    private Bitmap scanStep3(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        Mat bgrMat = CvUtils.cvtColor(srcMat, Imgproc.COLOR_RGB2BGR);
        Mat smoothMat = CvUtils.contrast(CvUtils.light(CvUtils.gray(bgrMat), 20), 1.2f);
        Mat resultMat = CvUtils.threshold(smoothMat, 200, "white");
        return CvUtils.matToBitmap(resultMat);
    }
    private Bitmap scanStep4(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        Mat bgrMat = CvUtils.cvtColor(srcMat, Imgproc.COLOR_RGB2BGR);
        Mat resultMat = CvUtils.dilate(CvUtils.threshold(CvUtils.light(CvUtils.contrast(CvUtils.gray(bgrMat), 1.2f), 20), 180,"black"));
        return CvUtils.matToBitmap(resultMat);
    }
    private Bitmap scanMeanShiftPic(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        srcMat = CvUtils.cvtColor(srcMat, Imgproc.COLOR_BGRA2BGR);
        for (int i = 0; i < 1; i++){
            srcMat = CvUtils.meanShiftFilter(srcMat);
        }
        //Mat resultMat = CvLib.drawContours(edgeMat, srcMat);
        //int[] coo = CvLib.getMatCoor(resultMat);
        //Mat cropedMat = CvLib.sharp(CvLib.cropMat(srcMat, coo[0], coo[1], coo[2], coo[3]));
        //cropedMat = CvLib.cvtColor(cropedMat, Imgproc.COLOR_BGR2RGB);
        return CvUtils.matToBitmap(srcMat);
    }
    private int[] getScanedCoo(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        //srcMat = CvLib.cvtColor(srcMat, Imgproc.COLOR_BGR2GRAY);
        Mat edgeMat = CvUtils.dilate(CvUtils.canny(srcMat));
        Mat resultMat = CvUtils.drawContours(edgeMat, srcMat);
        int[] coo4 = CvUtils.getLTRBCoo(resultMat);
        //int[] coo = CvUtils.getMinAreaCoo4(edgeMat, srcMat);
        return coo4;
    }

}

package com.xrt.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.xrt.R;
import com.xrt.thirdpartylib.cv.CvUtils;
import com.xrt.accesser.originpic.OriginPicAccesserFactory;
import com.xrt.accesser.originpic.OriginPicAccesser;
import com.xrt.constant.CommonExtraTag;
import com.xrt.tools.IOUtils;
import com.xrt.widget.FreeSelectImageView;
import com.xrt.widget.HorizonScrollRadioButton;

import org.opencv.core.Mat;

import java.util.Arrays;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

public class ImgProcessActivity extends AppCompatActivity {
    private OriginPicAccesser mOriginPicAccesser;
    private String mPicPath;
    private String mRelativePath;
    private ChangeBounds mChangeBounds;
    private Bitmap mOriginPic;
    private Bitmap mBlackRedProcessedPic;
    private Bitmap mBlackWhiteProcessedPic;
    private Bitmap mLighterProcessedPic;
    private Bitmap mProcessedPic;
    //view fields
    private ConstraintLayout mRootView;
    private Button mBackButton;
    private Button mCompleteButton;
    private Button mRotateButton;
    private ConstraintLayout mToolbar;
    private HorizonScrollRadioButton mProcessSelectBar;
    private Button mNextStepButton;
    private ProgressBar mLoadingPicProgressBar;
    private ConstraintLayout mCropFootbar;
    private FreeSelectImageView mFreeSelectImageView;
    //flag fields
    private boolean isProcessMode = false;
    private int mProcessType = TYPE_ORIGIN;
    //常量
    private static final int TYPE_ORIGIN = 1;
    private static final int TYPE_BLACK_RED = 2;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imgprocess);
        getIntentOnCreate();
        initNormalVar();
        initView();
        setViewsListener();
        Thread thread = new Thread(() -> {
            mOriginPic = mOriginPicAccesser.getOriginPic(mRelativePath, 1);
            int[] coo = getScanedCoo(mOriginPic);
            mFreeSelectImageView.post(() -> {
                mFreeSelectImageView.setImgWithSelectBox(mOriginPic, coo);
                mFreeSelectImageView.setVisibility(View.VISIBLE);
                mLoadingPicProgressBar.setVisibility(View.GONE);
                mCropFootbar.setVisibility(View.VISIBLE);
            });
        });
        thread.start();
    }
    private void getIntentOnCreate(){
        Intent intent = getIntent();
        String picPath = intent.getStringExtra(CommonExtraTag.IMGPROCESS_PIC_ABS_PATH);
        mPicPath = picPath;
        mRelativePath = picPath.replace(getExternalFilesDir("").getPath(), "");
    }
    private void initNormalVar(){
        mOriginPicAccesser = OriginPicAccesserFactory.createOriginImgAccesser(this);
        mChangeBounds = new ChangeBounds();
        mChangeBounds.setDuration(200);
    }
    private void initView(){
        mRootView = findViewById(R.id.rootview_activity_imgprocess);
        mToolbar = findViewById(R.id.toolbar_activity_imgprocess);
        mBackButton = findViewById(R.id.button_back_activity_imgprocess);
        mRotateButton = findViewById(R.id.button_rotate_activity_imgprocess);
        mCompleteButton = findViewById(R.id.button_complete_toolbar_activity_imgprocess);
        initProcessSelectBar();
        mNextStepButton = findViewById(R.id.button_nextstep_activity_imgprocess);
        mLoadingPicProgressBar = findViewById(R.id.progressbar_loadingimg_activity_imgprocess);
        mCropFootbar = findViewById(R.id.footbar_crop_activity_imgprocess);
        mFreeSelectImageView = findViewById(R.id.freeselectimageview_activity_imgprocess);
    }
    private int[] getScanedCoo(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        Mat edgeMat = CvUtils.dilate(CvUtils.canny(srcMat));
        Mat resultMat = CvUtils.drawContours(edgeMat, srcMat);
        int[] coo4 = CvUtils.getLTRBCoo(resultMat);
        return coo4;
    }
    private void initProcessSelectBar(){
        mProcessSelectBar = findViewById(R.id.horizonscrollradiobutton_process_activity_imgprocess);
        mProcessSelectBar.setItemLayout(R.layout.item_horizonradiobutton_activity_imgprocess);
        mProcessSelectBar.setSpecFirstClickPosition(0);
        mProcessSelectBar.setClickAffect(
                (rootView) -> {
                    TextView mask = rootView.findViewById(R.id.textview_mask_item_imgprocessbutton_activity_imgprocess);
                    mask.setVisibility(View.VISIBLE);
                }
        );
        mProcessSelectBar.setResetAffect(
                (rootView) -> {
                    TextView mask = rootView.findViewById(R.id.textview_mask_item_imgprocessbutton_activity_imgprocess);
                    mask.setVisibility(View.INVISIBLE);
                }
        );
    }
    private void setViewsListener(){
        mBackButton.setOnClickListener((v) -> {
            if (isProcessMode){
                quitProcessInterface();
                return;
            }
            finish();
        });
        mNextStepButton.setOnClickListener((v) -> {
            enterProcessInterface();
        });
        mCompleteButton.setOnClickListener((v) -> {
            if (mProcessedPic != null){
                IOUtils.saveImgFileWithJpg(mPicPath, mProcessedPic, 20);
                Log.d("mxrt", "setPic");
                setResult(PictureViewActivity.OPTION_UPATE_PROCESS, getIntent());
            }
            finish();
        });
    }
    private void changeImgProcessBarState(boolean isRestore){
        //ViewGroup.LayoutParams lp = mProcessSelectBar.getLayoutParams();
        if (isRestore){
            mProcessSelectBar.setVisibility(View.INVISIBLE);
        }else{
            ConstraintSet cs = new ConstraintSet();
            cs.clone(mRootView);
            cs.clear(mFreeSelectImageView.getId(), ConstraintSet.BOTTOM);
            cs.connect(mFreeSelectImageView.getId(), ConstraintSet.BOTTOM, mProcessSelectBar.getId(), ConstraintSet.TOP);
            cs.applyTo(mRootView);
            mProcessSelectBar.setVisibility(View.VISIBLE);
        }
    }
    private void changeCropFootBarState(boolean isRestore){
        if (isRestore){
            ConstraintSet cs = new ConstraintSet();
            cs.clone(mRootView);
            cs.clear(mFreeSelectImageView.getId(), ConstraintSet.BOTTOM);
            cs.connect(mFreeSelectImageView.getId(), ConstraintSet.BOTTOM, mCropFootbar.getId(), ConstraintSet.TOP);
            cs.applyTo(mRootView);
            mCropFootbar.setVisibility(View.VISIBLE);
        }else{
            mCropFootbar.setVisibility(View.GONE);
        }
    }
    private void changeCompleteButtonState(boolean isRestore){
        if (isRestore){
            mCompleteButton.setVisibility(View.INVISIBLE);
        }else{
            mCompleteButton.setVisibility(View.VISIBLE);
        }
    }
    private void enterProcessInterface(){
        if (!isProcessMode){
            Bitmap cropedPic = cropOriginPic();
            updateProcessBar(cropedPic);
            //TransitionManager.beginDelayedTransition(mRootView, mChangeBounds);
            changeCropFootBarState(false);
            changeImgProcessBarState(false);
            changeCompleteButtonState(false);
            isProcessMode = true;
        }
    }
    private void updateProcessBar(Bitmap cropedPic){
        Matrix matrix = new Matrix();
        matrix.postScale(0.2f, 0.2f);
        Bitmap thumb = Bitmap.createBitmap(cropedPic, 0, 0, cropedPic.getWidth(), cropedPic.getHeight(), matrix,true);
        String[] labels = new String[]{"原图", "白底黑字红章", "白底黑字", "増亮"};
        Bitmap[] previewPics = new Bitmap[]{thumb, blackRedProcess(thumb), blackWhiteProcess(thumb), lighterProcess(thumb)};
        mProcessedPic = cropedPic;
        mProcessSelectBar.update(4, Arrays.asList(
                () -> {
                    mFreeSelectImageView.setCropedImg(cropedPic);},
                () -> {
                    if (mBlackRedProcessedPic == null){
                        mBlackRedProcessedPic = blackRedProcess(cropedPic);
                    }
                    mProcessedPic = mBlackRedProcessedPic;
                    mFreeSelectImageView.setCropedImg(mBlackRedProcessedPic);
                },
                () -> {
                    if (mBlackWhiteProcessedPic == null){
                        mBlackWhiteProcessedPic = blackWhiteProcess(cropedPic);
                    }
                    mProcessedPic = mBlackWhiteProcessedPic;
                    mFreeSelectImageView.setCropedImg(mBlackWhiteProcessedPic);
                },
                () -> {
                    if (mLighterProcessedPic == null){
                        mLighterProcessedPic = lighterProcess(cropedPic);
                    }
                    mProcessedPic = mLighterProcessedPic;
                    mFreeSelectImageView.setCropedImg(mLighterProcessedPic);
                }
        ), (view, position) -> {
            ImageView imageView = view.findViewById(R.id.imageview_item_imgprocessbutton_activity_imgprocess);
            imageView.setImageBitmap(previewPics[position]);
            TextView textView = view.findViewById(R.id.textview_item_imgprocessbutton_activity_imgprocess);
            textView.setText(labels[position]);
        });
    }
    private Bitmap lighterProcess(Bitmap pic){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        Mat resultMat = CvUtils.lighterProcess(srcMat);
        return CvUtils.matToBitmap(resultMat);
    }
    private Bitmap blackRedProcess(Bitmap pic) {
        Mat srcMat = CvUtils.bitmapToMat(pic);
        Mat resultMat = CvUtils.redBlackProcess(srcMat);
        return CvUtils.matToBitmap(resultMat);
    }
    private Bitmap blackWhiteProcess(Bitmap pic) {
        Mat srcMat = CvUtils.bitmapToMat(pic);
        Mat resultMat = CvUtils.blackWhiteProcess(srcMat);
        return CvUtils.matToBitmap(resultMat);
    }
    private Bitmap cropOriginPic(){
        Bitmap cropedPic = mFreeSelectImageView.getCropedImg();
        mFreeSelectImageView.setCropedImg(cropedPic);
        return cropedPic;
    }
    private void resetToCrop(){
        mFreeSelectImageView.setImgWithSelectBoxReback(mOriginPic);
        mBlackRedProcessedPic = null;
        mBlackWhiteProcessedPic = null;
        mLighterProcessedPic = null;
        mProcessSelectBar.reset();
    }
    private void quitProcessInterface(){
        if (isProcessMode){
            resetToCrop();
            //TransitionManager.beginDelayedTransition(mRootView, mChangeBounds);
            changeCropFootBarState(true);
            changeImgProcessBarState(true);
            changeCompleteButtonState(true);
            isProcessMode = false;
        }

    }



}

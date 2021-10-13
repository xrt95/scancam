package com.xrt.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

/*
 * <p>ImageView,TextView形成一个ImageTextButton。再由多个ImageTextButton在水平方向组合成一个HorizonImageTextButtonGroup。</P>
 * <p>ImageView和TextView都支持隐藏，宽高调整</p>
 */
public class HorizonImageTextButtonGroup extends ViewGroup {
    private static final String TAG = "mXrt";
    private float mWidthMarginPercent = 0f;//整个ButtonGroup和父View之间的左右margin占父View宽度的百分比
    private float mHeightMarginPercent = 0f;//整个ButtonGroup和父View之间的上下margin占父View高度的百分比
    private float mEachWidthPaddingPercent = 0f;//ButtonGroup中每个Button的左右padding占自己原始宽度的百分比
    private float mEachHeightPaddingPercent = 0f;//ButtonGroup中每个Button的上下padding占自己原始高度的百分比
    private List<ImageTextButton> mImageTextButtonList = new ArrayList<>();
    private Context mContext;

    //onMeasure和onLayout共享的变量
    private int mWidth;
    private int mHeight;
    private int mAfterMarginWidth;
    private int mAfterMarginHeight;
    private int mEachChildOriginWidth;
    private int mEachChildOriginHeight;
    private int mEachChildWidth;
    private int mEachChildHeight;

    public HorizonImageTextButtonGroup(Context context, AttributeSet attr){
        super(context, attr);
        mContext = context;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        //TestTool.anlysMeasureSpec(TAG, widthMeasureSpec, heightMeasureSpec);
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        int childCount = getChildCount();
        mAfterMarginWidth = (int)(mWidth * (1 - mWidthMarginPercent * 2));
        mAfterMarginHeight = (int)(mHeight * (1 - mHeightMarginPercent * 2));
        mEachChildOriginWidth = childCount == 0 ? 0 : (int)(mAfterMarginWidth / childCount);
        mEachChildOriginHeight = mAfterMarginHeight;
        mEachChildWidth = (int)(mEachChildOriginWidth * (1 - mEachWidthPaddingPercent * 2));
        mEachChildHeight = (int)(mEachChildOriginHeight * (1 - mEachHeightPaddingPercent * 2));
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(mEachChildWidth, MeasureSpec.EXACTLY);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(mEachChildHeight, MeasureSpec.EXACTLY);
        for (int i = 0; i < childCount; i++){
            View child = getChildAt(i);
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }
        setMeasuredDimension(mWidth, mHeight);
    }
    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b){
        int childCount = getChildCount();
        int widthMargin = (int)(mWidth * mWidthMarginPercent);
        int heightMargin = (int)(mHeight * mHeightMarginPercent);
        int eachChildWidthMargin = (int)(mEachChildOriginWidth * mEachWidthPaddingPercent);
        int eachChildHeightMargin = (int)(mEachChildOriginHeight * mEachHeightPaddingPercent);
        int widthUsed = widthMargin;
        int heightUsed = heightMargin + eachChildHeightMargin;
        View child;
        for (int i = 0; i < childCount; i++){
            widthUsed += eachChildWidthMargin;
            child = getChildAt(i);
            child.layout(widthUsed, heightUsed, widthUsed + mEachChildWidth, heightUsed + mEachChildHeight);
            widthUsed += eachChildWidthMargin + mEachChildWidth;
        }

    }
    public float getWidthMarginPercent(){
        return mWidthMarginPercent;
    }
    public void setWidthMarginPercent(float widthMarginPercent){
        mWidthMarginPercent = widthMarginPercent;
    }
    public float getHeightMarginPercent(){
        return mHeightMarginPercent;
    }
    public void setHeightMarginPercent(float heightMarginPercent){
        mHeightMarginPercent = heightMarginPercent;
    }
    public float getEachWidthPaddingPercent(){
        return mEachWidthPaddingPercent;
    }
    public void setEachWidthPaddingPercent(float eachWidthMarginPercent){
        mEachWidthPaddingPercent = eachWidthMarginPercent;
    }
    public float getEachHeightPaddingPercent() {
        return mEachHeightPaddingPercent;
    }
    public void setEachHeightPaddingPercent(float eachHeightMarginPecent){
        mEachHeightPaddingPercent = eachHeightMarginPecent;
    }
    public void setAllImageViewGone(){
        for (HorizonImageTextButtonGroup.ImageTextButton button : mImageTextButtonList){
            button.getImageView().setVisibility(View.GONE);
        }
    }
    public void setAllTextViewGone(){
        for (HorizonImageTextButtonGroup.ImageTextButton button : mImageTextButtonList){
            button.getTextView().setVisibility(View.GONE);
        }
    }
    public void setAllTextViewConstraintHeightPercent(float percent){
        for (HorizonImageTextButtonGroup.ImageTextButton button : mImageTextButtonList){
            button.setTextViewHeightPercent(percent);
        }
    }
    public void setAllTextViewConstraintWidthPercent(float percent){
        for (HorizonImageTextButtonGroup.ImageTextButton button : mImageTextButtonList){
            button.setTextViewWidthPercent(percent);
        }
    }
    public void setAllTextViewHorizonBias(float percent){
        for (HorizonImageTextButtonGroup.ImageTextButton button : mImageTextButtonList){
            button.setTextviewHorizonBias(percent);
        }
    }
    public void setAllTextViewVerticalBias(float percent){
        for (HorizonImageTextButtonGroup.ImageTextButton button : mImageTextButtonList){
            button.setTextviewVerticalBias(percent);
        }
    }
    public void setAllImageViewConstraintHeightPercent(float percent){
        for (HorizonImageTextButtonGroup.ImageTextButton button : mImageTextButtonList){
            button.setImageViewHeightPercent(percent);
        }
    }
    public void setAllImageViewConstraintWidthPercent(float percent){
        for (HorizonImageTextButtonGroup.ImageTextButton button : mImageTextButtonList){
            button.setImageViewWidthPercent(percent);
        }
    }
    public void setAllImageViewHorizonBias(float percent){
        for (HorizonImageTextButtonGroup.ImageTextButton button : mImageTextButtonList){
            button.setImageViewHorizonBias(percent);
        }
    }
    public void setAllImageViewVerticalBias(float percent){
        for (HorizonImageTextButtonGroup.ImageTextButton button : mImageTextButtonList){
            button.setImageViewVerticalBias(percent);
        }
    }
    public void setAllTextViewTextSize(float sp){
        for (HorizonImageTextButtonGroup.ImageTextButton button : mImageTextButtonList){
            button.setTextSize(sp);
        }
    }
    public void addImageTextButtom(int count){
        for (int i = 0; i < count; i++){
            ImageTextButton imageTextButton = new ImageTextButton(mContext);
            addView(imageTextButton);
            mImageTextButtonList.add(imageTextButton);
        }
    }
    public int getButtonCount(){
        return mImageTextButtonList.size();
    }
    public ImageTextButton getButtonAt(int i){
        return mImageTextButtonList.get(i);
    }

    public class ImageTextButton extends ConstraintLayout{
        private static final float IMAGE_WIDTH_PERCENT = 1f;
        private static final float IMAGE_HEIGHT_PERCENT = 0.5f;
        private static final float TEXT_WIDTH_PERCENT = 1f;
        private static final float TEXT_HEIGHT_PERCENT = 0.5f;
        private static final float IMAGE_VERTICAL_BIAS = 0f;
        private static final float TEXT_VERTICAL_BIAS = 0f;
        private ImageView mImageView;
        private TextView mTextView;

        public ImageTextButton(Context context){
            super(context);
            mImageView = new ImageView(context);
            mTextView = new TextView(context);
            init();
        }
        private void init(){
            mTextView.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            mImageView.setId(generateViewId());
            mTextView.setId(generateViewId());
            addView(mImageView);
            addView(mTextView);
            initConstraint();
        }
        private void initConstraint(){
            ConstraintSet cs = new ConstraintSet();
            cs.connect(mImageView.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
            cs.connect(mImageView.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
            cs.connect(mImageView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            cs.connect(mImageView.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            cs.constrainPercentWidth(mImageView.getId(), IMAGE_WIDTH_PERCENT);
            cs.constrainPercentHeight(mImageView.getId(), IMAGE_HEIGHT_PERCENT);
            cs.setVerticalBias(mImageView.getId(), IMAGE_VERTICAL_BIAS);
            cs.connect(mTextView.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
            cs.connect(mTextView.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
            cs.connect(mTextView.getId(), ConstraintSet.TOP, mImageView.getId(), ConstraintSet.BOTTOM);
            cs.connect(mTextView.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            cs.constrainPercentWidth(mTextView.getId(), TEXT_WIDTH_PERCENT);
            cs.constrainPercentHeight(mTextView.getId(), TEXT_HEIGHT_PERCENT);
            cs.setVerticalBias(mTextView.getId(), TEXT_VERTICAL_BIAS);
            cs.applyTo(this);

        }
        public ImageView getImageView(){
            return mImageView;
        }
        public TextView getTextView(){
            return mTextView;
        }
        public void setTextSize(float sp){
            mTextView.setTextSize(sp);
        }
        public void setTextViewHeightPercent(float percent){
            ConstraintSet cs = new ConstraintSet();
            cs.clone(this);
            cs.constrainPercentHeight(mTextView.getId(), percent);
            cs.applyTo(this);
        }
        public void setTextViewWidthPercent(float percent){
            ConstraintSet cs = new ConstraintSet();
            cs.clone(this);
            cs.constrainPercentWidth(mTextView.getId(), percent);
            cs.applyTo(this);
        }
        public void setTextviewHorizonBias(float percent){
            ConstraintSet cs = new ConstraintSet();
            cs.clone(this);
            cs.setHorizontalBias(mTextView.getId(), percent);
            cs.applyTo(this);
        }
        public void setTextviewVerticalBias(float percent){
            ConstraintSet cs = new ConstraintSet();
            cs.clone(this);
            cs.setVerticalBias(mTextView.getId(), percent);
            cs.applyTo(this);
        }
        public void setImageViewHeightPercent(float percent){
            ConstraintSet cs = new ConstraintSet();
            cs.clone(this);
            cs.constrainPercentHeight(mImageView.getId(), percent);
            cs.applyTo(this);
        }
        public void setImageViewWidthPercent(float percent){
            ConstraintSet cs = new ConstraintSet();
            cs.clone(this);
            cs.constrainPercentWidth(mImageView.getId(), percent);
            cs.applyTo(this);
        }
        public void setImageViewHorizonBias(float percent){
            ConstraintSet cs = new ConstraintSet();
            cs.clone(this);
            cs.setHorizontalBias(mImageView.getId(), percent);
            cs.applyTo(this);
        }
        public void setImageViewVerticalBias(float percent){
            ConstraintSet cs = new ConstraintSet();
            cs.clone(this);
            cs.setVerticalBias(mImageView.getId(), percent);
            cs.applyTo(this);
        }

    }
}

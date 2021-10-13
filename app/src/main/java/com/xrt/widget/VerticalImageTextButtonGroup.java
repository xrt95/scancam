package com.xrt.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

/*
 * <p>ImageView,TextView形成一个ImageTextButton。再由多个ImageTextButton在垂直方向组合成一个VerticalImageTextButtonGroup。</P>
 * <p>ImageView和TextView都支持隐藏，宽高调整</p>
 */
public class VerticalImageTextButtonGroup extends ViewGroup {
    private Context mContext;
    private List<ImageTextButton> mImageTextButtonList = new ArrayList<>();
    private int mWidth;
    private int mHeight;
    private int mEachChildWidth;
    private int mEachChildHeight;

    public VerticalImageTextButtonGroup(Context context, AttributeSet attr){
        super(context, attr);
        mContext = context;
    }
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        int childCount = getChildCount();
        mEachChildWidth = mWidth;
        mEachChildHeight = childCount == 0 ? 0 : (int)(mHeight / childCount);
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
        int heightUsed = 0;
        for (int i = 0; i < childCount; i++){
            View child = getChildAt(i);
            child.layout(0, heightUsed, mEachChildWidth, heightUsed + mEachChildHeight);
            heightUsed += mEachChildHeight;
        }
    }
    public void addImageTextButton(int count){
        for (int i = 0; i < count; i++){
            ImageTextButton imageTextButton = new ImageTextButton(mContext);
            mImageTextButtonList.add(imageTextButton);
            addView(imageTextButton);
        }
    }
    public void setAllImageViewGone(){
        for (ImageTextButton button : mImageTextButtonList){
            button.getImageView().setVisibility(View.GONE);
        }
    }
    public void setAllTextViewGone(){
        for (ImageTextButton button : mImageTextButtonList){
            button.getTextView().setVisibility(View.GONE);
        }
    }
    public void setAllTextViewConstraintHeightPercent(float percent){
        for (ImageTextButton button : mImageTextButtonList){
            button.setTextViewHeightPercent(percent);
        }
    }
    public void setAllTextViewConstraintWidthPercent(float percent){
        for (ImageTextButton button : mImageTextButtonList){
            button.setTextViewWidthPercent(percent);
        }
    }
    public void setAllTextViewHorizonBias(float percent){
        for (ImageTextButton button : mImageTextButtonList){
            button.setTextviewHorizonBias(percent);
        }
    }
    public void setAllImageViewConstrainHeightPercent(float percent){
        for (ImageTextButton button : mImageTextButtonList){
            button.setImageViewHeightPercent(percent);
        }
    }
    public void setAllImageViewConstraintWidthPercent(float percent){
        for (ImageTextButton button : mImageTextButtonList){
            button.setImageViewWidthPercent(percent);
        }
    }
    public void setAllImageViewHorizonBias(float percent){
        for (ImageTextButton button : mImageTextButtonList){
            button.setImageViewHorizonBias(percent);
        }
    }
    public int getButtonCount(){
        return mImageTextButtonList.size();
    }
    public ImageTextButton getButtonAt(int index){
        return mImageTextButtonList.get(index);
    }

    public class ImageTextButton extends ConstraintLayout {
        private static final float IMAGEVIEW_HEIGHT_PERCENT = 0.8f;
        private static final float IMAGEVIEW_WIDTH_PERCENT = 0.2f;
        private static final float IMAGEVIEW_HORIZON_BIAS = 0.05f;
        private static final float TEXTVIEW_HEIGHT_PERCENT = 0.6f;
        private static final float TEXTVIEW_WIDTH_PERCENT = 0.6f;
        private static final float TEXTVIEW_HORIZON_BIAS = 0f;

        private ImageView mImageView;
        private TextView mTextView;

        public ImageTextButton(Context context){
            super(context);
            mImageView = new ImageView(context);
            mTextView = new TextView(context);
            init();
        }
        private void init(){
            mImageView.setId(generateViewId());
            mTextView.setId(generateViewId());
            mTextView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            mTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            addView(mImageView);
            addView(mTextView);
            initConstraint();
        }
        private void initConstraint(){
            ConstraintSet cs = new ConstraintSet();
            cs.connect(mImageView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            cs.connect(mImageView.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            cs.connect(mImageView.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
            cs.connect(mImageView.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
            cs.constrainPercentHeight(mImageView.getId(), IMAGEVIEW_HEIGHT_PERCENT);
            cs.constrainPercentWidth(mImageView.getId(), IMAGEVIEW_WIDTH_PERCENT);
            cs.setHorizontalBias(mImageView.getId(), IMAGEVIEW_HORIZON_BIAS);
            cs.connect(mTextView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            cs.connect(mTextView.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            cs.connect(mTextView.getId(), ConstraintSet.LEFT, mImageView.getId(), ConstraintSet.RIGHT);
            cs.connect(mTextView.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
            cs.constrainPercentHeight(mTextView.getId(), TEXTVIEW_HEIGHT_PERCENT);
            cs.constrainPercentWidth(mTextView.getId(), TEXTVIEW_WIDTH_PERCENT);
            cs.setHorizontalBias(mTextView.getId(), TEXTVIEW_HORIZON_BIAS);
            cs.applyTo(this);
        }
        public ImageView getImageView(){
            return mImageView;
        }
        public TextView getTextView(){
            return mTextView;
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


    }
}

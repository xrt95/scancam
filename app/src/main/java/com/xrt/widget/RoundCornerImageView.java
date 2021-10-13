package com.xrt.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;

import com.xrt.R;

import androidx.appcompat.widget.AppCompatImageView;

public class RoundCornerImageView extends AppCompatImageView {
    private int mRadius;
    private int mLeftTopRadius;
    private int mRightTopRadius;
    private int mLeftBottomRadius;
    private int mRightBottomRadius;
    private int mDefaultRadius;
    private String mCornerBackgroundColor="#00000000";
    private Path mPath;
    private int mWidth;
    private int mHeight;

    public RoundCornerImageView(Context context, AttributeSet attr){
        super(context, attr);
        getStyledAttr(context, attr);
    }
    private void getStyledAttr(Context context, AttributeSet attr){
        TypedArray typedArray = context.obtainStyledAttributes(attr, R.styleable.RoundCornerImageView);
        mRadius = typedArray.getDimensionPixelOffset(R.styleable.RoundCornerImageView_radius, mDefaultRadius);
        mLeftTopRadius = typedArray.getDimensionPixelOffset(R.styleable.RoundCornerImageView_leftTopRadius, mDefaultRadius);
        mRightTopRadius = typedArray.getDimensionPixelOffset(R.styleable.RoundCornerImageView_rightTopRadius, mDefaultRadius);
        mLeftBottomRadius = typedArray.getDimensionPixelOffset(R.styleable.RoundCornerImageView_leftBottomRadius, mDefaultRadius);
        mRightBottomRadius = typedArray.getDimensionPixelOffset(R.styleable.RoundCornerImageView_rightBottomRadius, mDefaultRadius);
        if (mRadius != mDefaultRadius){
            mLeftTopRadius = mRightTopRadius = mLeftBottomRadius = mRightBottomRadius = mRadius;
        }
        mCornerBackgroundColor = typedArray.getString(R.styleable.RoundCornerImageView_cornerBackgroundColor);
        typedArray.recycle();

    }
    private void initPath(int width, int height){
        if (width != mWidth || height != mHeight){
            mPath = new Path();
            mPath.moveTo(mLeftTopRadius, 0);
            mPath.lineTo(width - mRightTopRadius, 0);
            mPath.quadTo(width, 0, width, mRightTopRadius);
            mPath.lineTo(width, height - mRightBottomRadius);
            mPath.quadTo(width, height, width - mRightBottomRadius, height);
            mPath.lineTo(mLeftBottomRadius, height);
            mPath.quadTo(0, height, 0, height - mLeftBottomRadius);
            mPath.lineTo(0, mLeftTopRadius);
            mPath.quadTo(0, 0, mLeftTopRadius, 0);
            mWidth = width;
            mHeight = height;
            //canvas.drawColor(Color.parseColor(mCornerBackgroundColor));
        }
    }
    @Override
    public void onDraw(Canvas canvas){
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        initPath(width, height);
        canvas.clipPath(mPath);
        super.onDraw(canvas);

    }
}

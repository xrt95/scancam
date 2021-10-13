package com.xrt.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.xrt.R;

public class PointDownHintView extends View implements View.OnTouchListener{
    private Paint mPen = new Paint();
    private Path mPath = new Path();
    private float mRadius;
    private float mCenterX;
    private float mCentetY;

    public PointDownHintView(Context context, AttributeSet attr){
        super(context, attr);
        init(context);
    }
    public PointDownHintView(Context context){
        super(context);
        init(context);
    }
    private void init(Context context){
        setOnTouchListener(this);
        mRadius = context.getResources().getDimensionPixelSize(R.dimen.hintpoint_pointdownhintview_width);
        mCenterX = -mRadius;
        mCentetY = -mRadius;
        initPen();
    }
    private void initPen(){
        mPen.setAntiAlias(true);
        mPen.setStyle(Paint.Style.STROKE);
        mPen.setStrokeWidth(4);
        mPen.setColor(Color.WHITE);
    }
    private void initRectPath(){
        mPath.reset();
    }
    private void initOvalPath(){
        mPath.reset();
        mPath.addCircle(mCenterX, mCentetY, mRadius, Path.Direction.CW);
        mPath.addCircle(mCenterX, mCentetY, mRadius / 10, Path.Direction.CW);
    }
    @Override
    public boolean onTouch(View view, MotionEvent event){
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                mCenterX = event.getX();
                mCentetY = event.getY();
                invalidate();
                break;
        }
        return false;
    }
    @Override
    public void onDraw(Canvas canvas){
        initOvalPath();
        canvas.drawPath(mPath, mPen);
    }

}

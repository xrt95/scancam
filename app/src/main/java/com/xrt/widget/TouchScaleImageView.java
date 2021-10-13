package com.xrt.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;

import com.xrt.tools.TestTool;

import androidx.appcompat.widget.AppCompatImageView;

public class TouchScaleImageView extends AppCompatImageView implements View.OnTouchListener {
    private GestureDetector mGesture;
    private ScaleGestureDetector mScaleGesture;
    private Matrix mMatrix = new Matrix();
    private OnTapUpListener mOnTapUpListener;
    private float mInitScaleFactor = 0;
    private float mLastDownX;
    private float mLastDownY;
    private boolean isLeftClamp = false;
    private boolean isRightClamp = false;
    private boolean isBlockEvent = false;
    private int mBanState;
    private static final int NO_BAN = 1;
    private static final int LEFT_BAN = 2;
    private static final int RIGHT_BAN = 3;
    private static final int ALL_BAN = 4;

    public TouchScaleImageView(Context context, AttributeSet attr){
        super(context, attr);
        init(context);
    }
    public TouchScaleImageView(Context context){
        super(context);
       init(context);
    }
    private void init(Context context){
        mGesture = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){
            @Override
            /*
             * 图片跟随手指移动
             */
            public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY){//distance代表的是视窗的移动距离。跟手指的移动方向是相反的。
                //Log.d("mxrt", "dx:" +distanceX +"dy:" + distanceY);
                RectF picBounds = getPicBounds();
                float actuallyDistance;
                if (distanceY < 0){//这段逻辑负责图片跟随手指向下移动。移动条件是满足了图片超出了ImageView的上边缘才能向下移动。
                    if (picBounds.top < 0){
                        actuallyDistance = Math.min(Math.abs(picBounds.top), Math.abs(distanceY));
                        mMatrix.postTranslate(0, actuallyDistance);
                    }
                }else{//这段逻辑负责图片跟随手指向上移动。移动条件是满足了图片超出了ImageView的下边缘才向上移动。
                    if (picBounds.bottom > getHeight()){
                        actuallyDistance = Math.min(Math.abs(picBounds.bottom - getHeight()), Math.abs(distanceY));
                        mMatrix.postTranslate(0, -actuallyDistance);
                    }
                }
                if (distanceX < 0){//这段逻辑负责图片跟随手指向右移动。移动条件是满足了图片超出了ImageView的左边界时才向右移动。
                    if (picBounds.left < 0){
                        actuallyDistance = Math.min(Math.abs(picBounds.left), Math.abs(distanceX));
                        mMatrix.postTranslate(actuallyDistance, 0);
                    }else{//这里的逻辑是视窗还要往左移但图片已经没有超出边界的话，就启动PicScrollView的事件拦截。此时PicScrollView会重新滑动。
                        getParent().requestDisallowInterceptTouchEvent(false);
                        if (picBounds.right > getWidth()){
                            isLeftClamp = true;
                        }
                    }
                }else{//这段逻辑负责图片跟随手指向左移动。移动条件是满足了图片超出了ImageView的右边界时才向左移动。
                    if (picBounds.right > getWidth()){
                        actuallyDistance = Math.min(Math.abs(picBounds.right - getWidth()), Math.abs(distanceX));
                        mMatrix.postTranslate(-actuallyDistance, 0);
                    }else {//这里的逻辑是视窗还要往右移但图片已经没有超出右边界的话，就启动PicScrollView的事件拦截。此时PicScrollView会重新滑动。
                        getParent().requestDisallowInterceptTouchEvent(false);
                        if (picBounds.left < 0){
                            isRightClamp = true;
                        }
                    }
                }
                setScaleType(ScaleType.MATRIX);
                setImageMatrix(mMatrix);
                return false;
            }
            @Override
            public boolean onSingleTapUp(MotionEvent event){
                //增加isBlockEvent标志位的判断是为了防止双指缩放时总是触发这里的回调。
                if (mOnTapUpListener != null && !isBlockEvent){
                    mOnTapUpListener.onTapUp();
                }
                return false;
            }
            @Override
            public boolean onDoubleTap(MotionEvent event){
                restore();
                return false;
            }
        });
        mScaleGesture = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            /*
             * 图片跟随双指缩放
             */
            public boolean onScale(ScaleGestureDetector detector) {
                //Log.d("mxrt", "onScale");
                float scaleFactor = detector.getScaleFactor();
                //Log.d("mxrt", "scaleFactor:" + scaleFactor);
                mMatrix.postTranslate(getAdjustDistanceX(), getAdjustDistanceY());
                mMatrix.postScale( scaleFactor,  scaleFactor, detector.getFocusX(), detector.getFocusY());
                setScaleType(ScaleType.MATRIX);
                setImageMatrix(mMatrix);
                return true;
            }
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }
            @Override
            /*
             * 图片一旦缩小至比FIT_CENTER状态小，就恢复成FIT_CENTER状态。
             */
            public void onScaleEnd(ScaleGestureDetector detector) {
                float[] matrixValues = getMatrixValues(mMatrix);
                float scaleX = matrixValues[Matrix.MSCALE_X];
                float scaleY = matrixValues[Matrix.MSCALE_Y];
                if (scaleX < mInitScaleFactor || scaleY < mInitScaleFactor){
                    restore();
                }
            }
        });
        setOnTouchListener(this);
    }

    /*
     * 判断图片是否可以移动。根据需要将图片是否可以移动分为了四种情况。
     * 1、LEFT_BAN: 如果图片左边触边，右边没触边。
     * 2、RIGHT_BAN: 如果图片右边触边，左边没触边。
     * 3、ALL_BAN: 图片左右都触边。
     * 4、NO_BAN: 左右都没触边。
     */
    private int canScroll(){
        RectF picBounds = getPicBounds();
        //TestTool.printRectF("mxrt", picBounds);
        float left = picBounds.left;
        float top = picBounds.top;
        float right = picBounds.right;
        float bottom = picBounds.bottom;
        if (left > 0){
            if (right >= getWidth()){
                return LEFT_BAN;
            }else {
                return ALL_BAN;
            }
        }
        if (right < getWidth()){
            if (left <= 0){
                return RIGHT_BAN;
            }else{
                return ALL_BAN;
            }
        }
        return NO_BAN;
    }
    /*
     * 获取图片在缩放过程中横向所产生留白的填补距离。
     */
    private float getAdjustDistanceX(){
        RectF picBounds = getPicBounds();
        float dx = 0;
        float leftDelta = picBounds.left;
        float rightDelta = picBounds.right - getWidth();
        //左边界留白并且右边界有多余距离
        if (leftDelta > 0 && rightDelta >= leftDelta){
            dx = -leftDelta;
        }
        //右边界留白并且左边界有多余距离
        if (rightDelta < 0 && leftDelta <= rightDelta){
            dx = -rightDelta;
        }

        return dx;
    }
    /*
     * 获取图片在缩放过程中纵向所产生留白的填补距离。
     */
    private float getAdjustDistanceY(){
        RectF picBounds = getPicBounds();
        float dy = 0;
        float topDelta = picBounds.top;
        float bottomDelta = picBounds.bottom - getHeight();
        //上边界留白并且下边界有多余距离
        if (topDelta > 0 && bottomDelta >= topDelta){
            dy = -topDelta;
        }
        //下边界留白并且上边界有多余距离
        if (bottomDelta < 0 && topDelta <= bottomDelta){
            dy = -bottomDelta;
        }
        return dy;
    }
    /*
     * 获取图片相对于ImageView的四个边坐标。
     */
    private RectF getPicBounds(){
        Drawable pic = getDrawable();
        if (pic == null){
            return new RectF(0, 0, 0, 0);
        }
        int picWidth = pic.getIntrinsicWidth();
        int picHeight = pic.getIntrinsicHeight();
        RectF resultRect = new RectF(0, 0, picWidth, picHeight);
        mMatrix.mapRect(resultRect);
        return resultRect;
    }
    /*
     * 将图片恢复到FIT_CENTER的状态
     */
    public void restore(){
        reset();
        initScaleMatrix();
        setScaleType(ScaleType.MATRIX);
        setImageMatrix(mMatrix);
    }
    private void printPicWidthAndHeight(){
        Drawable d = getDrawable();
        if (d == null){
            return ;
        }
        int dw = d.getIntrinsicWidth();
        int dh = d.getIntrinsicHeight();
        Log.d("mxrt", "picwidth:" + dw);
        Log.d("mxrt", "picheight:" + dh);
    }
    /*
     * 初始化图片的大小和位置，达到ScaleType.FIT_CENTER的效果。
     */
    private void initScaleMatrix(){
        if (mInitScaleFactor != 0){
            return;
        }
        Drawable pic = getDrawable();
        if (pic == null){
            return;
        }
        int picOriginWidth = getDrawable().getIntrinsicWidth();
        int picOriginHeight = getDrawable().getIntrinsicHeight();
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        if (viewHeight == 0 || viewWidth == 0){
            return;
        }
        mInitScaleFactor = Math.min((float)viewWidth / picOriginWidth, (float)viewHeight / picOriginHeight);
        int dx = (viewWidth - picOriginWidth) / 2;
        int dy = (viewHeight - picOriginHeight) / 2;
        mMatrix.postTranslate(dx, dy);
        mMatrix.postScale(mInitScaleFactor, mInitScaleFactor, (float)viewWidth / 2, (float)viewHeight / 2);
    }
    /**
     * @param matrix
     * 获取Matrix中的值
     */
    private float[] getMatrixValues(Matrix matrix){
        float[] values = new float[9];
        matrix.getValues(values);
        return values;
    }
    /*
     * 重置Matrix和初始缩放比。
     */
    public void reset(){
        mInitScaleFactor = 0;
        mMatrix.reset();
    }
    @Override
    public boolean onTouch(View view, MotionEvent event){
        switch(event.getAction()){
            case MotionEvent.ACTION_MOVE:
                //此处额外增加对ACTION_MOVE的拦截主要还是为了防止轻微的滑动就触发onScroll
                //isBlockEvent标志位可以防止双指缩放时触发onScroll。
                //只要左右任意一边有撞边都禁止触发onScroll
                mBanState = canScroll();
                if (event.getPointerCount() == 1 && (mBanState != NO_BAN || isBlockEvent)){
                    return true;
                }
                break;
        }
        return false;
    }
    @Override
    public boolean onTouchEvent(MotionEvent event){
        //TestTool.judgeEventAction("mxrt", "", event);
        //Log.d("mxrt", "eventCount:" + event.getPointerCount());
        if (event.getPointerCount() == 1){
            mGesture.onTouchEvent(event);
        }
        if (event.getPointerCount() == 2){
            mScaleGesture.onTouchEvent(event);
        }
        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                initScaleMatrix();
                //TestTool.printRectF("mxrt", getPicBounds());
                mLastDownX = event.getX();
                mLastDownY = event.getY();
                mBanState = canScroll();
                if (mBanState == NO_BAN){
                    getParent().requestDisallowInterceptTouchEvent(true);//取消拦截，图片自由滑动
                }else{
                    getParent().requestDisallowInterceptTouchEvent(false);//恢复拦截，图片禁止滑动
                }
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                isBlockEvent = true;//取消拦截。
                break;
            case MotionEvent.ACTION_MOVE:
                float downX = event.getX();
                float downY = event.getY();
                float directionX = downX - mLastDownX;
                float directionY = downY - mLastDownY;
                if (event.getPointerCount() == 1 && isLeftClamp){
                    if (directionX < 0){
                        getParent().requestDisallowInterceptTouchEvent(true);//这里让图片在左边撞边之后可以向右移动
                        isLeftClamp = false;
                    }
                }
                if (event.getPointerCount() == 1 && isRightClamp){
                    if (directionX > 0){
                        getParent().requestDisallowInterceptTouchEvent(true);//这里让图片在右边撞边之后可以向左移动
                        isRightClamp = false;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);//恢复父View容器拦截ACTION_MOVE事件，让父View可以恢复滚动。
                isBlockEvent = false;
                break;
        }
        return super.onTouchEvent(event);
    }
    /**
     * @param listener 监听器
     * 传入单击响应接口
     */
    public void setOnTapUpListener(OnTapUpListener listener){
        mOnTapUpListener = listener;
    }
    /*
     * 点击响应接口
     */
    public interface OnTapUpListener{
        void onTapUp();
    }

}

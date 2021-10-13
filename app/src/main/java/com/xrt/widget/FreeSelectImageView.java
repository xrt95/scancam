package com.xrt.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.xrt.R;
import com.xrt.thirdpartylib.cv.CvUtils;
import com.xrt.tools.Utils;

import org.opencv.core.Mat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FreeSelectImageView extends FrameLayout {
    private ImageView mImageView;
    private FreeSelectBox mFreeSelectBox;
    private Bitmap mBitmap;

    public FreeSelectImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    private void init(Context context){
        mImageView = new ImageView(context);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mImageView.setLayoutParams(lp);
        mFreeSelectBox = new FreeSelectBox(context);
        mFreeSelectBox.setBackgroundResource(R.color.tranparent);
        mFreeSelectBox.setAdaptImageView(mImageView);
        addView(mImageView);
        addView(mFreeSelectBox);
    }

    public void setImgWithSelectBox(Bitmap bitmap, int[] coo){
        int imgWidth = bitmap.getWidth();
        int imgHeight = bitmap.getHeight();
        mFreeSelectBox.setVisibility(View.VISIBLE);
        mFreeSelectBox.setPicActualWidthAndHeight(imgWidth, imgHeight);
        if (coo == null){
            RectF imgBounds = mFreeSelectBox.getPicBounds(imgWidth, imgHeight);
            int[] coo8 = Utils.Coo4ToCoo8(Utils.RectFToCoo4(imgBounds));
            mFreeSelectBox.setCoo8(coo8);
        }else{
            if (coo.length == 4){
                mFreeSelectBox.setCoo4(coo);
            }else if(coo.length == 8){
                mFreeSelectBox.setCoo8(coo);
            }
        }
        mFreeSelectBox.post(() -> {
            mFreeSelectBox.adjust();
            mFreeSelectBox.initFourTouchMovePointPosition();
        });
        mImageView.setImageBitmap(bitmap);
        mBitmap = bitmap;
    }
    public void setImgWithSelectBoxReback(Bitmap bitmap){
        mImageView.setImageBitmap(bitmap);
        mFreeSelectBox.adjust();
        mFreeSelectBox.setVisibility(View.VISIBLE);
    }
    public Bitmap getCropedImg(){
        if (mBitmap == null){
            return mBitmap;
        }
        Point[] originPoints = mFreeSelectBox.getOriginSelectedFourPoint();
        Mat srcMat = CvUtils.bitmapToMat(mBitmap);
        Mat cropedMat = CvUtils.perspectiveChange(srcMat, originPoints);
        return CvUtils.matToBitmap(cropedMat);
    }
    public void setCropedImg(Bitmap img){
        mFreeSelectBox.setVisibility(View.INVISIBLE);
        mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mImageView.setImageBitmap(img);
    }
    private class FreeSelectBox extends FrameLayout {
        private Matrix mCoorMatrix = new Matrix();
        private int mPicWidth;
        private int mPicHeight;
        private int mViewWidth;
        private int mViewHeight;
        private RectF mPicBounds;
        private int mDownZone;
        private float mPointWidthPercent = 0.09f;
        private int[] mOrigin4Coo = new int[4];
        private int[] mOrigin8Coo = new int[8];
        private TouchMovePoint mLeftTopPoint;
        private TouchMovePoint mRightTopPoint;
        private TouchMovePoint mLeftBottomPoint;
        private TouchMovePoint mRightBottomPoint;
        private ImageView mAdaptImageView;
        private Path mPath = new Path();
        private Paint mPaint = new Paint();
        private static final int LEFT_TOP = 1;
        private static final int RIGHT_TOP = 2;
        private static final int LEFT_BOTTOM = 3;
        private static final int RIGHT_BOTTOM = 4;

        public FreeSelectBox(@NonNull Context context) {
            super(context);
            init(context);
        }

        private void init(Context context){
            mLeftTopPoint = new TouchMovePoint(context);
            mRightTopPoint = new TouchMovePoint(context);
            mLeftBottomPoint = new TouchMovePoint(context);
            mRightBottomPoint = new TouchMovePoint(context);
            mLeftTopPoint.setBackgroundResource(R.drawable.left_top_botton_thin);
            mRightTopPoint.setBackgroundResource(R.drawable.right_top_button_thin);
            mLeftBottomPoint.setBackgroundResource(R.drawable.left_bottom_button_thin);
            mRightBottomPoint.setBackgroundResource(R.drawable.right_bottom_button_thin);
            mLeftTopPoint.setId(generateViewId());
            mRightTopPoint.setId(generateViewId());
            mLeftBottomPoint.setId(generateViewId());
            mRightBottomPoint.setId(generateViewId());
            addView(mLeftTopPoint);
            addView(mRightTopPoint);
            addView(mLeftBottomPoint);
            addView(mRightBottomPoint);
            post(() -> {
                adjust();
                initFourTouchMovePointPosition();
            });
        }
        private void adjust(){
            adjustImageView();
            mLeftTopPoint.setMoveBounds(mPicBounds);
            mRightTopPoint.setMoveBounds(mPicBounds);
            mLeftBottomPoint.setMoveBounds(mPicBounds);
            mRightBottomPoint.setMoveBounds(mPicBounds);
        }
        public void resetCoorMatrix(){
            int pointWidth = getPointWidth();
            getImageViewAdaptMatrix(mCoorMatrix, getWidth(), getHeight(), mPicWidth, mPicHeight, pointWidth, pointWidth, pointWidth, pointWidth);
        }
        private void setPath(){
            mPath.reset();
            Point[] selectedPoints = getSelectedFourPoint();
            Point leftTopPoint = selectedPoints[0];
            Point rightTopPoint = selectedPoints[1];
            Point leftBottomPoint = selectedPoints[2];
            Point rightBottomPoint = selectedPoints[3];
            mPath.moveTo(leftTopPoint.x, leftTopPoint.y);
            mPath.lineTo(rightTopPoint.x , rightTopPoint.y);
            mPath.lineTo(rightBottomPoint.x, rightBottomPoint.y);
            mPath.lineTo(leftBottomPoint.x, leftBottomPoint.y);
            mPath.lineTo(leftTopPoint.x, leftTopPoint.y);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(5);
            mPaint.setColor(Color.RED);
            mPaint.setAntiAlias(true);
        }
        private Point[] getSelectedFourPoint(){
            Point leftTopPos = mLeftTopPoint.getCenterPoint();
            Point rightTopPos = mRightTopPoint.getCenterPoint();
            Point leftBottomPos = mLeftBottomPoint.getCenterPoint();
            Point rightBottomPos = mRightBottomPoint.getCenterPoint();
            Point actualLeftTopPos = new Point();
            Point actualRightTopPos = new Point();
            Point actualLeftBottomPos = new Point();
            Point actualRightBottomPos = new Point();
            int halfPointWidth = getPointWidth() / 2;
            actualLeftTopPos.x = leftTopPos.x + halfPointWidth;
            actualLeftTopPos.y = leftTopPos.y + halfPointWidth;
            actualRightTopPos.x = rightTopPos.x - halfPointWidth;
            actualRightTopPos.y = rightTopPos.y + halfPointWidth;
            actualLeftBottomPos.x = leftBottomPos.x + halfPointWidth;
            actualLeftBottomPos.y = leftBottomPos.y - halfPointWidth;
            actualRightBottomPos.x = rightBottomPos.x - halfPointWidth;
            actualRightBottomPos.y = rightBottomPos.y - halfPointWidth;
            Point[] selectedPoints = new Point[4];
            selectedPoints[0] = actualLeftTopPos;
            selectedPoints[1] = actualRightTopPos;
            selectedPoints[2] = actualLeftBottomPos;
            selectedPoints[3] = actualRightBottomPos;
            return selectedPoints;
        }
        public Point[] getOriginSelectedFourPoint(){
            Point[] selectedPoints = getSelectedFourPoint();
            Point[] resultPoints = new Point[4];
            int[] srcArray = new int[8];
            for (int i = 0; i < selectedPoints.length; i++){
                Point point = selectedPoints[i];
                srcArray[2 * i] = point.x;
                srcArray[2 * i + 1] = point.y;
            }
            Matrix inverMatrix = new Matrix();
            mCoorMatrix.invert(inverMatrix);
            float[] floatScrArray = Utils.changeIntArrayToFloatArray(srcArray);
            float[] floatDstArray = new float[8];
            inverMatrix.mapPoints(floatDstArray, floatScrArray);
            int[] dstArray = Utils.changeFloatArrayToIntArray(floatDstArray);
            for (int i = 0; i < resultPoints.length; i++){
                Point point = new Point();
                point.x = dstArray[2 * i] ;
                point.y = dstArray[2 * i + 1];
                resultPoints[i] = point;
            }
            return resultPoints;
        }
        private int getFingerDownZone(float x, float y){
            int centerWidth = mViewWidth / 2;
            int centerHeight = mViewHeight / 2;
            if (x < centerWidth){
                if (y < centerHeight){
                    return LEFT_TOP;
                }else{
                    return LEFT_BOTTOM;
                }
            }else{
                if (y < centerHeight){
                    return RIGHT_TOP;
                }else{
                    return RIGHT_BOTTOM;
                }
            }
        }
        @Override
        public boolean onInterceptTouchEvent(MotionEvent event){
            switch (event.getAction()){
                case MotionEvent.ACTION_MOVE:
                    return true;
            }
            return false;
        }
        /*
        @Override
        public boolean onTouchEvent(MotionEvent event){
            switch(event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    mDownZone = getFingerDownZone(event.getX(), event.getY());
                case MotionEvent.ACTION_MOVE:
                    float x = event.getX();
                    float y = event.getY();
                    int halfPointWidth = mPointWidth / 2;
                    float leftBound = (mPicBounds.left - halfPointWidth);
                    float topBound = (mPicBounds.top - halfPointWidth);
                    float rightBound =(mPicBounds.right + halfPointWidth);
                    float bottomBound = (mPicBounds.bottom + halfPointWidth);
                    if (x < leftBound) x = leftBound;
                    if (x > rightBound) x = rightBound;
                    if (y < topBound) y = topBound;
                    if (y >bottomBound) y = bottomBound;
                    switch(mDownZone){
                        case LEFT_TOP:
                            mLeftTopPoint.setCenterMatchPos((int)x, (int)y);
                            break;
                        case RIGHT_TOP:
                            mRightTopPoint.setCenterMatchPos((int)x, (int)y);
                            break;
                        case LEFT_BOTTOM:
                            mLeftBottomPoint.setCenterMatchPos((int)x, (int)y);
                            break;
                        case RIGHT_BOTTOM:
                            mRightBottomPoint.setCenterMatchPos((int)x, (int)y);
                            break;
                    }
                    return true;
            }
            return false;
        }
         */
        @Override
        public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            int childCount = getChildCount();
            int pointWidth = (int)(width * mPointWidthPercent);
            for (int i = 0; i < childCount; i++){
                View child = getChildAt(i);
                child.measure(MeasureSpec.makeMeasureSpec(pointWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(pointWidth, MeasureSpec.EXACTLY));
            }
            setMeasuredDimension(width, height);
        }
        @Override
        public void onLayout(boolean changed, int l, int t, int r, int b){
            super.onLayout(changed, l, t, r, b);
        }
        @Override
        public void onDraw(Canvas canvas){
            super.onDraw(canvas);
            setPath();
            canvas.drawPath(mPath, mPaint);
        }
        public void setPicActualWidthAndHeight(int width, int height){
            mPicWidth = width;
            mPicHeight = height;
            //requestLayout();
        }
        private int[] getActual4Coo(){
            int[] actualCoo;
            float[] floatActualCoo = new float[4];
            float[] floatOriginCoo = Utils.changeIntArrayToFloatArray(mOrigin4Coo);
            mCoorMatrix.mapPoints(floatActualCoo, floatOriginCoo);
            actualCoo = Utils.changeFloatArrayToIntArray(floatActualCoo);
            return actualCoo;
        }
        private int[] getBox8Coo(Matrix matrix){
            int[] actualCoo;
            float[] floatActualCoo = new float[8];
            float[] floatOriginCoo = Utils.changeIntArrayToFloatArray(mOrigin8Coo);
            matrix.mapPoints(floatActualCoo, floatOriginCoo);
            actualCoo = Utils.changeFloatArrayToIntArray(floatActualCoo);
            return actualCoo;
        }
        private int getPointWidth(){
            int pointWidth = (int)(getMeasuredWidth() * mPointWidthPercent);
            return pointWidth;
        }
        /*
         * 按左上，右上，右下，左下的顺序设置4个点的初始位置
         */
        private void initFourTouchMovePointPosition(){
            resetCoorMatrix();
            int pointWidth = getPointWidth();
            int justWidth = pointWidth / 2;
            //int[] actualCoo = getActual4Coo();
            int[] actualCoo = getBox8Coo(mCoorMatrix);
            int leftTopX = actualCoo[0] - justWidth;
            int leftTopY = actualCoo[1] - justWidth;
            int rightTopX = actualCoo[2]- justWidth;
            int rightTopY = actualCoo[3]- justWidth;
            int rightBottomX = actualCoo[4] - justWidth;
            int rightBottomY = actualCoo[5]- justWidth;
            int leftBottomX = actualCoo[6]- justWidth;
            int leftBottomY = actualCoo[7]- justWidth;
            //Log.d("mxrt", String.format("actural l:%d, t:%d, r:%d, b:%d", left, top, right, bottom));
            setLeftTopPointPos(leftTopX - justWidth, leftTopY - justWidth);
            setRightTopPointPos(rightTopX + justWidth, rightTopY - justWidth);
            setLeftBottomPointPos(leftBottomX - justWidth, leftBottomY + justWidth);
            setRightBottomPointPos(rightBottomX + justWidth, rightBottomY + justWidth);
        }
        public void setCoo4(int[] coo){
            mOrigin4Coo = coo;
            mOrigin8Coo = Utils.Coo4ToCoo8(coo);
        }
        public void setCoo8(int[] coo){
            mOrigin8Coo = coo;
        }
        private void setLeftTopPointPos(int x, int y){
            mLeftTopPoint.setPos(x, y);
            //mLeftTopPoint.setLayoutParams(mLeftTopPointLp);
        }
        private void setRightTopPointPos(int x, int y){
            mRightTopPoint.setPos(x, y);
            //mRightTopPoint.setLayoutParams(mRightTopPointLp);
        }
        private void setLeftBottomPointPos(int x, int y){
            mLeftBottomPoint.setPos(x, y);
            //mLeftBottomPoint.setLayoutParams(mLeftBottomPointLp);
        }
        private void setRightBottomPointPos(int x, int y){
            mRightBottomPoint.setPos(x, y);
            //mRightBottomPoint.setLayoutParams(mRightBottomPointLp);
        }
        public void setAdaptImageView(ImageView imageView){
            mAdaptImageView = imageView;

        }
        private void adjustImageView(){
            mAdaptImageView.setScaleType(ImageView.ScaleType.MATRIX);
            Matrix matrix = new Matrix();
            int pointWidth = getPointWidth();
            getImageViewAdaptMatrix(matrix, getWidth(), getHeight(), mPicWidth, mPicHeight, pointWidth, pointWidth, pointWidth, pointWidth);
            mAdaptImageView.setImageMatrix(matrix);
            mPicBounds = getPicBounds(mPicWidth, mPicHeight, matrix);
        }
        private RectF getPicBounds(int picWidth, int picHeight){
            RectF resultRect = new RectF(0, 0, picWidth, picHeight);
            mCoorMatrix.mapRect(resultRect);
            return resultRect;
        }
        private RectF getPicBounds(int picWidth, int picHeight, Matrix matrix){
            RectF resultRect = new RectF(0, 0, picWidth, picHeight);
            matrix.mapRect(resultRect);
            return resultRect;
        }
        private void getImageViewAdaptMatrix(Matrix matrix, int viewWidth, int viewHeight, int picWidth, int picHeight, int leftMargin, int topMargin, int rightMargin, int bottomMargin){
            matrix.reset();
            int widhtSpace = viewWidth - leftMargin - rightMargin;
            int heightSpace = viewHeight - topMargin - bottomMargin;
            float scaleFactor = Utils.getPicFitCenterInitFactor(widhtSpace, heightSpace, picWidth, picHeight);
            int dx = (viewWidth - picWidth) / 2;
            int dy = (viewHeight - picHeight) / 2;
            matrix.postTranslate(dx, dy);
            matrix.postScale(scaleFactor, scaleFactor, viewWidth / 2, viewHeight / 2);
        }

        private class TouchMovePoint extends View {
            private float mLastX;
            private float mLastY;
            private RectF mMoveBounds;
            private FrameLayout.LayoutParams mLp;

            public TouchMovePoint(Context context){
                super(context);
            }
            @Override
            public boolean onTouchEvent(MotionEvent event){
                //TestTool.judgeEventAction("mxrt", event);
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        mLastX = event.getRawX();
                        mLastY = event.getRawY();
                        getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int)(event.getRawX() - mLastX);
                        int dy = (int)(event.getRawY() - mLastY);
                        Point centerPoint = getCenterPoint();
                        float cx = centerPoint.x + dx;
                        float cy = centerPoint.y + dy;
                        if (mMoveBounds != null){
                            int halfPointWidth = getWidth() / 2;
                            float leftBound = (mMoveBounds.left - halfPointWidth);
                            float topBound = (mMoveBounds.top - halfPointWidth);
                            float rightBound =(mMoveBounds.right + halfPointWidth);
                            float bottomBound = (mMoveBounds.bottom + halfPointWidth);
                            if (cx < leftBound) cx = leftBound;
                            if (cx > rightBound) cx = rightBound;
                            if (cy < topBound) cy = topBound;
                            if (cy >bottomBound) cy = bottomBound;
                        }
                        setCenterMatchPos((int)cx, (int)cy);
                        mLastX = event.getRawX();
                        mLastY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
                return false;
            }
            public Point getCenterPoint(){
                Point centerPoint = new Point();
                int centerX = (getRight() + getLeft()) / 2;
                int centerY = (getTop() + getBottom()) / 2;
                centerPoint.x = centerX;
                centerPoint.y = centerY;
                return centerPoint;
            }
            public void setPos(int marginX, int marginY){
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)getLayoutParams();
                lp.leftMargin = marginX;
                lp.topMargin = marginY;
                setLayoutParams(lp);
            }
            public void setCenterMatchPos(int x, int y){
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)getLayoutParams();
                int halfWidth = getWidth() / 2;
                int halfHeight = getHeight() / 2;
                lp.leftMargin = x - halfWidth;
                lp.topMargin = y - halfHeight;
                setLayoutParams(lp);
            }
            public void setMoveBounds(RectF bounds){
                mMoveBounds = bounds;
            }
        }
    }
}

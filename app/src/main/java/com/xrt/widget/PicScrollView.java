package com.xrt.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;

import com.xrt.tools.TestTool;
import com.xrt.tools.UiTools;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ScaleGestureDetectorCompat;

public class PicScrollView extends HorizontalScrollView implements View.OnTouchListener{
    private Context mContext;
    public PicSwitchView mPicSwitchView;
    private GestureDetector mGesture;
    private List<PicView> mPicViewList = new ArrayList<>();
    private ChangeBounds mChangeBounds;
    private OnPictureChanged mOnPictureChangedListener;
    private OnTapUpListener mOnTapUpListener;
    public int currentViewIndex;
    private int mBaseWidth;
    private int mStartWidth;
    private TouchScaleImageView.OnTapUpListener mImageViewTapListener;

    public PicScrollView(Context context, AttributeSet attr){
        super(context, attr);
        mContext = context;
        mChangeBounds = new ChangeBounds();
        mChangeBounds.setDuration(200);
        mPicSwitchView = new PicSwitchView(context, attr);
        addView(mPicSwitchView);//默认应该是layout_width和layout_height都是match_parent
        setOnTouchListener(this);
        mGesture = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){
            private final int TRIGGER_VALUE = 500;
            @Override
            public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY){
                //event1是ACTION_DOWN。是副本
                //event2是ACTION_MOVE，如果返回true，就会被捕获了的。逻辑应该是获得了满足条件的ACTION_MOVE的时候，就会触发回调，返回true或者是false。返回true的话，ACTION_MOVE就会被捕获了。
                return false;
            }
            @Override
            public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY){
                //event1是ACTION_DOWN, 是副本。因为ACTION_DOWN肯定先要往下传，在经过onFling的时候肯定只能只留一个副本。
                //event2是ACTION_UP，如果返回true，就会被捕获了的。逻辑应该是通过流经的ACTION_MOVE计算滑动速度，如果满足条件，在ACTION_UP传入的时候就会触发回调，返回true或false。返回true的话，ACTION_UP就会被捕获了。
                //onFling不会捕获ACTION_MOVE
                PicScrollView picScrollView = (new WeakReference<PicScrollView>(PicScrollView.this)).get();
                if (velocityX <= -TRIGGER_VALUE){
                    picScrollView.modifyCurrentViewIndexBy(1);
                }else if(velocityX >= TRIGGER_VALUE){
                    picScrollView.modifyCurrentViewIndexBy(-1);
                }
                return false;
            }
            @Override
            public boolean onSingleTapUp(MotionEvent e){
                if (mOnTapUpListener != null){
                    mOnTapUpListener.onTapUp();
                }
                return false;
            }
        });
    }
    /*
     * 返回当前展示的TouchScaleImageView
     */
    private TouchScaleImageView getCurrentImageView(){
        return getPicViewAt(getCurrentIndex()).getImageView();
    }
    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY){
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
    }
    @Override
    public boolean onTouch(View view, MotionEvent event){
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                mBaseWidth = mPicSwitchView.getBaseChildWidth();
                mStartWidth = mPicSwitchView.getStartWidth();
                break;
        }
        return false;
    }
    /*
     * 这里可以通过调整v值来调整滑动惯性。
     */
    @Override
    public void fling(int v){
        super.fling(v);
    }
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event){
        return super.onInterceptTouchEvent(event);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event){
        mGesture.onTouchEvent(event);
        //TestTool.judgeEventAction("mxrt", "picscrollview ", event);
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_UP:
                skip(true);
                return true;//这里一定要返回，不能把up给ScrollView进行默认的时间处理。
        }
        return super.onTouchEvent(event);

    }
    /**
     * @param useAnim true表示滑动时带动画，false表示滑动时不带动画
     * 产生实际的滑动。
     */
    private void skip(boolean useAnim){
        int xScrollTo = currentViewIndex * mBaseWidth;
        if (useAnim){
            smoothScrollTo(xScrollTo, 0);
        }else{
            scrollTo(xScrollTo, 0);
        }
        PicView currentPicView = getPicViewAt(currentViewIndex);
        if (useAnim){
            TransitionManager.beginDelayedTransition(currentPicView, mChangeBounds);
        }
        currentPicView.changeConstraint(1f);
        if (currentViewIndex != getPicViewCount() - 1){
            PicView rightPicView = getPicViewAt(currentViewIndex + 1);
            rightPicView.changeConstraint(0f);
            rightPicView.getImageView().setScaleType(ImageView.ScaleType.FIT_CENTER);
            rightPicView.getImageView().reset();
        }
        if(currentViewIndex != 0){
            PicView leftPicView = getPicViewAt(currentViewIndex - 1);
            leftPicView.changeConstraint(0f);
            leftPicView.getImageView().setScaleType(ImageView.ScaleType.FIT_CENTER);
            leftPicView.getImageView().reset();
        }
    }
    /**
     * @param d 正值表示向后滑动d个图片位，负值表示向前滑动d个图片位
     * 根据当前的位置，向前或者向后滑动多少个图片位。
     */
    private void modifyCurrentViewIndexBy(int d){
        int childCount = mPicSwitchView.getChildCount();
        currentViewIndex += d;
        if (currentViewIndex < 0){
            currentViewIndex = 0;
        }else if (currentViewIndex > childCount - 1){
            currentViewIndex = childCount - 1;
        }
        if(mOnPictureChangedListener != null){
            mOnPictureChangedListener.onPictureIndexChange(currentViewIndex + 1, childCount);
        }
    }
    /**
     * @param count 添加多少个图片位
     * 一次性向PicScrollView添加指定数量的图片位
     */
    public void addPicView(int count){
        for (int i = 0; i < count; i++){
            PicView picView = new PicView(mContext);
            picView.getImageView().setOnTapUpListener(mImageViewTapListener);
            if (i == 0){
                picView.changeConstraint(1f);
            }else{
                picView.changeConstraint(0f);
            }
            mPicSwitchView.addView(picView);
            mPicViewList.add(picView);
            if(mOnPictureChangedListener != null){
                mOnPictureChangedListener.onPictureIndexChange(1, count);
            }
        }
    }
    /**
     * @param index 指定图片位
     * 移除指定位置的图片位
     */
    public void removePicViewAt(int index){
        View view = mPicViewList.get(index);
        mPicSwitchView.removeView(view);
        mPicViewList.remove(index);
    }
    /**
     * @param index 指定图片位
     * 获取指定位置的图片位
     */
    public PicView getPicViewAt(int index){
        return mPicViewList.get(index);
    }
    /**
     * 获取当前有多少个图片位
     */
    public int getPicViewCount(){
        return mPicViewList.size();
    }
    /**
     * 获取当前图片位的索引值
     */
    public int getCurrentIndex(){
        return currentViewIndex;
    }
    /**
     * @param count 正值表示向后跳转count个图片位，负值表示向前跳转count个图片位
     * @param useAnim true表示移动时带动画，false表示移动时不带动画
     * 相对当前的位置，向前或向后跳转count个图片位。
     */
    public void skipPicBy(int count, boolean useAnim){
        modifyCurrentViewIndexBy(count);
        mBaseWidth = mPicSwitchView.getBaseChildWidth();
        skip(useAnim);
    }
    /**
     * @param listener 图片移动监听器
     * 传入图片移动监听器
     */
    public void setOnPictureChangedListener(OnPictureChanged listener){
        mOnPictureChangedListener = listener;
    }
    /**
     * @param listener 单击监听器
     * 传入单击监听器
     */
    public void setOnTapUpListener(OnTapUpListener listener){
        mOnTapUpListener = listener;
    }

    public class PicSwitchView extends ViewGroup {
        private static final float PICVIEW_WIDTH_PERCENT = 1f;
        private static final float PICVIEW_WIDTH_MARGIN_PERCENT = 0f;
        private DisplayMetrics mDisplayMetrics;
        private int mStartWidth;
        private int mWidthMargin;
        private int mBaseChildWidth;

        public PicSwitchView(Context context, AttributeSet attr){
            super(context, attr);
            mDisplayMetrics = UiTools.getScreenMetrics(context);
        }
        @Override
        public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
            //TestTool.anlysMeasureSpec(TAG, widthMeasureSpec, heightMeasureSpec);
            int width = resolveSize(mDisplayMetrics.widthPixels, widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            int childWidth = (int)(width * PICVIEW_WIDTH_PERCENT);
            mBaseChildWidth = childWidth;
            mWidthMargin = (int)(PICVIEW_WIDTH_MARGIN_PERCENT * width);
            mStartWidth = (int)(width * (1 - PICVIEW_WIDTH_PERCENT) / 2);
            int childCount = getChildCount();
            int newWidth = mStartWidth * 2 + (mWidthMargin + childWidth) * childCount;
            View child;
            for (int i = 0; i < childCount; i++){
                child = getChildAt(i);
                child.measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            }
            setMeasuredDimension(newWidth, height);
        }
        @Override
        public void onLayout(boolean changed, int l, int t, int r, int b){
            int height = b - t;
            int childCount = getChildCount();
            View child;
            int childWidth;
            int widthStartPosition = mStartWidth;
            for (int i = 0; i < childCount; i++){
                child = getChildAt(i);
                childWidth = child.getMeasuredWidth();
                child.layout(widthStartPosition, 0, widthStartPosition + childWidth, height);
                widthStartPosition += childWidth + mWidthMargin;
            }
        }
        public int getBaseChildWidth(){
            return mBaseChildWidth;
        }
        public int getStartWidth(){
            return mStartWidth;
        }
    }

    public class PicView extends ConstraintLayout{
        private final static float INIT_SIZE_RATE = 0.85f;
        private TouchScaleImageView mImageView;
        private ConstraintSet mCS;

        public PicView(Context context){
            super(context);
            init(context);
        }
        private void init(Context context){
            mImageView = new TouchScaleImageView(context);
            mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            mImageView.setId(generateViewId());
            addView(mImageView);
            initConstraint();
        }
        /*
         * 初始化ImageView在图片位中约束
         */
        private void initConstraint(){
            mCS = new ConstraintSet();
            mCS.connect(mImageView.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
            mCS.connect(mImageView.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
            mCS.connect(mImageView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            mCS.connect(mImageView.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            mCS.applyTo(this);
        }
        /*
         * 调整ImageView相对于图片位的宽高百分比
         */
        public void changeConstraint(float percent){
            mCS.constrainPercentWidth(mImageView.getId(), INIT_SIZE_RATE + (1-INIT_SIZE_RATE) * percent);
            mCS.constrainPercentHeight(mImageView.getId(), INIT_SIZE_RATE + (1-INIT_SIZE_RATE) * percent);
            mCS.applyTo(this);
        }
        /*
         * 获取ImageView
         */
        public TouchScaleImageView getImageView(){
            return mImageView;
        }
    }

    public interface OnPictureChanged{
        void onPictureIndexChange(int index, int totalCount);
    }
    public interface OnTapUpListener{
        void onTapUp();
    }
    public void setImageViewTapUpListener(TouchScaleImageView.OnTapUpListener listener){
        mImageViewTapListener = listener;
    }

}

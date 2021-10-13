package com.xrt.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xrt.R;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class RadioButtonGroup extends RecyclerView {
    private int mLayoutResId;
    private boolean mCanScroll = false;
    private int mDesireHeight = 0;
    private int mItemCount;
    private Adapter<ItemViewHolder> mAdapter;
    private int mCols;
    private int mSpecFirstClickPosition = -1;
    private int mCurrentClickPosition = -1;
    private ClickAffect mClickAffect;
    private ResetAffect mResetAffect;
    private BindDataListener mBindDataListener;
    private List<ClickListener> mClickListeners;
    private static final int UPDATE = 1;

    public RadioButtonGroup(Context context, AttributeSet attr){
        super(context, attr);
        init(context, attr);
    }
    public RadioButtonGroup(Context context, AttributeSet attr, int defStyleAttr){
        super(context, attr, defStyleAttr);
        init(context, attr);
    }
    private void init(Context context, AttributeSet attr){
        TypedArray ta = context.obtainStyledAttributes(attr, R.styleable.RadioButtonGroup);
        mCols = ta.getInt(R.styleable.RadioButtonGroup_cols, 2);
        ta.recycle();
        post(() -> {
            mAdapter = new RecyclerViewAdapter();
            GridLayoutManager layoutManager = new ScrollBanedGridLayoutManager(context, mCols);
            setAdapter(mAdapter);
            setLayoutManager(layoutManager);
            RecyclerView.ItemDecoration itemDecoration = new ItemDecoration();
            addItemDecoration(itemDecoration);
        });
        setItemAnimator(null);//妈的，关动画
    }
    public class ScrollBanedGridLayoutManager extends GridLayoutManager{

        public ScrollBanedGridLayoutManager(Context context, int cols){
            super(context, cols);
        }
        @Override
        public boolean canScrollVertically(){
            return mCanScroll;
        }

    }
    public class ItemViewHolder extends RecyclerView.ViewHolder{
        View rootView;

        public ItemViewHolder(View view){
            super(view);
            rootView = view;
        }
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<ItemViewHolder>{

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup viewParent, int viewType){
            View view = LayoutInflater.from(getContext()).inflate(mLayoutResId, viewParent, false);
            int rows = (int)Math.ceil(mAdapter.getItemCount() / (float)mCols);
            int itemHeight;
            if (mCanScroll){
                itemHeight = mDesireHeight;
            }else{
                itemHeight = viewParent.getHeight() / rows;
            }
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            lp.height = itemHeight;
            return new ItemViewHolder(view);
        }
        @Override
        public void onBindViewHolder(ItemViewHolder viewHolder, int position){
        }
        @Override
        public void onBindViewHolder(ItemViewHolder viewHolder, int position, List<Object> payLoads){
            if (payLoads.isEmpty()){
                mBindDataListener.bindData(viewHolder.rootView, position);
                if (mClickListeners != null){
                    setItemOnClickListener(viewHolder.rootView, position, mClickListeners.get(position));
                }
                if (position == mCurrentClickPosition){
                    if (mClickAffect != null){
                        mClickAffect.clickAffect(viewHolder.rootView);
                    }
                    viewHolder.rootView.setClickable(false);
                }
            }else{
                for (int i = 0; i < payLoads.size(); i++){
                    int flag = (int)payLoads.get(i);
                    switch (flag){
                        case UPDATE:
                            if (position == mCurrentClickPosition){
                                if (mClickAffect != null){
                                    mClickAffect.clickAffect(viewHolder.rootView);
                                }
                                viewHolder.rootView.setClickable(false);
                            }else{
                                if (mResetAffect != null){
                                    mResetAffect.resetAffect(viewHolder.rootView);
                                }
                                viewHolder.rootView.setClickable(true);
                            }
                            break;
                    }
                }
            }
        }
        @Override
        public int getItemCount(){
            return mItemCount;
        }
    }
    private class ItemDecoration extends RecyclerView.ItemDecoration{
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView recyclerView, RecyclerView.State state){
            outRect.top = 2;
            outRect.bottom = 2;
            outRect.left = 2;
            outRect.right = 2;
        }
    }
    private void setItemOnClickListener(View view, int position, ClickListener listener){
        view.setOnClickListener((v) -> {
            listener.onClick();
            mCurrentClickPosition = position;
            mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount(), UPDATE);
        });
    }
    public void setLayoutResId(int resId){
        mLayoutResId = resId;
    }
    public void setSpecFirstClickPosition(int position){
        mSpecFirstClickPosition = position;
    }
    public void setCanScroll(boolean canScroll, int desiredHeight){
        mCanScroll = canScroll;
        mDesireHeight = desiredHeight;
    }
    public void reset(){
        mCurrentClickPosition = mSpecFirstClickPosition;
        mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount(), UPDATE);
    }

    /**
     * 申请指定数量的Item
     * @param count
     */
    public void setItemCount(int count){
        mItemCount = count;
    }

    /**
     * 设置点击效果。
     * @param clickAffect
     */
    public void setClickAffect(ClickAffect clickAffect){
        mClickAffect = clickAffect;
    }

    /**
     * 设置无点击时效果。
     * @param resetAffect
     */
    public void setResetAffect(ResetAffect resetAffect){
        mResetAffect = resetAffect;
    }

    /**
     *
     * @param bindDataListener
     */
    public void setBindDataListener(BindDataListener bindDataListener){
        mBindDataListener = bindDataListener;
    }
    public void setClickListeners(List<ClickListener> listeners){
        mClickListeners = listeners;
    }
    public interface ClickAffect{
        void clickAffect(View rootView);
    }
    public interface ResetAffect{
        void resetAffect(View rootView);
    }
    public interface BindDataListener{
        void bindData(View rootView, int position);
    }
    public interface ClickListener{
        void onClick();
    }

}

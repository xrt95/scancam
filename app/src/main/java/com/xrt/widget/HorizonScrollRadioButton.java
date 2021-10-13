package com.xrt.widget;

import android.content.ClipData;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xrt.R;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class HorizonScrollRadioButton extends RecyclerView {
    private Adapter<ItemViewHolder> mAdapter;
    private int mLayoutResId;
    private int mItemCount;
    private int mCurrentClickPosition = -1;
    private int mSpecInitClickPosition= -1;
    private ClickAffect mClickAffect;
    private ResetAffect mResetAffect;
    private BindDataListener mBindDataListener;
    private List<ClickListener> mClickListeners;

    private static final int UPDATE = 1;

    public HorizonScrollRadioButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public HorizonScrollRadioButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    public void init(){
        post(() -> {
            mAdapter = new RecyclerViewAdapter();
            setAdapter(mAdapter);
            GridLayoutManager layoutManager = new GridLayoutManager(this.getContext(), 1, HORIZONTAL, false);
            setLayoutManager(layoutManager);
            mCurrentClickPosition = mSpecInitClickPosition;
        });
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
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            lp.width = viewParent.getHeight();
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
    private void setItemOnClickListener(View view, int position, ClickListener listener){
        view.setOnClickListener((v) -> {
            listener.onClick();
            mCurrentClickPosition = position;
            mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount(), UPDATE);
        });
    }
    public void setItemLayout(int resId){
        mLayoutResId = resId;
    }
    public void setItemCount(int count){
        mItemCount = count;
    }
    public void setClickAffect(ClickAffect clickAffect){
        mClickAffect = clickAffect;
    }
    public void setResetAffect(ResetAffect resetAffect){
        mResetAffect = resetAffect;
    }
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
    public void update(int itemCount, List<ClickListener> listeners, BindDataListener bindDataListener){
        mItemCount = itemCount;
        mClickListeners = listeners;
        mBindDataListener = bindDataListener;
        post(() -> {
            mAdapter.notifyDataSetChanged();
        });
    }
    public void reset(){
        mCurrentClickPosition = mSpecInitClickPosition;
        mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount(), UPDATE);
    }
    public void setSpecFirstClickPosition(int position){
        mSpecInitClickPosition = position;
    }
}

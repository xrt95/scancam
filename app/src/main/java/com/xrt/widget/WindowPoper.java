package com.xrt.widget;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

public class WindowPoper extends FrameLayout {
    private View mPopWindowRootView;
    private TextView mMask;
    private PopupWindow mPopWindow;
    private Customizer mCustomizer;
    private DismissListener mDissListener;
    private int mDefaultMaskColor = Color.parseColor("#22000000");
    private PopupWindow.OnDismissListener mPopwindowDismissListener = new PopupWindow.OnDismissListener() {
        @Override
        public void onDismiss() {
            mMask.setBackgroundColor(Color.TRANSPARENT);
            if (mDissListener != null){
                mDissListener.onDismiss();
            }
        }
    };

    public WindowPoper(ViewGroup parentView, int layoutResID, int width, int height){
        super(parentView.getContext());
        Context context = parentView.getContext();
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        setLayoutParams(layoutParams);
        mMask = new TextView(context);
        mMask.setBackgroundColor(Color.TRANSPARENT);
        addView(mMask);
        setId(generateViewId());
        parentView.addView(this);
        mPopWindowRootView = LayoutInflater.from(context).inflate(layoutResID, null);
        mPopWindow = new PopupWindow(mPopWindowRootView, width, height, true);
        mPopWindow.setOnDismissListener(mPopwindowDismissListener);
    }
    public void setCustomizer(Customizer customizer){
        mCustomizer = customizer;
    }
    public void setDismissListener(DismissListener dismissListener){ mDissListener = dismissListener; }
    public void pop(int gravity, int maskColor){
        if (mCustomizer != null){
            mCustomizer.onCustom(mPopWindowRootView);
        }
        mPopWindow.showAtLocation(mPopWindowRootView, gravity, 0, 0);
        mMask.setBackgroundColor(maskColor);
    }
    public void pop(int gravity, boolean useMask){
        if (useMask){
            pop(gravity, mDefaultMaskColor);
        }else{
            pop(gravity, Color.TRANSPARENT);
        }
    }
    public void dismiss(){
        mPopWindow.dismiss();
    }
    public interface Customizer{
        void onCustom(View rootView);
    }
    public interface DismissListener{
        void onDismiss();
    }
}

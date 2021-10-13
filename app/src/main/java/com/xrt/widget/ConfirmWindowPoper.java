package com.xrt.widget;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.xrt.tools.UiTools;

public class ConfirmWindowPoper {
    private View mWindowRootView;
    private PopupWindow mPopwindow;
    private TextView mTitleTextView;
    private TextView mContentTextView;
    private Button mConfirmBtn;
    private Button mCancelBtn;
    private DisplayMetrics mDisplayMetrics;

    public ConfirmWindowPoper(Context context, int layoutResId, int titleTextViewId, int contentTextViewId, int confirmBtnId, int cancelBtnId){
        mWindowRootView = LayoutInflater.from(context).inflate(layoutResId, null);
        mDisplayMetrics = UiTools.getScreenMetrics(context);
        mTitleTextView = mWindowRootView.findViewById(titleTextViewId);
        mContentTextView = mWindowRootView.findViewById(contentTextViewId);
        mConfirmBtn = mWindowRootView.findViewById(confirmBtnId);
        mCancelBtn = mWindowRootView.findViewById(cancelBtnId);
    }
    public void popWindow(String title, String content, View.OnClickListener confirmListener, View.OnClickListener cancelListener){
        mPopwindow = new PopupWindow(mWindowRootView, mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels);
        mTitleTextView.setText(title);
        mContentTextView.setText(content);
        mConfirmBtn.setOnClickListener(confirmListener);
        mCancelBtn.setOnClickListener(cancelListener);
        mPopwindow.showAtLocation(mWindowRootView, Gravity.CENTER, 0, 0);
    }
    public void dismissWindow(){
        mPopwindow.dismiss();
    }
}

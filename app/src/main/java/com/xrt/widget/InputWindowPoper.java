package com.xrt.widget;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.xrt.tools.UiTools;

public class InputWindowPoper {
    private Context mContext;
    private View mPopWindowRootView;
    private PopupWindow mPopwindow;
    private TextView mTitleTextView;
    private EditText mEditor;
    private Button mConfirmBtn;
    private Button mCancelBtn;
    private DisplayMetrics mDisplayMetrics;

    public InputWindowPoper(Context context, int layoutId, int titleTextViewId, int inputEditId, int confirmBtnId, int cancalBtnId){
        mContext = context;
        mPopWindowRootView = LayoutInflater.from(context).inflate(layoutId, null);
        mTitleTextView = mPopWindowRootView.findViewById(titleTextViewId);
        mEditor = mPopWindowRootView.findViewById(inputEditId);
        mConfirmBtn = mPopWindowRootView.findViewById(confirmBtnId);
        mCancelBtn = mPopWindowRootView.findViewById(cancalBtnId);
        mDisplayMetrics = UiTools.getScreenMetrics(context);
    }
    public void popup(String title, String confirmBtnText, String cancelBtnText, View.OnClickListener confirmBtnListener, View.OnClickListener cancelBtnListener){
        mPopwindow = new PopupWindow(mPopWindowRootView, (int)(mDisplayMetrics.widthPixels * 0.8f), (int)(mDisplayMetrics.heightPixels * 0.2f));
        mTitleTextView.setText(title);
        mConfirmBtn.setText(confirmBtnText);
        mCancelBtn.setText(cancelBtnText);
        mConfirmBtn.setOnClickListener(confirmBtnListener);
        mCancelBtn.setOnClickListener(cancelBtnListener);
        mEditor.requestFocus();
        mPopwindow.setFocusable(true);
        mPopwindow.showAtLocation(mPopWindowRootView, Gravity.CENTER, 0, 0);
        popKeyBoard();
    }
    public EditText getEditText(){
        return mEditor;
    }
    private void popKeyBoard(){
        InputMethodManager imm = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }
}

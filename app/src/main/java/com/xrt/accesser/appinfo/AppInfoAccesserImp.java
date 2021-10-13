package com.xrt.accesser.appinfo;

import android.content.Context;
import android.content.SharedPreferences;

import com.xrt.constant.StringConstant;

class AppInfoAccesserImp implements AppInfoAccesser{
    private SharedPreferences mAppPreferences;
    private SharedPreferences.Editor mEditor;
    private Context mContext;


    public AppInfoAccesserImp(Context context){
        mContext = context;
        mAppPreferences = mContext.getSharedPreferences(StringConstant.APPINFO_SP_NAME, Context.MODE_PRIVATE);
        mEditor = mAppPreferences.edit();
    }
    @Override
    public boolean isAuthoritiesRequested() {
        boolean reuslt = mAppPreferences.getBoolean(StringConstant.APPINFO_IS_AUTHORITIES_REQ,false);
        return reuslt;
    }
    @Override
    public void authoritiesRequestCompleted() {
        mEditor.putBoolean(StringConstant.APPINFO_IS_AUTHORITIES_REQ, true);
        mEditor.commit();
    }
}

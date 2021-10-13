package com.xrt.accesser.appinfo;

import android.content.Context;

public class AppInfoAccesserFactory {
    public static AppInfoAccesser createAppInfoAccesser(Context context){
        return new AppInfoAccesserImp(context);
    }
}

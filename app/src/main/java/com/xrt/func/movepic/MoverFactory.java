package com.xrt.func.movepic;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public class MoverFactory {
    public static Mover createMover(Context context, String srcScanDirName, String dstScanDirName){
        return new PicMover(context, srcScanDirName, dstScanDirName);
    }
}

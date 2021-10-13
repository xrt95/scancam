package com.xrt.accesser.scanpic;

import android.content.Context;

public class ScanPicAccesserFactory {
    public static ScanPicAccesser createScanPicAccesser(Context context){
        return new PrivateFileScanPicAccesser(context);
    }
}

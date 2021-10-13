package com.xrt.constant;

import android.os.Build;

public class VersionCompatInfo {
    public static final boolean CAN_REQUEST_PERMISSIONS = Build.VERSION.SDK_INT >= 23;
    public static final boolean CAN_CREATE_URI_DIRECTLY = Build.VERSION.SDK_INT < 24;
}

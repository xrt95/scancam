package com.xrt.accesser.originpic;

import android.content.Context;

public class OriginPicAccesserFactory {
    public static OriginPicAccesser createOriginImgAccesser(Context context){
        return new PrivateFileOriginPicAccesser(context);
    }
}

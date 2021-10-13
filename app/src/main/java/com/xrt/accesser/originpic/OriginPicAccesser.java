package com.xrt.accesser.originpic;

import android.graphics.Bitmap;

public interface OriginPicAccesser {
    void storeOriginPic(String relativePath, Bitmap pic, int quanlity);
    void deleteOriginPic(String relativePath);
    void deleteOriginPicDir(String relativeDirPath);
    Bitmap getOriginPic(String relativePath, int inSampleSize);
    boolean isOriginPicExist(String relativePath);
    String getOriginPicAbsPath(String relativePath);
    String getOriginDirAbsPath(String dirName);
}

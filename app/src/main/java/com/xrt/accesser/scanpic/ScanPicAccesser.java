package com.xrt.accesser.scanpic;

import android.content.SharedPreferences;
import android.graphics.Bitmap;

import java.util.List;
import java.util.Map;

public interface ScanPicAccesser {
    void reScan();
    int getScanItemCount();
    List<String> getScanItemDirNames();
    List<String> getScanItemDirAbsPaths();
    String getScanItemDirAbsPath(String scanItemDirName);
    SharedPreferences getScanSharePreferences();
    SharedPreferences getScanItemSharePreferences(String scanItemDirName);
    List<String> getScanItemPicNames(String scanItemDirName);
    List<String> getScanItemPicAbsPaths(String scanItemDirName);
    int getScanItemPicCount(String scanItemDirName);
    String getScanItemTitle(String sanItemDirName);
    Map<String, Integer> getPicOrderMap(String scanItemDirName);
    Bitmap getScanItemPreviewPic(String scanItemDirName, int sampleSize);
}

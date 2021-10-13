package com.xrt.func.movepic;

import android.content.Context;
import android.content.SharedPreferences;

import com.xrt.accesser.originpic.OriginPicAccesser;
import com.xrt.accesser.originpic.OriginPicAccesserFactory;
import com.xrt.accesser.scanpic.ScanPicAccesser;
import com.xrt.accesser.scanpic.ScanPicAccesserFactory;
import com.xrt.tools.IOUtils;

import java.io.File;

class PicMover implements Mover{
    private SharedPreferences mSrcPreferences;
    private SharedPreferences.Editor mSrcEditor;
    private SharedPreferences mDstPreferences;
    private SharedPreferences.Editor mDstEditor;
    private ScanPicAccesser mScanPicAccesser;
    private OriginPicAccesser mOriginPicAccesser;
    private String mSrcScanDirName;
    private String mDstScanDirName;
    private int mNextIndex;

    public PicMover(Context context, String srcScanDirName, String dstScanDirName){
        mSrcScanDirName = srcScanDirName;
        mDstScanDirName = dstScanDirName;
        mScanPicAccesser = ScanPicAccesserFactory.createScanPicAccesser(context);
        mOriginPicAccesser = OriginPicAccesserFactory.createOriginImgAccesser(context);
        mSrcPreferences = mScanPicAccesser.getScanItemSharePreferences(srcScanDirName);
        mSrcEditor = mSrcPreferences.edit();
        mDstPreferences = mScanPicAccesser.getScanItemSharePreferences(dstScanDirName);
        mDstEditor = mDstPreferences.edit();
        mNextIndex = mScanPicAccesser.getScanItemPicCount(dstScanDirName);
    }
    @Override
    public boolean moveScanPic(String srcFileName, String dstFileName){
        String srcAbsPath = mScanPicAccesser.getScanItemDirAbsPath(mSrcScanDirName) + File.separator + srcFileName;
        String dstAbsPath = mScanPicAccesser.getScanItemDirAbsPath(mDstScanDirName) + File.separator + dstFileName;
        boolean isMove = IOUtils.moveFile(srcAbsPath, dstAbsPath);
        if (isMove){
            mDstEditor.putInt(dstAbsPath, mNextIndex);
            mDstEditor.apply();
            mNextIndex += 1;
        }
        return isMove;
    }
    @Override
    public boolean moveOriginPic(String srcFileName, String dstFileName){
        String srcAbsPath = mOriginPicAccesser.getOriginDirAbsPath(mSrcScanDirName) + File.separator + srcFileName;
        String dstAbsPath = mOriginPicAccesser.getOriginDirAbsPath(mDstScanDirName) + File.separator + dstFileName;
        return IOUtils.moveFile(srcAbsPath, dstAbsPath);
    }
}

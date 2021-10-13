package com.xrt.accesser.originpic;

import android.content.Context;
import android.graphics.Bitmap;

import com.xrt.tools.IOUtils;
import com.xrt.tools.Utils;

import java.io.File;
import java.util.List;

class PrivateFileOriginPicAccesser implements OriginPicAccesser {
    private static final String ORIGIN_PIC_DIR = "origin";
    private String mOriginPicAbsDir;
    private Context mContext;

    public PrivateFileOriginPicAccesser(Context context){
        mContext = context;
        mOriginPicAbsDir = context.getExternalFilesDir(ORIGIN_PIC_DIR).getPath();
    }
    public void storeOriginPic(String relativePath, Bitmap pic, int quanlity){
        List<String> partsOfPath = Utils.splitPath(relativePath);
        String fileName = partsOfPath.get(partsOfPath.size() - 1);
        String dirPath = relativePath.replace(fileName, "");
        String dirAbsPath = mContext.getExternalFilesDir(ORIGIN_PIC_DIR + dirPath).getPath();
        String storePath = dirAbsPath + fileName;
        IOUtils.saveImgFileWithJpg(storePath, pic, quanlity);
    }
    public void deleteOriginPic(String relativePath){
        String storePath = mOriginPicAbsDir + relativePath;
        IOUtils.deleteDirectoryOrFile(storePath);
    }
    public void deleteOriginPicDir(String relativeDirPath){
        String dirPath = mOriginPicAbsDir + relativeDirPath;
        IOUtils.deleteDirectoryOrFile(dirPath);
    }
    public Bitmap getOriginPic(String relativePath, int inSampleSize){
        String storePath = mOriginPicAbsDir + relativePath;
        return IOUtils.getAbridgeBitmap(storePath, inSampleSize);
    }
    public boolean isOriginPicExist(String relativePath){
        String picPath = mOriginPicAbsDir + relativePath;
        File file = new File(picPath);
        return file.exists();
    }
    @Override
    public String getOriginPicAbsPath(String relativePath) {
        return mOriginPicAbsDir + relativePath;
    }
    @Override
    public String getOriginDirAbsPath(String dirName) {
        String path = mContext.getExternalFilesDir(ORIGIN_PIC_DIR + File.separator + dirName).getPath();
        return path;
    }
}

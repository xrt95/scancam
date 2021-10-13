package com.xrt.accesser.picinfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.xrt.tools.IOUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * 管理每个"scan_xxx"目录下的图片。每个图片对应返回PicInfo对象，包含图片的Bitmap对象、储存路径。
 * 功能包括：
 *      加载图片，可设置图片加载完成的监听器。
 *      更新图片
 *      更新SharePreference
 */
public class PicInfoAccesser {
    private Context mContext;
    private String mItemPicDirAbsPath;
    private String mItemPicDirName;
    private String mItemPdfDirAbsPath;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mPreferenceEditor;
    private List<PicInfo> mLoadedPicInfos = new ArrayList<>();
    private Map<String, ?> mLastMap;
    private PicLoadedListener mPicLoadedListener;
    private static final int SAMPLE_SIZE = 4;

    public PicInfoAccesser(Context context, String itemPicDirAbsPath){
        mContext = context;
        mItemPicDirAbsPath = itemPicDirAbsPath;
        mItemPicDirName = mItemPicDirAbsPath.replace(context.getExternalFilesDir("").getPath() + File.separator, "");
        mPreferences = context.getSharedPreferences(mItemPicDirName, context.MODE_PRIVATE);
        mPreferenceEditor = mPreferences.edit();
        mLastMap = mPreferences.getAll();
    }
    public List<PicInfo> getPicInfos(){
        return mLoadedPicInfos;
    }
    public void loadPicInfo(int inSampleSize){
        List<String> picAbsPaths = IOUtils.getSpecSufixFilePaths(mItemPicDirAbsPath, new String[]{"jpg"});
        if (picAbsPaths == null){
            return ;
        }
        List<String> orderAdjustFilePaths = getOrderAdjustedPicPaths(picAbsPaths);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;
        for (int i = 0; i < orderAdjustFilePaths.size(); i++){
            String picAbsPath = orderAdjustFilePaths.get(i);
            Bitmap loadedPic = BitmapFactory.decodeFile(picAbsPath, options);
            PicInfo info = new PicInfo();
            info.path = picAbsPath;
            info.bitmap = loadedPic;
            mLoadedPicInfos.add(info);
            if (mPicLoadedListener != null){
                mPicLoadedListener.picLoaded(loadedPic);
            }
        }
    }
    public ArrayList<String> getOrderAdjustedPicAbsPaths(){
        List<String> picPaths= IOUtils.getSpecSufixFilePaths(mItemPicDirAbsPath , new String[]{"jpg"});
        return getOrderAdjustedPicPaths(picPaths);
    }
    /**
     * 获取PicviewActivity对应图片目录下顺序经调整的图片完整路径列表。
     * @param absFileNames PicviewActivity对应图片目录下的图片完整路径列表
     */
    private ArrayList<String> getOrderAdjustedPicPaths(List<String> absFileNames){
        ArrayList<String> resultList = new ArrayList<>();
        String[] orderAdjustedFileNamesArray = new String[absFileNames.size()];
        for (int i = 0; i < absFileNames.size(); i++){
            String absFileName = absFileNames.get(i);
            int order = mPreferences.getInt(absFileName, i);
            orderAdjustedFileNamesArray[order] = absFileName;
        }
        resultList.addAll(Arrays.asList(orderAdjustedFileNamesArray));
        return resultList;
    }
    public void updatePreferences(Map<String, Integer> map){
        mPreferenceEditor.clear();
        for (Map.Entry entry : map.entrySet()){
            String path = (String)entry.getKey();
            int order = (int)entry.getValue();
            mPreferenceEditor.putInt(path, order);
            mPreferenceEditor.commit();
        }
        mPreferenceEditor.apply();
        mLastMap = mPreferences.getAll();
    }

    /**
     * 获取图片对应的顺序
     * @return
     */
    public Map<String, Integer> getPicInfosOrderMap(){
        Map<String, Integer> resultMap = new HashMap<>();
        for (int i = 0; i < mLoadedPicInfos.size(); i++){
            PicInfo picInfo = mLoadedPicInfos.get(i);
            String picPath = picInfo.getPath();
            resultMap.put(picPath, i);
        }
        return resultMap;
    }

    /**
     * 根据SharePreferencee更新mLoadedPicInfos。
     * @param inSampleSize
     * @return
     */
    public Object[] updatePicInfo(int inSampleSize, List<String> needToUpdatePicPaths){
        List<String> picFilePaths = IOUtils.getSpecSufixFilePaths(mItemPicDirAbsPath, new String[]{"jpg"});
        Object[] res = new Object[2];
        boolean isUpdate = false;
        if (picFilePaths == null){
            return res;
        }
        List<String> orderAdjustFilePaths = getOrderAdjustedPicPaths(picFilePaths);
        List<PicInfo> resultList = new ArrayList<>();
        if (needToUpdatePicPaths != null){
            for (int i = 0; i < needToUpdatePicPaths.size(); i++){
                mLastMap.remove(needToUpdatePicPaths.get(i));
            }
        }
        for (int i = 0; i < orderAdjustFilePaths.size(); i++){
            Bitmap pic;
            String picFilePath = orderAdjustFilePaths.get(i);
            if (mLastMap.containsKey(picFilePath)){
                pic = mLoadedPicInfos.get((Integer)mLastMap.get(picFilePath)).bitmap;
            }else{
                pic = IOUtils.getAbridgeBitmap(picFilePath, inSampleSize);
                isUpdate = true;
            }
            PicInfo info = new PicInfo();
            info.bitmap = pic;
            info.path = picFilePath;
            resultList.add(info);
        }
        int delta = orderAdjustFilePaths.size() - mLoadedPicInfos.size();
        res[0] = delta;
        res[1] = isUpdate;
        mLastMap = mPreferences.getAll();
        mLoadedPicInfos = resultList;
        return res;
    }
    public void setPicLoadedListener(PicLoadedListener picLoadedListener){
        mPicLoadedListener = picLoadedListener;
    }
    public interface PicLoadedListener{
        void picLoaded(Bitmap pic);
    }

    public static class PicInfo {
        Bitmap bitmap;
        String path;

        public Bitmap getBitmap(){
            return bitmap;
        }
        public String getPath(){
            return path;
        }
    }

}

package com.xrt.accesser.scanpic;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import com.xrt.constant.StringConstant;
import com.xrt.tools.IOUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class PrivateFileScanPicAccesser implements ScanPicAccesser{
    private Context mContext;
    private SharedPreferences mScanPreferences;
    private List<String> mScanDirNames = new ArrayList<>();
    private List<String> mScanDirAbsPaths = new ArrayList<>();

    public PrivateFileScanPicAccesser(Context context){
        mContext = context;
        scanPrivateScanFile(context);
    }
    @Override
    public void reScan() {
        scanPrivateScanFile(mContext);
    }
    private void scanPrivateScanFile(Context context){
        mScanPreferences = context.getSharedPreferences(StringConstant.SCAN_PREFERENCE_FILENAME, Context.MODE_PRIVATE);
        File privateFile = context.getExternalFilesDir("");
        String externalPath = privateFile.getPath();
        String[] scanDirNames = privateFile.list();
        mScanDirNames.clear();
        mScanDirAbsPaths.clear();
        for (String scanDirName : scanDirNames){
            if (scanDirName.startsWith(StringConstant.SCAN_FILENAME_START_WITH)){
                mScanDirNames.add(scanDirName);
                String scanDirAbsPath = externalPath + File.separator + scanDirName;
                mScanDirAbsPaths.add(scanDirAbsPath);
            }
        }
        Collections.sort(mScanDirNames, (o1, o2) -> {
             return -o1.compareTo(o2);
        });
        Collections.sort(mScanDirAbsPaths, (o1, o2) -> {
            return -o1.compareTo(o2);
        });
    }
    private ArrayList<String> getOrderAdjustedPicPaths(List<String> absFileNames){
        ArrayList<String> resultList = new ArrayList<>();
        String[] orderAdjustedFileNamesArray = new String[absFileNames.size()];
        for (int i = 0; i < absFileNames.size(); i++){
            String absFileName = absFileNames.get(i);
            int order = mScanPreferences.getInt(absFileName, i);
            orderAdjustedFileNamesArray[order] = absFileName;
        }
        resultList.addAll(Arrays.asList(orderAdjustedFileNamesArray));
        return resultList;
    }
    @Override
    public int getScanItemCount(){return mScanDirNames.size();}
    @Override
    public List<String> getScanItemDirNames(){return mScanDirNames;}
    @Override
    public List<String> getScanItemDirAbsPaths(){return mScanDirAbsPaths;}
    @Override
    public String getScanItemDirAbsPath(String scanItemDirName) {
        return mContext.getExternalFilesDir("").getPath() + File.separator + scanItemDirName;
    }
    @Override
    public SharedPreferences getScanSharePreferences(){return mScanPreferences;}
    @Override
    public SharedPreferences getScanItemSharePreferences(String scanItemDirName){
        return mContext.getSharedPreferences(scanItemDirName, Context.MODE_PRIVATE);
    }
    @Override
    public List<String> getScanItemPicNames(String scanItemDirName){
        List<String> orderedScanItemPicNames = new ArrayList<>();
        for (String picAbsPath : getScanItemPicAbsPaths(scanItemDirName)){
            orderedScanItemPicNames.add(new File(picAbsPath).getName());
        }
        return orderedScanItemPicNames;
    }
    @Override
    public int getScanItemPicCount(String scanItemDirName) {
        return getScanItemPicNames(scanItemDirName).size();
    }
    @Override
    public List<String> getScanItemPicAbsPaths(String scanItemDirName){
        File scanItemFile = mContext.getExternalFilesDir(scanItemDirName);
        List<String> scanItemPicPaths = IOUtils.getSpecSufixFilePaths(scanItemFile, new String[]{"jpg"});
        return getOrderAdjustedPicPaths(scanItemPicPaths);
    }
    @Override
    public String getScanItemTitle(String scanItemDirName){
        Map<String, ?> map = mScanPreferences.getAll();
        return (String)map.get(scanItemDirName);
    }
    @Override
    public Map<String, Integer> getPicOrderMap(String scanItemDirName){
        return (Map<String, Integer>)getScanItemSharePreferences(scanItemDirName).getAll();
    }
    @Override
    public Bitmap getScanItemPreviewPic(String scanItemDirName, int sampleSize){
        Map<String, Integer> map = getPicOrderMap(scanItemDirName);
        for (Map.Entry<String, Integer> entry : map.entrySet()){
            if (entry.getValue() == 0){
                return IOUtils.getAbridgeBitmap(entry.getKey(), sampleSize);
            }
        }
        return IOUtils.getAbridgeBitmap(getScanItemPicAbsPaths(scanItemDirName).get(0), sampleSize);
    }
}

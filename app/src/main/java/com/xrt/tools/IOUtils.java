package com.xrt.tools;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IOUtils {
    public static final int FILE_SIZE_UNIT_B = 1;
    public static final int FILE_SIZE_UNIT_KB = 2;
    public static final int FILE_SIZE_UNIT_MB = 3;

    /**
     * @param path 给定的完整路径
     * 获得指定路径下所有子文件的完整路径。完整路径包含扩展名
     */
    public static String[] getAllFileName(String path){
        File parentFile = new File(path);
        return getAllFileName(parentFile);
    }
    /**
     * @param parentFile 给定的File
     * 获得指定File下所有子文件的完整路径。完整路径包含扩展名
     */
    public static String[] getAllFileName(File parentFile){
        String parentPath = parentFile.getPath();
        String[] fileNames = parentFile.list();
        String[] resultFileNames = new String[fileNames.length];
        for (int i = 0; i < fileNames.length; i++){
            resultFileNames[i] = parentPath + "/" + fileNames[i];
        }
        return resultFileNames;
    }
    /**
     * @param path 给定的完整路径
     * @param surfix 指定的后缀，不需要加.号
     * 获得指定路径下所有符合指定扩展名的子文件。
     */
    public static List<File> getSpecSufixFiles(String path, String[] surfix){
        List<File> resultFilesList = new ArrayList<>();
        List<String> filePaths = getSpecSufixFilePaths(path, surfix);
        for (int i = 0; i < filePaths.size(); i++){
            File file = new File(filePaths.get(i));
            resultFilesList.add(file);
        }
        return resultFilesList;
    }
    /**
     * @param path 给定的完整路径
     * @param surfix 指定的后缀，不需要加.号
     * 获得指定路径下所有符合指定扩展名的子文件的完整路径。完整路径包含扩展名
     */
    public static List<String> getSpecSufixFilePaths(String path, String[] surfix){
        File parentFile = new File(path);
        return getSpecSufixFilePaths(parentFile, surfix);
    }
    /**
     * @param parentFile 给定的File
     * @param surfix 指定的后缀，不需要加.号
     * 获得指定File下所有符合指定扩展名的子文件的完整路径。完整路径包含扩展名
     */
    public static List<String> getSpecSufixFilePaths(File parentFile, String[] surfix){
        String parentPath = parentFile.getPath();
        String[] files = parentFile.list();
        List<String> specFilesNameList = new ArrayList<>();
        for (int i = 0; i < files.length; i++){
            for (int j = 0; j < surfix.length; j++){
                String fileSurfix = files[i].substring(files[i].indexOf(".") + 1);
                if (fileSurfix.equals(surfix[j])){
                    specFilesNameList.add(parentPath + "/" + files[i]);
                }
            }
        }
        return specFilesNameList;
    }
    /**
     * @param path 给定的完整路径
     * @param surfix 指定的后缀。不需要加.号
     * 获取给定路径下满足指定后缀的所有文件的Byte[]数组
     */
    public static List<byte[]> getSpecFileByteList(String path, String[] surfix){
        List<String> filterFiles = getSpecSufixFilePaths(path, surfix);
        List<byte[]> resultByteList = new ArrayList<>();
        for (int i = 0; i < filterFiles.size(); i++){
            String filtedFileName = filterFiles.get(i);
            File filtedFile = new File(filtedFileName);
            try(FileInputStream fileInputStream = new FileInputStream(filtedFile)){
                byte[] bytes = new byte[fileInputStream.available()];
                fileInputStream.read(bytes);
                resultByteList.add(bytes);
            }
            catch(Exception e){
                Log.d("mXrt", e.getMessage());
            }
        }
        return resultByteList;
    } /**
     * @param path 给定的完整路径
     * @param surfix 指定的后缀。不需要加.号
     * 获取给定路径下满足指定后缀的所有文件的Drawable对象
     */
    public static List<Drawable> getSpecFileDrawableList(String path, String[] surfix){
        List<byte[]> fileByteList = getSpecFileByteList(path, surfix);
        List<String> specFileName = getSpecSufixFilePaths(path, surfix);
        List<Drawable> resultDrawableList = new ArrayList<>();
        for (int i = 0; i < fileByteList.size(); i++){
            byte[] bytes = fileByteList.get(i);
            ByteArrayInputStream byteInputStream = new ByteArrayInputStream(bytes);
            Drawable drawable = Drawable.createFromStream(byteInputStream, specFileName.get(i));
            resultDrawableList.add(drawable);
        }
        return resultDrawableList;
    }
    /**
     * @param path 给定的完整路径
     * @param surfix 指定的后缀。不需要加.号
     * @param sampleSize 给定的采样率
     * 获取给定路径和采样率下满足指定后缀的所有文件的Bitmap对象
     */
    public static List<Bitmap> getAbridgeSpecFileBitmapList(String path, String[] surfix, int sampleSize){
        List<String> specFileName = getSpecSufixFilePaths(path, surfix);
        List<Bitmap> resultBitmapList = new ArrayList<>();
        for (int i = 0; i < specFileName.size(); i++){
            String filePath = specFileName.get(i);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inSampleSize = sampleSize;
            resultBitmapList.add(BitmapFactory.decodeFile(filePath, options));
        }
        return resultBitmapList;
    }
    /**
     * @param path 图片的完整路径
     * @param inSampleSize 给定的采样率从
     * 给定图片的完整路径和采样率，返回Bitmap
     */
    public static Bitmap getAbridgeBitmap(String path, int inSampleSize){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;
        return BitmapFactory.decodeFile(path, options);
    }
    /**
     * @param path 图片的完整路径
     * @param desireWidth 图片的期望显示宽度
     * @param desireHeight 图片的期望显示高度
     * @param defaultInsampleSize 默认采样率
     * 给定图片的完整路径和预期宽高，自动计算合适采样率，返回Bitmap。在采样率无法计算出来时采用默认采样率。
     */
    public static Bitmap getAbridgeBitmap(String path, int desireWidth, int desireHeight, int defaultInsampleSize){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int inSampleSize = defaultInsampleSize;
        if (desireWidth > 0 && desireHeight > 0){
            int widthRatio = (int)Math.ceil(options.outWidth / desireWidth);
            int heightRatio = (int)Math.ceil(options.outHeight / desireHeight);
            if (heightRatio > 1 || widthRatio > 1){
                inSampleSize = Math.max(heightRatio, widthRatio);
            }
        }
        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        //Log.d("mxrt", "insamplesize:" + inSampleSize);
        return BitmapFactory.decodeFile(path, options);//路径不对的话会返回null，只是ImageView.setImageBitmap支持传入null。
    }
    /**
     * @param path 要删除的目录或文件的完整路径。
     * 给定目录完整路径，删除整个目录，包括目录下的子文件和子目录。亦可删除单个文件。
     */
    public static boolean deleteDirectoryOrFile(String path){
        File fileToDel = new File(path);
        File[] subFiles = fileToDel.listFiles();
        if (fileToDel.isFile()){
            return fileToDel.delete();
        }
        if (subFiles != null){
            for (int i = 0; i < subFiles.length; i++){
                File subFile = subFiles[i];
                if (subFile.isFile()){
                    subFile.delete();
                }else{
                    deleteDirectoryOrFile(subFile.getPath());
                }
            }
        }
        return fileToDel.delete();
    }
    public static void saveImgFileWithJpg(String savePath, Bitmap pic, int qulity){
        File saveFile = new File(savePath);
        try(FileOutputStream fileOutputStream = new FileOutputStream(saveFile)){
            pic.compress(Bitmap.CompressFormat.JPEG, qulity, fileOutputStream);
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    /**
     * @param dirPath 给定目录的完整路径
     * @param unit 单位
     * @param isRecursive 是否递归计算目录下的所有子目录的文件大小
     * 返回给定目录下所有的子文件大小的总和。
     */
    public static float getSubFileTotalSize(String dirPath, int unit, boolean isRecursive){
        File parentFile = new File(dirPath);
        File[] subFiles = parentFile.listFiles();
        float sumSize = 0;
        if (subFiles == null){
            return 0;
        }else{
            for (File subFile : subFiles){
                if (subFile.isFile()){
                    sumSize += subFile.length();
                }else{
                    if (isRecursive){
                        sumSize += getSubFileTotalSize(subFile.getPath(), FILE_SIZE_UNIT_B, isRecursive);
                    }
                }
            }
        }
        switch(unit){

            case FILE_SIZE_UNIT_KB:
                return BToKB(sumSize);
            case FILE_SIZE_UNIT_MB:
                return BToMB(sumSize);
            case FILE_SIZE_UNIT_B:
            default:
                return sumSize;
        }
    }
    /**
     * @param B 字节数
     * B转KB
     */
    public static float BToKB(float B){
        return B / 1024;
    }
    /**
     * @param B 字节数
     * B转MB
     */
    public static float BToMB(float B){
        return B / (1024 * 1024);
    }
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param srcAbsPath
     * @param dstAbsPath
     * @return
     */
    public static boolean moveFile(String srcAbsPath, String dstAbsPath){
        try{
            FileInputStream inputStream = new FileInputStream(new File(srcAbsPath));
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            FileOutputStream outputStream = new FileOutputStream(new File(dstAbsPath));
            outputStream.write(bytes);
            return true;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }
}

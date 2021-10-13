package com.xrt.accesser.systempic;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;

public class SystemPicAccesser {

    private ContentResolver mResolver;
    private Cursor mCursor;
    private ImgsLoadedListener mLoadedListener;
    private static final Uri SYSTEM_THUMBNAIL_IMG_URI = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI;
    private static final Uri SYSTEM_IMG_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    public SystemPicAccesser(ContentResolver resolver){
        mResolver = resolver;
    }
    public void init(){
        String[] proj = new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.ORIENTATION};
        mCursor = mResolver.query(SYSTEM_IMG_URI, proj, null, null, MediaStore.Images.Media.DATE_MODIFIED + " DESC");
        if (mLoadedListener != null){
            mLoadedListener.imgsLoaded();
        }
    }
    public int getAllImgCount(){
        if (mCursor == null){
            return 0;
        }
        return mCursor.getCount();
    }
    public Uri getImgUriByPosition(int position){
        mCursor.moveToPosition(position);
        int idIndex = mCursor.getColumnIndex(MediaStore.Images.Media._ID);
        String id = mCursor.getString(idIndex);
        Uri imgUri = Uri.parse(SYSTEM_IMG_URI.toString() + File.separator + id);
        return imgUri;
    }
    public String getImgAbsPathByPosition(int position){
        mCursor.moveToPosition(position);
        int pathIndex = mCursor.getColumnIndex(MediaStore.Images.Media.DATA);
        String path = mCursor.getString(pathIndex);
        //Log.d("mxrt", "" + path);
        return path;
    }
    public String getImgOrientationByPosition(int position){
        mCursor.moveToPosition(position);
        int orientationIndex = mCursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION);
        String orientation = mCursor.getString(orientationIndex);
        return orientation;
    }
    public void setImgsLoadedListener(ImgsLoadedListener listener){
        mLoadedListener = listener;
    }
    public interface ImgsLoadedListener{
        void imgsLoaded();
    }


}
package com.xrt.tools;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.xrt.constant.VersionCompatInfo;

import java.io.File;

import androidx.core.content.FileProvider;

public class ShareTools {
    //分享渠道常量
    public static final int SHARE_WEIXIN = 1;
    public static final int SHARE_QQ = 2;
    public static final int SHARE_MORE = 3;

    /**
     * @param filePath 要分享文件的完整路径
     * @param fileType 分享的文件类型
     * @param context
     * @param fileProviderName 在Manifest文件配置的FileProvider的authorties值
     * @param channel_type 渠道类型
     * 调用系统或者其他软件提供的文件分享页面进行分享
     */
    public static void shareFile(String filePath, String fileType, Context context, String fileProviderName, int channel_type)
        throws Exception
    {
        try{
            File file = new File(filePath);
            Intent intent = new Intent();
            intent.setAction("android.intent.action.SEND");
            intent.setType(fileType);
            ComponentName comp;
            switch(channel_type){
                case SHARE_WEIXIN:
                    comp = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareImgUI");
                    intent.setComponent(comp);
                    break;
                case SHARE_QQ:
                    comp = new ComponentName("com.tencent.mobileqq", "com.tencent.mobileqq.activity.JumpActivity");
                    intent.setComponent(comp);
                    break;
                case SHARE_MORE:
                    break;
            }
            Uri fileUri;
            if (VersionCompatInfo.CAN_CREATE_URI_DIRECTLY){
                fileUri = Uri.fromFile(file);
            }else{
                fileUri = FileProvider.getUriForFile(context, fileProviderName, file);
            }
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            context.startActivity(intent);
        }catch (Exception e){
            throw new Exception();
        }
    }
    /**
     * @param packageName 应用包名
     * 给定应用包名，判断应用是否存在
     */
    public static boolean checkAppInstalled(Context context, String packageName){
        PackageInfo packageInfo = null;
        try{
            packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
        }catch(PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
        if (packageInfo == null){
            return false;
        }else{
            return true;
        }
    }

}

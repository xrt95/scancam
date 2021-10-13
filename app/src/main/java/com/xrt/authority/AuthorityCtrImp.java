package com.xrt.authority;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.ViewGroup;

import com.xrt.constant.StringConstant;
import com.xrt.widget.WindowPoper;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class AuthorityCtrImp implements AuthorityController{
    private Activity mActivity;
    private Context mContext;
    private static final String PACKAGE_URI_SCHEME = "package:";

    public AuthorityCtrImp(Activity activity){
        mActivity = activity;
        mContext = activity.getApplicationContext();
    }
    @Override
    public boolean lackPermission(String permission) {
        return ContextCompat.checkSelfPermission(mContext, permission) == PackageManager.PERMISSION_DENIED;
    }
    @Override
    public boolean lackPermissions(String[] permissions) {
        for (String permission : permissions){
            return lackPermission(permission);
        }
        return false;
    }
    @Override
    public boolean validPermissionsWithHint(String[] permissions, int requestCode) {
        if (lackPermissions(permissions)){
            initDialog(permissions, requestCode);
            return false;
        }
        return true;
    }
    private void initDialog(String[] permissions, int requestCode){
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setMessage("缺少相关权限，将再次请求权限，若请求再次被拒绝可能导致权限无法再打开");
        builder.setPositiveButton("再次请求权限", (dialog, which) -> {
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.setOnDismissListener(dialog -> {
            requestPermissions(permissions, requestCode);
        });
        alertDialog.show();
    }
    private void startAuthoritiesSetting(){
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse(PACKAGE_URI_SCHEME + mActivity.getPackageName()));
        mActivity.startActivity(intent);
    }
    @Override
    public void requestPermissions(String[] permissions, int requestCode) {
        if (lackPermissions(permissions)){
            ActivityCompat.requestPermissions(mActivity, permissions, requestCode);
        }
    }
}

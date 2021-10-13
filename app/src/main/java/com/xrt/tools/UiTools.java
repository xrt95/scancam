package com.xrt.tools;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class UiTools {
    private static String TAG = "mXrt";
    /**
     * dp值转px值
     */
    public static int dpTopx(Context context, int dp){
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
    /**
     * 获取屏幕宽高。单位像素
     */
    public static DisplayMetrics getScreenMetrics(Context context){
        WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics;
    }
    /**
     * 获取屏幕实际宽高。单位像素
     */
    public static DisplayMetrics getRealScreenMetrics(Context context){
        WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        return metrics;
    }
    /**
     * 获取状态栏高度。单位像素
     */
    public static int getStatusBarHeight(Context context){
        Resources res = context.getResources();
        int statusBarHeightId = res.getIdentifier("status_bar_height", "dimen", "android");
        return statusBarHeightId > 0 ? res.getDimensionPixelSize(statusBarHeightId) : 0;
    }
    /**
     * 获取虚拟键高度。单位像素
     */
    public static int getNavBarHeight(Context context){
        Resources res = context.getResources();
        int navBarHeightId = res.getIdentifier("navigation_bar_height", "dimen", "android");
        return navBarHeightId > 0 && hasNavBar(context)? res.getDimensionPixelSize(navBarHeightId) : 0;
    }
    /**
     * 判断是否启用虚拟键
     */
    public static boolean hasNavBar(Context context){
        Resources res = context.getResources();
        int hasNavBarId = res.getIdentifier("config_showNavigationBar", "bool", "android");
        boolean result = res.getBoolean(hasNavBarId);
        return result && xiaomiHasNavBar(context);
    }
    /**
     * 小米判断是否启用了虚拟键
     */
    public static boolean xiaomiHasNavBar(Context context){
        int val = Settings.Global.getInt(context.getContentResolver(), "force_fsg_nav_bar", 0);
        return val == 0;//这里返回了默认值0意味着没有启用全面屏手势，那意味着就是开了虚拟键
    }
    /**
     * 将状态栏变成深色模式
     */
    public static void StatusBarDarkMode(Activity activity){
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }else{
                //为android6.0以下的MIUI、Flyme提供支持
                setMIUIStatusBarColorMode(activity, true);
                setFlymeStatusBarColorMode(activity, true);
            }
        }
    }
    /**
     * 6.0以下小米状态栏深色模式设置
     */
    private static void setMIUIStatusBarColorMode(Activity activity, boolean isDark){
        Window window = activity.getWindow();
        if (window != null){
            Class windowClazz = window.getClass();
            try{
                int darkModeFlag = 0;
                Class layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
                Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
                darkModeFlag = field.getInt(layoutParams);
                Method extraFlagField = windowClazz.getMethod("setExtraFlags", int.class, int.class);
                if (isDark){
                    extraFlagField.invoke(window, darkModeFlag, darkModeFlag);
                }else{
                    extraFlagField.invoke(window, 0, darkModeFlag);
                }
            }catch(Exception e){}
        }
    }
    /**
     * 6.0以下Flyme状态栏深色模式设置
     */
    private static void setFlymeStatusBarColorMode(Activity activity, boolean isDark){
        Window window = activity.getWindow();
        if (window != null){
            try{
                WindowManager.LayoutParams lp = window.getAttributes();
                Field darkFlag = WindowManager.LayoutParams.class.getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON");
                Field meizuFlags = WindowManager.LayoutParams.class.getDeclaredField("meizuFlags");
                darkFlag.setAccessible(true);
                meizuFlags.setAccessible(true);
                int bit = darkFlag.getInt(null);
                int value = darkFlag.getInt(lp);
                if (isDark){
                    value |= bit;
                }else{
                    value &= ~bit;
                }
                meizuFlags.setInt(lp, value);
                window.setAttributes(lp);
            }catch(Exception e){}
        }
    }
    public static Bitmap rotateBitmap(Bitmap srcBitmap, float degree){
        Matrix matrix = new Matrix();
        matrix.postRotate(degree, srcBitmap.getHeight() / 2f, srcBitmap.getWidth() / 2f);
        int translateX = srcBitmap.getHeight();
        float[] values = new float[9];
        matrix.getValues(values);
        matrix.postTranslate(translateX - values[Matrix.MTRANS_X], -values[Matrix.MTRANS_Y]);
        Bitmap newBitmap = Bitmap.createBitmap(srcBitmap.getHeight(), srcBitmap.getWidth(), srcBitmap.getConfig());
        Canvas canvas = new Canvas(newBitmap);
        Paint paint = new Paint();
        canvas.drawBitmap(srcBitmap, matrix ,paint);
        return newBitmap;
    }

}



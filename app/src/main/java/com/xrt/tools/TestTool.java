package com.xrt.tools;

import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.Map;

public class TestTool {
    public static void anlysMeasureSpec(String tag, int widthMeasureSpec, int heightMeasureSpec){
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        Log.d(tag, "width: " + width);
        Log.d(tag, "widthMode: " + judgeMeasureSpecMode(widthMode));
        Log.d(tag, "height: " + height);
        Log.d(tag, "heightMode: " + judgeMeasureSpecMode(heightMode));
        Log.d(tag, "-----------------------------------");

    }
    public static void printMapElement(String tag, Map<String, ?> map){
        String resultString = "{";
        for (Map.Entry entry : map.entrySet()){
            String ele = entry.getKey() + ":" + entry.getValue().toString() + ", ";
            resultString += ele;
        }
        resultString += "}";
        Log.d(tag, resultString);
    }
    public static String judgeMeasureSpecMode(int mode){
        switch(mode){
            case View.MeasureSpec.EXACTLY:
                return "EXACTLY";
            case View.MeasureSpec.AT_MOST:
                return "AT_MOST";
            case View.MeasureSpec.UNSPECIFIED:
                return "UNSPECIFIED";
            default:
                return "no match";
        }
    }
    public static void judgeEventAction(String tag, MotionEvent event){
        judgeEventAction(tag, "", event);
    }
    public static void judgeEventAction(String tag, String additionTag, MotionEvent event){
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                Log.d(tag, additionTag + "ACTION: DOWN");
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(tag, additionTag + "ACTION: MOVE");
                break;
            case MotionEvent.ACTION_UP:
                Log.d(tag, additionTag + "ACTION: UP");
                break;
            case MotionEvent.ACTION_CANCEL:
                Log.d(tag, additionTag + "ACTION: CANCEL");
                break;
            case MotionEvent.ACTION_SCROLL:
                Log.d(tag, additionTag + "ACTION: SCROLL");
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.d(tag, additionTag + "ACTION: POINTER_DOWN");
                break;
            case MotionEvent.ACTION_POINTER_UP:
                Log.d(tag, additionTag + "ACTION: POINTER_UP");
                break;
            default:
                Log.d(tag, additionTag + "ACTION: no match:" + event.getAction());
                break;
        }
    }
    public static void judgeViewIsNull(String printTag, View view){
        if (view == null){
            Log.d(printTag, "view is null");
        }else{
            Log.d(printTag, "view is not null");

        }
    }
    public static <T> void  printStringArray(String tag, T[] stringArray){
        String result = "[";
        for (T s : stringArray){
            result += s;
            result += ",";
        }
        result += "]";
        Log.d(tag, result);
    }
    public static void  printIntArray(String tag, int[] intArray){
        String result = "[";
        for (int s : intArray){
            result += s;
            result += ",";
        }
        result += "]";
        Log.d(tag, result);
    }
    public static void  printByteArray(String tag, byte[] byteArray){
        String result = "[";
        for (int s : byteArray){
            result += s;
            result += ",";
        }
        result += "]";
        Log.d(tag, result);
    }
    public static void  printFloatArray(String tag, float[] stringArray){
        String result = "[";
        for (float s : stringArray){
            result += s;
            result += ",";
        }
        result += "]";
        Log.d(tag, result);
    }
    public static void judgeObjectIsNull(String tag, String additionalTag, Object obj){
        if (obj == null){
            Log.d(tag, additionalTag + "is null");
        }else{
            Log.d(tag, additionalTag + "is not null");
        }
    }
    public static void printRect(String tag, Rect rect){
        Log.d(tag, String.format("Rect: left: %d top: %d right: %d bottom: %d", rect.left, rect.top, rect.right, rect.bottom));
    }
    public static void printRectF(String tag, RectF rectf){
        Log.d(tag, String.format("Rect: left: %f top: %f right: %f bottom: %f", rectf.left, rectf.top, rectf.right, rectf.bottom));
    }
    /*
    @Override
    protected void onCreate(){
        super.onCreate();
        Log.d(TAG, getLocalClassName() + ": onCreate");
    }
    @Override
    protected void onResume(){
        super.onResume();
        Log.d(TAG, getLocalClassName() + ": onResume");
    }
    @Override
    protected void onStart(){
        super.onStart();
        Log.d(TAG, getLocalClassName() + ": onStart");
    }
    @Override
    protected void onPause(){
        super.onPause();
        Log.d(TAG, getLocalClassName() + ": onPause");
    }
    @Override
    protected void onStop(){
        super.onStop();
        Log.d(TAG, getLocalClassName() + ": onStop");
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.d(TAG,  getLocalClassName() + ": onDestroy");
    }
    @Override
    protected void onRestart(){
        super.onRestart();
        Log.d(TAG, getLocalClassName() + ": onRestart");
    }
     */

}

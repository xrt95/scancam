package com.xrt.tools;

import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    /**
     * @param timestamp 时间戳数值
     * @param format 输出的时间格式
     * 将时间戳转换为指定格式
     */
    public static String timestampToFormat(long timestamp, String format){
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(timestamp);
    }
    /**
     * 计算两点之间的距离
     */
    public static double getPointDistance(int x1, int y1, int x2, int y2){
        return Math.sqrt(((x2 - x1)^2 + (y2 - y1)^2));
    }
    /**
     * @param srcList 被复制的列表。
     * 复制列表
     */
    public static ArrayList<String> copyList(List<String> srcList){
        ArrayList<String> resultList = new ArrayList<>();
        resultList.addAll(Arrays.asList(new String[srcList.size()]));
        Collections.copy(resultList, srcList);
        return resultList;
    }
    /**
     * 例如给定参数specIndex = 5, totalIndex = 9, 生成列表：[5, 4, 6, 3, 7, 2, 8, 1, 9, 0];
     */
    public static List<Integer> generateSpecIndexList(int specIndex, int maxIndex){
        List<Integer> resultList = new ArrayList<>();
        resultList.add(specIndex);
        int headCount = specIndex;
        int tailCount = maxIndex - specIndex;
        int range = Math.max(headCount, tailCount);
        for (int i = 0; i < range; i++){
            int times = i + 1;
            int headIndex = specIndex - times;
            int tailIndex = specIndex + times;
            if (headIndex >= 0){
                resultList.add(headIndex);
            }
            if (tailIndex <= maxIndex){
                resultList.add(tailIndex);
            }
        }
        return resultList;

    }
    public static float[] changeIntArrayToFloatArray(int[] intArray){
        float[] floatArray = new float[intArray.length];
        for (int i = 0; i < intArray.length; i++){
            floatArray[i] = (float)intArray[i];
        }
        return floatArray;
    }
    public static int[] changeFloatArrayToIntArray(float[] floatArray){
        int[] intArray = new int[floatArray.length];
        for (int i = 0; i < floatArray.length; i++){
            intArray[i] = (int)floatArray[i];
        }
        return intArray;
    }
    public static float getPicFitCenterInitFactor(int viewWidth, int viewHeight, int picWidth, int picHeight){
        if (picWidth == 0 || picHeight == 0){
            return 0f;
        }
        return Math.min((float)viewWidth / picWidth, (float)viewHeight / picHeight);
    }
    public static Matrix getPicFitCenterMatrix(int viewWidth, int viewHeight, int picWidth, int picHeight){
        Matrix matrix = new Matrix();
        float initFactor = getPicFitCenterInitFactor(viewWidth, viewHeight, picWidth, picHeight);
        int dx = (viewWidth - picWidth) / 2;
        int dy = (viewHeight - picHeight) / 2;
        matrix.postTranslate(dx, dy);
        matrix.postScale(initFactor, initFactor, (float)viewWidth / 2, (float)viewHeight / 2);
        return matrix;
    }
    public static boolean isPointArrayEqual(Point[] points1, Point[] points2){
        if (points1 == null || points2 == null){
            return false;
        }
        if (points1.length != points2.length){
            return false;
        }
        for (int i = 0; i < points1.length; i++){
            Point point1 = points1[i];
            Point point2 = points2[i];
            if (point1.x != point2.x || point1.y != point1.y){
                return false;
            }
        }
        return true;
    }
    /**
     * 将left, top, right, bottom补齐成8个坐标值。
     * @param coo4 left, top, right, bottom;
     * @return 返回包含8个整数的整数数组。依次为左上角，右上角，右下角，左下角的4个点的x,y坐标。
     */
    public static int[] Coo4ToCoo8(int[] coo4){
        int left = coo4[0];
        int top = coo4[1];
        int right = coo4[2];
        int bottom = coo4[3];
        int[] coo8 = new int[8];
        coo8[0] = left; coo8[1] = top; coo8[2] = right; coo8[3] = top; coo8[4] = right; coo8[5] = bottom; coo8[6] = left; coo8[7] = bottom;
        return coo8;
    }
    /**
     * 将left, top, right, bottom补齐成4个Android.graphics.Point。
     * @param coo4 left, top, right, bottom;
     * @return 返回包含4个Point的数组。依次为左上角，右上角，右下角，左下角的4个点。
     */
    public static Point[] Coo4ToPoint4(int[] coo4){
        int left = coo4[0];
        int top = coo4[1];
        int right = coo4[2];
        int bottom = coo4[3];
        Point[] points4 = new Point[4];
        points4[0].x = left; points4[0].y = top; points4[1].x = right; points4[1].y = top; points4[2].x = right; points4[2].y = bottom; points4[3].x = left; points4[3].y = bottom;
        return points4;
    }

    /**
     * 将包含4个点的对应的8个坐标值转化为数组
     * @param points 包含4个顶点的Point数组
     * @return
     */
    public static int[] Point4ToCoo8(Point[] points){
        int[] coo8 = new int[8];
        for (int i = 0; i < points.length; i++){
            Point point = points[i];
            coo8[2 * i] = point.x;
            coo8[2 * i + 1] = point.y;
        }
        return coo8;
    }
    public static int[] RectToCoo4(Rect rect){
        int[] coo4 = new int[4];
        coo4[0] = rect.left;
        coo4[1] = rect.top;
        coo4[2] = rect.right;
        coo4[3] = rect.bottom;
        return coo4;
    }
    public static int[] RectFToCoo4(RectF rectf){
        int[] coo4 = new int[4];
        coo4[0] = (int)rectf.left;
        coo4[1] = (int)rectf.top;
        coo4[2] = (int)rectf.right;
        coo4[3] = (int)rectf.bottom;
        return coo4;
    }
    public static Rect RectFToRect(RectF rectF){
        return new Rect((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
    }
    /*
     *
     */
    public static List<String> splitPath(String path){
        List<String> partsOfPath = new ArrayList<>();
        Matcher matcher = Pattern.compile("/[^/]+").matcher(path);
        while(matcher.find()){
            partsOfPath.add(matcher.group());
        }
        return partsOfPath;
    }
}

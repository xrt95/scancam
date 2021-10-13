package com.xrt.thirdpartylib.cv;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.TermCriteria;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.opencv.core.Core.BORDER_CONSTANT;

public class CvUtils {
    public static final int CHANNEL_FST = 1;
    public static final int CHANNER_SEC = 2;
    public static final int CHANNEL_TRD = 3;
    /**
     * Mat相减
     */
    public static Mat sub(Mat srcMat1, Mat srcMat2) {
        Mat dstMat = new Mat();
        Core.subtract(srcMat1, srcMat2, dstMat);
        return dstMat;
    }
    /**
     * Mat相加
     */
    public static Mat add(Mat srcMat1, Mat srcMat2){
        Mat dstMat = new Mat();
        Core.add(srcMat1, srcMat2, dstMat);
        return dstMat;
    }
    /**
     * Mat相乘
     */
    public static Mat multiply(Mat srcMat, double factor){
        Mat dstMat = new Mat();
        Core.multiply(srcMat, new Scalar(factor), dstMat);
        return dstMat;
    }
    /*
     * 按深度将Mat拆分
     */
    public static ArrayList<Mat> split(Mat srcMat) {
        ArrayList<Mat> splitedMat = new ArrayList<>();
        Core.split(srcMat, splitedMat);
        return splitedMat;
    }
    public static Mat adaptThreshold(Mat grayedSrcMat){
        Mat resultMat = new Mat();
        Imgproc.adaptiveThreshold(grayedSrcMat, resultMat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 5, 0);
        return resultMat;
    }
    public static Mat lighterProcess(Mat rgbSrcMat){
        return light(rgbSrcMat, 20);
    }
    public static Mat blackWhiteProcess(Mat rgbSrcMat){
        return selfProcess(rgbSrcMat, (r, g, b) -> {
            return contrastPixel(r, g, b, 100, 1.3f, 30, 0.8f, 255);
        });
    }
    public static Mat redBlackProcess(Mat rgbSrcMat){
        Mat contrastedMat = selfProcess(rgbSrcMat, (r, g, b) -> {
            return contrastPixel(r, g, b, 30, 1.3f, 30, 0.6f, 200);
        });
        return addColor(contrastedMat, rgbSrcMat, CHANNEL_FST);
    }
    public static Mat selfProcess(Mat rgbSrcMat, PixelProcess pixelProcesser){
        List<Mat> splitedMat = CvUtils.split(rgbSrcMat);
        Mat rmat = splitedMat.get(0);
        Mat gmat = splitedMat.get(1);
        Mat bmat = splitedMat.get(2);
        byte[] rdata = new byte[rmat.cols() * rmat.rows() * rmat.channels()];
        byte[] gdata = new byte[gmat.cols() * gmat.rows() * gmat.channels()];
        byte[] bdata = new byte[bmat.cols() * bmat.rows() * bmat.channels()];
        rmat.get(0,0, rdata);
        gmat.get(0,0, gdata);
        bmat.get(0,0, bdata);
        Mat rdstMat = new Mat(rmat.rows(), rmat.cols(), CvType.CV_8U);
        Mat gdstMat = new Mat(gmat.rows(), gmat.cols(), CvType.CV_8U);
        Mat bdstMat = new Mat(bmat.rows(), bmat.cols(), CvType.CV_8U);
        for (int i = 0; i < rdata.length; i++){
            int rval = rdata[i] & 0xff;
            int gval = gdata[i] & 0xff;
            int bval = bdata[i] & 0xff;
            byte[] resultRGB = pixelProcesser.processPixel(rval, gval, bval);
            //byte[] resultRGB = processPixel(rval, gval, bval, fallThreshold, greaterThreshold, greaterRate, lessThreshold, lessRate);
            rdata[i] = resultRGB[0];
            gdata[i] = resultRGB[1];
            bdata[i] = resultRGB[2];
        }
        rdstMat.put(0, 0, rdata);
        gdstMat.put(0, 0, gdata);
        bdstMat.put(0, 0, bdata);
        Mat colorResultMat = new Mat();
        List<Mat> colorResultMatList = new ArrayList<>();
        colorResultMatList.add(rdstMat);colorResultMatList.add(gdstMat);colorResultMatList.add(bdstMat);
        Core.merge(colorResultMatList ,colorResultMat);
        return colorResultMat;
    }
    public static Mat selfThreshold(Mat grayedSrcMat, Mat coloredSrcMat, int threshold, float greaterRate, float lessRate, boolean isMax, boolean isMin){
        List<Mat> splitedMat = CvUtils.split(coloredSrcMat);
        Mat rmat = splitedMat.get(0);
        Mat gmat = splitedMat.get(1);
        Mat bmat = splitedMat.get(2);
        byte[] rdata = new byte[rmat.cols() * rmat.rows() * rmat.channels()];
        byte[] gdata = new byte[gmat.cols() * gmat.rows() * gmat.channels()];
        byte[] bdata = new byte[bmat.cols() * bmat.rows() * bmat.channels()];
        rmat.get(0,0, rdata);
        gmat.get(0,0, gdata);
        bmat.get(0,0, bdata);
        int srcCols = grayedSrcMat.cols();
        int srcRows = grayedSrcMat.rows();
        int channels = grayedSrcMat.channels();
        byte[] srcData = new byte[srcCols * srcRows * channels];
        byte[] dstData = new byte[srcCols * srcRows * channels];
        grayedSrcMat.get(0, 0 , srcData);
        Mat resultMat = new Mat(srcRows, srcCols, CvType.CV_8U);
        Mat rdstMat = new Mat(srcRows, srcCols, CvType.CV_8U);
        Mat gdstMat = new Mat(srcRows, srcCols, CvType.CV_8U);
        Mat bdstMat = new Mat(srcRows, srcCols, CvType.CV_8U);
        for (int i = 0; i < srcData.length; i++){
            int srcVal = srcData[i] & 0xff;
            int rval = rdata[i] & 0xff;
            int gval = gdata[i] & 0xff;
            int bval = bdata[i] & 0xff;
            byte dstVal;
            byte rdst;
            byte gdst;
            byte bdst;
            if (srcVal >= threshold){
                if (isMax){
                    dstVal = (byte)(0xff);
                    rdst = (byte)(0xff);
                    gdst = (byte)(0xff);
                    bdst = (byte)(0xff);
                }else{
                    float v = srcVal * greaterRate;
                    if (v > 255){
                        dstVal = (byte)(0xff);
                    }else{
                        dstVal = (byte)((int)(srcVal * greaterRate) & 0xff);
                    }

                    float v1 = rval * greaterRate;
                    if (v1 > 255){
                        rdst = (byte)(0xff);
                    }else{
                        rdst = (byte)((int)(rval * greaterRate) & 0xff);
                    }

                    float v2 = gval * greaterRate;
                    if (v2 > 255){
                        gdst = (byte)(0xff);
                    }else{
                        gdst = (byte)((int)(gval * greaterRate) & 0xff);
                    }

                    float v3 = bval * greaterRate;
                    if (v3 > 255){
                        bdst = (byte)(0xff);
                    }else{
                        bdst = (byte)((int)(bval * greaterRate) & 0xff);
                    }

                }
            }else {
                if (isMin){
                    dstVal = 0;
                }else{
                    dstVal = (byte)((int)(srcVal * lessRate) & 0xff);
                }

                if (isMin){
                    rdst = 0;
                }else{
                    rdst = (byte)((int)(rval * lessRate) & 0xff);
                }

                if (isMin){
                    gdst = 0;
                }else{
                    gdst = (byte)((int)(gval * lessRate) & 0xff);
                }

                if (isMin){
                    bdst = 0;
                }else{
                    bdst = (byte)((int)(bval * lessRate) & 0xff);
                }
            }
            dstData[i] = dstVal;
            rdata[i] = rdst;
            gdata[i] = gdst;
            bdata[i] = bdst;
        }
        resultMat.put(0, 0, dstData);
        rdstMat.put(0, 0, rdata);
        gdstMat.put(0, 0, gdata);
        bdstMat.put(0, 0, bdata);
        Mat colorResultMat = new Mat();
        List<Mat> colorResultMatList = new ArrayList<>();
        colorResultMatList.add(rdstMat);colorResultMatList.add(gdstMat);colorResultMatList.add(bdstMat);
        Core.merge(colorResultMatList ,colorResultMat);
        return colorResultMat;
    }
    private static byte[] contrastPixel(int r, int g, int b, int greaterThreshold, float greaterRate, int lessThreshold, float lessRate, int maxVal){
        byte[] resultRGB = new byte[3];
        int avg = (int)((0.3 * r + 0.59 * g + 0.11 * b));
        byte tempVal;
        tempVal = (byte)(avg & 0xff);
        int v;
        maxVal = Math.max(maxVal, 255);
        if (avg > greaterThreshold){
            v = (int)(avg * greaterRate);
            if (v > maxVal){
                tempVal = (byte)(maxVal & 0xff);
            }else{
                tempVal = (byte)(v & 0xff);
            }
        }
        if (avg <= lessThreshold){
            v = (int)(avg * lessRate);
            if (v > maxVal){
                tempVal = (byte)(maxVal & 0xff);
            }else{
                tempVal = (byte)(v & 0xff);
            }
        }
        resultRGB[0] = tempVal;
        resultRGB[1] = tempVal;
        resultRGB[2] = tempVal;
        return resultRGB;
    }
    public static Mat threshold(Mat grayedSrcMat, int threshold, String bgColor){
        Mat resultMat = new Mat();
        int type;
        switch (bgColor){
            case "black":
               type = Imgproc.THRESH_BINARY_INV;
               break;
            case "white":
            default:
               type = Imgproc.THRESH_BINARY;
               break;
        }
        Imgproc.threshold(grayedSrcMat, resultMat, threshold, 255, type);
        return resultMat;
    }
    /*
     * 腐蚀操作
     */
    public static Mat erode(Mat srcMat){
        Mat sElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));
        Mat dstMat = new Mat();
        Imgproc.erode(srcMat, dstMat, sElement);
        sElement.release();
        return dstMat;
    }
    /*
     * 膨胀操作
     */
    public static Mat dilate(Mat srcMat){
        Mat sElement = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(2, 2));
        Mat dstMat = new Mat();
        Imgproc.dilate(srcMat, dstMat, sElement);
        sElement.release();
        return dstMat;
    }
    /*
     * 灰度操作
     */
    public static Mat gray(Mat srcMat){
        Mat dstMat = new Mat();
        Imgproc.cvtColor(srcMat, dstMat, Imgproc.COLOR_BGR2GRAY);
        return dstMat;
    }
    /*
     * 自定义的锐化操作
     */
    public static Mat sharp(Mat srcMat) {
        Mat bgrMat = CvUtils.cvtColor(srcMat, Imgproc.COLOR_RGB2BGR);
        Mat grayMat = erode(contrast(light(gray(bgrMat), 20), 1.2f));
        Mat rgbMat = cvtColor(grayMat, Imgproc.COLOR_GRAY2BGR);
        Mat resultMat = addColor(rgbMat, srcMat, CHANNEL_FST);
        return resultMat;
    }
    /**
     * 将srcMat的rgb中的其中一个颜色加到dstMat上去。
     */
    public static Mat addColor(Mat dstMat, Mat srcMat, int colorChannel){
        Mat remainOneColorMat = remainOneColor(srcMat, colorChannel);
        Mat resultMat = new Mat();
        Core.add(dstMat, remainOneColorMat, resultMat);
        return resultMat;
    }
    /*
     * 只保留srcMat的rgb中的某个颜色。
     */
    public static Mat remainOneColor(Mat srcMat, int colorChannel){
        Mat oneColorMat = getOneOfMat(srcMat, colorChannel);
        Mat secMat = Mat.zeros(new Size(oneColorMat.cols(), oneColorMat.rows()), oneColorMat.type());
        Mat trdMat = Mat.zeros(new Size(oneColorMat.cols(), oneColorMat.rows()), oneColorMat.type());
        ArrayList<Mat> mergeList = new ArrayList<>();
        switch (colorChannel){
            case CHANNEL_FST:
                mergeList.add(oneColorMat);
                mergeList.add(secMat);
                mergeList.add(trdMat);
                break;
            case CHANNER_SEC:
                mergeList.add(secMat);
                mergeList.add(oneColorMat);
                mergeList.add(trdMat);
                break;
            case CHANNEL_TRD:
                mergeList.add(secMat);
                mergeList.add(trdMat);
                mergeList.add(oneColorMat);
                break;
            default:
                mergeList.add(oneColorMat);
                mergeList.add(secMat);
                mergeList.add(trdMat);
                break;
        }
        Mat resultMat = new Mat();
        Core.merge(mergeList, resultMat);
        return resultMat;
    }
    /*
     * 仅返回srcMat的rgb中某个颜色的Mat
     */
    public static Mat getOneOfMat(Mat srcMat, int colorChannel){
        ArrayList<Mat> splitedMat = split(srcMat);
        Mat blueMat = splitedMat.get(0);
        Mat greenMat = splitedMat.get(1);
        Mat redMat = splitedMat.get(2);
        Mat avgMat;
        switch(colorChannel){
            case CHANNEL_FST:
                greenMat = multiply(greenMat, 0.5);
                redMat = multiply(redMat, 0.5);
                avgMat = add(greenMat, redMat);
                return sub(blueMat, avgMat);
            case CHANNER_SEC:
                blueMat = multiply(blueMat, 0.5);
                redMat = multiply(redMat, 0.5);
                avgMat = add(blueMat, redMat);
                return sub(greenMat, avgMat);
            case CHANNEL_TRD:
                redMat = multiply(redMat, 1);
                greenMat = multiply(greenMat, 0.5);
                blueMat = multiply(blueMat, 0.5);
                avgMat = add(greenMat, blueMat);
                return sub(redMat, avgMat);
        }
        return splitedMat.get(0);
    }
    /*
     * 边缘识别操作
     */
    public static Mat canny(Mat srcMat){
        Mat edge = new Mat();
        Imgproc.Canny(srcMat, edge, 50, 150, 3, true);
        return edge;
    }
    /**
     * @param arg 放大或缩小的比例值。
     * 对比度增强或弱化操作。本质上是按比例扩大或缩小Mat的值。
     */
    public static Mat contrast(Mat srcMat, float arg){
        Mat dstMat = new Mat();
        Core.multiply(srcMat, new Scalar(arg, arg, arg), dstMat);
        return dstMat;
    }
    /**
     * @param arg 增加或减小的值。
     * 亮度增加或减小。本质上是按固定值增加或减小Mat的值。
     */
    public static Mat light(Mat srcMat, int arg){
        Mat dstMat = new Mat();
        Core.add(srcMat, new Scalar(arg, arg, arg), dstMat);
        return dstMat;
    }
    /*
     * 高斯模糊操作。
     */
    public static Mat gasBlur(Mat srcMat){
        Mat dstMat = new Mat();
        Imgproc.GaussianBlur(srcMat, dstMat, new Size(0,0), 3);
        return dstMat;
    }
    public static Mat blur(Mat srcMat){
        Mat resultMat = new Mat();
        Imgproc.blur(srcMat, resultMat, new Size(2, 2));
        return resultMat;
    }
    /*
     * 将Mat转为Bitmap
     */
    public static Bitmap matToBitmap(Mat mat){
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }
    /*
     * 将Bitmap转为Mat
     */
    public static Mat bitmapToMat(Bitmap bitmap){
        Mat resultMat = new Mat();
        Utils.bitmapToMat(bitmap, resultMat);
        return resultMat;
    }
    /*
     * 将Mat保存到指定路径下
     */
    public static void savaMat(String path, Mat mat){
        Imgcodecs.imwrite(path, mat);
    }
    /*
     * 从指定路径的图片加载为Mat
     */
    public static Mat readMat(String path){return Imgcodecs.imread(path);}
    /**
     * @param colorType Imgproc类下xxx2xxx的静态常量。如Imgproc.GRAY2BGR。指定了颜色类型的转换
     * 将srcMat转换成指定colorType的Mat进行输出。
     */
    public static Mat cvtColor(Mat srcMat, int colorType) {
        Mat resultMat = new Mat();
        Imgproc.cvtColor(srcMat, resultMat, colorType);
        return resultMat;
    }
    /**
     * @param binMat 经过canny操作输出的二值Mat。
     * @param srcMat 通过srcMat获取宽高，创建与srcMat等宽高的输出Mat.
     * 画轮廓操作。
     */
    public static Mat drawContours(Mat binMat, Mat srcMat){
        Mat resultMat = Mat.zeros(srcMat.size(), srcMat.type());
        List<MatOfPoint> contours = findContours(binMat);
        List<MatOfPoint> filtedContours = filterContours(contours);
        for (int i = 0; i < filtedContours.size(); i++){
            Imgproc.drawContours(resultMat, filtedContours, i, new Scalar(255, 0, 0), 2);
        }
        return dilate(resultMat);
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
     * 将left, top, right, bottom补齐成4个Open.cv.Point。
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
    public static android.graphics.Point[] getRotatedMinAreaPoint4(Mat binMat, Mat srcMat){
        List<MatOfPoint> contours = findContours(binMat);
        List<MatOfPoint> filtedContours = filterContours(contours);
        List<MatOfPoint2f> coutours2f = new ArrayList<>();
        for (MatOfPoint point : filtedContours){
            MatOfPoint2f point2f = new MatOfPoint2f(point.toArray());
            coutours2f.add(point2f);
        }
        Point[] points = new Point[4];
        points[0] = new Point(0, 0);
        points[1] = new Point(srcMat.width(), 0);
        points[2] = new Point(srcMat.width(), srcMat.height());
        points[3] = new Point(0, srcMat.height());
        for (int i = 0; i < coutours2f.size(); i++){
            MatOfPoint2f contour = coutours2f.get(i);
            RotatedRect rrect = Imgproc.minAreaRect(contour);
            Point centerPoint = rrect.center;
            double distance = Imgproc.pointPolygonTest(contour, centerPoint, true);
            if (distance >= 500){
                rrect.points(points);
            }
        }
        Point[] orderAdjustPoints = new Point[4];
        orderAdjustPoints[0] = points[1];
        orderAdjustPoints[1] = points[2];
        orderAdjustPoints[2] = points[3];
        orderAdjustPoints[3] = points[0];
        return changeCvPointToAndroidPoint(orderAdjustPoints);
    }
    public static int[] getMinAreaCoo4(Mat binMat, Mat srcMat){
        Mat resultMat = Mat.zeros(srcMat.size(), srcMat.type());
        List<MatOfPoint> contours = findContours(binMat);
        List<MatOfPoint> filtedContours = filterContours(contours);
        List<MatOfPoint2f> coutours2f = new ArrayList<>();
        Rect rect = new Rect(0, 0, srcMat.width(), srcMat.height());
        for (MatOfPoint point : filtedContours){
            MatOfPoint2f point2f = new MatOfPoint2f(point.toArray());
            coutours2f.add(point2f);
        }
        for (int i = 0; i < coutours2f.size(); i++){
            MatOfPoint2f contour = coutours2f.get(i);
            RotatedRect rrect = Imgproc.minAreaRect(contour);
            Point centerPoint = rrect.center;
            double distance = Imgproc.pointPolygonTest(contour, centerPoint, true);
            if (distance >= 500){
                rect = rrect.boundingRect();
            }
        }
        int[] coo = new int[4];
        coo[0] = rect.x;
        coo[1] = rect.y;
        coo[2] = rect.x + rect.width;
        coo[3] = rect.y + rect.height;
        return coo;
    }
    public static android.graphics.Point[] getHullPoints(Mat binMat, Mat srcMat){
        List<MatOfPoint> contours = findContours(binMat);
        List<MatOfPoint> filtedContours = filterContours(contours);
        android.graphics.Point[] resultPoints = new android.graphics.Point[4];
        for (int k = 0; k < resultPoints.length; k++){
            resultPoints[k] = new android.graphics.Point(0, 0);
        }
        for (int i = 0; i < filtedContours.size(); i++){
            MatOfInt matOfInt = new MatOfInt();
            MatOfPoint contour = filtedContours.get(i);
            Imgproc.convexHull(contour, matOfInt, true);
            int[] indexs = matOfInt.toArray();
            Log.d("mxrt", "indexl:" + indexs.length);

            if (indexs.length == 4){
                Log.d("mxrt", "4");
                Point[] contourPoints = contour.toArray();
                Point[] convellPoints = new Point[indexs.length];
                for (int j = 0; j < indexs.length; j++){
                    convellPoints[j] = contourPoints[indexs[j]];
                }
                resultPoints = changeCvPointToAndroidPoint(convellPoints);
                return resultPoints;
            }
        }
        return resultPoints;
    }
    public static int[] getHull8Coo(Mat binMat, Mat srcMat){
        android.graphics.Point[] points = getHullPoints(binMat, srcMat);
        int[] resultCoo = new int[8];
        for (int i = 0; i < points.length; i++){
            android.graphics.Point point = points[i];
            resultCoo[2 * i] = point.x;
            resultCoo[2 * i + 1] = point.y;
        }
        return resultCoo;
    }
    /*
     * 过滤轮廓操作。
     */
    private static List<MatOfPoint> filterContours(List<MatOfPoint> contours){
        List<MatOfPoint> resultContours = new ArrayList<>();
        for (MatOfPoint contour : contours){
            RotatedRect area = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));
            double w = area.size.width;
            double h = area.size.height;
            if (w > 800 | h > 800){
                resultContours.add(contour);
            }
        }
        return resultContours;
    }
    /**
     * @param binMat 经过canny操作输出的二值Mat。
     * 找轮廓操作。
     */
    private static List<MatOfPoint> findContours(Mat binMat){
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        hierarchy.release();
        return contours;
    }
    /*
     * 直线检测操作。
     */
    public static Mat lineDetect(Mat srcMat){
        Mat resultMat = srcMat.clone();
        Mat edges = canny(srcMat);
        Mat lines = new Mat();
        Imgproc.HoughLinesP(edges, lines, 1, Math.PI / 180, 10, 200, 20);
        Mat zeroMat = Mat.zeros(srcMat.size(), srcMat.type());
        for (int i = 0; i < lines.rows(); i++){
            int[] oneLine = new int[4];
            lines.get(i, 0, oneLine);
            Imgproc.line(zeroMat, new Point(oneLine[0], oneLine[1]), new Point(oneLine[2], oneLine[3]), new Scalar(0, 0, 255), 2, 8, 0);
        }
        zeroMat.copyTo(resultMat);
        lines.release();
        edges.release();
        zeroMat.release();
        return resultMat;
    }
    /*
     * 圆角检测操作。
     */
    public static Mat cornerDetect(Mat srcMat){
        Mat grayMat = new Mat();
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        Mat responseMat = new Mat();
        Imgproc.cornerHarris(grayMat, responseMat, 2, 3, 0.04);
        Mat normalizeMat = new Mat();
        Core.normalize(responseMat, normalizeMat, 0, 255, Core.NORM_MINMAX, CvType.CV_32F);
        Mat resultMat = new Mat();
        resultMat.create(srcMat.size(), srcMat.type());
        srcMat.copyTo(resultMat);
        int width = normalizeMat.cols();
        int height = normalizeMat.rows();
        float[] data = new float[width * height];
        normalizeMat.get(0, 0, data);
        for (int i = 0; i < data.length; i++){
            if ((int)data[i] > 100){
                Imgproc.circle(resultMat, new Point(i%width, i/width), 5, new Scalar(0, 0, 255),2, 8, 0);
            }
        }
        grayMat.release();
        responseMat.release();
        normalizeMat.release();
        return resultMat;
    }
    /*
     * 通过十字采样线识别大致轮廓。返回轮廓的left, top, right, bottom坐标。
     */
    public static int[] getLTRBCoo(Mat srcMat){
        int channel = srcMat.channels();
        Mat grayMat = new Mat();
        if (channel > 1){
            Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        }
        else{
            grayMat = srcMat;
        }
        int width = grayMat.cols();
        int height = grayMat.rows();
        byte[] matData = new byte[width * height];
        grayMat.get(0,0, matData);
        Log.i("xrtd", "matData length:"+matData.length);
        int[] rowEndPoint = getRowEndPoint(width, height, matData, new float[]{0.5f});
        int[] colEndPoint = getColEndPoint(width, height, matData, new float[]{0.5f});
        int[] coo = new int[]{0, width, 0, height};
        coo[0] = rowEndPoint[0];//L
        coo[1] = colEndPoint[0];//T
        coo[2] = rowEndPoint[1] != 0?rowEndPoint[1]:width;//R
        coo[3] = colEndPoint[1] != 0?colEndPoint[1]:height;//B
        return coo;
    }
    public static int[] getRowEndPoint(int width, int height, byte[] matData, float[] args){
        int[] endPoint = new int[2];
        Collection<Integer> leftPoint = new ArrayList();
        Collection<Integer> rightPoint = new ArrayList();
        for (float arg:args){
            byte[] rowData = getOneRowData(width,  (int)(arg * height), matData);
            int[] rowEndPoint = rightLeftEndpoint(rowData, 0);
            leftPoint.add(rowEndPoint[0]);
            rightPoint.add(rowEndPoint[1]);
        }
        endPoint[0] = Collections.min(leftPoint);
        endPoint[1] = Collections.max(rightPoint);
        return  endPoint;
    }
    public static int[] getColEndPoint(int width, int height, byte[] matData, float[] args){
        int[] endPoint = new int[2];
        Collection<Integer> topPoint = new ArrayList();
        Collection<Integer> bottomPoint = new ArrayList();
        for (float arg:args){
            byte[] rowData = getOneColData(width, height, (int)(arg * width), matData);
            int[] rowEndPoint = rightLeftEndpoint(rowData, 0);
            topPoint.add(rowEndPoint[0]);
            bottomPoint.add(rowEndPoint[1]);
        }
        endPoint[0] = Collections.min(topPoint);
        endPoint[1] = Collections.max(bottomPoint);
        return  endPoint;
    }
    public static int[] rightLeftEndpoint(byte[] data,int tVal){
        int length = data.length;
        int[] resultIndex = new int[2];
        for (int i = length / 2; i >= 0; i--){
            if ((int)data[i] > tVal){
                resultIndex[0] = i;
                break;
            }
        }
        for (int i = length / 2; i < length; i++){
            if ((int)data[i] > tVal){
                resultIndex[1] = i;
                break;
            }
        }
        return resultIndex;
    }
    public static byte[] getOneRowData(int width, int rowId, byte[] data){
        int startIndex = width * rowId;
        byte[] resultData = new byte[width];
        for (int i = 0; i < width; i++){
            resultData[i] = data[startIndex + i];
        }
        return resultData;
    }
    public static byte[] getOneColData(int width, int height, int colId, byte[] data){
        byte[] resultData = new byte[height];
        for (int i = 0; i < height; i++){
            resultData[i] = data[i * width +colId];
        }
        return resultData;
    }
    public static Mat cropMat(Mat srcMat, int left, int right, int top, int bottom){
        ArrayList<Mat> splitedMat =  split(srcMat);
        Mat blueMat = splitedMat.get(0);
        Mat greenMat = splitedMat.get(1);
        Mat redMat = splitedMat.get(2);
        Mat blueCropMat = cropOneMat(blueMat, left, right, top, bottom);
        Mat greenCropMat = cropOneMat(greenMat, left, right, top, bottom);
        Mat redCropMat = cropOneMat(redMat, left, right, top, bottom);
        ArrayList<Mat> mergeList = new ArrayList<>();
        mergeList.add(blueCropMat);
        mergeList.add(greenCropMat);
        mergeList.add(redCropMat);
        Mat resultMat = new Mat();
        Core.merge(mergeList, resultMat);
        blueMat.release(); greenMat.release(); redMat.release();
        blueCropMat.release(); greenCropMat.release(); redCropMat.release();
        return resultMat;
    }
    public static Mat cropOneMat(Mat oneMat, int left, int right, int top, int bottom){
        int matWidth = oneMat.cols();
        int matHeight = oneMat.rows();
        int cropWidth = right - left;
        int cropHeight = bottom - top;
        int index = top * matWidth + left;
        Log.i("xrtd", "" + matWidth + "|" + matHeight +  "|" +cropWidth +  "|" +cropHeight +  "|" +index);
        byte[] cropData = new byte[cropWidth * cropHeight];
        byte[] matData = new byte[matWidth * matHeight];
        oneMat.get(0, 0, matData);
        for (int i = 0; i < cropHeight; i++){
            for (int j = 0; j < cropWidth; j++){
                cropData[i * cropWidth + j] = matData[index+j];
            }
            index = index + matWidth;
        }
        Mat resultMat = new Mat(new Size(cropWidth, cropHeight), oneMat.type());
        Log.i("xrtd", "oneMat type:" + oneMat.type());
        resultMat.put(0, 0, cropData);
        return resultMat;
    }
    /**
     * @param srcMat 输入Mat
     * @param targetCoors 给定的left, top, right, bottom，来指定目标区域。
     * 透视变换操作。
     */
    public static Mat perspectiveChange(Mat srcMat, int[] targetCoors){
        int srcWidth = srcMat.width();
        int srcHeight = srcMat.height();
        Point[] srcCorners = new Point[4];
        srcCorners[0] = new Point(targetCoors[0], targetCoors[1]);
        srcCorners[1] = new Point(targetCoors[2], targetCoors[1]);
        srcCorners[2] = new Point(targetCoors[0], targetCoors[3]);
        srcCorners[3] = new Point(targetCoors[2], targetCoors[3]);
        int dstWidth = targetCoors[2] - targetCoors[0];
        int dstHeight = targetCoors[3] - targetCoors[1];
        Point[] dstCorners = new Point[4];
        dstCorners[0] = new Point(0, 0);
        dstCorners[1] = new Point(dstWidth, 0);
        dstCorners[2] = new Point(0, dstHeight);
        dstCorners[3] = new Point(dstWidth, dstHeight);
        Mat srcCornerMat = new MatOfPoint2f(srcCorners);
        Mat dstCornerMat = new MatOfPoint2f(dstCorners);
        Mat warpMat = Imgproc.getPerspectiveTransform(srcCornerMat, dstCornerMat);
        Mat resultMat = new Mat(dstHeight, dstWidth, srcMat.type());
        Imgproc.warpPerspective(srcMat, resultMat, warpMat, resultMat.size(), Imgproc.INTER_LINEAR, BORDER_CONSTANT);
        return resultMat;
    }
    /**
     * @param srcMat
     * @param scanedPoints 指定目标区域的四个点。Point是android.graphics包下的。
     * 给定四个选点，透视变换出目标区域的图像。效果上类似截出指定区域的图像。
     */
    public static Mat perspectiveChange(Mat srcMat, android.graphics.Point[] scanedPoints){
        Point[] srcPoint;
        srcPoint = changeAndroidPointToCvPoint(scanedPoints);
        Point leftTopPoint = srcPoint[0];
        Point rightTopPoint = srcPoint[1];
        Point leftBottomPoint = srcPoint[2];
        Point rightBottomPoint = srcPoint[3];
        int xMax1 = Math.max(Math.abs((int)(leftTopPoint.x - rightTopPoint.x)), Math.abs((int)(leftTopPoint.x - rightBottomPoint.x)));
        int xMax2 = Math.max(Math.abs((int)(leftBottomPoint.x - rightTopPoint.x)), Math.abs((int)(leftBottomPoint.x - rightBottomPoint.x)));
        int dstWidth = Math.min(xMax1, xMax2);
        int yMax1 = Math.max(Math.abs((int)(leftTopPoint.y - leftBottomPoint.y)), Math.abs((int)(leftTopPoint.y - rightBottomPoint.y)));
        int yMax2 = Math.max(Math.abs((int)(rightTopPoint.y - leftBottomPoint.y)), Math.abs((int)(rightTopPoint.y - rightBottomPoint.y)));
        int dstHeight = Math.max(yMax1, yMax2);
        Point[] dstCorners = new Point[4];
        dstCorners[0] = new Point(0, 0);
        dstCorners[1] = new Point(dstWidth, 0);
        dstCorners[2] = new Point(0, dstHeight);
        dstCorners[3] = new Point(dstWidth, dstHeight);
        Mat srcCornerMat = new MatOfPoint2f(srcPoint);
        Mat dstCornerMat = new MatOfPoint2f(dstCorners);
        Mat warpMat = Imgproc.getPerspectiveTransform(srcCornerMat, dstCornerMat);
        Mat resultMat = new Mat(dstHeight, dstWidth, srcMat.type());
        Imgproc.warpPerspective(srcMat, resultMat, warpMat, new Size(dstWidth, dstHeight), Imgproc.INTER_LINEAR, BORDER_CONSTANT);
        return resultMat;
    }
    /*
     * 将android.graphics.Point转为opencv.core.Point
     */
    public static Point[] changeAndroidPointToCvPoint(android.graphics.Point[] points){
        Point[] resultPoints = new Point[points.length];
        for (int i = 0; i < points.length; i++){
            Point point = new Point();
            android.graphics.Point androidPoint = points[i];
            point.x = androidPoint.x;
            point.y = androidPoint.y;
            resultPoints[i] = point;
        }
        return resultPoints;
    }
    /*
     * 将opencv.core.Point转为Android.graphsic.Point
     */
    public static android.graphics.Point[] changeCvPointToAndroidPoint(Point[] cvPoints){
        android.graphics.Point[] resultPoints = new android.graphics.Point[cvPoints.length];
        for (int i = 0; i < cvPoints.length; i++){
            android.graphics.Point point = new android.graphics.Point();
            Point cvPoint = cvPoints[i];
            point.x = (int)cvPoint.x;
            point.y = (int)cvPoint.y;
            resultPoints[i] = point;
        }
        return resultPoints;
    }
    /**
     * @param srcMat 输入的灰度图。
     * 灰度均衡化操作。srcMat一定要灰度图。
     */
    public static Mat equalizeHist(Mat srcMat){
        Mat resultMat = new Mat();
        Imgproc.equalizeHist(srcMat, resultMat);
        return resultMat;
    }
    /**
     * @param srcMat 数据类型一定要是8U3C。通常直接读取的图片同时RGBA的，一定要转为3通道再输入。
     * 颜色均衡操作。输出Mat 数据类型和通道数和srcMat一致。
     */
    public static Mat meanShiftFilter(Mat srcMat){
        Mat resultMat = new Mat();
        TermCriteria t = new TermCriteria(TermCriteria.COUNT|TermCriteria.EPS, 1, 0.1);
        Imgproc.pyrMeanShiftFiltering(srcMat, resultMat, 2, 10, 2, t);
        return resultMat;
    }
    public static Bitmap cropWith4Points(Bitmap pic, android.graphics.Point[] selectedPoints){
        Mat srcMat = CvUtils.bitmapToMat(pic);
        Mat resultMat = CvUtils.perspectiveChange(srcMat, selectedPoints);
        Bitmap newPic = CvUtils.matToBitmap(resultMat);
        return newPic;
    }
    interface PixelProcess{
        byte[] processPixel(int r, int g, int b);
    }
}

package com.xrt.thirdpartylib.itext;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class PdfUtils {

    /**
     * @param imgPaths 图片路径
     * @param pdfOutputPath pdf的输出路径
     * @param outputQuality 图片的输出质量 取值范围0-100
     * 给定图片路径，将图片转为pdf输出到指定路径。
     */
    public static void imgToPdf(List<String> imgPaths, String pdfOutputPath, int outputQuality){
        Document document = new Document();
        File outputFile = new File(pdfOutputPath);
        try(FileOutputStream outputStream = new FileOutputStream(outputFile)){
            PdfWriter.getInstance(document, outputStream);
            document.open();
            document.setMargins(0, 0, 0, 0);
            for (int i = 0; i < imgPaths.size(); i++){
                String imgPath = imgPaths.get(i);
                Bitmap pic = BitmapFactory.decodeFile(imgPath);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                pic.compress(Bitmap.CompressFormat.JPEG, outputQuality, stream);
                Image image = Image.getInstance(stream.toByteArray());
                float widthScaleRate = ((document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin()) / image.getWidth()) * 100;
                float heightScaleRate = ((document.getPageSize().getHeight() - document.topMargin() - document.bottomMargin()) / image.getHeight()) * 100;
                float scaleRate = Math.min(widthScaleRate, heightScaleRate);
                image.scalePercent(scaleRate);
                image.setAlignment(Image.ALIGN_CENTER);
                document.newPage();
                document.add(image);
            }
            document.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}

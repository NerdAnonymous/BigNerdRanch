package com.nerd.crimenote;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;

/**
 * 图片处理工具类
 * <p>
 * 图片占用内存计算公式
 * 占用内存=图片长*图片宽*字节
 * 图片长=图片原始长*（设备dpi/文件dpi）
 * 图片宽=图片原始宽*（设备dpi/文件dpi）
 * <p>
 * MDPI(160DPI)
 * HDPI(240DPI)
 * XHDPI(320DPI)
 * XXHDPI(480DPI)
 * XXXHDPI(640DPI)
 * <p>
 * dip=设备独立像素
 * dpi=每英寸像素点
 * <p>
 * 像素计算公式：px=(dpi/160)*dip
 */
public class PictureUtils {

    public static Bitmap getScaledBitmap(String path, Activity activity) {
        Point point = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(point);
        return getScaledBitmap(path, point.x, point.y);
    }

    public static Bitmap getScaledBitmap(String path, int destWidth, int destHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path);

        int srcWidth = options.outWidth;
        int srcHeight = options.outHeight;

        int inSampleSize = 1;
        if (srcWidth > destWidth || srcHeight > destHeight) {
            if (srcWidth > srcHeight) {
                inSampleSize = Math.round(srcHeight / destHeight);
            } else {
                inSampleSize = Math.round(srcWidth / destWidth);
            }
        }

        options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;

        return BitmapFactory.decodeFile(path, options);
    }
}

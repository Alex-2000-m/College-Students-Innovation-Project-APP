package com.example.demo1;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.Log;

public class PictureProcess {
    public static int[] bitmapBit;
    public static byte[] bitmapByte;

    //  该类测试代码
    public static void test(){
        Log.d("test","hello cjgg");
    }

    //  压缩图片到指定宽高
    public static Bitmap resizeBitmap(String imgPath, int w, int h)
    {
        Bitmap bitmap = BitmapFactory.decodeFile(imgPath);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scaleWidth = ((float) w) / width;
        float scaleHeight = ((float) h) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);//使用bitmap，从(x,y)点开始，截取宽width，高height，根据matrix进行缩放，filter=true表示对其进行剪裁操作
        return resizedBitmap;
    }


    //将图片二值化
    public static Bitmap singleThreshold(final Bitmap bm, int digit) {

        int width = bm.getWidth();
        int height = bm.getHeight();
        int color;
        int r, g, b, a;
        Log.d("bitmap",width+" "+height);
        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);//创建一个图片对象

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];

        bitmapBit = new int[width * height];
        bitmapByte = new byte[width * height/8];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height); //获取图片的颜色像素

        for (int j = 0; j < width * height; j++) {
            //获取单个颜色的argb数据
            color = oldPx[j];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
            a = Color.alpha(color);
            //计算单点的灰度值
            int gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);
            //根据阈值对比，低于的设置为黑色，高于的设置为白色
            if(gray < digit) {
                bitmapBit[j] = 0;
                gray = 0;
            } else {
                bitmapBit[j] = 1;
                gray = 255;
            }
            newPx[j] = Color.argb(a,gray,gray,gray);
        }

        bitmapByte = BitMapArrayToByteMapArray(bitmapBit,width*height);
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        return bmp;
    }

    //  提供位数组转换成字节数组来存储
    public static byte[] BitMapArrayToByteMapArray(int[] bitMapArray,int bitLen){
        int byteLen = bitLen / 8;
        byte ByteMapArray[] = new byte[byteLen];
        byte bitToByte = 0;
        for(int i=0;i<bitLen;i++){
            bitToByte += bitMapArray[i] << (7-(i%8));
            if(i%8 == 0){
                ByteMapArray[i/8] = bitToByte;
                bitToByte = 0;
            }
        }
        return ByteMapArray;
    }
}

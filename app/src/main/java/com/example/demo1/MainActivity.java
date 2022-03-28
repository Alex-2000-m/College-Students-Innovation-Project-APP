package com.example.demo1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LogPrinter;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.annotation.Target;
import java.io.File;
import java.lang.reflect.Array;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    public static final int CHOOSE_PHOTO=2;
    private ImageView picture;
    private Uri imageUri;
    byte[] bitmapByte;
    Bitmap compressImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        picture = (ImageView) findViewById(R.id.picture);
        Button chooseFromAlbum = (Button) findViewById(R.id.choose_from_Album);//从相册选择照片的逻辑部分
        chooseFromAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                }else {
                    openAlbum();
                }
            }
        });
    }

    //保存byte数组到文件
    public void save(byte[] bytes) {
        String stringBytes = bytes.toString();
//        String stringBytes = Arrays.toString(bytes);
        FileOutputStream out = null;
        BufferedWriter writer = null;
        try {
            out = openFileOutput("data", Context.MODE_PRIVATE);
            writer = new BufferedWriter(new OutputStreamWriter(out));
            writer.write(stringBytes);
            Log.d("save", "写入成功！" + stringBytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //    图像处理代码区域
    public static Bitmap singleThreshold(final Bitmap bm,int digit) {

        int width = bm.getWidth();
        int height = bm.getHeight();
        int color;
        int r, g, b, a;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);//创建一个图片对象

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
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
                gray = 0;
            } else {
                gray = 255;
            }
            newPx[j] = Color.argb(a,gray,gray,gray);
        }

        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        return bmp;
    }


//    public static Bitmap getImage(String srcPath){
//        BitmapFactory.Options newOpts = new BitmapFactory.Options();
//        newOpts.inJustDecodeBounds = true;
//        Bitmap bitmap = BitmapFactory.decodeFile(srcPath,newOpts);
//        newOpts.inJustDecodeBounds = false;
//        int w= newOpts.outWidth;
//        int h= newOpts.outHeight;
//        float hh = 200f;
//        float ww = 200f;
//        int be = 0;
//        if (w>h && w>ww){
//            be=(int)(newOpts.outWidth/ww);
//        }else if (w<=h && h>hh){
//            be=(int)(newOpts.outHeight/hh);
//        }
//        newOpts.inSampleSize=be;
//        bitmap = BitmapFactory.decodeFile(srcPath,newOpts);
//        return bitmap;
//    }

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



//      图像代码处理区域

    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO);//打开相册
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CHOOSE_PHOTO:
                Log.d("choosePhotoStage","进入选择图片阶段");

                    if(Build.VERSION.SDK_INT>=19){
                        //4.4以上系统使用这个方法处理图片
                        handleImageOnKitKat(data);
                        Log.d("over4.4","over4.4");
                    }else{
                        //4.4以下系统使用这个方法处理图片
                        handleImageBeforeKitKat(data);
                        Log.d("below4.4","below4.4");
                    }

                break;
            default:
                break;
        }
    }
    @TargetApi(19)
    private void handleImageOnKitKat(Intent data) {
        Log.d("test1","handle1方法被调用");
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
                if(imagePath==null){
                    Log.e("imagePath","null!!!!");
                }
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri= ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.valueOf(docId));
                imagePath = getImagePath(contentUri,null);
            }
        }else if("content".equalsIgnoreCase(uri.getScheme())){
            imagePath=getImagePath(uri,null);
        }else if("file".equalsIgnoreCase(uri.getScheme())){
            imagePath=uri.getPath();
        }
        compressImage=resizeBitmap(imagePath,200,200);
        displayImage(compressImage);
    }
    private void handleImageBeforeKitKat(Intent data){
        Log.d("test1","handle2方法被调用");
        Uri uri = data.getData();
        String imagePath=getImagePath(uri,null);
        compressImage=resizeBitmap(imagePath,200,200);
        displayImage(compressImage);
    }

    @SuppressLint("Range")
    private String getImagePath(Uri uri, String selection){
        String path = null;
        Cursor cursor = getContentResolver().query(uri,null,selection,null,null);
        if(cursor!=null){
            if(cursor.moveToFirst()){
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

//展示图片
    private void displayImage(Bitmap compressImage) {
        Log.d("showmsg", "进入展示图片的阶段");
            Bitmap bitmap = compressImage;
            Log.d("???", "zhi:" + bitmap);
            //picture.setImageBitmap(bitmap);
            Bitmap twoColorBitmap = singleThreshold(bitmap, 127);//灰度、二值化处理,twoColorBitmap变量是处理后的图像的bitmap对象
            picture.setImageBitmap(twoColorBitmap);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            if (twoColorBitmap != null) {
                twoColorBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                bitmapByte = stream.toByteArray();//暂时变成byte数组
                if (bitmapByte != null) {
                    Log.d("bitmapByte", "bitmapByte: " + Arrays.toString(bitmapByte).length());//显示byte的值
                }
            } else {
                Toast.makeText(this, "获取图片失败", Toast.LENGTH_SHORT).show();
            }
    }
}
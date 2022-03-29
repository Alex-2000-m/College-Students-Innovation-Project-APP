package com.example.demo1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    public static final int CHOOSE_PHOTO=2;
    private ImageView picture;
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
        compressImage=PictureProcess.resizeBitmap(imagePath,200,200);
        displayImage(compressImage);
    }
    private void handleImageBeforeKitKat(Intent data){
        Log.d("test1","handle2方法被调用");
        Uri uri = data.getData();
        String imagePath=getImagePath(uri,null);
        compressImage=PictureProcess.resizeBitmap(imagePath,200,200);
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
        Log.d("displayImage", "进入展示图片的阶段");
        Bitmap bitmap = compressImage;
        Bitmap twoColorBitmap = PictureProcess.singleThreshold(bitmap, 127);//灰度、二值化处理,twoColorBitmap变量是处理后的图像的bitmap对象
        if (twoColorBitmap != null) {
            picture.setImageBitmap(twoColorBitmap);
            Log.d("displayImage", "图片展示成功:"+PictureProcess.bitmapByte.length);//显示byte的值
        } else {
            Toast.makeText(this, "获取图片失败", Toast.LENGTH_SHORT).show();
        }
    }
}
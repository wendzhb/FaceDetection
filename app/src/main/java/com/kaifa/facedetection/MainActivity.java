package com.kaifa.facedetection;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    public static final String TAG = MainActivity.class.getSimpleName();

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native void loadModel(String path);

    public native void process(Bitmap bitmap);

    public native void setSurface(Surface surface, int width, int heigh);

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private SurfaceView surfaceView;
    private ProgressDialog progressDialog;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                File dir = new File(Environment.getExternalStorageDirectory(), "face");
                copyAssetsFile("haarcascade_frontalface_alt.xml", dir);
                File file = new File(dir, "haarcascade_frontalface_alt.xml");
                loadModel(file.getAbsolutePath());


            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                dissmissLoading();
            }

            @Override
            protected Void doInBackground(Void... voids) {
                showLoading();

                return null;
            }
        }.execute();

    }

    private void copyAssetsFile(String name, File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir, name);
        if (!file.exists()) {
            try {
                InputStream is = getAssets().open(name);
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int len;

                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }

                fos.flush();
                fos.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void dissmissLoading() {
        progressDialog.dismiss();
    }

    private void showLoading() {
        progressDialog.show();
    }

    public void fromAlbum(View view) {
        Intent intent;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        intent.setType("image/*");
        //使用选取器并自定义标题
        startActivityForResult(Intent.createChooser(intent, "选择带识别图片"), 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 100 && data != null) {
            getResult(data.getData());
        }
    }

    private void getResult(Uri uri) {
//        safeRecycled();
        String imagePath = null;
        if (uri != null) {
            //魅族手机上发现有一个相册管家，从这里选取图片会得到类似：file:///storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport145545454.jpg 的URI
            if ("file".equals(uri.getScheme())) {
                Log.i(TAG, "path uri 获取图片");
                imagePath = uri.getPath();
            } else if ("content".equals(uri.getScheme())) {
                Log.i(TAG, "content uri 获取图片");
                String[] filePathColums = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(uri, filePathColums, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndex(filePathColums[0]);
                        imagePath = cursor.getString(columnIndex);
                    }
                    cursor.close();
                }
            }
        }

        if (!TextUtils.isEmpty(imagePath)) {
            bitmap = toBitmap(imagePath);
            safeProcess();
        }
    }

    private void safeProcess() {
        if (bitmap != null && !bitmap.isRecycled()){
            process(bitmap);
        }
    }

    private Bitmap toBitmap(String imagePath) {
        if (TextUtils.isEmpty(imagePath)) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        int width_tmp = options.outWidth, height_tmp = options.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp <= 640 && height_tmp <= 480) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = scale;
        opts.outWidth = width_tmp;
        opts.outHeight = height_tmp;

        return BitmapFactory.decodeFile(imagePath, opts);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        setSurface(holder.getSurface(),640,480);
        safeProcess();
    }



    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}

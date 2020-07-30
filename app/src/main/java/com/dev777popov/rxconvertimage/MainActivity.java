package com.dev777popov.rxconvertimage;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.observers.DefaultObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private final static int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 111;
    private static int RESULT_LOAD_IMAGE = 1;
    private Bitmap bitmap;
    private LinearLayout progress;
    private LinearLayout lImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progress = findViewById(R.id.lProgress);
        lImage = findViewById(R.id.lImage);

        checkPermission();

        Button buttonLoadImage = findViewById(R.id.buttonLoadPicture);
        buttonLoadImage.setOnClickListener(arg0 -> {
            Intent i = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, RESULT_LOAD_IMAGE);
        });
    }


    private void rxMethod() {
        lImage.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        Observable.just(bitmap)
                .subscribeOn(Schedulers.io())
                .map(this::saveToSD)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DefaultObserver<Bitmap>() {
                    @Override
                    public void onNext(@NonNull Bitmap bitmap) {
                        if (bitmap != null) {
                            lImage.setVisibility(View.VISIBLE);
                            progress.setVisibility(View.GONE);
                            Toast.makeText(getApplicationContext(), getText(R.string.success), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Toast.makeText(getApplicationContext(), getText(R.string.failed), Toast.LENGTH_SHORT).show();
                        lImage.setVisibility(View.VISIBLE);
                        progress.setVisibility(View.GONE);
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }


    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);

        } else {
            return true;
        }
        return false;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = null;
            if (selectedImage != null) {
                cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
            }
            if (cursor != null) {
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();
                ImageView imageView = (ImageView) findViewById(R.id.imgView);
                imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                bitmap = BitmapFactory.decodeFile(picturePath);

                rxMethod();
            }
        }
    }


    public Bitmap saveToSD(Bitmap outputImage) {

        File sdCardDirectory = Environment.getExternalStorageDirectory();
        File image = new File(sdCardDirectory, "download.png");
        boolean success = false;
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(image);
            outputImage.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (success) {
            return bitmap;

        } else {
            return null;
        }

    }

}
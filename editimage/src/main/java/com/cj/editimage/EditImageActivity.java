package com.cj.editimage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;

import com.cj.editimage.helper.Util;
import com.cj.editimage.widget.EditImageView;
import com.cj.editimage.widget.ProgressDialog;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;


public class EditImageActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * 图片是否被编辑过
     */
    public static final String IS_EDITED = "extra:isEdited";
    private static final String EXTRA_IMAGE_PATH = "extra:imagePath";
    private static final String EXTRA_BACK_IMAGE_PATH = "extra:backImagePath";
    private EditImageView editImageView;
    private String imagePath;
    private String backImagePath;
    private RadioButton rbLine;
    private RadioButton rbOval;
    private RadioButton rbRect;
    private RadioButton rbCancel;
    private InternalHandle handler;
    private ProgressDialog progressDialog;

    public static Intent getIntent(Context context, String imagePath, String backImagePath) {
        Intent i = new Intent(context, EditImageActivity.class);
        i.putExtra(EXTRA_IMAGE_PATH, imagePath);
        i.putExtra(EXTRA_BACK_IMAGE_PATH, backImagePath);
        return i;
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getExtras() == null) {
            return;
        }

        Bundle extras = intent.getExtras();
        imagePath = extras.getString(EXTRA_IMAGE_PATH);
        backImagePath = extras.getString(EXTRA_BACK_IMAGE_PATH);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_image);
        handler = new InternalHandle(this);

        Util.setTranslucentStatus(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        editImageView = findViewById(R.id.edit_image_view);
        rbLine = findViewById(R.id.btn_line);
        rbOval = findViewById(R.id.btn_oval);
        rbRect = findViewById(R.id.btn_rect);
        rbCancel = findViewById(R.id.btn_cancel);

        rbLine.setOnClickListener(this);
        rbOval.setOnClickListener(this);
        rbRect.setOnClickListener(this);
        rbCancel.setOnClickListener(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        getSupportActionBar().setTitle("");

        handleIntent(getIntent());

        if (!TextUtils.isEmpty(imagePath)) {
            Bitmap smallBitmap = getSmallBitmap(imagePath, 1080, 1080);
            editImageView.setImageBitmap(smallBitmap);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_picture, menu);
        MenuItem item = menu.findItem(R.id.menu_save);
        item.setOnMenuItemClickListener(item1 -> {
            if (editImageView.isEdited()) {
                savePicture();

            } else {
                completeSaveBitmap();
            }
            return false;
        });
        return super.onCreateOptionsMenu(menu);
    }

    private void backImage() {
        Util.copyFile(this, imagePath, backImagePath);
    }

    @SuppressLint("CheckResult")
    private void savePicture() {
        progressDialog = new ProgressDialog();
        progressDialog.show(getSupportFragmentManager(), "save");
        new Thread(() -> {
            //如果编辑了图片备份原图
            backImage();
            Bitmap customBitmap = editImageView.getCustomBitmap();
            saveBitmap(customBitmap);
            if (customBitmap != null && !customBitmap.isRecycled()) {
                customBitmap.recycle();
            }
            handler.sendEmptyMessage(200);
        }).start();
    }

    /**
     * 得到压缩后的图片
     *
     * @param imagePath    图片路径
     * @param targetWidth  目标宽度
     * @param targetHeight 目标高度
     * @return
     */
    public Bitmap getSmallBitmap(String imagePath, int targetWidth, int targetHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        int sourceWidth = options.outWidth;
        int sourceHeight = options.outHeight;

        int sampleSize = computeSampleSize(sourceWidth, sourceHeight, targetWidth, targetHeight);
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        return BitmapFactory.decodeFile(imagePath, options);
    }

    private int computeSampleSize(int imageWidth, int imageHeight,
                                  int targetWidth, int targetHeight) {
        int sampleSize = 1;
        while (imageWidth / sampleSize > targetWidth || imageHeight / sampleSize > targetHeight) {
            sampleSize = sampleSize << 1;
        }
        return sampleSize;
    }

    private void saveBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }

        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(imagePath), 1024 * 8);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void onBackPressed() {
        savePicture();
    }

    @Override
    public void onClick(View v) {
        if (v == rbLine) {
            editImageView.drawLine();

        } else if (v == rbOval) {
            editImageView.drawOval();

        } else if (v == rbRect) {
            editImageView.drawRect();

        } else if (v == rbCancel) {
            editImageView.cancelPreviousDraw();

        }
    }

    private void completeSaveBitmap() {
        if (progressDialog != null) {
            progressDialog.dismissAllowingStateLoss();
        }
        Intent i = new Intent();
        i.putExtra(IS_EDITED, editImageView.isEdited());
        setResult(RESULT_OK, i);
        super.onBackPressed();
    }

    private static class InternalHandle extends Handler {

        private WeakReference<EditImageActivity> ref;

        InternalHandle(EditImageActivity activity) {
            ref = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 200:
                    if (ref.get() != null) {
                        ref.get().completeSaveBitmap();
                    }
                    break;
            }
        }
    }
}

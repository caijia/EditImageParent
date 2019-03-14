package com.cj.editimageparent;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.cj.editimage.EditImageActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void editImage1(View view) {
        String imagePath = Environment.getExternalStorageDirectory() + "/aa.jpg";
        String backPath = Environment.getExternalStorageDirectory() + "/aa-back-img.jpg";
        Intent i = EditImageActivity.getIntent(this, imagePath, backPath);
        startActivityForResult(i, 100);
    }

    public void editImage2(View view) {
        String imagePath = Environment.getExternalStorageDirectory() + "/aa-back-img.jpg";
        String backPath = Environment.getExternalStorageDirectory() + "/aa11.jpg";
        Intent i = EditImageActivity.getIntent(this, imagePath, backPath);
        startActivityForResult(i, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }

        boolean isEdited = data.getBooleanExtra(EditImageActivity.IS_EDITED, false);
        Toast.makeText(this, isEdited ? "编辑了" : "没编辑", Toast.LENGTH_SHORT).show();
    }
}

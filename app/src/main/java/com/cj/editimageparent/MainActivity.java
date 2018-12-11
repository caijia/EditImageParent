package com.cj.editimageparent;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.cj.editimage.EditImageActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void editImage1(View view) {
        Intent i = EditImageActivity.getIntent(this,
                Environment.getExternalStorageDirectory() + "/cai.jpg");
        startActivity(i);
    }
}

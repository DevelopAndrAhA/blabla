package com.lesa_humdet;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class FullImageActivity extends AppCompatActivity {
    Conf conf = new Conf();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_full_image);
        ProgressBar progressBar = findViewById(R.id.progressBar2);
        ImageView personImg = findViewById(R.id.personImgFull);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.iconhumdet);
        personImg.setImageBitmap(bitmap);
        getSupportActionBar().hide();
        try{
            Picasso.with(this)
                    .load(conf.getDomen()+"image?imgname="+getIntent().getStringExtra("photoName")+".jpg/")
                    .placeholder(R.drawable.hum_icon)
                    .into(personImg, new Callback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError() {

                        }
                    });
        }catch (Exception e){}
    }
}
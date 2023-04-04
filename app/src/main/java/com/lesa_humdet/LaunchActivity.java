package com.lesa_humdet;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.lesa_humdet.db.AlohaDb;
import com.lesa_humdet.db.City;
import com.lesa_humdet.db.Country;
import com.lesa_humdet.db.Region;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LaunchActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        ImageView imageView = findViewById(R.id.imageView);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.iconhumdet);
        imageView.setImageBitmap(bitmap);
        getSupportActionBar().hide();


        String custom_font = "fonts/JackportRegularNcv.ttf";



        Typeface CF = Typeface.createFromAsset(getAssets(), custom_font);
        ((TextView) findViewById(R.id.textView)).setTypeface(CF);

        ImageButton button = findViewById(R.id.imageButton2);

        button.setOnClickListener(e -> {
            Intent intent = new Intent(this,ResultSearchActivity.class);
            intent.putExtra("main",true);
            startActivity(intent);
        });



        AlohaDb alohaDb = new AlohaDb(LaunchActivity.this);
        SQLiteDatabase sqLiteDatabase = alohaDb.getReadableDatabase();
        alohaDb.iniDb(sqLiteDatabase);
        if(alohaDb.getCountry(0)==null){
            new DbInittask().execute();
        }




    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }

    class DbInittask extends AsyncTask<Void,Void,Void>{
        AlohaDb alohaDb = new AlohaDb(LaunchActivity.this);
        SQLiteDatabase sqLiteDatabase = alohaDb.getWritableDatabase();
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(LaunchActivity.this);
            dialog.setCancelable(false);
            dialog.setTitle("Initializing city list, please wait 1 minute");
            dialog.show();
            alohaDb.iniDb(sqLiteDatabase);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            insert("country",alohaDb,getAssets());
            insert("region",alohaDb,getAssets());
            insert("city",alohaDb,getAssets());
            alohaDb.close();
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }
    public void insert(String type, AlohaDb alohaDb, AssetManager assetManager){
        if(type.equals("country")){
            try {
                InputStream inputStream = assetManager.open("country_id.txt");
                InputStream inputStream2 = assetManager.open("country_name.txt");
                //DataInputStream dataInputStream = new DataInputStream(inputStream);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                BufferedReader reader2 = new BufferedReader(new InputStreamReader(inputStream2));
                while(reader.ready()){
                    int id = Integer.valueOf(reader.readLine());
                    String name = reader2.readLine();
                    Country country = new Country();
                    country.setId(id);
                    country.setName(name);
                    alohaDb.addCountry(country);
                }
            } catch (IOException e) {}
        }else if(type.equals("region")){
            try {
                InputStream inputStream = assetManager.open("region_id.txt");
                InputStream inputStream2 = assetManager.open("region_name.txt");
                InputStream inputStream3 = assetManager.open("region_country_id.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                BufferedReader reader2 = new BufferedReader(new InputStreamReader(inputStream2));
                BufferedReader reader3 = new BufferedReader(new InputStreamReader(inputStream3));
                while(reader.ready()){
                    Region region = new Region();
                    region.setId(Integer.parseInt(reader.readLine()));
                    region.setName(reader2.readLine());
                    region.setCountry_id(Integer.parseInt(reader3.readLine()));
                    alohaDb.addRegion(region);
                }
            } catch (IOException e) {}
        }else if(type.equals("city")){
            try {
                InputStream inputStream = assetManager.open("city_id.txt");
                InputStream inputStream2 = assetManager.open("city_name.txt");
                InputStream inputStream3 = assetManager.open("city_region_id.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                BufferedReader reader2 = new BufferedReader(new InputStreamReader(inputStream2));
                BufferedReader reader3 = new BufferedReader(new InputStreamReader(inputStream3));
                while(reader.ready()){
                    City city = new City();
                    city.setId(Integer.parseInt(reader.readLine()));
                    city.setName(reader2.readLine());
                    city.setRegion_id(Integer.parseInt(reader3.readLine()));
                    alohaDb.addCity(city);
                }
            } catch (IOException e) {}
        }

    }

}
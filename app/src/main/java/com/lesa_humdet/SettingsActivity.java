package com.lesa_humdet;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.lesa_humdet.db.AlohaDb;
import com.lesa_humdet.db.City;
import com.lesa_humdet.db.Country;
import com.lesa_humdet.db.Region;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private String [] array;
    Conf conf = new Conf();
    SharedPreferences mSettings;
    SharedPreferences.Editor editor;
    Spinner spinner = null;
    TextView cityName;
    int city_id = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettings = getSharedPreferences(conf.getShared_pref_name(), Context.MODE_PRIVATE);
        editor = mSettings.edit();
        int lang = mSettings.getInt(conf.getLANG(),0);
        if(lang==conf.getRU()){
            array = getResources().getStringArray(R.array.app_lang_ru);
        }else if(lang==conf.getEN()){
            array = getResources().getStringArray(R.array.app_lang_en);
        }else if(lang==conf.getAR()){
            array = getResources().getStringArray(R.array.app_lang_ar);
        }else{
            array = getResources().getStringArray(R.array.app_lang_ru);
        }
        setContentView(R.layout.activity_settings);
        getSupportActionBar().setTitle(array[21]);
        CheckBox checkBox = findViewById(R.id.checkBox);
        checkBox.setText(array[17]);
        checkBox.setChecked(true);

        boolean bool = mSettings.getBoolean("save_photo",false);
        if(bool){
            checkBox.setChecked(true);
        }else{
            checkBox.setChecked(false);
        }
        TextView textView3 = findViewById(R.id.textView3);
        TextView textView4 = findViewById(R.id.textView4);
        cityName = findViewById(R.id.textView5);
        textView3.setText(array[24]);
        textView4.setText(array[29]);
        Button button = findViewById(R.id.button3);
        Button button4 = findViewById(R.id.button4);
        button.setText(array[18]);
        button4.setText(array[28]);
        cityName.setText(mSettings.getString("city",""));
        city_id = mSettings.getInt("city_id",0);
        button.setOnClickListener(e -> {
            if(checkBox.isChecked()){
                editor.putBoolean("save_photo",true);
            }else{
                editor.putBoolean("save_photo",false);
            }
            String selected = spinner.getSelectedItem().toString();
            if(selected.equals("English")){
                editor.putInt(conf.getLANG(),2);
            }else if(selected.equals("Русский")){
                editor.putInt(conf.getLANG(),1);
            }else if(selected.equals("عربي")){
                editor.putInt(conf.getLANG(),3);
            }else{
                editor.putInt(conf.getLANG(),1);
            }
            editor.putString("city",cityName.getText().toString());
            editor.putInt("city_id",city_id);
            editor.apply();
            AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
            builder.setTitle(array[21]);
            builder.setMessage(array[19]);
            builder.setPositiveButton(array[36], new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intentService = new Intent(SettingsActivity.this,MyService.class);
                    stopService(intentService);

                    Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    finish();
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();

        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        spinner = findViewById(R.id.spinner);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCountryAlert();
            }
        });

    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }

    public void showCountryAlert(){
        AlohaDb alohaDb = new AlohaDb(SettingsActivity.this);
        SQLiteDatabase sqLiteDatabase = alohaDb.getReadableDatabase();
        alohaDb.iniDb(sqLiteDatabase);
        List<Country> list = alohaDb.getAllCountry();


        AlertDialog.Builder builderSingle = new AlertDialog.Builder(SettingsActivity.this);
        builderSingle.setIcon(R.drawable.location_icon);
        builderSingle.setTitle(array[27]);

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(SettingsActivity.this, android.R.layout.select_dialog_singlechoice);
        for(int i=0;i<list.size();i++){
            arrayAdapter.add(list.get(i).getName());
        }
        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                List<Region> regions = alohaDb.getRegions(which);
                final ArrayAdapter<String> arrayAdapterReg = new ArrayAdapter<String>(SettingsActivity.this, android.R.layout.select_dialog_singlechoice);
                for(int i=0;i<regions.size();i++){
                    arrayAdapterReg.add(regions.get(i).getName());
                }


                AlertDialog.Builder builderInnerRegion = new AlertDialog.Builder(SettingsActivity.this);
                builderInnerRegion.setAdapter(arrayAdapterReg, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        List<City> cities = alohaDb.getCities(regions.get(i).getId());
                        final ArrayAdapter<String> arrayCity = new ArrayAdapter<String>(SettingsActivity.this, android.R.layout.select_dialog_singlechoice);
                        for(int k=0;k<cities.size();k++){
                            arrayCity.add(cities.get(k).getName());
                        }

                        AlertDialog.Builder builderInnerCity = new AlertDialog.Builder(SettingsActivity.this);
                        builderInnerCity.setAdapter(arrayCity, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int c) {
                                dialog.dismiss();
                                cityName.setText(arrayCity.getItem(c));
                                cityName.setText(arrayCity.getItem(c));
                                city_id = cities.get(c).getId();
                            }
                        });
                        builderInnerCity.show();
                    }
                });
                builderInnerRegion.show();
            }
        });
        builderSingle.show();
    }
}
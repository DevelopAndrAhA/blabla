package com.lesa_humdet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ResultSearchActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    int REQUEST_OVERLAY_PERMISSION=1000;
    MyPermissions per = new MyPermissions(ResultSearchActivity.this,ResultSearchActivity.this);
    OkHttpClient client = new OkHttpClient();
    private String [] array;
    Conf conf = new Conf();


    SharedPreferences mSettings;
    JSONArray jsonArray;
    ListView listView = null;
    List<String[]> list = null;
    List<JSONObject[]> jsonObjects = null;
    SwipeRefreshLayout swipeRefreshLayout = null;
    TextView textView = null;
    TextView textView3 = null;


    Button searchBtn = null;
    Button trainBtn = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettings = getSharedPreferences(conf.getShared_pref_name(), Context.MODE_PRIVATE);
        setContentView(R.layout.activity_result_search);


        String custom_font = "fonts/JackportRegularNcv.ttf";
        Typeface CF = Typeface.createFromAsset(getAssets(), custom_font);
        textView = findViewById(R.id.textView2);
        textView3 = findViewById(R.id.textView3);
        textView.setTypeface(CF);
        textView3.setTypeface(CF);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorScheme(R.color.purple_200, R.color.teal_200, R.color.purple_200, R.color.teal_200);

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
        boolean main = getIntent().getBooleanExtra("main",false);
        if(main){
            textView.setText(array[22]);//metka 2
            getSupportActionBar().setTitle(array[39]);
        }else{
            textView.setText(array[16]);
            textView3.setText(array[34]);
            getSupportActionBar().setTitle(array[7]);
        }

        searchBtn = findViewById(R.id.button3);
        trainBtn = findViewById(R.id.button4);
        searchBtn.setText(array[2]);
        trainBtn.setText(array[11]);

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),SearchActivity.class);
                startActivity(intent );
            }
        });
        trainBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),NeuroTrainingActivity.class);
                startActivity(intent );
            }
        });

        ImageButton img_activity = findViewById(R.id.img_activity);
        img_activity.setOnClickListener(e -> {
            if(isOnline()){
                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(intent );
            }else{
                Toast.makeText(ResultSearchActivity.this,array[35],Toast.LENGTH_LONG).show();
            }
        });
        ImageButton settings_btn = findViewById(R.id.settings_btn);
        settings_btn.setOnClickListener(e -> {
            Intent intent2 = new Intent(this,SettingsActivity.class);
            startActivity(intent2);
        });
        listView = findViewById(R.id.lisView);
        list = new ArrayList<String[]>();
        jsonObjects = new ArrayList<JSONObject[]>();
        per.getMyApplicationPermissions();
        try{
            jsonArray = new JSONArray(getIntent().getStringExtra("jsonArray"));
            for(int i=0;i<jsonArray.length();i++){
                String[] urlMas3 = new String[3];
                JSONObject [] jsonObject = new JSONObject[3];
                try{
                    urlMas3[0] = jsonArray.getJSONObject(i).getString("photoName");
                    jsonObject[0] = jsonArray.getJSONObject(i);
                }catch (Exception e){}
                try{
                    urlMas3[1] = jsonArray.getJSONObject(i+1).getString("photoName");
                    jsonObject[1] = jsonArray.getJSONObject(i+1);
                    i = i+1;
                }catch (Exception e){}
                try{
                    urlMas3[2] = jsonArray.getJSONObject(i+1).getString("photoName");
                    jsonObject[2] = jsonArray.getJSONObject(i+1);
                    i = i+1;
                }catch (Exception e){}
                list.add(urlMas3);
                jsonObjects.add(jsonObject);

                if(list==null || list.size()==0) textView.setVisibility(View.VISIBLE);
                CustomArrayAdapter adapter = new CustomArrayAdapter(ResultSearchActivity.this, list,jsonObjects,jsonArray.toString());
                runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.alertDialogBuilder();
                    }
                });
                listView.setAdapter(adapter);
                listView.setClickable(false);
            }
        }catch (Exception e){}


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!Settings.canDrawOverlays(this)){
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(Settings.canDrawOverlays(this)){
                Intent intentService = new Intent(ResultSearchActivity.this,MyService.class);
                startService(intentService);
            }else{
                Toast.makeText(ResultSearchActivity.this,array[30],Toast.LENGTH_LONG).show();
            }
        }

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {

            }
        });

        if(isOnline()&&jsonArray==null){
            getDataWithoutMap();
            new StatusOfBanner().execute();
        }else if(!isOnline()){
            Toast.makeText(ResultSearchActivity.this,array[35],Toast.LENGTH_LONG).show();
        }
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

    @Override
    public void onRefresh() {
        if(isOnline()){
            getDataWithoutMap();
        }else if(!isOnline()){
            Toast.makeText(ResultSearchActivity.this,array[35],Toast.LENGTH_LONG).show();
        }
        swipeRefreshLayout.setRefreshing(true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_bookmarks) {
            Intent intent = new Intent(getApplicationContext(),BookmarksActivity.class);
            startActivity(intent);
            return true;
        }else if(item.getItemId() == R.id.history) {
            Intent intent = new Intent(getApplicationContext(),HistoryActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void  getDataWithoutMap(){

        ProgressDialog dialog = new ProgressDialog(ResultSearchActivity.this);
        dialog.setMessage(array[1]);
        dialog.show();

        JSONArray[] jsonArray = new JSONArray[1];
        String url = "getFirstData4imgs/";
        com.squareup.okhttp.Request request = new com.squareup.okhttp.Request.Builder()
                .url(conf.getDomen()+ url)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {}
            @Override
            public void onResponse(Response response) throws IOException {
                listView = findViewById(R.id.lisView);
                list = new ArrayList<String[]>();
                jsonObjects = new ArrayList<JSONObject[]>();
                String res = response.body().string();
                if(response.code()!=200){
                    return;
                }
                    try{
                        jsonArray[0] = new JSONArray(res);
                    }catch (Exception e){}
                    if(jsonArray[0] != null && jsonArray[0].length()!=0){
                        for(int i=0;i<jsonArray[0].length();i++){
                            String[] urlMas3 = new String[3];
                            JSONObject [] jsonObject = new JSONObject[3];
                            try{
                                urlMas3[0] = jsonArray[0].getJSONObject(i).getString("photoName");
                                jsonObject[0] = jsonArray[0].getJSONObject(i);
                            }catch (Exception e){}
                            try{
                                urlMas3[1] = jsonArray[0].getJSONObject(i+1).getString("photoName");
                                jsonObject[1] = jsonArray[0].getJSONObject(i+1);
                                i = i+1;
                            }catch (Exception e){}
                            try{
                                urlMas3[2] = jsonArray[0].getJSONObject(i+1).getString("photoName");
                                jsonObject[2] = jsonArray[0].getJSONObject(i+1);
                                i = i+1;
                            }catch (Exception e){}
                            list.add(urlMas3);
                            jsonObjects.add(jsonObject);
                        }
                        CustomArrayAdapter adapter = new CustomArrayAdapter(ResultSearchActivity.this, list,jsonObjects,jsonArray.toString());
                        runOnUiThread(new Runnable() {
                            public void run() {
                                adapter.alertDialogBuilder();
                                listView.setAdapter(adapter);
                                listView.setVisibility(View.VISIBLE);
                                textView.setVisibility(View.GONE);
                                textView3.setVisibility(View.GONE);
                                dialog.dismiss();
                            }
                        });
                        swipeRefreshLayout.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        }, 3000);
                    }else{
                        runOnUiThread(new Runnable() {
                            public void run() {
                                textView.setVisibility(View.VISIBLE);
                                textView3.setVisibility(View.VISIBLE);
                                listView.setVisibility(View.GONE);
                            }
                        });
                        swipeRefreshLayout.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        }, 3000);

                    }
            }
        });


    }


    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }


    class StatusOfBanner extends AsyncTask<Void,Void,Void>{
        String statusCode = "N";
        @Override
        protected Void doInBackground(Void... voids) {
            String url = "ads_sts/";
            com.squareup.okhttp.Request request = new com.squareup.okhttp.Request.Builder()
                    .url(conf.getDomen()+ url)
                    .build();
            Call call = client.newCall(request);
            try{
                Response response = call.execute();
                if(response.code()==200){
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    if(!jsonObject.getString("status").equals("N")){
                        statusCode = jsonObject.getString("status");
                    }
                }
            }catch (Exception e){}
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            AdView mAdView = findViewById(R.id.adView);
            if(!statusCode.equals("N")){
                AdRequest adRequest = new AdRequest.Builder().build();
                mAdView.loadAd(adRequest);
            }else{
                mAdView.setVisibility(View.GONE);
            }

        }
    }


}
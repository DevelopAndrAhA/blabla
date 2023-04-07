package com.lesa_humdet;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class BookmarksActivity extends AppCompatActivity {
    SharedPreferences mSettings;
    Conf conf = new Conf();
    private String [] array;
    JSONArray bookmarks = null;
    ListView listView = null;
    List<String[]> list = null;
    List<JSONObject[]> jsonObjects = null;
    OkHttpClient client = new OkHttpClient();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettings = getSharedPreferences(conf.getShared_pref_name(), Context.MODE_PRIVATE);
        setContentView(R.layout.activity_bookmarks);
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


        try {
            bookmarks = new JSONArray(mSettings.getString("bookmarks","[]"));
            Log.e("TAG", bookmarks.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        listView = findViewById(R.id.lisView);
        list = new ArrayList<String[]>();
        jsonObjects = new ArrayList<JSONObject[]>();
        TextView textView3 = findViewById(R.id.textView2);
        if(bookmarks.length()==0){
            textView3.setText(array[0]);
        }
        try{
            for(int i=0;i<bookmarks.length();i++){
                String[] urlMas3 = new String[3];
                JSONObject[] jsonObject = new JSONObject[3];
                try{
                    urlMas3[0] = bookmarks.getJSONObject(i).getString("photoName");
                    jsonObject[0] = bookmarks.getJSONObject(i);
                }catch (Exception e){}
                try{
                    urlMas3[1] = bookmarks.getJSONObject(i+1).getString("photoName");
                    jsonObject[1] = bookmarks.getJSONObject(i+1);
                    i = i+1;
                }catch (Exception e){}
                try{
                    urlMas3[2] = bookmarks.getJSONObject(i+1).getString("photoName");
                    jsonObject[2] = bookmarks.getJSONObject(i+1);
                    i = i+1;
                }catch (Exception e){}
                list.add(urlMas3);
                jsonObjects.add(jsonObject);

                CustomArrayAdapter adapter = new CustomArrayAdapter(BookmarksActivity.this, list,jsonObjects,bookmarks.toString());
                runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.alertDialogBuilder();
                    }
                });
                listView.setAdapter(adapter);
                listView.setClickable(false);
            }
        }catch (Exception e){}



        if(isOnline()){
            new StatusOfBanner().execute();
        }else if(!isOnline()){
            Toast.makeText(BookmarksActivity.this,array[35],Toast.LENGTH_LONG).show();
        }

    }




    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }


    class StatusOfBanner extends AsyncTask<Void,Void,Void> {
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

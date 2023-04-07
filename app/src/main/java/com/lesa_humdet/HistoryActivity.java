package com.lesa_humdet;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
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
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class HistoryActivity extends AppCompatActivity {

    OkHttpClient client = new OkHttpClient();
    private String [] array;
    Conf conf = new Conf();
    SharedPreferences mSettings;
    JSONArray jsonArray;
    ListView listView = null;
    List<String[]> list = null;
    List<JSONObject[]> jsonObjects = null;
    TextView textView = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        mSettings = getSharedPreferences(conf.getShared_pref_name(), Context.MODE_PRIVATE);


        String custom_font = "fonts/JackportRegularNcv.ttf";
        Typeface CF = Typeface.createFromAsset(getAssets(), custom_font);
        textView = findViewById(R.id.textView2);
        textView.setTypeface(CF);



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

        listView = findViewById(R.id.lisView);
        list = new ArrayList<String[]>();


        if(isOnline()&&jsonArray==null){
            getDataWithoutMap();
            new StatusOfBanner().execute();
        }else if(!isOnline()){
            Toast.makeText(HistoryActivity.this,array[35],Toast.LENGTH_LONG).show();
        }
    }


    public void  getDataWithoutMap(){

        String deviceId = mSettings.getString("deviceId","*");
        ProgressDialog dialog = new ProgressDialog(HistoryActivity.this);
        dialog.setMessage(array[1]);
        dialog.show();

        JSONArray[] jsonArray = new JSONArray[1];
        String url = "history?deviceId="+deviceId;
        com.squareup.okhttp.Request request = new com.squareup.okhttp.Request.Builder()
                .url(conf.getDomen()+ url)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {e.printStackTrace(); dialog.dismiss();}
            @Override
            public void onResponse(Response response) throws IOException {
                listView = findViewById(R.id.lisView);
                list = new ArrayList<String[]>();
                jsonObjects = new ArrayList<JSONObject[]>();
                String res = response.body().string();
                Log.e("res",res);
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
                    CustomArrayAdapter adapter = new CustomArrayAdapter(HistoryActivity.this, list,jsonObjects,jsonArray.toString());
                    runOnUiThread(new Runnable() {
                        public void run() {
                            adapter.alertDialogBuilder();
                            listView.setAdapter(adapter);
                            listView.setVisibility(View.VISIBLE);
                            textView.setVisibility(View.GONE);
                            dialog.dismiss();
                        }
                    });
                }else{
                    runOnUiThread(new Runnable() {
                        public void run() {
                            textView.setVisibility(View.VISIBLE);
                            listView.setVisibility(View.GONE);
                        }
                    });
                }
                dialog.dismiss();
            }
        });


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

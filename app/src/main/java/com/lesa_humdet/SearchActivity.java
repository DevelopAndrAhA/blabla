package com.lesa_humdet;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.icu.util.Calendar;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.Face;
import com.lesa_humdet.db.AlohaDb;
import com.lesa_humdet.db.City;
import com.lesa_humdet.db.Country;
import com.lesa_humdet.db.Region;
import com.lesa_humdet.tflite.SimilarityClassifier;
import com.lesa_humdet.tflite.TFLiteObjectDetectionAPIModel;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

public class SearchActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    final int Pic_image=299;
    private String [] array;
    Conf conf = new Conf();
    int city_id = 0;
    SharedPreferences mSettings;
    SharedPreferences.Editor editor;
    Button buttonSearch = null;
    private FaceDetector faceDetector;
    EditText fromDate = null;
    EditText toDate = null;
    JSONArray jsonArray;

    boolean fromDateBool = false;
    boolean toDateBool = false;

    private SimilarityClassifier detector;
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    private static final String TF_OD_API_MODEL_FILE = "mobile_face_net.tflite";
    private static final int TF_OD_API_INPUT_SIZE = 112;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mSettings = getSharedPreferences(conf.getShared_pref_name(), Context.MODE_PRIVATE);
        editor = mSettings.edit();

        bOptions = new BitmapFactory.Options();
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();

        faceDetector = FaceDetection.getClient(options);
        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
        } catch (final Exception e) {
            e.printStackTrace();
        }


        int lang = mSettings.getInt(conf.getLANG(),0);
        city_id = mSettings.getInt("city_id",0);
        if(lang==conf.getRU()){
            array = getResources().getStringArray(R.array.app_lang_ru);
        }else if(lang==conf.getEN()){
            array = getResources().getStringArray(R.array.app_lang_en);
        }else if(lang==conf.getAR()){
            array = getResources().getStringArray(R.array.app_lang_ar);
        }else{
            array = getResources().getStringArray(R.array.app_lang_ru);
        }

        getSupportActionBar().setTitle(array[2]);
        Button button = findViewById(R.id.button);
        EditText cityText = findViewById(R.id.editTextCity);
        TextView fromDateTitle = findViewById(R.id.textView6);
        TextView toDateTitle = findViewById(R.id.textView7);
        button.setText(array[3]);
        fromDateTitle.setText(array[37]);
        toDateTitle.setText(array[38]);
        cityText.setText(array[33]+" : "+mSettings.getString("city",""));
        cityText.setFocusable(false);
        cityText.setFocusableInTouchMode(false);
        cityText.setClickable(true);
        cityText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCountryAlert(cityText);
            }
        });
        ImageView imageView = findViewById(R.id.imageView);
        bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.hum_icon);
        imageView.setImageBitmap(bitmap);
        buttonSearch = findViewById(R.id.buttonUpload);
        buttonSearch.setText(array[2]);
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 if(isOnline()){
                     SearchTask searchTask = new SearchTask(SearchActivity.this);
                     searchTask.startSearch();
                }else{
                    Toast.makeText(SearchActivity.this,array[35],Toast.LENGTH_LONG).show();
                }

            }
        });
        TextView datelabel = findViewById(R.id.dateLabel);
        datelabel.setText(array[5]);
        Calendar now = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            now = Calendar.getInstance();
        }
        fromDate = findViewById(R.id.editTextDate2);
        toDate = findViewById(R.id.editTextDate3);

        try{
            DatePickerDialog dpd = DatePickerDialog.newInstance(
                    SearchActivity.this,
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
            );
            DatePickerDialog finalDpd = dpd;
            fromDate.setFocusable(false);
            fromDate.setFocusableInTouchMode(false);
            fromDate.setClickable(true);
            fromDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fromDateBool = true;
                    finalDpd.show(getSupportFragmentManager(), "Datepickerdialog");
                }
            });
            toDate.setFocusable(false);
            toDate.setFocusableInTouchMode(false);
            toDate.setClickable(true);
            toDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    toDateBool = true;
                    finalDpd.show(getSupportFragmentManager(), "Datepickerdialog");
                }
            });
        }catch (Exception e){}

        Date tmpDate = new Date();
        String dateMas [] = tmpDate.toString().split(" ");
        int locDateDay = Integer.parseInt(dateMas[2]);
        int locDatemMonth = tmpDate.getMonth()+1;
        String locDateDayStr = null;
        String locDatemMonthStr = null;
        if(locDateDay<10){
            locDateDayStr="0"+locDateDay;
        }else{
            locDateDayStr=locDateDay+"";
        }
        if(locDatemMonth<10){
            locDatemMonthStr="0"+locDatemMonth;
        }else{
            locDatemMonthStr=locDatemMonth+"";
        }
        String locDateYear = dateMas[5]+"-"+locDatemMonthStr+"-"+locDateDayStr;


        fromDate.setText(locDateYear);
        toDate.setText(locDateYear);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);



        fromDate = findViewById(R.id.editTextDate2);
        toDate = findViewById(R.id.editTextDate3);


    }



    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        finish();
        return true;
    }


    public void faceDatector(Bitmap bitmap,String imgname){
        InputImage image = InputImage.fromBitmap(bitmap, 0);
       try {
           faceDetector
                   .process(image)
                   .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                       @Override
                       public void onSuccess(List<Face> faces) {
                           if (faces.size() == 0) {
                               Toast.makeText(SearchActivity.this,array[6],Toast.LENGTH_LONG).show();
                               buttonSearch.setVisibility(View.INVISIBLE);
                               return;
                           }
                           faceDetect(bitmap,faces,imgname);
                       }
                   }).getResult();
       }catch (Exception e){
           e.printStackTrace();
       }
    }

    public void selectImage(View view){
        Intent intentImage = new Intent(Intent.ACTION_PICK);
        intentImage.setType("image/*");
        startActivityForResult(intentImage, Pic_image);

    }

    BitmapFactory.Options bOptions = null;
    Bitmap bitmap = null;
    InputStream input = null;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == Pic_image) {
            uriToSend = data.getData();
            try {
                input = getContentResolver().openInputStream(uriToSend);
                //bOptions.inSampleSize = 2;
                bitmap = BitmapFactory.decodeStream(input, null, bOptions);
                File fileToSend = new File(getRealPathFromUri(uriToSend));
                ImageView imageView = findViewById(R.id.imageView);
                imageView.setImageBitmap(bitmap);
                faceDatector(bitmap,fileToSend.getName());
            } catch (FileNotFoundException e) {e.printStackTrace();}
        }
    }
    Bitmap faceBmp112_112=null;
    String masToSend = "";
    public void faceDetect(Bitmap bitmap,List<Face> faces,String imageName){
        Face f = faces.get(0);
        try{
            Rect rect = f.getBoundingBox();
            Bitmap orBmp = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), false);
            Bitmap faceBmp = Bitmap.createBitmap(orBmp, rect.left, rect.top, rect.width(), rect.height());
            faceBmp112_112 = Bitmap.createScaledBitmap(faceBmp, 112, 112, false);
            buttonSearch.setVisibility(View.VISIBLE);
            final List<SimilarityClassifier.Recognition> resultsAux = detector.recognizeImage(faceBmp112_112, true);
            SimilarityClassifier.Recognition result = resultsAux.get(0);
            float mas[][] = (float[][]) result.getExtra();
            for(int i=0;i<mas[0].length;i++){
                masToSend += mas[0][i];
                if(i!=mas[0].length-1){
                    masToSend = masToSend+",";
                }
            }
        }catch (Exception e){
            Toast.makeText(SearchActivity.this,array[15],Toast.LENGTH_SHORT).show();
            buttonSearch.setVisibility(View.INVISIBLE);
        }
    }

    Uri uriToSend=null;
    private String getRealPathFromUri(Uri contentUri){
        String result=null;
        Cursor cursor = getContentResolver().query(contentUri,null,null,null,null);
        if(cursor == null){
            result = contentUri.getPath();
        }else{
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        monthOfYear = monthOfYear+1;

        if(fromDateBool){
            if(monthOfYear<10){
                fromDate.setText(year+"-0"+monthOfYear+"-"+dayOfMonth);
            }else{
                fromDate.setText(year+"-"+monthOfYear+"-"+dayOfMonth);
            }
            fromDateBool = false;
        }else if(toDateBool){
            if(monthOfYear<10){
                toDate.setText(year+"-0"+monthOfYear+"-"+dayOfMonth);
            }else{
                toDate.setText(year+"-"+monthOfYear+"-"+dayOfMonth);
            }
            toDateBool = false;
        }
    }

    class SearchTask {
        private ExecutorService executorService;
        private ProgressDialog dialog;
        private Context context;

        public SearchTask(Context context) {
            this.context = context;
            this.executorService = Executors.newSingleThreadExecutor();
        }

        public void startSearch() {
            dialog = new ProgressDialog(context);
            dialog.setMessage(array[2]);
            dialog.show();

            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        OkHttpClient client = new OkHttpClient();
                        String url = "search?fromDate="+fromDate.getText().toString()+"&toDate="+toDate.getText().toString()+"&crop="+ masToSend+"&lat=0&lng=0&city_id="+city_id+"/";
                        com.squareup.okhttp.Request request1 = new com.squareup.okhttp.Request.Builder()
                                .url(conf.getDomen()+ url)
                                .build();
                        Call call1 = client.newCall(request1);
                        final Response response = call1.execute();
                        String res = response.body().string();
                        try{
                            jsonArray = new JSONArray(res);
                        }catch (Exception e){}
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        if(jsonArray!=null){
                            Intent intent = new Intent(SearchActivity.this,ResultSearchActivity.class);
                            intent.putExtra("jsonArray",jsonArray.toString());
                            startActivity(intent);
                            jsonArray = null;
                        }else{
                            Toast.makeText(SearchActivity.this,array[13],Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }

        public void cancelSearch() {
            executorService.shutdownNow();
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }
    public void showCountryAlert(EditText cityText){
        AlohaDb alohaDb = new AlohaDb(SearchActivity.this);
        SQLiteDatabase sqLiteDatabase = alohaDb.getReadableDatabase();
        alohaDb.iniDb(sqLiteDatabase);
        List<Country> list = alohaDb.getAllCountry();


        AlertDialog.Builder builderSingle = new AlertDialog.Builder(SearchActivity.this);
        builderSingle.setIcon(R.drawable.location_icon);
        builderSingle.setTitle(array[27]);

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(SearchActivity.this, android.R.layout.select_dialog_singlechoice);
        for(int i=0;i<list.size();i++){
            arrayAdapter.add(list.get(i).getName());
        }
        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                List<Region> regions = alohaDb.getRegions(which);
                final ArrayAdapter<String> arrayAdapterReg = new ArrayAdapter<String>(SearchActivity.this, android.R.layout.select_dialog_singlechoice);
                for(int i=0;i<regions.size();i++){
                    arrayAdapterReg.add(regions.get(i).getName());
                }


                AlertDialog.Builder builderInnerRegion = new AlertDialog.Builder(SearchActivity.this);
                builderInnerRegion.setAdapter(arrayAdapterReg, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        List<City> cities = alohaDb.getCities(regions.get(i).getId());
                        final ArrayAdapter<String> arrayCity = new ArrayAdapter<String>(SearchActivity.this, android.R.layout.select_dialog_singlechoice);
                        for(int k=0;k<cities.size();k++){
                            arrayCity.add(cities.get(k).getName());
                        }

                        AlertDialog.Builder builderInnerCity = new AlertDialog.Builder(SearchActivity.this);
                        builderInnerCity.setAdapter(arrayCity, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int c) {
                                city_id = cities.get(c).getId();
                                cityText.setText(array[33]+" : "+cities.get(c).getName());
                                dialog.dismiss();
                                Toast.makeText(SearchActivity.this,array[19],Toast.LENGTH_LONG).show();
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

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}
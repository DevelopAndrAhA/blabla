package com.lesa_humdet;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

public class MyPermissions {


    private static final int REQUEST_ID_READ_WRITE_PERMISSION = 100;
    Context context;
    Activity activity;

    public MyPermissions(Context context,Activity activity) {
        this.context = context;
        this.activity = activity;
    }

    public void getMyApplicationPermissions(){
        int writePermission = ActivityCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        int cameraPermission = ActivityCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA);
        int internetPermission = ActivityCompat.checkSelfPermission(context,
                Manifest.permission.INTERNET);
        int locationPermission = ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int locationPermission2 = ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        int readPhoneState = ActivityCompat.checkSelfPermission(context,
                Manifest.permission.READ_PHONE_STATE);

        if (writePermission != PackageManager.PERMISSION_GRANTED ||
                readPermission != PackageManager.PERMISSION_GRANTED||
                cameraPermission != PackageManager.PERMISSION_GRANTED||
                internetPermission != PackageManager.PERMISSION_GRANTED||
                locationPermission2 != PackageManager.PERMISSION_GRANTED||
                locationPermission != PackageManager.PERMISSION_GRANTED||
                readPhoneState != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.requestPermissions(
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA,
                                Manifest.permission.INTERNET,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.READ_PHONE_STATE},
                        REQUEST_ID_READ_WRITE_PERMISSION
                );
            }
        }

    }

}

/* Запрос на поверх всех окон
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if(!Settings.canDrawOverlays(this)){
        // ask for setting
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        }
        }

        @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // permission granted...
                } else {
                    // permission not granted...
                }
            }
        }
    }




        */

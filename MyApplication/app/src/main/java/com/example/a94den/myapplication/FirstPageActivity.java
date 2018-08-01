package com.example.a94den.myapplication;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.Manifest;

import android.widget.Toast;

public class FirstPageActivity extends AppCompatActivity {

    SharedPreferences sp;
    Intent intent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_page);

        sp = getSharedPreferences("HDSurvive", 0);
        final SharedPreferences.Editor sedt = sp.edit();
        intent = new Intent(FirstPageActivity.this, MainActivity.class);

        //sedt.putBoolean("firstTime", true);
       // sedt.commit();
        if(!sp.getBoolean("firstTime",true))
        startActivity(intent);



        findViewById(R.id.btnGO).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sedt.putBoolean("firstTime", false);
                sedt.commit();
                startActivity(intent);

            }
        });
    }





}

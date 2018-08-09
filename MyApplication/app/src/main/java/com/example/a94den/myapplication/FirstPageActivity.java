package com.example.a94den.myapplication;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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
import android.os.ParcelUuid;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class FirstPageActivity extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    SharedPreferences sp;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_page);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        sp = getSharedPreferences("HDSurvive", 0);
        final SharedPreferences.Editor sedt = sp.edit();
        intent = new Intent(FirstPageActivity.this, Splash.class);

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

    public void FindBT() {
        // super.onStart();
        //Enable Bluetooth Request
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            //BTlbl.setText("No bluetooth adapter available");
            Toast.makeText(FirstPageActivity.this,"No bluetooth adapter available",Toast.LENGTH_LONG).show();
        }

        //boolean reqRes = EnableBTReq();

        //Find HC-05 from paired devices
        if (EnableBTReq()) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equals("HC-05")) {
                        mmDevice = device;
                        //BTlbl.setText("Bluetooth Device " + device.getName() + " Found");
                        break;
                    }
                }
            }
        } else EnableBTReq();

        // Open Bluetooth connection
        try {
            openBT();
            sendData();
        } catch (IOException ex) {
            //BTlbl.setText("Error in Opening Bluetooth connection!");
            Toast.makeText(FirstPageActivity.this,"Error in Opening Bluetooth connection!",Toast.LENGTH_LONG).show();
        }

    }

    boolean EnableBTReq(){
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            Toast.makeText(FirstPageActivity.this,enableBluetooth.getAction(),Toast.LENGTH_LONG).show();
            startActivityForResult(enableBluetooth, 0);
            if (mBluetoothAdapter.isEnabled())
                return  true;
            return false;
        }
        return true;

    }

    void openBT() throws IOException
    {
        ParcelUuid[] uuid = mmDevice.getUuids();
        // UUID uuid = UUID.fromString("00001101-0000-1000-8000-00211303E4CB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid[0].getUuid());
        if(!mmSocket.isConnected()) {
            mBluetoothAdapter.cancelDiscovery();
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();

            Toast.makeText(FirstPageActivity.this,"Bluetooth Connection Opened",Toast.LENGTH_LONG).show();
            //openedBT = true;
        }
    }

    void sendData() throws IOException
    {
        String msg = "1";
        mmOutputStream.write(msg.getBytes());
        //myLabel.setText("Data Sent");
    }

}



















/////////// Old First Page Code (on "GO" click) ///////////
//Send SMS permission
       /* if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String [] {Manifest.permission.SEND_SMS},MY_PERMISSIONS_REQUEST_SEND_SMS);
        }*/
//GPS Turn ON
// locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//configure_button();
//Intent serviceIntent = new Intent(FirstPageActivity.this, MyService.class);
//startService(serviceIntent);
/////////// Old First Page Code (on "GO" click) ///////////
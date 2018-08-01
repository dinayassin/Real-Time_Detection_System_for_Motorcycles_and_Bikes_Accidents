package com.example.a94den.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.location.Location;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.telephony.SmsManager;
import android.text.method.LinkMovementMethod;
import android.util.SparseBooleanArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.CompoundButton;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;

    TextView BTlbl, locationlbl, timelbl, datalbl, listTitle;
    Button delBtn;
    ArrayAdapter<String> adapter;
    List<String> list, checkedIndexes;
    EditText phoneNum;
    ListView contactsList;
    public static Switch switchEnabled;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition, checkedCount, numsListSize;
    volatile boolean stopWorker;
    private LocationManager locationManager;
    LocationListener  listener;
    String wazeURL, googlemapsURL, messageStr, data="";
    Location accidentLocation;
    SmsManager sms;
    SharedPreferences sp;
    DialogInterface.OnClickListener dialogClickListener;


    static boolean getSwitchState(){
        return switchEnabled.isChecked();
    }
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /////////// Old First Page Code (on "GO" click) ///////////
        //Send SMS permission
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String [] {Manifest.permission.SEND_SMS},MY_PERMISSIONS_REQUEST_SEND_SMS);
        }

        //GPS Turn ON
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        configure_button();
        //Intent serviceIntent = new Intent(FirstPageActivity.this, MyService.class);
        //startService(serviceIntent);
        /////////// Old First Page Code (on "GO" click) ///////////

        sp = getSharedPreferences("HDSurvive", 0);
        final SharedPreferences.Editor sedt = sp.edit();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        sms = SmsManager.getDefault();

        checkedIndexes = new ArrayList<>(Arrays.asList(new String[]{}));
        checkedCount = 0;

        //listview init
        contactsList = (ListView)findViewById(R.id.contactsList);

        contactsList.setOnTouchListener(new View.OnTouchListener() {
            // Setting on Touch Listener for handling the touch inside ScrollView
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Disallow the touch request for parent scroll on touch of child view
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        //get numbers from sp
        list = new ArrayList<>(Arrays.asList(new String[]{}));
           numsListSize = sp.getInt( "numsList_size", 0);
            for(int g=0; g<numsListSize; g++)
                list.add(g, sp.getString("numsList_" + g, null));

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, list);
        contactsList.setAdapter(adapter);

        phoneNum = (EditText)findViewById(R.id.txtPhoneNum);
        BTlbl = (TextView)findViewById(R.id.BTlbl);
        delBtn = (Button)findViewById(R.id.delBtn);
        delBtn.setVisibility(View.INVISIBLE);
        listTitle = (TextView)findViewById(R.id.listTitle);
        datalbl = (TextView)findViewById(R.id.datalbl);
        timelbl = (TextView)findViewById(R.id.timelbl);
        locationlbl = (TextView)findViewById(R.id.lblLocation);
        locationlbl.setMovementMethod(LinkMovementMethod.getInstance());
        switchEnabled = (Switch) findViewById(R.id.switchEnabled);
        //onStart();

        //pick contact from phone contacts
        findViewById(R.id.btnContacts).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                pickContact.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                startActivityForResult(pickContact, 1);
            }
        });

        //add picked contact to contacts listview
        findViewById(R.id.btnAdd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumStr = phoneNum.getText().toString();
                if(phoneNumStr.length() != 0)
                {
                    list.add(phoneNumStr);
                    adapter.notifyDataSetChanged();
                    sedt.putInt("numsList_size", list.size());
                    for(int n=0;n<list.size();n++)
                    sedt.putString("numsList_" + n, list.get(n));
                    sedt.commit();
                    phoneNum.setText("");
                }
            }
        });


        // multiple listview items checkboxes selection for delete
        contactsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckedTextView checkedTextView = ((CheckedTextView)view);
                checkedTextView.setChecked(!checkedTextView.isChecked());
               // String val =(String) parent.getItemAtPosition(position);
                if(checkedTextView.isChecked()) {
                    checkedCount++;
                    checkedIndexes.add(parent.getItemAtPosition(position).toString());
                }
                else {
                    checkedCount--;
                    checkedIndexes.remove(parent.getItemAtPosition(position).toString());
                }
                if(checkedCount > 0)
                    delBtn.setVisibility(View.VISIBLE);
                else
                    delBtn.setVisibility(View.INVISIBLE);
            }
        });

        //delete selected items from listview
        dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Do your DELETE progress
                        int checkedIndexesSize = checkedIndexes.size();
                        if(checkedIndexesSize > 0) {
                            for (int r = 0; r < checkedIndexesSize; r++) {
                                sedt.remove(checkedIndexes.get(r));
                                sedt.putInt("numsList_size", list.size() - 1);
                                sedt.commit();
                                list.remove(checkedIndexes.get(r));
                                adapter.notifyDataSetChanged();
                            }
                            for (int n = 0; n < list.size(); n++)
                                sedt.putString("numsList_" + n, list.get(n));
                            sedt.commit();
                            checkedIndexes.clear();
                            checkedIndexes = new ArrayList<>(Arrays.asList(new String[]{}));
                            checkedIndexesSize = 0;
                            checkedCount = 0;
                            delBtn.setVisibility(View.INVISIBLE);
                            contactsList.setAdapter(adapter);
                        }
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //Do your CANCEL progress
                        delBtn.setVisibility(View.INVISIBLE);
                        contactsList.setAdapter(adapter);
                        break;
                }
            }
        };

       //show delete dialog
        delBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder ab = new AlertDialog.Builder(MainActivity.this);
                ab.setMessage("Delete contact?").setNegativeButton("CANCEL", dialogClickListener)
                        .setPositiveButton("DELETE", dialogClickListener).show();
            }
        });

        switchEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET}
                                ,10);
                    }
                    return;
                }
                locationManager.requestLocationUpdates("gps", 0, 0, listener);
                accidentLocation =locationManager.getLastKnownLocation("gps");
                if(isChecked) {
                    onStart();
                }
                else {
                    //BTlbl.setText("Please Enable the system.");
                    Toast.makeText(MainActivity.this,"Please Enable the system",Toast.LENGTH_LONG).show();
                    locationlbl.setText("");
                    datalbl.setText("");
                    timelbl.setText("");
                }
            }
        });

        //GPS
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET}
                        ,10);
            }
            return;
        }
        locationManager.requestLocationUpdates("gps", 0, 0, listener);
        accidentLocation =locationManager.getLastKnownLocation("gps");
        //onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean previouslyStarted = prefs.getBoolean(getString(R.string.pref_previously_started), false);
        if(!previouslyStarted) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean(getString(R.string.pref_previously_started), Boolean.TRUE);
            edit.commit();
        }
    }


    void configure_button() {
        // first check for permissions
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}
                        , 10);
            }
            return;
        }
        //locationManager.requestLocationUpdates("gps", 5000, 0, listener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 10:
                configure_button();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // check whether the result is ok
        if (resultCode == RESULT_OK) {
            // Check for the request code, we might be usign multiple startActivityForReslut
            switch (requestCode) {
                case 1:
                    Uri uri = data.getData();
                    //Query the content uri
                    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                    cursor.moveToFirst();
                    // column index of the phone number
                    int  phoneIndex =cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    // column index of the contact name
                    int  nameIndex =cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    phoneNum.setText(cursor.getString(phoneIndex));
                    break;
            }
        }
        else Toast.makeText(MainActivity.this,"Failed to pick contact!",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        //Enable Bluetooth Request
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            //BTlbl.setText("No bluetooth adapter available");
            Toast.makeText(MainActivity.this,"No bluetooth adapter available",Toast.LENGTH_LONG).show();
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
       // while (switchEnabled.isChecked()) {
            try {
                openBT();
            } catch (IOException ex) {
                //BTlbl.setText("Error in Opening Bluetooth connection!");
                Toast.makeText(MainActivity.this,"Error in Opening Bluetooth connection!",Toast.LENGTH_LONG).show();
            }
        //}
    }
    boolean EnableBTReq(){
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            Toast.makeText(MainActivity.this,enableBluetooth.getAction(),Toast.LENGTH_LONG).show();
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

            //BTlbl.setText("Bluetooth Opened");
            Toast.makeText(MainActivity.this,"Bluetooth Connection Opened",Toast.LENGTH_LONG).show();
            //openedBT = true;
        }
        // Receive data via Bluetooth
       // while (EnableBTReq()) {
         //   if (switchEnabled.isChecked())
                beginListenForData();
           // myLabel.setText("Please Enable the system.");
    //    }
    }

    // Receive data via Bluetooth function
    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    final long start = System.currentTimeMillis();
                   /* if (!switchEnabled.isChecked()) {
                        myLabel.setText("Please Enable the system.");
                        stopWorker = true;
                    }*/
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                   data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run() {
                                            long elapsedTime = 0;
                                            //datalbl.setText("Received data: " + data);
                                            if (data.equals("1\r")) {
                                                data = "";
                                                    //GPS - Turn on GPS & Get location
                                                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}
                                                                    , 10);
                                                        }
                                                        return;
                                                    }

                                                locationManager.requestLocationUpdates("gps", 0, 0, listener);
                                                accidentLocation = locationManager.getLastKnownLocation("gps");

                                                if (accidentLocation != null) {
                                                    double lat = accidentLocation.getLatitude();
                                                    double lon = accidentLocation.getLongitude();

                                                    //Create location link & message str
                                                    wazeURL = Uri.parse("https://waze.com/ul?ll=" + lat + "," + lon + "navigate=yes").toString();
                                                    googlemapsURL = Uri.parse("https://maps.google.com/maps?saddr=" + lat + "," + lon).toString();
                                                    messageStr = "ACCIDENT DETECTED !!!\nStart Navigation with,\nWaze: " + wazeURL + "\nor\nGoogle Maps: " + googlemapsURL + "\n\nSent by- HD Survive app";
                                                    //GSM - Send SMS
                                                    ArrayList<String> msgStringArray = sms.divideMessage(messageStr);
                                                    //for(int i=0; i<list.size(); i++)
                                                    //sms.sendMultipartTextMessage(list.get(i), null, msgStringArray, null, null);

                                                } else {
                                                    messageStr = "Accident detected !!! Unable to find location";
                                                    //GSM - Send SMS
                                                    //for(int i=0; i<list.size(); i++)
                                                    //sms.sendTextMessage(list.get(i), null, messageStr, null, null);

                                                }
                                                //locationlbl.setText(messageStr);
                                                //elapsedTime = System.currentTimeMillis() - start;
                                                //timelbl.setText("Time (msec): " + elapsedTime);
                                            }
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });
        //onStart();
        workerThread.start();
    }

    // Close Bluetooth connection
    void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        //BTlbl.setText("Bluetooth Connection Closed");
        Toast.makeText(MainActivity.this,"Bluetooth Connection Closed",Toast.LENGTH_LONG).show();
    }
}

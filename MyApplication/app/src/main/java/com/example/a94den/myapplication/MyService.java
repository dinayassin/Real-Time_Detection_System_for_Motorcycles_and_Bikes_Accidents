package com.example.a94den.myapplication;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by 94den on 05-Jun-18.
 */

public class MyService extends Service {
    private final IBinder binder = new LocalBinder();
    private boolean isRunning  = false;

    // Class used for the client Binder.
    public class LocalBinder extends Binder {
        MyService getService() {
            // Return this instance of MyService so clients can call public methods
            return MyService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        isRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Creating new thread for my service
        //Always write your long running tasks in a separate thread, to avoid ANR
        new Thread(new Runnable() {
            @Override
            public void run() {

                    Intent dialogIntent = new Intent(MyService.this, MainActivity.class);
                    dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(dialogIntent);

                //Stop service once it finishes its task
                //stopSelf();
            }
        }).start();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
    }
}

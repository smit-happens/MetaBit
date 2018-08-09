package com.barely.just.metabit;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Debug;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.builder.filter.Comparison;
import com.mbientlab.metawear.builder.filter.ThresholdOutput;
import com.mbientlab.metawear.builder.function.Function1;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Logging;

import bolts.Continuation;
import bolts.Task;


public class MainActivity extends Activity implements ServiceConnection {

    private static final String LOG_TAG = "freefall";
    private BtleService.LocalBinder serviceBinder;
    private MetaWearBoard mwBoard;
    private Accelerometer accelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, BtleService.class),
                this, Context.BIND_AUTO_CREATE);

        findViewById(R.id.start).setOnClickListener(view -> {
            accelerometer.acceleration().start();
            accelerometer.start();
        });
        findViewById(R.id.stop).setOnClickListener(view -> {
            accelerometer.stop();
            accelerometer.acceleration().stop();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        // Typecast the binder to the service's LocalBinder class
        serviceBinder = (BtleService.LocalBinder) service;

        Log.i(LOG_TAG, "Service Connected");

        String mwMacAddress= "C4:28:94:29:A6:39";   // board's MAC address
        BluetoothManager btManager= (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothDevice btDevice= btManager.getAdapter().getRemoteDevice(mwMacAddress);

        mwBoard= serviceBinder.getMetaWearBoard(btDevice);
        mwBoard.connectAsync().onSuccessTask(task -> {
            accelerometer = mwBoard.getModule(Accelerometer.class);
            accelerometer.configure()
                    .odr(50f)
                    .commit();
            return accelerometer.acceleration().addRouteAsync(source ->
                    source.stream((Subscriber) (data, env) -> {
                        Log.i(LOG_TAG, data.value(Acceleration.class).toString());
                    }));
        }).continueWith((Continuation<Route, Void>) task -> {
            if (task.isFaulted()) {
                Log.e(LOG_TAG, mwBoard.isConnected() ? "Error setting up route" : "Error connecting", task.getError());
            } else {
                Log.i(LOG_TAG, "Connected");
//                debug = mwBoard.getModule(Debug.class);
//                logging= mwBoard.getModule(Logging.class);
            }

            return null;
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }
}
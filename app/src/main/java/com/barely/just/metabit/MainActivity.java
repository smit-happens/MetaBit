package com.barely.just.metabit;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.module.Accelerometer;

import bolts.Continuation;

/*
 * Imports below for Chart example app huccccccccccccccccccccccccccccccccccc
 */
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.WindowManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.util.ArrayList;


public class MainActivity extends Activity implements ServiceConnection {

    private static final String LOG_TAG = "freefall";
    private BtleService.LocalBinder serviceBinder;
    private MetaWearBoard mwBoard;
    private Accelerometer accelerometer;
    private LineChart[] mCharts = new LineChart[4];

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

        /*
          Chart huccccc code below
         */
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mCharts[0] = findViewById(R.id.chart1);
        mCharts[1] = findViewById(R.id.chart2);
        mCharts[2] = findViewById(R.id.chart3);
        mCharts[3] = findViewById(R.id.chart4);

        Typeface mTf = Typeface.createFromAsset(getAssets(), "OpenSans-Bold.ttf");

        for (int i = 0; i < mCharts.length; i++) {

            LineData data = getData(36, 100);
            data.setValueTypeface(mTf);

            // add some transparency to the color with "& 0x90FFFFFF"
            setupChart(mCharts[i], data, mColors[i % mColors.length]);
        }

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

    /**
     * CHART STUFF
     * @param count
     * @param range
     * @return
     */
    private LineData getData(int count, float range) {

        ArrayList<Entry> yVals = new ArrayList<Entry>();

        for (int i = 0; i < count; i++) {
            float val = (float) (Math.random() * range) + 3;
            yVals.add(new Entry(i, val));
        }

        // create a dataset and give it a type
        LineDataSet set1 = new LineDataSet(yVals, "DataSet 1");
        // set1.setFillAlpha(110);
        // set1.setFillColor(Color.RED);

        set1.setLineWidth(1.75f);
        set1.setCircleRadius(5f);
        set1.setCircleHoleRadius(2.5f);
        set1.setColor(Color.WHITE);
        set1.setCircleColor(Color.WHITE);
        set1.setHighLightColor(Color.WHITE);
        set1.setDrawValues(false);

        // create a data object with the datasets
        LineData data = new LineData(set1);

        return data;
    }

    /**
     * CHART STUFF
     */
    private int[] mColors = new int[] {
            Color.rgb(137, 230, 81),
            Color.rgb(240, 240, 30),
            Color.rgb(89, 199, 250),
            Color.rgb(250, 104, 104)
    };

    /**
     * CHART STUFF HERE
     * @param chart
     * @param data
     * @param color
     */
    private void setupChart(LineChart chart, LineData data, int color) {

        ((LineDataSet) data.getDataSetByIndex(0)).setCircleColorHole(color);

        // no description text
        chart.getDescription().setEnabled(false);

        // mChart.setDrawHorizontalGrid(false);
        //
        // enable / disable grid background
        chart.setDrawGridBackground(false);
//        chart.getRenderer().getGridPaint().setGridColor(Color.WHITE & 0x70FFFFFF);

        // enable touch gestures
        chart.setTouchEnabled(true);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(false);

        chart.setBackgroundColor(color);

        // set custom chart offsets (automatic offset calculation is hereby disabled)
        chart.setViewPortOffsets(10, 0, 10, 0);

        // add data
        chart.setData(data);

        // get the legend (only possible after setting data)
        Legend l = chart.getLegend();
        l.setEnabled(false);

        chart.getAxisLeft().setEnabled(false);
        chart.getAxisLeft().setSpaceTop(40);
        chart.getAxisLeft().setSpaceBottom(40);
        chart.getAxisRight().setEnabled(false);

        chart.getXAxis().setEnabled(false);

        // animate calls invalidate()...
        chart.animateX(2500);
    }
}
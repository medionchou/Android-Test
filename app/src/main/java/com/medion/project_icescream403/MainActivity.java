package com.medion.project_icescream403;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog dialog;
    private HandlerExtension mHandler;
    private Thread clientThread;
    private Thread scaleThread;
    private ScaleManager scaleManager;
    private Client client;
    private List< List<Recipe> > recipeGroup;

    private Scale[] scales;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initObject();

        if (scales.length > 0) {
            clientThread.start();
            scaleThread.start();
        }
        else {
            /** TODO:
             *      Perhaps need to deliver Recipe data to the intent restarted.
             **/

            restartActivity();
        }
    }

    private void initObject() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        List<UsbDevice> scaleList = new ArrayList<>();

        for (UsbDevice device : deviceList.values()) {
            Log.v("Client", device.toString());
            if (device.getVendorId() == 1659) {
                scaleList.add(device);
            }
        }

        scales = new Scale[scaleList.size()];
        for (int i = 0; i < scaleList.size(); i++) {
            scales[i] = new Scale(scaleList.get(i), this);
        }

        Log.v("Client", "Connected USB: " + String.valueOf(scales.length));

        dialog = new ProgressDialog(this);
        mHandler = new HandlerExtension(this);
        client = new Client(mHandler, this);
        clientThread = new Thread(client);
        scaleManager = new ScaleManager(scales);
        scaleThread = new Thread(scaleManager);

    }

    private void deRefObject() {
        dialog = null;
        mHandler = null;
        client = null;
        clientThread = null;
        scaleManager = null;
        scaleThread = null;

        for (int i = 0; i < scales.length; i++) {
            unregisterReceiver(scales[i].getBroadcastReceiver());
            scales[i].stopDataRetrieval(true);
            scales[i].stopConnectUsb(true);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (clientThread == null && scaleThread == null) {
            Log.v("Client", "thread is null");
            initObject();
            if (recipeGroup != null) {
                client.setRecipeGroup(recipeGroup);
            }

            if (scales.length > 0) {
                clientThread.start();
                scaleThread.start();
            }
            else {
                restartActivity();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v("Client", "onStop");
        if (recipeGroup == null) {
            recipeGroup = client.getRecipeGroup();
        }
        clientThread.interrupt();
        client.terminate();
        if (dialog.isShowing())
            dialog.dismiss();

        deRefObject();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        recipeGroup = null;
    }

    private void displayDialog(int state) {
        switch (state) {
            case ClientState.CONNECTING:
                dialog.setTitle(getString(R.string.progress_title));
                dialog.setMessage(getString(R.string.progress_message));
                dialog.show();
                dialog.setCancelable(false);
                break;
            case ClientState.CONNECTED:
                Log.v("Client", "Dismiss");
                dialog.dismiss();
                break;
        }
    }

    private void restartActivity() {
        Timer timer = new Timer();

        dialog.setTitle(getString(R.string.progress_warning));
        dialog.setMessage(getString(R.string.restart_message));
        dialog.show();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (dialog.isShowing())
                    dialog.dismiss();
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        }, 5000);
    }

    public void retainRecipe(List< List<Recipe> > recipeGroup) {
        this.recipeGroup = recipeGroup;

        for (int i = 0; i < recipeGroup.size(); i++) {
            List<Recipe> tmp = recipeGroup.get(i);
            for (int j = 0; j < tmp.size(); j++) {
                Log.v("Client", tmp.get(j).toString());
            }
        }
    }

    private static class HandlerExtension extends Handler {
        private WeakReference<MainActivity> weakRef;

        public HandlerExtension(MainActivity activity) {
            weakRef = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MainActivity activity = weakRef.get();

            if (activity != null) {
                activity.displayDialog(msg.what);
            }
        }
    }

    private class ScaleManager implements Runnable {

        private Scale[] scales;
        private int scaleCount;
        private boolean stop = false;

        public ScaleManager(Scale[] scales) {
            this.scales = scales;
            scaleCount = scales.length;
        }

        @Override
        public void run() {

            while (!stop) {
                try {
                    Thread.sleep(1000);
                    Log.v("Client", String.valueOf(scales[scaleCount - 1].getScaleWeight()));
                } catch (InterruptedException e) {
                    Log.v("Query", e.toString());
                }
            }
        }

        public void setScaleCount(int val) {
            scaleCount = val;
        }

        public int getScaleCount() {
            return scaleCount;
        }

        public void terminate() {
            stop = true;
        }
    }


    public void debug1(View view) {

        client.setCmd("DEBUG_RECIPE 1<END>");
    }

    public void debug2(View view) {

        client.setCmd("DEBUG_RECIPE 2<END>");
    }

}

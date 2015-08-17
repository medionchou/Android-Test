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
import android.widget.Button;
import android.widget.TextView;

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
    private Button confirmButton;
    private Timer timer;
    private Scale[] scales;
    private boolean isAnyUsbConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initObject();

        for (int i = 0; i < scales.length; i++)
            isAnyUsbConnected |= scales[i].isUsbConnected();


        if (scales.length > 0 && isAnyUsbConnected) {
            clientThread.start();
            scaleThread.start();
        }
        else {
            /** TODO:
             *      Perhaps need to deliver Recipe data to the intent restarted.
             **/
            restartActivity(States.USB_RESTART);
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
        confirmButton = (Button) findViewById(R.id.confirm);
        confirmButton.setEnabled(false);
        confirmButton.setOnClickListener(new ButtonListener());
        timer = new Timer();
    }

    private void deRefObject() {
        dialog = null;
        mHandler = null;
        client = null;
        clientThread = null;
        scaleManager = null;
        scaleThread = null;
        timer = null;

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
                restartActivity(States.USB_RESTART);
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
        scaleThread.interrupt();
        client.terminate();
        scaleManager.terminate();
        timer.cancel();
        if (dialog.isShowing())
            dialog.dismiss();
        deRefObject();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //recipeGroup = null;
    }

    private void displayDialog(int state) {
        switch (state) {
            case States.CONNECTING:
                dialog.setTitle(getString(R.string.progress_title));
                dialog.setMessage(getString(R.string.progress_message));
                dialog.show();
                dialog.setCancelable(false);
                break;
            case States.CONNECTED:
                Log.v("Client", "Dismiss");
                dialog.dismiss();
                break;
            case States.PDA_CONNECTING:
                dialog.setTitle(getString(R.string.progress_title));
                dialog.setMessage(getString(R.string.pda_connecting));
                dialog.show();
                dialog.setCancelable(false);
                break;
            case States.PDA_CONNECTED:
                dialog.dismiss();
                break;
        }
    }

    public void restartActivity(int type) {
        if (type == States.USB_RESTART) {
            dialog.setTitle(getString(R.string.progress_warning));
            dialog.setMessage(getString(R.string.usb_restart_message));
        } else if (type == States.SERVER_RESTART) {
            dialog.setTitle(getString(R.string.progress_warning));
            dialog.setMessage(getString(R.string.server_restart_message));
        }
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
        private int scaleIndex;
        private int recipeIndex;
        private boolean stop = false;

        public ScaleManager(Scale[] scales) {
            this.scales = scales;
            scaleCount = scales.length;
            scaleIndex = 0;
            recipeIndex = 0;
        }

        @Override
        public void run() {
            final TextView recipeIDView = (TextView) findViewById(R.id.recipeID_text_view);
            final TextView productIDView = (TextView) findViewById(R.id.productID_text_view);
            final TextView productWeightView = (TextView) findViewById(R.id.product_weight_text_view);
            final TextView scaleWeightView = (TextView) findViewById(R.id.scale_weight_text_view);

            while (!stop) {
                try {

                    if (!scales[scaleIndex].isDataPrepared()) continue;

                    Thread.sleep(1000);

                    final double scaleWeight = scales[scaleIndex].getScaleWeight().getWeight();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (recipeGroup != null ) {
                                if (recipeGroup.size() > 0) {

                                    List<Recipe> recipeList = recipeGroup.get(0);
                                    Recipe recipe = recipeList.get(recipeIndex);
                                    double productWeight = recipe.getWeight();
                                    recipeIDView.setText(recipe.getIngredientName());
                                    productIDView.setText(recipe.getProductName());
                                    productWeightView.setText(String.valueOf(productWeight) + " " + recipe.getWeightUnit());

                                    if (Math.abs(productWeight - scaleWeight) < 0.1)
                                        confirmButton.setEnabled(true);
                                    else
                                        confirmButton.setEnabled(false);

                                } else {

                                    recipeIDView.setText(R.string.no_data);
                                    productIDView.setText(R.string.no_data);
                                    productWeightView.setText(R.string.no_data);
                                    confirmButton.setEnabled(false);
                                }
                            }
                            scaleWeightView.setText(String.valueOf(scaleWeight) + " KG");
                        }
                    });

                } catch (InterruptedException e) {
                    Log.e("Client", "ScaleManager " + e.toString());
                }
            }

        }

        public void setScaleIndex(int val) {
            scaleIndex = val;
        }

        public int getScaleIndex() {
            return scaleIndex;
        }

        public void setRecipeIndex(int val) {
            recipeIndex = val;
        }

        public int getRecipeIndex() {
            return recipeIndex;
        }

        public int getScaleCount() {
            return scaleCount;
        }

        public void terminate() {
            stop = true;
        }
    }

    private class ButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            int scaleIndex = (scaleManager.getScaleIndex() + 1) % scaleManager.getScaleCount() ;
            int recipeIndex = scaleManager.getRecipeIndex();
            int recipeLength = recipeGroup.get(0).size();
            scaleManager.setScaleIndex(scaleIndex);

            if (recipeIndex == (recipeLength - 1)) {
                recipeIndex = 0;
                scaleManager.setRecipeIndex(recipeIndex);
                recipeGroup.remove(0);
                /*
                    TODO:
                        to notify server that task has been completed.
                 */
            } else {
                recipeIndex = recipeIndex + 1;
                scaleManager.setRecipeIndex(recipeIndex);
            }
        }
    }


    public void debug1(View view) {

        client.setCmd("DEBUG_RECIPE 1<END>");
    }

    public void debug2(View view) {

        client.setCmd("DEBUG_RECIPE 2<END>");
    }

}

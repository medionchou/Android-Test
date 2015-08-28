package com.medion.project_icescream403;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private final int VENDOR_ID = 1659;

    private ProgressDialog dialog;
    private ProgressDialog pdaDialog;
    private HandlerExtension mHandler;
    private Thread clientThread;
    private Thread scaleThread;
    private ScaleManager scaleManager;
    private Client client;
    private List<List<Recipe>> recipeGroup;
    private Button confirmButton;
    private Button nextButton;
    private Timer timer;
    private Scale[] scales;
    private TextView scannedItemTextView;
    private List<UsbDevice> scaleList;

    private boolean isAnyUsbConnected; ///
    private int globalState;

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
        } else {
            /** TODO:
             *      Perhaps need to deliver Recipe data to the intent restarted.
             **/
            restartActivity(States.USB_RESTART);
        }
    }

    private void initObject() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        scaleList = new ArrayList<>();

        for (UsbDevice device : deviceList.values()) {
            Log.v("Client", device.toString());
            if (device.getVendorId() == VENDOR_ID) {
                //Log.v("Client", device.toString());
                scaleList.add(device);
            }
        }
        Log.v("Client", "HET");

        Collections.sort(scaleList, new Comparator<UsbDevice>() {
            @Override
            public int compare(UsbDevice lhs, UsbDevice rhs) {
                return lhs.getDeviceName().compareTo(rhs.getDeviceName());
            }
        });

        scales = new Scale[scaleList.size()];
        for (int i = 0; i < scaleList.size(); i++) {
            // creating scales asyntask to receive scale data
            //Log.v("Client", scaleList.get(i).toString());
            if (i == 0)
                scales[i] = new Scale(scaleList.get(i), this, String.valueOf(i), false);
            else
                scales[i] = new Scale(scaleList.get(i), this, String.valueOf(i), true);

        }

        Log.v("Client", "Connected USB: " + String.valueOf(scales.length));

        dialog = new ProgressDialog(this);
        pdaDialog = new ProgressDialog(this);
        mHandler = new HandlerExtension(this);
        client = new Client(mHandler, this);
        clientThread = new Thread(client);
        scaleManager = new ScaleManager(scales);
        scaleThread = new Thread(scaleManager);
        confirmButton = (Button) findViewById(R.id.confirm);
        confirmButton.setEnabled(false);
        confirmButton.setOnClickListener(new ConfirmButtonListener());
        nextButton = (Button) findViewById(R.id.nextBatch);
        nextButton.setEnabled(false);
        nextButton.setOnClickListener(new NextButtonListener());
        scannedItemTextView = (TextView) findViewById(R.id.scanned_item_text_view);
        timer = new Timer();
        isAnyUsbConnected = false;
    }

    private void deRefObject() {
        dialog = null;
        pdaDialog = null;
        mHandler = null;
        client = null;
        clientThread = null;
        scaleManager = null;
        scaleThread = null;
        timer = null;
        confirmButton = null;
        nextButton = null;

        for (int i = 0; i < scales.length; i++) {

            stopScale(i);
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
            } else {
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
        scaleManager.terminate();
        client.terminate();
        dialog.cancel();
        pdaDialog.cancel();
        timer.cancel();
        scannedItemTextView.setText("歷史配料");
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
                globalState = States.CONNECTING;
                dialog.setTitle(getString(R.string.progress_title));
                dialog.setMessage(getString(R.string.progress_message));
                dialog.show();
                dialog.setCancelable(false);
                break;
            case States.CONNECTED:
                Log.v("Client", "Dismiss");
                globalState = States.CONNECTED;
                if (dialog.isShowing())
                    dialog.dismiss();
                break;
            case States.PDA_CONNECTING:
                globalState = States.PDA_CONNECTING;
                dialog.setTitle(getString(R.string.progress_title));
                dialog.setMessage(getString(R.string.pda_connecting));
                dialog.show();
                dialog.setCancelable(false);
                Log.v("Client", "Waiting for PDA Connection");
                break;
            case States.PDA_CONNECTED:
                globalState = States.PDA_CONNECTED;
                if (dialog.isShowing())
                    dialog.dismiss();
                break;
        }
    }

    private void stopScale(int index) {
        try {
            unregisterReceiver(scales[index].getBroadcastReceiver());
            scales[index].stopDataRetrieval(true);
            scales[index].stopConnectUsb(true);
        } catch(IllegalArgumentException e) {
            Log.e("Client", "IllegalArgumentException: " + e.toString());
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
        }, 10000);

    }


    public void retainRecipe(List<List<Recipe>> recipeGroup) {
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
        private int scaleCount; //how many scales are connected
        private int scaleIndex; //index of scale
        private int recipeIndex; // recipe entry in a recipe list
        private int pdaState; // pda status
        private boolean stop = false; // indicate terminatio

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
                    if (stop) break;

                    if (!scales[scaleIndex].isDataPrepared()) continue;
                    if (dialog.isShowing() || globalState != States.PDA_CONNECTED) {
                        Thread.sleep(1000);
                        continue;
                    }

                    Thread.sleep(1000);
                    if (client == null) break;

                    String serialNum = client.getSerialNumber();
                    final String ingredientName;
                    final String productName;
                    final String productWeightText;
                    final String productID;
                    final boolean enabled;
                    final double scaleWeight =  scales[scaleIndex].getScaleWeight().getWeight();

                    if (recipeGroup != null) {
                        if (recipeGroup.size() > 0) {

                            List<Recipe> recipeList = recipeGroup.get(0);
                            Recipe recipe = recipeList.get(recipeIndex);
                            double productWeight = recipe.getWeight();

                            ingredientName = recipe.getIngredientName();
                            productName = recipe.getProductName();
                            productWeightText = String.valueOf(productWeight) + " " + recipe.getWeightUnit();
                            productID = recipe.getProductID();
                            //serialNum = client.getSerialNumber();

                            if (serialNum.equals("")) {
                                /*
                                    While recipe is empty, and then receiving a new recipe input from server. After that, invoke progressDialog to ask
                                    PDA scan barcode.
                                 */
                                pdaState = States.PDA_SCANNING;
                                client.setSerialNumber("Unchecked");
                            } else if (productID.equals(serialNum)) {
                                /*
                                    PDA query match the specified serial Number
                                 */
                                pdaState = States.PDA_SCANNING_CORRECT;
                                client.setSerialNumber("Unchecked");
                                client.setCmd("QUERY_REPLY\tOK<END>");
                            } else if (!productID.equals(serialNum) && !serialNum.equals("Unchecked")) {
                                /*
                                    Send wrong command if worker scan incorrect barcode.
                                 */
                                pdaState = States.PDA_SCANNING;
                                client.setSerialNumber("Unchecked");
                                client.setCmd("QUERY_REPLY\tWRONG<END>");
                            }

                            if (Math.abs(productWeight - scaleWeight) < 0.1)
                                enabled = true;
                            else
                                enabled = false;

                        } else {
                            if (!serialNum.equals("")) {
                                /*
                                    Send empty command if no recipe contain here.
                                */
                                client.setCmd("QUERY_REPLY\tEMPTY<END>");
                                client.setSerialNumber("");
                            }
                            pdaState = States.PDA_NO_INPUT_DATA;
                            ingredientName = getString(R.string.no_data);
                            productName = getString(R.string.no_data);
                            productWeightText = getString(R.string.no_data);
                            productID = getString(R.string.no_data);
                            enabled = false;
                        }

                    } else {
                        if (!serialNum.equals("")) {
                            /*
                                Send empty command if no recipe contain here.
                             */
                            client.setCmd("QUERY_REPLY\tEMPTY<END>");
                            client.setSerialNumber("");
                        }

                        pdaState = States.PDA_NO_INPUT_DATA;
                        ingredientName = getString(R.string.no_data);
                        productName = getString(R.string.no_data);
                        productWeightText = getString(R.string.no_data);
                        productID = getString(R.string.no_data);
                        enabled = false;
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (pdaState == States.PDA_SCANNING) {
                                pdaDialog.setTitle(getString(R.string.pda_progress_status));
                                pdaDialog.setMessage(productID + " " + productName);
                                pdaDialog.show();
                                pdaDialog.setCancelable(false);
                                pdaState = States.PDA_IDLING;
                                Log.v("Client", "Updating GGG");
                            } else if (pdaState == States.PDA_SCANNING_CORRECT) {
                                if (pdaDialog.isShowing())
                                    pdaDialog.dismiss();
                            }

                            recipeIDView.setText(ingredientName);
                            productIDView.setText(productName);
                            productWeightView.setText(productWeightText);
                            scaleWeightView.setText(String.valueOf(scaleWeight) + " KG");
                            if (enabled)
                                scaleWeightView.setTextColor(Color.GREEN);
                            else
                                scaleWeightView.setTextColor(Color.RED);

                            confirmButton.setEnabled(true);
                        }
                    });


                } catch (InterruptedException e) {
                    Log.e("Client", "ScaleManager " + e.toString());
                }
            }

        }

        public void setPdaState(int state) {
            pdaState = state;
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

    private class ConfirmButtonListener implements View.OnClickListener {
        private int count = 1;

        @Override
        public void onClick(View v) {
            int scaleIndex = scaleManager.getScaleIndex();
            int oldIndex = scaleIndex;
            int newIndex;
            /*int recipeIndex = scaleManager.getRecipeIndex();
            int recipeLength = recipeGroup.get(0).size();*/


            scaleIndex = (scaleIndex + 1) % scaleManager.getScaleCount();
            newIndex = scaleIndex;
            Log.v("Client", String.valueOf(scaleIndex));
            if (newIndex != oldIndex) {
                stopScale(oldIndex);
                scales[scaleIndex] = new Scale(scaleList.get(scaleIndex), MainActivity.this, String.valueOf(scaleIndex), false);
            }
            scaleManager.setScaleIndex(scaleIndex);


            /*if (recipeIndex == (recipeLength - 1)) {
                recipeIndex = 0;
                scaleManager.setRecipeIndex(recipeIndex);
                recipeGroup.remove(0);
                client.setCmd("RECIPE_DONE<END>");
                scannedItemTextView.setText("歷史配料");

                if (recipeGroup.size() > 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    AlertDialog alertDialog;

                    builder.setTitle(R.string.alert_dialog_title)
                           .setMessage(R.string.alert_dialog_message);
                    alertDialog = builder.create();
                    alertDialog.show();
                    nextButton.setEnabled(true);
                } else {
                     set serialNumber to empty in order to reset the state to original.
                    client.setSerialNumber("");
                }

            } else {
                TextView scaleWeightView = (TextView) findViewById(R.id.scale_weight_text_view);
                String tmp = scannedItemTextView.getText().toString();
                List<Recipe> recipeList = recipeGroup.get(0);
                Recipe recipe = recipeList.get(recipeIndex);

                if (tmp.length() > 0)
                    tmp += "\n";
                tmp += "第" + String.valueOf(count) + "筆:\n" + "\t物料名稱: " + recipe.getProductName() + "\n\t秤得重量: " + scaleWeightView.getText().toString();
                scannedItemTextView.setText(tmp);
                recipeIndex = recipeIndex + 1;
                scaleManager.setRecipeIndex(recipeIndex);
                scaleManager.setPdaState(States.PDA_SCANNING);
                confirmButton.setEnabled(false);
                count++;
            }*/
        }
    }

    private class NextButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            scaleManager.setPdaState(States.PDA_SCANNING);
            nextButton.setEnabled(false);
        }
    }


}

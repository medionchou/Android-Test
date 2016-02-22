package com.medion.project_icescream403;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;


import org.w3c.dom.Text;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private List<String> weight;
    private Button confirmButton;
    private Button nextButton;
    private Timer timer;
    private ScaleInterface[] scales; //
    private TextView recipeGroupTextView;
    private MarqueeTextView runningTextView;
    private List<UsbDevice> scaleList;
    private LinearLayout layout;

    private boolean isAnyUsbConnected; ///
    private int globalState;

    private int count;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initObject();

//        clientThread.start();
//        scaleThread.start();

        for (int i = 0; i < scales.length; i++)
            isAnyUsbConnected |= scales[i].isUsbConnected();

        if (scales.length > 0 && isAnyUsbConnected) {
            clientThread.start();
            scaleThread.start();
        } else {
            restartActivity(States.USB_RESTART);
        }
    }

    private void initObject() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        scaleList = new ArrayList<>();

        /**
         * TODO: undo for loop and sorting.
         */

        for (UsbDevice device : deviceList.values()) {
            if (device.getVendorId() == VENDOR_ID) {
                scaleList.add(device);
            }
        }

        Collections.sort(scaleList, new Comparator<UsbDevice>() {
            @Override
            public int compare(UsbDevice lhs, UsbDevice rhs) {
                return lhs.getDeviceName().compareTo(rhs.getDeviceName());
            }
        });

//         for Debug
//        for (int i = 0; i < 4; i++) {
//            scaleList.add(null);
//        }


        /**
         * TODO: rename ScaleSimulator
         */
        scales = new Scale[scaleList.size()];
        for (int i = 0; i < scaleList.size(); i++) {
            // creating scales asyntask to receive scale data
            //Log.v("MyLog", scaleList.get(i).toString());
            if (i == 0)
                scales[i] = new Scale(scaleList.get(i), this, String.valueOf(i), false);
            else
                scales[i] = new Scale(scaleList.get(i), this, String.valueOf(i), true);

        }

        dialog = new ProgressDialog(this);
        pdaDialog = new ProgressDialog(this);
        confirmButton = (Button) findViewById(R.id.confirm);
        confirmButton.setEnabled(false);
        confirmButton.setOnClickListener(new ConfirmButtonListener());
        nextButton = (Button) findViewById(R.id.nextBatch);
        nextButton.setEnabled(false);
        nextButton.setOnClickListener(new NextButtonListener());
        recipeGroupTextView = (TextView) findViewById(R.id.recipe_group_text_view);
        layout = (LinearLayout) findViewById(R.id.detail_recipe_layout);
        runningTextView = (MarqueeTextView) findViewById(R.id.marquee_text_view);
        timer = new Timer();
        isAnyUsbConnected = false;
        weight = new ArrayList<>();
        mHandler = new HandlerExtension(this);
        client = new Client(mHandler, this);
        clientThread = new Thread(client);
        scaleManager = new ScaleManager(scales, this);
        scaleThread = new Thread(scaleManager);

        drawDetailRecipe(scaleManager.getRecipeIndex());
        count = 0;

        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                String msg = ((TextView) MainActivity.this.dialog.findViewById(android.R.id.message)).getText().toString();

                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (count == 40) {
                        count = 0;
                        dialog.dismiss();
                    } else {
                        count++;
                    }
                }
                return false;
            }
        });
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
            Log.v("MyLog", "thread is null");
            initObject();
//            if (recipeGroup != null) {
//                client.setRecipeGroup(recipeGroup);
//            }

            if (scales.length > 0) {
                clientThread.start();
                scaleThread.start();

            } else {
                restartActivity(States.USB_RESTART);
            }
        }
        recipeGroupTextView.setText("配方清單");
        layout.removeAllViews();
        createTextView("配方內容", Color.GRAY);
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v("MyLog", "onStop");
//        if (recipeGroup == null) {
//            recipeGroup = client.getRecipeGroup();
//        }
        clientThread.interrupt();
        scaleThread.interrupt();
        scaleManager.terminate();
        client.terminate();
        dialog.cancel();
        pdaDialog.cancel();
        timer.cancel();
        deRefObject();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //recipeGroup = null;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final LinearLayout ip_portLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.ip_port_layout, null);
                builder.setTitle("設定IP及PORT");
                builder.setView(ip_portLayout);

                builder.setPositiveButton("確認", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences settings = getSharedPreferences("IPFILE", 0);
                        SharedPreferences.Editor editor = settings.edit();
                        EditText ipView = (EditText) ip_portLayout.findViewById(R.id.ip);
                        EditText portView = (EditText) ip_portLayout.findViewById(R.id.port);
                        String ipTest = ipView.getText().toString();
                        String portTest = portView.getText().toString();
                        String ip = "127.0.0.1";
                        Pattern pattern = Pattern.compile("[0-9]{1,3}+\\.[0-9]{1,3}+\\.[0-9]{1,3}+\\.[0-9]{1,3}+");
                        Matcher matcher = pattern.matcher(ipTest);
                        int port = 0;
                        boolean checker;

                        checker = matcher.matches();

                        if (checker) {
                            if (!portTest.equals("")) {
                                if (Integer.valueOf(portTest) <= 65536) {
                                    port = Integer.valueOf(portTest);
                                    checker = true;
                                } else {
                                    checker = false;
                                }
                            }
                        }

                        if (checker) {
                            String[] strip = ipTest.split("\\.");
                            boolean isMatch = true;

                            for (String tmp : strip) {
                                if (Integer.valueOf(tmp) > 255)
                                    isMatch = false;
                            }

                            if (isMatch)
                                ip = ipTest;

                            checker = isMatch;
                        }


                        if (checker) {
                            editor.putString("IP", ip);
                            editor.putInt("PORT", port);
                            editor.apply();
                        } else {
                            AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                            alert.setTitle("警告");
                            alert.setMessage("IP 或 PORT 設定錯誤");
                            alert.show();
                        }
                    }
                });
                builder.show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void displayDialog(int state) {
        switch (state) {
            case States.CONNECTING:
                globalState = States.CONNECTING;
                dialog.setTitle(setDialogText(getString(R.string.progress_title), 1));
                dialog.setMessage(setDialogText(getString(R.string.progress_message), 3));
                dialog.show();
                dialog.setCancelable(false);
                break;
            case States.CONNECTED:
                Log.v("MyLog", "Dismiss");
                globalState = States.CONNECTED;
                if (dialog.isShowing())
                    dialog.dismiss();
                break;
            case States.PDA_CONNECTING:
                globalState = States.PDA_CONNECTING;
                dialog.setTitle(setDialogText(getString(R.string.progress_title), 1));
                dialog.setMessage(setDialogText(getString(R.string.pda_connecting), 3));
                dialog.show();
                dialog.setCancelable(false);
                Log.v("MyLog", "Waiting for PDA Connection");
                break;
            case States.PDA_CONNECTED:
                globalState = States.PDA_CONNECTED;
                if (dialog.isShowing())
                    dialog.dismiss();
                Log.v("MyLog", "Connecting");
                break;
        }
    }

    private void stopScale(int index) {
        try {
            /**
             * TODO: undo unregisterReceiver
             */
            unregisterReceiver(scales[index].getBroadcastReceiver());
            scales[index].stopDataRetrieval(true);
            scales[index].stopConnectUsb(true);
        } catch (IllegalArgumentException e) {
            Log.e("MyLog", "IllegalArgumentException: " + e.toString());
        }

    }

    public void setRunningTextView(String text) {
        runningTextView.setText(text);
    }

    public void setNewRunningTextView(String text) {
        runningTextView.setNewText(text);
    }

    public void restartActivity(int type) {
        if (type == States.USB_RESTART) {
            dialog.setTitle(setDialogText(getString(R.string.progress_warning), 1));
            dialog.setMessage(setDialogText(getString(R.string.usb_restart_message), 3));
        } else if (type == States.SERVER_RESTART) {
            dialog.setTitle(setDialogText(getString(R.string.progress_warning), 1));
            dialog.setMessage(setDialogText(getString(R.string.server_restart_message), 3));
        }
        dialog.setCancelable(false);
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
        scaleManager.setRecipeGroup(recipeGroup);
        this.recipeGroup = recipeGroup;
        drawRecipeGroup();
    }

    public void setPrecision(List<String> precision) {
        scaleManager.setPrecision(precision);
    }

    public SpannableString setDialogText(String text, float size) {
        SpannableString ss = new SpannableString(text);
        ss.setSpan(new RelativeSizeSpan(size), 0, ss.length(), 0);
        return ss;
    }

    public ProgressDialog getDialog() {
        return dialog;
    }

    public ProgressDialog getPdaDialog() {
        return pdaDialog;
    }

    public Client getClient() {
        return client;
    }

    public int getGlobalState() {
        return globalState;
    }

    private void drawDetailRecipe(int index) {
        final int pos = index;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                layout.removeAllViews();

                createTextView("配方內容", Color.GRAY);
                if (recipeGroup != null) {
                    if (recipeGroup.size() > 0) {
                        List<Recipe> recipes = recipeGroup.get(0);
                        for (int i = 0; i < recipes.size(); i++) {
                            String contents = recipes.get(i).getProductName();
                            int color = Color.GRAY;

                            if (i < pos) {
                                contents += "\t秤得重量: " +  weight.get(i);
                                color = Color.GREEN;
                            }
                            createTextView(contents, color);
                        }
                    }
                }
            }
        });
    }

    private void createTextView(String words, int color) {
        TextView tv = new TextView(MainActivity.this);
        tv.setText(words);
        tv.setTextSize(50);
        tv.setBackgroundColor(color);
        layout.addView(tv);
    }

    private void drawRecipeGroup() {
        String contents = "配方清單\n";
        final String result;
        int count = 0;

        for (List<Recipe> recipes : recipeGroup) {
            Recipe recipe = recipes.get(0);
            contents += recipe.getIngredientName();

            if (count < recipeGroup.size() - 1)
                contents += "\n";
            count++;
        }
        result = contents;

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recipeGroupTextView.setText(result);
            }
        });

        drawDetailRecipe(scaleManager.getRecipeIndex());
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

    private class ConfirmButtonListener implements View.OnClickListener {

        private int count = 1;

        @Override
        public void onClick(View v) {
            int scaleIndex = scaleManager.getScaleIndex();
            int oldIndex = scaleIndex;
            int newIndex;
            int recipeIndex = scaleManager.getRecipeIndex();
            int recipeLength = recipeGroup.get(0).size();

            scaleIndex = (scaleIndex + 1) % scaleManager.getScaleCount();
            newIndex = scaleIndex;
            Log.v("MyLog", String.valueOf(scaleIndex));

            if (recipeIndex == (recipeLength - 1)) {
                scaleIndex = 0;
            }

            if (newIndex != oldIndex) {
                stopScale(oldIndex);
                /**
                 *  TODO:  Redo Scale
                 */
                scales[scaleIndex] = new Scale(scaleList.get(scaleIndex), MainActivity.this, String.valueOf(scaleIndex), false);
            }

            scaleManager.setScaleIndex(scaleIndex);

            if (recipeIndex == (recipeLength - 1)) {
                String ingredientID = recipeGroup.get(0).get(0).getIngredientID();
                String command = "RECIPE_DONE\t" + ingredientID + "\t";
                TextView scaleWeightView = (TextView) findViewById(R.id.scale_weight_text_view);
                String ww = scaleWeightView.getText().toString().substring(0, scaleWeightView.getText().toString().length() - 3);
                weight.add(ww);

                for (int i = 0; i < weight.size(); i++) {
                    String tmp = weight.get(i);
                    command += tmp;

                    if (i < weight.size() - 1)
                        command += "\t";
                }
                command += "<END>";


                weight.clear();
                recipeIndex = 0;
                scaleManager.setRecipeIndex(recipeIndex);
                recipeGroup.remove(0);
                drawDetailRecipe(scaleManager.getRecipeIndex());

                if (recipeGroup.size() != 0)
                    drawRecipeGroup();
                else
                    recipeGroupTextView.setText("配方清單");

                client.setCmd(command);


                if (recipeGroup.size() > 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    AlertDialog alertDialog;

                    builder.setTitle(R.string.alert_dialog_title)
                            .setMessage(R.string.alert_dialog_message);
                    alertDialog = builder.create();
                    alertDialog.show();
                    nextButton.setEnabled(true);
                } else {
                    // set serialNumber to empty in order to reset the state to original.
                    client.setSerialNumber("");
                }

            } else {
                TextView scaleWeightView = (TextView) findViewById(R.id.scale_weight_text_view);

                String ww = scaleWeightView.getText().toString().substring(0, scaleWeightView.getText().toString().length() - 3);
                weight.add(ww);
                recipeIndex = recipeIndex + 1;
                scaleManager.setRecipeIndex(recipeIndex);
                scaleManager.setPdaState(States.PDA_SCANNING);
                confirmButton.setEnabled(false);
                drawDetailRecipe(scaleManager.getRecipeIndex());
                count++;
            }
        }
    }

    private class NextButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            scaleManager.setPdaState(States.PDA_SCANNING);
            scaleManager.setScaleIndex(0);
            nextButton.setEnabled(false);
        }
    }


}

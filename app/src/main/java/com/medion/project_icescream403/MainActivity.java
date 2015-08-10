package com.medion.project_icescream403;


import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog dialog;
    private HandlerExtension mHandler;
    private Thread t;
    private Client client;
    private List< List<Mix> > mixGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initObject();
        t.start();
    }

    private void initObject() {

        dialog = new ProgressDialog(this);
        mHandler = new HandlerExtension(this);
        client = new Client(mHandler);
        t = new Thread(client);
    }

    private void deRefObject() {
        dialog = null;
        mHandler = null;
        client = null;
        t = null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (t == null) {
            Log.v("Client", "thread is null");
            initObject();
            if (mixGroup != null) {
                client.setMixGroup(mixGroup);
            }

            t.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v("Client", "onStop");
        if (mixGroup == null) {
            mixGroup = client.getMixGroup();
        }
        t.interrupt();
        client.terminate();
        if (dialog.isShowing())
            dialog.dismiss();

        deRefObject();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mixGroup = null;
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

    public void connect(View view) {

        client.setCmd("CONNECT MS_M<END>");
    }

    public void debug1(View view) {

        client.setCmd("DEBUG_MIX 1<END>");
    }

    public void debug2(View view) {

        client.setCmd("DEBUG_MIX 2<END>");
    }
}

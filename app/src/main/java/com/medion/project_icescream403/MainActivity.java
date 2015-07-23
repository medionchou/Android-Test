package com.medion.project_icescream403;

import android.content.Intent;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.medion.project_icescream403.Client;
import com.medion.project_icescream403.ClientState;
import com.medion.project_icescream403.R;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class MainActivity extends ActionBarActivity {


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
        mHandler = new HandlerExtension();
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

    private void drawGUI() {
        TextView textView = (TextView) findViewById(R.id.text_view);

        textView.setText("Socket built");

    }

    private class HandlerExtension extends Handler {

        @Override
        public void handleMessage(Message msg) {
            displayDialog(msg.what);

            super.handleMessage(msg);
        }
    }

    public void sendToClient(View view) {
        EditText editText = (EditText) findViewById(R.id.edit_view);
        TextView textView = (TextView) findViewById(R.id.text_view);

        if (editText.length() > 0) {
            textView.setText(editText.getText());
            client.setCmd(editText.getText().toString(), true);
            editText.setText("");
        }
    }
}

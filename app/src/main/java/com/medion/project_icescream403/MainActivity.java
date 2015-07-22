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

public class MainActivity extends ActionBarActivity {


    private ProgressDialog dialog;
    private HandlerExtension mHandler;
    private Thread t;
    private Client client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dialog = new ProgressDialog(this);
        mHandler = new HandlerExtension();
        client = new Client(mHandler);
        t = new Thread(client);

        t.start();
    }

    @Override
    protected synchronized void onPause() {
        super.onPause();

        // block connection until activity restart again
        try {
            t.wait();
        } catch (InterruptedException e) {
            Log.w("Client", "Thread wait error!" + e.getMessage());
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
        // block connection until activity restart again
        /*try {
            t.wait();
        } catch (InterruptedException e) {

        }*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (t.isInterrupted())
            notify();
    }

    @Override
    protected void onDestroy() {
        t.interrupt();
        super.onDestroy();
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

/*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/

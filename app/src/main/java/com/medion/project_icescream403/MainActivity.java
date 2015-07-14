package com.medion.project_icescream403;

import android.support.v7.app.ActionBarActivity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.medion.project_icescream403.Client;
import com.medion.project_icescream403.ClientState;
import com.medion.project_icescream403.R;

public class MainActivity extends ActionBarActivity {


    private ProgressDialog dialog;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            TextView textView = (TextView) findViewById(R.id.text_view);

            switch (msg.what) {
                case ClientState.connecting:
                    dialog.setTitle("Info");
                    dialog.setMessage("Connecting!!");
                    dialog.show();
                    dialog.setCancelable(false);
                    break;
                case ClientState.connected:
                    textView.setText("Socket built already!");
                    dialog.dismiss();
                    break;


            }
            super.handleMessage(msg);
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dialog = new ProgressDialog(this);
        Thread t = new Thread(new Client(mHandler));
        t.start();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


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

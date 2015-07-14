package com.medion.project_icescream403;


import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;


/**
 * Created by Medion on 2015/7/15.
 */
public class Client implements Runnable {
    private final String host = "140.113.210.29";
    private final int port = 8000;
    private Handler mHandler;
    private Socket socket;
    private boolean isSend;

    private int test = 0;

    public Client(Handler mHandler) {
        this.mHandler = mHandler;
        isSend = false;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        setUpConnection();

    }

    private void setUpConnection() {
        try {
            while(true) {
                if (socket == null || socket.isConnected() == false) {
                    Message msg = new Message();
                    msg.what = ClientState.connecting;
                    mHandler.sendMessage(msg);
                    Thread.sleep(1500);
                    socket = new Socket(host, port);
                    isSend = true;
                } else if (socket.isConnected()){
                    if (isSend) {
                        Message msg = new Message();
                        msg.what = ClientState.connected;
                        mHandler.sendMessage(msg);
                        isSend = false;
                    }
                    if (test == 0) {
                        DataInputStream in = new DataInputStream(socket.getInputStream());
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        String word = null;

                        out.writeUTF("CONNECT SH_1<END>");

                        word = in.readUTF();

                        if (word != null) {
                            Log.v("Client", word);
                            Thread.sleep(3000);
                        } else {
                            Log.v("Client", "Not receive from server");

                        }

                        out.writeUTF("CONNECT SH_1<END>");

                        word = in.readUTF();
                        if (word != null) {
                            Log.v("Client", word);
                            Thread.sleep(3000);
                        } else {
                            Log.v("Client", "Not receive from server");

                        }

                        test = 1;
                    }
                }
            }
        } catch(UnknownHostException e) {

        } catch(IOException e ) {

        } catch(InterruptedException e) {

        }
    }
}

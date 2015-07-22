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
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by Medion on 2015/7/15.
 */
public class Client implements Runnable {
    private final String SERVER_IP = "140.113.210.29";
    private final int SERVER_PORT = 9000;

    private Handler mHandler;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String cmd;
    private String serverReply;
    private List< List<Mix> > mixGroup;

    private boolean needWrite;
    private boolean updateGUI;

    private int state;


    public Client(Handler mHandler) {
        this.mHandler = mHandler;
        updateGUI = false;
        cmd = null;
        serverReply = "";
        needWrite = false;
        state = ClientState.WRITE;
        mixGroup = new LinkedList<>();
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        setUpConnection();

    }

    private void setUpConnection() {
        try {

            while(true) {
                if (socket == null || !socket.isConnected()) {
                    /*
                        Waiting for connection and retry to connect to server
                     */
                    Message msg = mHandler.obtainMessage();
                    msg.what = ClientState.CONNECTING;
                    mHandler.sendMessage(msg);
                    Thread.sleep(1500);
                    socket = new Socket(SERVER_IP, SERVER_PORT);
                    in = new DataInputStream(socket.getInputStream());
                    out = new DataOutputStream(socket.getOutputStream());
                    updateGUI = true;

                } else if (socket.isConnected()){
                    /*
                        Already connect to server
                     */
                    if (updateGUI) {
                        Message msg = mHandler.obtainMessage();
                        msg.what = ClientState.CONNECTED;
                        mHandler.sendMessage(msg);
                        updateGUI = false;
                    }

                    exchangeDataWithServer();
                }
            }
        } catch(UnknownHostException e) {
            /*Server not exist*/
            Log.e("Client", "UnknownHostException 88");

        } catch(IOException e ) {
            /*Socket error*/
            Log.e("Client", "IOException 92");

        } catch(InterruptedException e) {
            /**/
            Log.e("Client", "InterruptedException 96");
        }
    }

    private void exchangeDataWithServer() {
        try {

            if ((state == ClientState.WRITE) && needWrite) {

                byte[] writeData = cmd.getBytes();
                out.write(writeData);
                needWrite = false;
                if (cmd.contains("<END>"))
                    state = ClientState.READ;

            } else if (state == ClientState.READ){
                /**
                 * TODO:
                 *  need to implement whether server send back CONNECT OK msg to continue next step
                 */
                int bytesRead;
                byte[] readData = new byte[1024];

                bytesRead = in.read(readData);
                if (bytesRead > 0) {
                    String tmp = new String(readData, 0, bytesRead, Charset.forName("UTF-8"));
                    serverReply += tmp;

                    if (serverReply.contains("<END>")) {

                        if (serverReply.contains("MIX")) {
                            groupMix(serverReply);
                        }
                        Log.v("Client", serverReply);
                        serverReply = "";
                        state = ClientState.WRITE;
                    }
                } else { // Server socket closed
                    socket.close();
                    socket = null;
                }
            }

        } catch (IOException e) {
            /*Error on interacting with server*/
            Log.e("Client", "IOException 136");
            socket = null;
        }
    }

    public void setCmd(String cmd, boolean needWrite) {
        this.cmd = cmd;
        this.needWrite = needWrite;
    }


    private void groupMix(String serverReply) {
        String[] ingredients = serverReply.split("\\t|<N>|<END>");
        List<Mix> item = new LinkedList<>();

        for (int i = 0; i < ingredients.length; i=i+4) {
            item.add(new Mix(ingredients[i+1], ingredients[i+2], Double.parseDouble(ingredients[i+3])));
        }
        mixGroup.add(item);

        for (int i = 0; i < mixGroup.size(); i++) {
            List<Mix> tmp = mixGroup.get(i);
            for (int j = 0; j < tmp.size(); j++) {
                Log.v("Client", tmp.get(j).toString());
            }
        }
    }

    private class Mix {
        private String id;
        private String name;
        private double weight;

        public Mix(String id, String name, double weight) {
            this.id = id;
            this.name = name;
            this.weight = weight;
        }

        public String toString() {
            return id + " " + name + " " + weight;
        }
    }
}

package com.medion.project_icescream403;


import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by Medion on 2015/7/15.
 */
public class Client implements Runnable {
    private final String host = "140.113.210.29";
    private final int port = 9000;
    private Handler mHandler;
    private Socket socket;
    private String cmd;
    private String serverReply;
    private List< List<Mix> > mixGroup;

    private boolean needWrite;
    private boolean toUpdateGUI;

    private int state;


    public Client(Handler mHandler) {
        this.mHandler = mHandler;
        toUpdateGUI = false;
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
                if (socket == null || socket.isConnected() == false) {
                    /*
                        Waiting for connection and retry to connect server
                     */
                    Message msg = mHandler.obtainMessage();
                    msg.what = ClientState.CONNECTING;
                    mHandler.sendMessage(msg);
                    Thread.sleep(1500);
                    socket = new Socket(host, port);
                    toUpdateGUI = true;
                } else if (socket.isConnected()){
                    /*
                        Already connect to server
                     */
                    if (toUpdateGUI) {
                        Message msg = mHandler.obtainMessage();
                        msg.what = ClientState.CONNECTED;
                        mHandler.sendMessage(msg);
                        toUpdateGUI = false;
                    }

                    exchangeDataWithServer();
                    /*DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    byte[] bytes = new byte[1024];

                    if (test == 0) {
                        byte[] bytes1 = new String("CONNECT MS_M<END>").getBytes();
                        int bytesRead;
                        out.write(bytes1);
                        bytesRead = in.read(bytes1);

                        if (bytesRead > 0) {
                            String tmp = new String(bytes1, 0, bytesRead, Charset.forName("UTF-8"));
                            if (tmp.contains("<END>")) {
                                Log.v("Clinet", tmp);
                            }
                        }

                        test = 1;
                    } else {
                        Log.v("Clinet", "Receiving");
                        int bytesRead;
                        bytesRead = in.read(bytes);

                        if (bytesRead > 0) {
                            String tmp = new String(bytes, 0, bytesRead);
                            Log.v("Clinet", tmp);
                        }
                        Log.v("Clinet", "Receiving End");
                    }*/
                }
            }
        } catch(UnknownHostException e) {
            /*Server not exist*/

        } catch(IOException e ) {
            /*Socket error*/

        } catch(InterruptedException e) {
            /**/
        }
    }

    private void exchangeDataWithServer() {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            if ((state == ClientState.WRITE) && needWrite) {

                byte[] writeData = cmd.getBytes();
                out.write(writeData);
                needWrite = false;
                if (cmd.contains("<END>"))
                    state = ClientState.READ;

            } else if (state == ClientState.READ){

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

                        serverReply = "";
                        state = ClientState.WRITE;
                    }
                }
            }

        } catch (IOException e) {
            /*Error on interacting with server*/
        }
    }

    public void setCmd(String cmd, boolean needWrite) {
        this.cmd = cmd;
        this.needWrite = needWrite;
    }

    private void groupMix(String serverReply) {

        String[] ingredients = serverReply.split("<N>|<END>");
        List<Mix> item = new LinkedList<>();

        for (int i = 0; i < ingredients.length; i++) {
            if (ingredients[i].length() > 0) {
                String[] element = ingredients[i].split(" ");

                item.add(new Mix(Integer.parseInt(element[1]), element[2], Double.parseDouble(element[3])));
            }
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
        private int id;
        private String name;
        private double weight;

        public Mix(int id, String name, double weight) {
            this.id = id;
            this.name = name;
            this.weight = weight;
        }

        public String toString() {
            return id + " " + name + " " + weight;
        }
    }
}

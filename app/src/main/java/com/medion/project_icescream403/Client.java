package com.medion.project_icescream403;


import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private InputStream in;
    private OutputStream out;
    private String cmd;
    private String serverReply;
    private List< List<Mix> > mixGroup;

    private boolean needWrite;
    private boolean updateGUI;
    private boolean isTerminated;

    private int state;


    public Client(Handler mHandler) {
        this.mHandler = mHandler;
        updateGUI = false;
        needWrite = false;
        isTerminated = false;
        cmd = null;
        serverReply = "";
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
            while(!isTerminated) {
                if (socket == null) {
                    /*
                        Waiting for connection and retry to connect to server
                     */
                    Log.v("Client", "updateGUI connecting");
                    Message msg = mHandler.obtainMessage();
                    msg.what = ClientState.CONNECTING;
                    mHandler.sendMessage(msg);
                    Thread.sleep(1500);
                    socket = new Socket(SERVER_IP, SERVER_PORT);
                    in = socket.getInputStream();
                    out = socket.getOutputStream();
                    updateGUI = true;

                } else if (socket != null){
                    /*
                        Already connect to server
                     */
                    if (socket.isConnected()) {
                        if (updateGUI) {
                            Log.v("Client", "updateGUI");
                            Message msg = mHandler.obtainMessage();
                            msg.what = ClientState.CONNECTED;
                            mHandler.sendMessage(msg);
                            updateGUI = false;
                        }

                        exchangeDataWithServer();
                    }
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
            Log.e("Client", "Thread sleep exceptiong 105 " + e.toString());

        } finally {
            try {
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
                if (in != null) {
                    in.close();
                    in = null;
                }
                if (out != null) {
                    out.close();
                    out = null;
                }
            } catch (IOException err) {
                Log.e("Client", "IOException 111 " +  err.toString());
            }
        }
    }

    private void exchangeDataWithServer() {
        try {

            if ((state == ClientState.WRITE) && needWrite) {

                byte[] writeData = cmd.getBytes();
                out.write(writeData);
                needWrite = false;
                String subStr = cmd.substring(cmd.length() - 5 , cmd.length());
                Log.v("Client", subStr);
                if (subStr.equals("<END>"))
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
                    String tmp = new String(readData, 0, bytesRead);
                    serverReply += tmp;

                    String subStr = serverReply.substring(serverReply.length() - 5, serverReply.length());
                    Log.v("Client", subStr);
                    if (subStr.equals("<END>")) {
                        serverReply = new String(serverReply.getBytes(), Charset.forName("UTF-8"));

                        if (serverReply.contains("MIX")) {
                            groupMix(serverReply);
                        }
                        Log.v("Client", serverReply);
                        serverReply = "";
                        state = ClientState.WRITE;
                    }
                } else { // Server socket closed
                    in.close();
                    out.close();
                    socket.close();
                    socket = null;
                    in = null;
                    out = null;
                }
            }

        } catch (IOException e) {
            /*Error on interacting with server*/
            Log.e("Client", "IOException 136");
            try {
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
                if (in != null) {
                    in.close();
                    in = null;
                }
                if (out != null) {
                    out.close();
                    out = null;
                }
            } catch (IOException err) {
                Log.e("Client", "IOException 111 " +  err.toString());
            }
        }
    }

    public void setCmd(String cmd, boolean needWrite) {
        this.cmd = cmd;
        this.needWrite = needWrite;
    }

    public List< List<Mix> > getMixGroup(){
        return mixGroup;
    }

    public void setMixGroup(List< List<Mix>> mix) {
        mixGroup = mix;
    }

    public void terminate() {
        isTerminated = true;

        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
            if (in != null) {
                in.close();
                in = null;
            }
            if (out != null) {
                out.close();
                out = null;
            }
        } catch (IOException err) {
            Log.e("Client", "IOException 220 " +  err.toString());
        }

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

}

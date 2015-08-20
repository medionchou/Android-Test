package com.medion.project_icescream403;


import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by Medion on 2015/7/15.
 */
public class Client implements Runnable {
    private final String SERVER_IP = "140.113.167.14";
    private final int SERVER_PORT = 9000;

    private Handler mHandler;
    private ByteBuffer inputBuffer;
    private SocketChannel socketChannel;
    private String cmd;
    private String serverReplayBuffer;
    private String serialNum;
    private List< List<Recipe> > recipeGroup;
    private CharBuffer outStream;
    private MainActivity mainActivity;
    private int client_state;

    private boolean updateGUI;
    private boolean isTerminated;



    public Client(Handler mHandler, MainActivity mainActivity) {
        this.mHandler = mHandler;
        updateGUI = false;
        isTerminated = false;
        cmd = "";
        serverReplayBuffer = "";
        serialNum = "";
        inputBuffer = ByteBuffer.allocate(1024);
        recipeGroup = new LinkedList<>();
        this.mainActivity = mainActivity;

    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        setUpConnection();
    }

    private void setUpConnection() {
        try {
            while(!isTerminated) {
                if (socketChannel == null) {

                    /*
                        Waiting for connection and retry to connect to server
                     */
                    Log.v("Client", "updateGUI connecting");
                    Message msg = mHandler.obtainMessage();
                    msg.what = States.CONNECTING;
                    mHandler.sendMessage(msg);
                    //Thread.sleep(3000);
                    socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(false);
                    socketChannel.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT));

                    while (!socketChannel.finishConnect()) {
                        // Waiting for connection
                        Thread.sleep(3000);
                    }

                    updateGUI = true;
                    client_state = States.CONNECT_INITIALZING;

                } else if (socketChannel != null) {
                    /*
                        Already connect to server
                     */

                    // update ProgressDialog. Dismiss connecting dialog and show PDA connecting.
                    if (updateGUI) {
                        Log.v("Client", "updateGUI");
                        Message msg = mHandler.obtainMessage();
                        msg.what = States.CONNECTED;
                        mHandler.sendMessage(msg);
                        updateGUI = false;

                        msg = mHandler.obtainMessage();
                        msg.what = States.PDA_CONNECTING;
                        mHandler.sendMessage(msg);
                    }

                    // socket read if server sends data.
                    int num;
                    while ((num = socketChannel.read(inputBuffer)) > 0) {

                        inputBuffer.flip();
                        serverReplayBuffer += Charset.defaultCharset().decode(inputBuffer);
                        inputBuffer.clear();
                        while(serverReplayBuffer.contains("<END>")) {
                            int endIndex = serverReplayBuffer.indexOf("<END>") + 5;

                            if (endIndex == -1)
                                endIndex = 0;

                            String endLine = serverReplayBuffer.substring(0, endIndex);

                            Log.v("Client", endLine);

                            if (endLine.contains("CONNECT_OK<END>")) {
                                client_state = States.CONNECT_OK;
                            } else if (endLine.contains("RECIPE") && !endLine.contains("RECIPE_DONE")) {
                                groupMix(endLine);
                            } else if (endLine.contains("PDA_ON<END>")) {
                                Message msg = mHandler.obtainMessage();
                                msg.what = States.PDA_CONNECTED;
                                mHandler.sendMessage(msg);
                            } else if (endLine.contains("PDA_OFF<END>")) {
                                Message msg = mHandler.obtainMessage();
                                msg.what = States.PDA_CONNECTING;
                                mHandler.sendMessage(msg);
                            } else if (endLine.contains("QUERY_SPICE")) {
                                serialNum = endLine.split("\\t|<END>")[1];
                                Log.v("Client", serialNum + " " + "Test");
                            }
                            serverReplayBuffer = serverReplayBuffer.replace(endLine, "");
                        }
                    }

                    if (num < 0)
                        throw new IOException("Server disconnect");

                    // socket write if string cmd not empty
                    switch(client_state) {
                        case States.CONNECT_INITIALZING:
                            outStream = CharBuffer.wrap("CONNECT MS_M<END>");
                            while (outStream.hasRemaining()) {
                                socketChannel.write(Charset.defaultCharset().encode(outStream));
                            }
                            Thread.sleep(500);
                            outStream.clear();
                            break;
                        case States.CONNECT_OK:
                            if (cmd.length() > 0) {
                                Log.v("Client", "Sending");
                                outStream = CharBuffer.wrap(cmd);
                                while (outStream.hasRemaining()) {
                                    socketChannel.write(Charset.defaultCharset().encode(outStream));
                                }
                                cmd = "";
                                outStream.clear();
                            }
                            break;
                    }

                }
            }
        } catch(UnknownHostException e) {
            /*Server not exist*/
            Log.e("Client", "UnknownHostException 88 "  + e.toString());

        } catch(IOException e ) {
            /*Socket error*/
            Log.e("Client", "IOException 92 " + e.toString());

            // restart activity if connection fails.
            if (e.toString().contains("Server disconnect") || e.toString().contains("SocketTimeoutException")) {
                Log.v("Client", "Reconnect!!");
                mainActivity.runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                mainActivity.restartActivity(States.SERVER_RESTART);
                            }
                        }
                );
            }
        } catch(InterruptedException e) {
            /**/
            Log.e("Client", "Thread sleep exceptiong 105 " + e.toString());

        } finally {
            try {
                if (socketChannel != null)
                    socketChannel.close();
            } catch (IOException err) {
                Log.e("Client", "IOException 111 " +  err.toString());
            }
        }
    }


    /*
        For debug typein
     */
    public String getSerialNumber() {
        // serialNum empty means receive new input from empty state.
        return serialNum;
    }
    public void setSerialNumber(String serialNum) {
        this.serialNum = serialNum;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public List< List<Recipe> > getRecipeGroup(){
        return recipeGroup;
    }

    public void setRecipeGroup(List<List<Recipe>> mix) {
        recipeGroup = mix;
    }

    public void terminate() {
        isTerminated = true;
        try {
            if (socketChannel != null)
                socketChannel.close();

        } catch (IOException err) {
            Log.e("Client", "IOException 220 " +  err.toString());
        }

    }

    private void groupMix(String serverReply) {

        String[] ingredients = serverReply.split("\\t|<N>|<END>");
        List<Recipe> item = new LinkedList<>();

        for (int i = 0; i < ingredients.length; i=i+7) {
            item.add(new Recipe(ingredients[i+1], ingredients[i+2], ingredients[i+3], ingredients[i+4], Double.parseDouble(ingredients[i+5]), ingredients[i+6]));

        }
        recipeGroup.add(item);
        mainActivity.retainRecipe(recipeGroup);

    }

}

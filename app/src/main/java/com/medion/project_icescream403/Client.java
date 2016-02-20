package com.medion.project_icescream403;


import android.app.AlertDialog;
import android.content.SharedPreferences;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by Medion on 2015/7/15.
 */
public class Client implements Runnable {
    private String SERVER_IP = "192.168.1.250";
    private int SERVER_PORT = 9000;

    private Handler mHandler;
    private ByteBuffer inputBuffer;
    private SocketChannel socketChannel;
    private String cmd;
    private String serverReplayBuffer;
    private String serialNum;
    private String msg;
    private String oldMsg;
    private List<List<Recipe>> recipeGroup;
    private CharBuffer outStream;
    private MainActivity mainActivity;
    private int client_state;
    private List<Byte> buffer;
    private List<String> precision;

    private boolean updateGUI;
    private boolean isTerminated;


    public Client(Handler mHandler, MainActivity mainActivity) {
        this.mHandler = mHandler;
        updateGUI = false;
        isTerminated = false;
        cmd = "";
        serverReplayBuffer = "";
        serialNum = "";
        msg = "";
        oldMsg = "";
        inputBuffer = ByteBuffer.allocate(1024);
        recipeGroup = new LinkedList<>();
        this.mainActivity = mainActivity;

        SharedPreferences settings = mainActivity.getSharedPreferences("IPFILE", 0);
        SERVER_IP = settings.getString("IP", Command.SERVER_IP);
        SERVER_PORT = settings.getInt("PORT", 9000);

        buffer = new ArrayList<>();
        precision = new ArrayList<>();
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        setUpConnection();
    }

    private void setUpConnection() {
        try {
            com.medion.project_icescream403.Log.getRequest("<h2>*** Client Start ***</h2>");
            while (!isTerminated) {
                if (socketChannel == null) {

                    /*
                        Waiting for connection and retry to connect to server
                     */
                    Log.v("MyLog", "updateGUI connecting");
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
                        Log.v("MyLog", "updateGUI");
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
                        while (inputBuffer.hasRemaining()) {
                            buffer.add(inputBuffer.get());
                        }

                        inputBuffer.clear();
                    }

                    if (num < 0)
                        throw new IOException("Server disconnect");

                    if (buffer.size() > 0) {
                        if (buffer.get(buffer.size() - 1) > 0) {
                            byte[] tmp = new byte[buffer.size()];
                            for (int i = 0; i < tmp.length; i++)
                                tmp[i] = buffer.get(i);
                            serverReplayBuffer += new String(tmp, "UTF-8");
                            buffer.clear();
                        }
                    }

                    while (serverReplayBuffer.contains("<END>")) {
                        int endIndex = serverReplayBuffer.indexOf("<END>") + 5;

                        if (endIndex == -1)
                            endIndex = 0;

                        String endLine = serverReplayBuffer.substring(0, endIndex);

                        Log.v("MyLog", endLine);

                        if (endLine.contains("CONNECT_OK<END>")) {
                            client_state = States.CONNECT_OK;
                        } else if (endLine.contains("RECIPE") && !endLine.contains("RECIPE_DONE")) {
                            groupMix(endLine);
                            com.medion.project_icescream403.Log.getRequest("<b><font size=\"5\" color=\"#7AC405\"> RECIPE: </font></b>" + endLine);
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
                            com.medion.project_icescream403.Log.getRequest("<b><font size=\"5\" color=\"#7AC405\"> QUERY_SPICE: </font></b>" + endLine);
                        } else if (endLine.contains("MSG")) {
                            String tmp;
                            tmp = endLine.replace("<END>", "");
                            tmp = tmp.replace("MSG\t", "");
                            msg = tmp;
                        } else if (endLine.contains("AC_RANGE")) {
                            parsePrecision(endLine);
                            com.medion.project_icescream403.Log.getRequest("<b><font size=\"5\" color=\"#7AC405\"> AC_RANGE: </font></b>" + endLine);
                        }

                        serverReplayBuffer = serverReplayBuffer.replace(endLine, "");
                    }


                    // socket write if string cmd not empty
                    switch (client_state) {
                        case States.CONNECT_INITIALZING:
                            outStream = CharBuffer.wrap("CONNECT\tMS_M<END>");
                            while (outStream.hasRemaining()) {
                                socketChannel.write(Charset.defaultCharset().encode(outStream));
                            }
                            Thread.sleep(500);
                            outStream.clear();
                            break;
                        case States.CONNECT_OK:
                            if (cmd.length() > 0) {
                                Log.v("MyLog", cmd);
                                com.medion.project_icescream403.Log.getRequest("<b><font size=\"5\" color=\"blue\">Send Command: </font></b>" + cmd);
                                outStream = CharBuffer.wrap(cmd);
                                while (outStream.hasRemaining()) {
                                    socketChannel.write(Charset.defaultCharset().encode(outStream));
                                }
                                cmd = "";
                                outStream.clear();
                            }
                            break;
                    }


                    if (msg.length() > 0 && !oldMsg.equals(msg)) {
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mainActivity.setRunningTextView(msg);
                            }
                        });

                    } else if (msg.length() > 0) {
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Calendar cal = Calendar.getInstance();
                                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                                String date = dateFormat.format(cal.getTime());
                                mainActivity.setNewRunningTextView(msg + date);
                            }
                        });
                    }
                    oldMsg = msg;
                    Thread.sleep(1000);
                }
            }
        } catch(UnknownHostException e) {
            /*Server not exist*/
            Log.e("MyLog", "UnknownHostException 88 "  + e.toString());

        } catch(IOException e ) {
            /*Socket error*/
            Log.e("MyLog", "IOException 92 " + e.toString());
            com.medion.project_icescream403.Log.getRequest("<b><font size=\"5\" color=\"red\">Caught exception in service:</font></b>" + e.toString());
            // restart activity if connection fails.
            if (e.toString().contains("Server disconnect") || e.toString().contains("SocketTimeoutException") || e.toString().contains("ECONNRESET")) {
                Log.v("MyLog", "Reconnect!!");
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
            Log.e("MyLog", "Thread sleep exceptiong 105 " + e.toString());

        } finally {
            try {
                if (socketChannel != null)
                    socketChannel.close();
            } catch (IOException err) {
                Log.e("MyLog", "IOException 111 " + err.toString());
            }
        }
    }


    /*
        For debug typein
     */
    public String getSerialNumber() {
        /*
            In empty state, serialNum empty means receive new input.
            In nonempty state, serialNum empty means invoke progressDialog to request worker to
            scan barcode.
         */
        return serialNum;
    }

    public void setSerialNumber(String serialNum) {
        this.serialNum = serialNum;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public List<List<Recipe>> getRecipeGroup() {
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
            Log.e("MyLog", "IOException 220 " + err.toString());
        }

    }

    private void groupMix(String serverReply) {

        String[] ingredients = serverReply.split("\\t|<N>|<END>");
        List<Recipe> item = new LinkedList<>();

        for (int i = 0; i < ingredients.length; i = i + 7) {
            item.add(new Recipe(ingredients[i + 1], ingredients[i + 2], ingredients[i + 3], ingredients[i + 4], Double.parseDouble(ingredients[i + 5]), ingredients[i + 6]));

        }
        recipeGroup.add(item);
        mainActivity.retainRecipe(recipeGroup);

    }

    private void parsePrecision(String raw) {
        String[] parsed = raw.split("\t|<END>");


        if (precision.size() > 0)
            precision.clear();

        for (int i = 1; i < parsed.length; i++) {
            precision.add(parsed[i]);
        }

        mainActivity.setPrecision(precision);
    }

}

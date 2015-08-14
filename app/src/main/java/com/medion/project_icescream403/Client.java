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
    private String serverReply;
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
        serverReply = "";
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
                    msg.what = ClientState.CONNECTING;
                    mHandler.sendMessage(msg);
                    Thread.sleep(1500);
                    socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(false);
                    socketChannel.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT));

                    while (!socketChannel.finishConnect()) {
                        // Waiting for connection
                    }

                    updateGUI = true;
                    client_state = ClientState.CONNECT_INITIALZING;

                } else if (socketChannel != null) {
                    /*
                        Already connect to server
                     */
                    if (updateGUI) {
                        Log.v("Client", "updateGUI");
                        Message msg = mHandler.obtainMessage();
                        msg.what = ClientState.CONNECTED;
                        mHandler.sendMessage(msg);
                        updateGUI = false;
                    }


                    while (socketChannel.read(inputBuffer) > 0) {

                        inputBuffer.flip();
                        serverReply += Charset.defaultCharset().decode(inputBuffer);
                        inputBuffer.clear();
                        if (serverReply.contains("<END>")) {
                            Log.v("Client",serverReply);
                            if (serverReply.contains("CONNECT_OK<END>")) {
                                client_state = ClientState.CONNECT_OK;
                            }
                            if (serverReply.contains("RECIPE")) {
                                groupMix(serverReply);
                            }
                            serverReply = "";
                        }
                    }

                    switch(client_state) {

                        case ClientState.CONNECT_INITIALZING:
                            outStream = CharBuffer.wrap("CONNECT MS_M<END>");
                            while (outStream.hasRemaining()) {
                                socketChannel.write(Charset.defaultCharset().encode(outStream));
                            }
                            Thread.sleep(500);
                            outStream.clear();
                            break;
                        case ClientState.CONNECT_OK:
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
            Log.e("Client", "UnknownHostException 88"  + e.toString());

        } catch(IOException e ) {
            /*Socket error*/
            Log.e("Client", "IOException 92" + e.toString());

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
        /*
             Used to display Recipe data content.
         */

        /*for (int i = 0; i < recipeGroup.size(); i++) {
            List<Recipe> tmp = recipeGroup.get(i);
            for (int j = 0; j < tmp.size(); j++) {
                Log.v("Client", tmp.get(j).toString());
            }
        }*/
    }

}

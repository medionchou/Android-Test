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
    private List< List<Mix> > mixGroup;

    private boolean updateGUI;
    private boolean isTerminated;



    public Client(Handler mHandler) {
        this.mHandler = mHandler;
        updateGUI = false;
        isTerminated = false;
        cmd = "";
        serverReply = "";
        inputBuffer = ByteBuffer.allocate(1024);
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
                            Log.v("Test",serverReply);
                            if (serverReply.contains("MIX")) {
                                groupMix(serverReply);
                            }
                            serverReply = "";
                        }
                    }


                    if (cmd.length() > 0) {
                        CharBuffer outStream = CharBuffer.wrap(cmd);
                        while (outStream.hasRemaining()) {
                            socketChannel.write(Charset.defaultCharset().encode(outStream));
                        }
                        cmd = "";
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

    public List< List<Mix> > getMixGroup(){
        return mixGroup;
    }

    public void setMixGroup(List< List<Mix>> mix) {
        mixGroup = mix;
    }

    public void terminate() {
        isTerminated = true;
        try {
            socketChannel.close();

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

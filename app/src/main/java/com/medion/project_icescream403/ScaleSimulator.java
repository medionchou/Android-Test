package com.medion.project_icescream403;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by Medion on 2015/8/12.
 */
public class ScaleSimulator implements ScaleInterface {
    private String ACTION_USB_PERMISSION = "USB_PERMISSION";

    /*rs-232 transferring setting */
    byte[] mPortSetting = new byte[7];
    int USB_RECIP_INTERFACE = 0x01;
    int SET_LINE_REQUEST = 0x20;
    int PROLIFIC_CTRL_OUT_REQTYPE = UsbConstants.USB_TYPE_CLASS | USB_RECIP_INTERFACE;


    private UsbDevice usbDevice;
    private UsbInterface intf;
    private UsbEndpoint endpoint;
    private UsbDeviceConnection connection;
    private UsbManager usbManager;
    private RetrieveScaleData retrieveScaleData;
    private final ScaleWeight scaleWeight = new ScaleWeight();
    private boolean stop = false;
    private boolean disconnect = false;
    private boolean dataState = false;

    public ScaleSimulator(UsbDevice usbDevice, final MainActivity activity, String permission, boolean isStop) {
        scaleWeight.setPositive(true);
        scaleWeight.setWeightValue(0.0);
        dataState = true;
    }

    public void stopDataRetrieval(boolean stop) {
        this.stop = stop;
    }

    public void stopConnectUsb(boolean disconnect) {
        this.disconnect = disconnect;
    }

    public boolean isDataPrepared() {
        return dataState;
    }

    public boolean isUsbConnected() {
        return connection != null;
    }

    private void processProductWeight(String rawData) {
        /**
         * Data Format:
         * 0 \r\n
         * 1 \r\n
         * 2 NO :
         * 3 G :
         * 4 N :
         */
        String[] data = rawData.split("\\r\\n");
        String[] netWeightInfo = data[4].split("[ ]+");

        synchronized (scaleWeight) {
            if (netWeightInfo[1].equals("+")) {
                scaleWeight.setPositive(true);
            } else {
                scaleWeight.setPositive(false);
            }
            scaleWeight.setWeightValue(Double.valueOf(netWeightInfo[2]));

            dataState = true;
        }
    }

    public synchronized ScaleWeight getScaleWeight() {
        if (scaleWeight.getWeight() > 5.0)
            scaleWeight.setWeightValue(0.0);
        scaleWeight.setWeightValue(scaleWeight.getWeight() + 0.1);

        return scaleWeight;
    }

    public BroadcastReceiver getBroadcastReceiver() {
        return null;
    }

    private void setControlMsg() {
        int baud = 9600;
        mPortSetting[0] = (byte)(baud & 0xff);
        mPortSetting[1] = (byte)((baud >> 8) & 0xff);
        mPortSetting[2] = (byte)((baud >> 16) & 0xff);
        mPortSetting[3] = (byte)((baud >> 24) & 0xff);
        mPortSetting[4] = 0;
        mPortSetting[5] = 0;
        mPortSetting[6] = 8;
    }

    class RetrieveScaleData extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            while (connection == null) {
                Log.v("Client", "No connection " + ACTION_USB_PERMISSION);

                if (disconnect) {
                    return (Void)null;
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Log.v("Scale", e.toString());
                }
                connection = usbManager.openDevice(usbDevice);
            }

            //if (stop) return (Void)null;

            boolean isClaimed;
            isClaimed = connection.claimInterface(intf, true);

            Log.v("Client", ACTION_USB_PERMISSION +" "+isClaimed);

            if (isClaimed) {
                int resOfControl;

                resOfControl = connection.controlTransfer(PROLIFIC_CTRL_OUT_REQTYPE, SET_LINE_REQUEST, 0, 0, mPortSetting, mPortSetting.length, 5000);

                Log.v("Client", String.valueOf(resOfControl));

                if (resOfControl > 0) {

                    byte[] bytes = new byte[endpoint.getMaxPacketSize()];
                    String rawData = "";

                    while (!stop) {

                        int val;
                        val = connection.bulkTransfer(endpoint, bytes, bytes.length, 500);


                        if (val > 0) {

                            for (int i = 0; i < val; i++) {
                                if (bytes[i] < 0) {
                                    bytes[i] += 128;
                                }
                                byte[] tmp = new byte[1];
                                tmp[0] = bytes[i];
                                rawData += new String(tmp, 0, 1);

                                if (rawData.length() - 3 > 0) {
                                    if (rawData.charAt(rawData.length() - 3) == 'c') {
                                        if (rawData.length() == 130) {
                                            processProductWeight(rawData);
                                        }
                                        rawData = "";
                                    }
                                }
                            }
                        }
                    }
                }
            }

            connection.releaseInterface(intf);
            connection.close();

            return (Void)null;
        }

    }
}

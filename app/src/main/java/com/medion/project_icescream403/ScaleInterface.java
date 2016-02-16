package com.medion.project_icescream403;

import android.content.BroadcastReceiver;
import android.util.Log;

/**
 * Created by Medion-PC on 2016/2/16.
 */
public interface ScaleInterface {
    public void stopDataRetrieval(boolean stop);

    public void stopConnectUsb(boolean disconnect);

    public BroadcastReceiver getBroadcastReceiver();

    public boolean isDataPrepared();

    public boolean isUsbConnected();

    public ScaleWeight getScaleWeight();
}

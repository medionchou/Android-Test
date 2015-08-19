package com.medion.project_icescream403;

/**
 * Created by Medion on 2015/7/15.
 */
public class States {
    public final static int CONNECTING = 0x0000000;
    public final static int CONNECTED = 0x0000001;

    public final static int CONNECT_INITIALZING = 0x0000002;
    public final static int CONNECT_OK = 0x0000003;

    public final static int USB_RESTART = 0x0000004;
    public final static int SERVER_RESTART = 0x0000005;


    public final static int PDA_CONNECTING = 0x0000006;
    public final static int PDA_CONNECTED = 0x0000007;

    public final static int PDA_SCANNING = 0x00000008;
    public final static int PDA_SCANNING_CORRECT = 0x00000009;
    public final static int PDA_IDLING = 0x00000010;
    public final static int PDA_NO_INPUT_DATA = 0x00000011;


}

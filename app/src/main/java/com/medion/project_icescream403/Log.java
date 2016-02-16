package com.medion.project_icescream403;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by Medion-PC on 2016/2/3.
 */
public class Log {

    private static String site = "http://192.168.1.250/wlog.php?";

    public static synchronized void getRequest(String words) {
        try {
            String mySite = site + "ID=MS_M&Log=" + URLEncoder.encode(words + "\n", "UTF-8");
            URL url = new URL(mySite);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            rd.close();

        } catch (Exception e) {
            android.util.Log.e("MyLog", "Unable write log ! ");
        }
    }

}
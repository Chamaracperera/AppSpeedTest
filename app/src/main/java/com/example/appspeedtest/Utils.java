package com.example.appspeedtest;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.telephony.TelephonyManager;

public class Utils {

    public static String getISPName(Context context) {
        try {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String carrierName = manager.getNetworkOperatorName();
            return carrierName != null && !carrierName.isEmpty() ? carrierName : "Unknown ISP";
        } catch (Exception e) {
            return "Unknown ISP";
        }
    }
}

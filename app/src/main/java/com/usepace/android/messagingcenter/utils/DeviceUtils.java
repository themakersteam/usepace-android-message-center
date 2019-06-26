package com.usepace.android.messagingcenter.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

public class DeviceUtils {

    /**
     *
     * @param context
     * @param number
     * Call a mobile number when the device is having permission
     */
    public static void call(Activity context, String number) {
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
        try {
            context.startActivity(intent);
        }
        catch (SecurityException g) {}
        catch (ActivityNotFoundException e){}
    }



    public static boolean isConnectedToInternet(Context context){

        boolean connected = false;
        if (context != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            connected = networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
        }
        return connected;
    }

}

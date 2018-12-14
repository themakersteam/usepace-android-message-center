package com.usepace.android.messagingcenter.network.sendbird;

/**
 * Created by MohammedNabil on 12/3/17.
 */
import com.sendbird.android.SendBird;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

class SendBirdPlatformApiHeaders implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();
        Request.Builder builder = request.newBuilder()
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Jand/" + SendBird.getSDKVersion())
                .addHeader("SendBird", "Android," + SendBird.getOSVersion() + "," + SendBird.getSDKVersion() + "," + SendBird.getApplicationId())
                .addHeader("Connection", "close");
        return chain.proceed(builder.build());
    }
}
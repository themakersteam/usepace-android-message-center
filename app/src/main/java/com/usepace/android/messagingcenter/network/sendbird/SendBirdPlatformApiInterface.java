package com.usepace.android.messagingcenter.network.sendbird;


import com.google.gson.JsonElement;
import java.util.HashMap;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

/**
 * Created by MohammedNabil
 */

 interface SendBirdPlatformApiInterface {

    @GET("users/{user_id}/unread_message_count")
    Call<JsonElement> getTotalUnreadMessagesCount(@Header("Session-Key") String session_key, @Path("user_id") String user_id, @QueryMap(encoded = true) HashMap<String, Object> hashMap);

    @GET("group_channels/{channel_url}/messages/unread_count")
    Call<JsonElement> getTotalUnreadMessagesCountForChannel(@Header("Session-Key") String session_key, @Path("channel_url") String channelUrl, @QueryMap(encoded = true) HashMap<String, Object> hashMap);

    @POST("users/{user_id}/login")
    Call<JsonElement> login(@Path("user_id") String user_id, @Body HashMap<String, Object> body);
}

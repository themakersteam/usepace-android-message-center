package com.usepace.android.messagingcenter.network.sendbird;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.sendbird.android.SendBird;
import com.usepace.android.messagingcenter.model.ConnectionRequest;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SendBirdPlatformApi {


    private String Apilink; /** Api Link**/
    private final long request_time_out = 20;
    private SendBirdPlatformApiInterface apiInterface;
    private Retrofit retrofit = null;
    private String session_key = null;

    private static SendBirdPlatformApi sendBirdPlatformApi;

    public SendBirdPlatformApi() {
        String applicationId;
        try {
            applicationId = SendBird.getApplicationId();
        }
        catch (Exception e ){
            applicationId = "send";
        }
        Apilink = "https://api-" + applicationId + ".sendbird.com/v3/";
        apiInterface = getClient().create(SendBirdPlatformApiInterface.class);
    }


    /**
     *
     * @param connectionRequest
     * @param key
     */
    public void login(ConnectionRequest connectionRequest, final SendBirdPlatformApiCallbackInterface<String> key) {
        if (connectionRequest == null || (connectionRequest != null && connectionRequest.getUserId() == null)) {
            key.onError("Missing Connection Request ");
        }
        else {
            if (session_key != null) {
                key.onSuccess(session_key);
            }
            else {
                HashMap<String, Object> requestBody = new HashMap<>();
                requestBody.put("app_id", connectionRequest.getAppId());
                requestBody.put("access_token", connectionRequest.getAccessToken());
                apiInterface.login(connectionRequest.getUserId(), requestBody).enqueue(new Callback<JsonElement>() {
                    @Override
                    public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                        if (response.isSuccessful()) {
                            session_key = response.body().getAsJsonObject().get("key").getAsString();
                            key.onSuccess(session_key);
                        } else {
                            try {
                                key.onError(response.code() + " " + response.errorBody() != null ? response.errorBody().string() : "");
                            } catch (Exception e) {
                                key.onError(e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonElement> call, Throwable t) {
                        key.onError(t != null && t.getMessage() != null ? t.getMessage() : "Network Failed to load");
                    }
                });
            }
        }
    }

    /**
     *
     * @param connectionRequest
     * @param callback
     */
    private void getTotalUnreadMessagesCount(final ConnectionRequest connectionRequest, final SendBirdPlatformApiCallbackInterface<Integer> callback) {
        if (session_key == null) {
            callback.onError("Login is in process....");
        }
        HashMap<String, Object> map  = new HashMap<>();
        map.put("super_mode", "all");
        apiInterface.getTotalUnreadMessagesCount(session_key, connectionRequest.getUserId(), map).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body().getAsJsonObject().get("unread_count").getAsInt());
                }
                else {
                    try {
                        callback.onError(response.code() + " " + response.errorBody() != null ? response.errorBody().string() : "");
                    }
                    catch (Exception e) {
                        callback.onError(e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                callback.onError(t != null && t.getMessage() != null ? t.getMessage() : "Network Failed to load");
            }
        });
    }


    /**
     *
     * @param callback
     */
    private void getTotalUnreadMessagesCountForChannel(final ConnectionRequest connectionRequest, final String channel_url, final SendBirdPlatformApiCallbackInterface<Integer> callback) {
        if (session_key == null) {
            callback.onError("Login is in process....");
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("user_ids", connectionRequest.getUserId());
        apiInterface.getTotalUnreadMessagesCountForChannel(session_key, channel_url, map).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body().getAsJsonObject().getAsJsonObject("unread").get(connectionRequest.getUserId()).getAsInt());
                } else {
                    try {
                        callback.onError(response.code() + " " + response.errorBody() != null ? response.errorBody().string() : "");
                    } catch (Exception e) {
                        callback.onError(e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                callback.onError(t != null && t.getMessage() != null ? t.getMessage() : "Network Failed to load");
            }
        });
    }

    /**
     *
     * @param connectionRequest
     * @param channel
     * @param callback
     */
    public void getTotalUnReadMessageCount(ConnectionRequest connectionRequest, String channel, SendBirdPlatformApiCallbackInterface<Integer> callback) {
        if (channel == null) {
            getTotalUnreadMessagesCount(connectionRequest, callback);
        }
        else {
            getTotalUnreadMessagesCountForChannel(connectionRequest, channel, callback);
        }
    }

    /**
     *
     * @return RetroFit Client Object For creating Api Requests
     */
    private Retrofit getClient() {
        OkHttpClient.Builder client = new OkHttpClient.Builder()
                .readTimeout(request_time_out, TimeUnit.SECONDS)
                .writeTimeout(request_time_out, TimeUnit.SECONDS)
                .connectTimeout(request_time_out, TimeUnit.SECONDS);
        client.addInterceptor(new SendBirdPlatformApiHeaders());
        Gson gson = new GsonBuilder().setLenient().create();
        retrofit = new Retrofit.Builder()
                .baseUrl(Apilink)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client.build())
                .build();
        return retrofit;
    }

    /** Instance for Api **/
    public static SendBirdPlatformApi Instance() {
        if (sendBirdPlatformApi == null)
            sendBirdPlatformApi = new SendBirdPlatformApi();
        return sendBirdPlatformApi;
    }

}

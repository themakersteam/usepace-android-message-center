package com.usepace.android.messagingcenter.model;

public class ConnectionRequest {

    private String app_id;
    private String user_id;
    private String access_token;
    private String client;
    private String fcm_token;

    public ConnectionRequest() {
    }

    public ConnectionRequest(String client, String fcm_token, String app_id, String user_id, String access_token) {
        this.access_token = access_token;
        this.app_id = app_id;
        this.user_id = user_id;
        this.client = client;
        this.fcm_token = fcm_token;
    }


    public void setClient(String client) {
        this.client = client;
    }

    public String getClient() {
        return client;
    }

    public void setAppId(String app_id) {
        this.app_id = app_id;
    }

    public String getAppId() {
        return app_id;
    }


    public void setUserId(String user_id) {
        this.user_id = user_id;
    }

    public String getUserId() {
        return user_id;
    }

    public void setAccessToken(String access_token) {
        this.access_token = access_token;
    }

    public String getAccessToken() {
        return access_token;
    }

    public void setFcmToken(String fcm_token) {
        this.fcm_token = fcm_token;
    }

    public String getFcmToken() {
        return fcm_token;
    }
}

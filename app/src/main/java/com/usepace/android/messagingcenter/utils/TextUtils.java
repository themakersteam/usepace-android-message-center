package com.usepace.android.messagingcenter.utils;

import com.sendbird.android.GroupChannel;
import com.sendbird.android.Member;
import com.sendbird.android.SendBird;
import com.sendbird.android.User;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TextUtils {

    public static String getGroupChannelTitle(GroupChannel channel) {
        List<Member> members = channel.getMembers();

        if (members.size() < 2 || SendBird.getCurrentUser() == null) {
            return "No Members";
        } else if (members.size() == 2) {
            StringBuffer names = new StringBuffer();
            for (Member member : members) {
                if (member.getUserId().equals(SendBird.getCurrentUser().getUserId())) {
                    continue;
                }

                names.append(", " + member.getNickname());
            }
            return names.delete(0, 2).toString();
        } else {
            int count = 0;
            StringBuffer names = new StringBuffer();
            for (User member : members) {
                if (member.getUserId().equals(SendBird.getCurrentUser().getUserId())) {
                    continue;
                }

                count++;
                names.append(", " + member.getNickname());

                if (count >= 10) {
                    break;
                }
            }
            return names.delete(0, 2).toString();
        }
    }

    /**
     *
     * @return
     * @throws UnsupportedEncodingException
     */
    public static Map<String, String> getQueries(String uri, String prefix) throws UnsupportedEncodingException {
        if (uri == null) throw new UnsupportedEncodingException("");
        if (uri.contains(prefix)) {
            uri = uri.replaceAll(prefix, "");
            if (uri.contains("?"))
                uri = uri.replaceAll("\\?", "");
            return splitQuery(uri);
        }
        return new HashMap<>();
    }

    /**
     *
     * @param url
     * @return HashMap split for a url
     * e.g to a keyValue pair
     *
     * @throws UnsupportedEncodingException
     */
    public static Map<String, String> splitQuery(String url) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        String query = url;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }


    public static String getLocationUrlMessageIfExists(String mMessage) {
        if (mMessage.startsWith("location://")) {
            try {
                Map<String, String> quirys = TextUtils.getQueries(mMessage, "location://");
                if (quirys.containsKey("lat") && quirys.containsKey("long")) {
                    mMessage = "https://www.google.com/maps/search/?api=1&query=" +  quirys.get("lat") + "," + quirys.get("long");
                    return mMessage;
                }
            } catch (UnsupportedEncodingException e) {

            }
        }
        return mMessage;
    }

    /**
     * Calculate MD5
     *
     * @param data
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String generateMD5(String data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(data.getBytes());
        byte messageDigest[] = digest.digest();

        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < messageDigest.length; i++)
            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));

        return hexString.toString();
    }
}
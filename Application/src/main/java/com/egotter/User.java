package com.egotter;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.OAuthCredential;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class User {
    public String uid;
    public String screenName;
    public String token;
    public String secret;

    public User(String uid, String screenName, String token, String secret) {
        this.uid = uid;
        this.screenName = screenName;
        this.token = token;
        this.secret = secret;
    }

    public User(AuthResult authResult) {

        Map<String, Object> profile = authResult.getAdditionalUserInfo().getProfile();
        this.uid = String.valueOf(profile.get("id"));
        this.screenName = (String) profile.get("screen_name");

        OAuthCredential credential = (OAuthCredential) authResult.getCredential();
        this.token = credential.getAccessToken();
        this.secret = credential.getSecret();
    }

    public String toString() {
        JSONObject json = new JSONObject();
        try {
            json.put("uid", uid);
            json.put("screenName", screenName);
            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}

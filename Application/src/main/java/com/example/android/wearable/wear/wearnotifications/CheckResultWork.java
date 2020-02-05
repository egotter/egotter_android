package com.example.android.wearable.wear.wearnotifications;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CheckResultWork extends Worker {

    public static final String TAG = CheckResultWork.class.getSimpleName();
    public static final String URL = "TODO REPLACE";

    public CheckResultWork(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        Log.d(TAG, "doWork()");

        try {
            JSONObject response = getJSONObjectFromURL(URL);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return Result.success();
    }

    public static JSONObject getJSONObjectFromURL(String urlString) throws IOException, JSONException {

        HttpURLConnection urlConnection = null;

        URL url = new URL(urlString);

        urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(3000);
        urlConnection.setConnectTimeout(5000);

        urlConnection.setDoOutput(true);

        urlConnection.connect();

        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));

        char[] buffer = new char[1024];

        String jsonString = new String();

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        br.close();

        jsonString = sb.toString();

        System.out.println("JSON: " + jsonString);
        urlConnection.disconnect();

        return new JSONObject(jsonString);
    }
}
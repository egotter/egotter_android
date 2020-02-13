/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.egotter.fcm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.egotter.ApiClient;
import com.egotter.BuildConfig;
import com.egotter.MainActivity;
import com.egotter.R;
import com.example.android.wearable.wear.common.mock.MockDatabase;
import com.example.android.wearable.wear.common.util.NotificationUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * NOTE: There can only be one service in each app that receives FCM messages. If multiple
 * are declared in the Manifest then the first one will be chosen.
 *
 * In order to make this Java sample functional, you must remove the following from the Kotlin messaging
 * service in the AndroidManifest.xml:
 *
 * <intent-filter>
 *   <action android:name="com.google.firebase.MESSAGING_EVENT" />
 * </intent-filter>
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages
        // are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data
        // messages are the type
        // traditionally used with GCM. Notification messages are only received here in
        // onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated
        // notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages
        // containing both notification
        // and data payloads are treated as notification messages. The Firebase console always
        // sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            Log.d(TAG, "Message data payload: " + data);

            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                sendPushNotification(data.get("title"), data.get("body"), data);

                if (data.containsKey("one_sided_friends")) {
                    saveSummaryItemsList(data);
                }
            }

            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use WorkManager.
                scheduleJob();
            } else {
                // Handle message within 10 seconds
                handleNow();
            }

        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            RemoteMessage.Notification notification = remoteMessage.getNotification();
            Log.d(TAG, "Message Notification Body: " + notification.getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]


    // [START on_new_token]

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        saveInstanceId(token);
    }
    // [END on_new_token]

    /**
     * Schedule async work using WorkManager.
     */
    private void scheduleJob() {
        // [START dispatch_job]
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(MyWorker.class)
                .build();
        WorkManager.getInstance().beginWith(work).enqueue();
        // [END dispatch_job]
    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private void handleNow() {
        Log.d(TAG, "Short lived task is done.");
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send token to your app server.
    }

    private void saveInstanceId(String token) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("fcm_instance_id", token);
        editor.apply();

        sendInstanceIdToServer();
    }

    private void saveSummaryItemsList(Map<String, String> summaryPayload) {
        Log.d(TAG, "saveSummaryItemsList() summaryPayload " + summaryPayload);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("one_sided_friends", summaryPayload.get("one_sided_friends"));
        editor.putString("one_sided_followers", summaryPayload.get("one_sided_followers"));
        editor.putString("mutual_friends", summaryPayload.get("mutual_friends"));
        editor.putString("unfriends", summaryPayload.get("unfriends"));
        editor.putString("unfollowers", summaryPayload.get("unfollowers"));
        editor.putString("blocking_or_blocked", summaryPayload.get("blocking_or_blocked"));
        editor.apply();

        LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(new Intent("com.egotter.MainActivity"));
    }

    private void sendInstanceIdToServer() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (prefs.contains("fcm_instance_id") && prefs.contains("twitter_id") && prefs.contains("twitter_access_token") && prefs.contains("twitter_access_secret")) {
            ApiClient.sendInstanceIdToServer(prefs.getString("twitter_id", ""),
                    prefs.getString("fcm_instance_id", ""),
                    prefs.getString("twitter_access_token", ""),
                    prefs.getString("twitter_access_secret", ""),
                    new ApiClient.HttpTask.CallbackListener() {
                        @Override
                        public void onCallback(String result) {
                            Log.d(TAG, "HttpTask result " + result);
                        }
                    });
        }
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(getString(R.string.fcm_message))
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    private void sendPushNotification(String title, String body, Map<String, String> payload) {

        MockDatabase.BigTextStyleReminderAppData bigTextStyleReminderAppData =
                MockDatabase.getBigTextStyleData();

        String notificationChannelId =
                NotificationUtil.createNotificationChannel(this, bigTextStyleReminderAppData);

        Intent openUrlIntent = new Intent(Intent.ACTION_VIEW);
        openUrlIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        openUrlIntent.setData(Uri.parse(
                "https://egotter.com/timelines/" + payload.get("screen_name") + "?via=" + getCurrentVia()));

        PendingIntent notifyPendingIntent =
                PendingIntent.getActivity(this, 0, openUrlIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent actionPendingIntent =
                PendingIntent.getActivity(this, 0, openUrlIntent, 0);

        NotificationCompat.Action openTimelineAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_alarm_white_48dp,
                        getString(R.string.openTimeline),
                        actionPendingIntent)
                        .build();

        NotificationCompat.Builder notificationCompatBuilder =
                new NotificationCompat.Builder(
                        getApplicationContext(), notificationChannelId);

        NotificationCompat.Builder notificationBuilder = notificationCompatBuilder
                .setSmallIcon(R.drawable.ic_stat_name)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_alarm_white_48dp))
                .setContentIntent(notifyPendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(openTimelineAction);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationCompatBuilder.setCategory(Notification.CATEGORY_SOCIAL);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationCompatBuilder.setPriority(NotificationManager.IMPORTANCE_DEFAULT);
        }

        if (title != null && !title.equals("")) {
            notificationBuilder.setContentTitle(title);
        }

        if (body != null && !body.equals("")) {
            notificationBuilder.setContentText(body);
        }

        NotificationManagerCompat.from(getApplicationContext()).
                notify(MainActivity.NOTIFICATION_ID, notificationBuilder.build());
    }

    private String getCurrentVia() {
        return "android" + BuildConfig.VERSION_NAME;
    }
}

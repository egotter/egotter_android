/*
Copyright 2016 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.egotter;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Action;
import androidx.core.app.NotificationCompat.BigPictureStyle;
import androidx.core.app.NotificationCompat.BigTextStyle;
import androidx.core.app.NotificationCompat.InboxStyle;
import androidx.core.app.NotificationCompat.MessagingStyle;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.PeriodicWorkRequest;

import com.egotter.ApiClient.HttpTask.CallbackListener;
import com.egotter.dialogs.OpenPlayStoreDialogFragment;
import com.egotter.dialogs.SignOutConfirmationDialogFragment;
import com.egotter.handlers.BigPictureSocialIntentService;
import com.egotter.handlers.BigPictureSocialMainActivity;
import com.egotter.handlers.BigTextIntentService;
import com.egotter.handlers.BigTextMainActivity;
import com.egotter.handlers.InboxMainActivity;
import com.egotter.handlers.MessagingIntentService;
import com.egotter.handlers.MessagingMainActivity;
import com.example.android.wearable.wear.common.mock.MockDatabase;
import com.example.android.wearable.wear.common.util.NotificationUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Map;

import hotchemi.android.rate.AppRate;
import hotchemi.android.rate.OnClickButtonListener;

/**
 * The Activity demonstrates several popular Notification.Style examples along with their best
 * practices (include proper Wear support when you don't have a dedicated Wear app).
 */
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, CallbackListener {

    public static final String TAG = "MainActivity";

    public static final int NOTIFICATION_ID = 888;

    private static final int SEND_INSTANCE_ID_INTERVAL = 60000;

    // Used for Notification Style array and switch statement for Spinner selection.
    private static final String BIG_TEXT_STYLE = "BIG_TEXT_STYLE";
    private static final String BIG_PICTURE_STYLE = "BIG_PICTURE_STYLE";
    private static final String INBOX_STYLE = "INBOX_STYLE";
    private static final String MESSAGING_STYLE = "MESSAGING_STYLE";

    // Collection of notification styles to back ArrayAdapter for Spinner.
    private static final String[] NOTIFICATION_STYLES = {
            BIG_TEXT_STYLE, BIG_PICTURE_STYLE, INBOX_STYLE, MESSAGING_STYLE
    };

    private static final String[] NOTIFICATION_STYLES_DESCRIPTION = {
            "Demos reminder type app using BIG_TEXT_STYLE",
            "Demos social type app using BIG_PICTURE_STYLE + inline notification response",
            "Demos email type app using INBOX_STYLE",
            "Demos messaging app using MESSAGING_STYLE + inline notification responses"
    };

    private NotificationManagerCompat mNotificationManagerCompat;

    private int mSelectedNotification = 0;

    // RelativeLayout required for SnackBars to alert users when Notifications are disabled for app.
    private RelativeLayout mMainRelativeLayout;
    private Spinner mSpinner;
    private TextView mNotificationDetailsTextView;
    private TextView signInButton;
    private TextView currentUserButton;
    private TextView reauthenticateButton;
    private TextView termsOfServiceText;
    private TextView privacyPolicyText;
    private TextView openEgotterText;
    private TextView aboutThisApp;
    private TextView lastSyncTimeText;
    private TextView oneSidedFriendsText;
    private TextView oneSidedFollowersText;
    private TextView mutualFriendsText;
    private TextView unfriendsText;
    private TextView unfollowersText;
    private TextView blockingOrBlockedText;

    private View signedInLayout;
    private View notSignedInLayout;

    private BroadcastReceiver broadcastReceiver;

    private PeriodicWorkRequest workRequest;

    private FirebaseAuth firebaseAuth;
    private Long lastSyncTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMainRelativeLayout = (RelativeLayout) findViewById(R.id.mainRelativeLayout);
//        mNotificationDetailsTextView = (TextView) findViewById(R.id.notificationDetails);
//        mSpinner = (Spinner) findViewById(R.id.spinner);

        mNotificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());

        // Create an ArrayAdapter using the string array and a default spinner layout.
        ArrayAdapter<CharSequence> adapter =
                new ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        NOTIFICATION_STYLES);
        // Specify the layout to use when the list of choices appears.
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner.
//        mSpinner.setAdapter(adapter);
//        mSpinner.setOnItemSelectedListener(this);

        firebaseAuth = FirebaseAuth.getInstance();

        signedInLayout = findViewById(R.id.signedIn);
        notSignedInLayout = findViewById(R.id.notSignedIn);

        signInButton = findViewById(R.id.signInWithTwitter);
        currentUserButton = findViewById(R.id.currentUser);
        reauthenticateButton = findViewById(R.id.reauthenticateWithTwitter);
//        openEgotterText = findViewById(R.id.openEgotter);

        oneSidedFriendsText = findViewById(R.id.oneSidedFriends);
        oneSidedFollowersText = findViewById(R.id.oneSidedFollowers);
        mutualFriendsText = findViewById(R.id.mutualFriends);
        unfriendsText = findViewById(R.id.unfriends);
        unfollowersText = findViewById(R.id.unfollowers);
        blockingOrBlockedText = findViewById(R.id.blockingOrBlocked);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra("json")) {
                    String str = intent.getStringExtra("json");
                    if (str != null) {
                        try {
                            JSONObject json = new JSONObject(str);
                            saveSummaryItemsList(json);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                updateSummaryItemsList();
            }
        };

        // workRequest = new PeriodicWorkRequest.Builder(CheckResultWork.class, 1, TimeUnit.MINUTES).addTag(CheckResultWork.TAG).build();
        // WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(CheckResultWork.TAG, ExistingPeriodicWorkPolicy.REPLACE, workRequest);

        AppRate.with(this)
                .setInstallDays(3)
                .setLaunchTimes(10)
                .setRemindInterval(2)
                .setShowLaterButton(true)
                .setDebug(false)
                .setOnClickButtonListener(new OnClickButtonListener() {
                    @Override
                    public void onClickButton(int which) {
                        Log.d(TAG, Integer.toString(which));
                    }
                })
                .monitor();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(MainActivity.this).
                registerReceiver((broadcastReceiver), new IntentFilter("com.egotter.MainActivity"));
    }

    @Override
    protected void onResume() {
        super.onResume();

        getInstanceId();
        updateSummaryItemsList();

        if (isUserSignedIn()) {
            afterSignIn(getCurrentUser());
        } else {
            afterSignOut();
        }

        AppRate.with(this).clearAgreeShowDialog();
        AppRate.showRateDialogIfMeetsConditions(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "onItemSelected(): position: " + position + " id: " + id);

        mSelectedNotification = position;

        mNotificationDetailsTextView.setText(
                NOTIFICATION_STYLES_DESCRIPTION[mSelectedNotification]);
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Required
    }

    public void openEgotter(View view) {
        if (isUserSignedIn()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://egotter.com/timelines/" + getCurrentUser().screenName + "?via=" + getCurrentVia()));
            startActivity(intent);
        }
    }

    public void receivePromptReport(View view) {
        if (isUserSignedIn()) {
            getInstanceId();
            sendInstanceIdToServer(true);
        }
    }

    public void openTermsOfService(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://egotter.com/terms_of_service?via=" + getCurrentVia()));
        startActivity(intent);
    }

    public void openPrivacyPolicy(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://egotter.com/privacy_policy?via=" + getCurrentVia()));
        startActivity(intent);
    }

    public void openAboutThisApp(View view) {
        Intent intent = new Intent(this, AboutThisAppActivity.class);
        startActivity(intent);
    }

    private String getCurrentVia() {
        return "android" + BuildConfig.VERSION_NAME;
    }

    public void onClick(View view) {

        Log.d(TAG, "onClick()");

        boolean areNotificationsEnabled = mNotificationManagerCompat.areNotificationsEnabled();

        if (!areNotificationsEnabled) {
            // Because the user took an action to create a notification, we create a prompt to let
            // the user re-enable notifications for this application again.
            Snackbar snackbar = Snackbar
                    .make(
                            mMainRelativeLayout,
                            "You need to enable notifications for this app",
                            Snackbar.LENGTH_LONG)
                    .setAction("ENABLE", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Links to this app's notification settings
                            openNotificationSettingsForApp();
                        }
                    });
            snackbar.show();
            return;
        }

        String notificationStyle = NOTIFICATION_STYLES[mSelectedNotification];

        switch (notificationStyle) {
            case BIG_TEXT_STYLE:
                generateBigTextStyleNotification();
                break;

            case BIG_PICTURE_STYLE:
                generateBigPictureStyleNotification();
                break;

            case INBOX_STYLE:
                generateInboxStyleNotification();
                break;

            case MESSAGING_STYLE:
                generateMessagingStyleNotification();
                break;

            default:
                // continue below
        }
    }

    public void getInstanceId() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        String token = task.getResult().getToken();
                        saveInstanceId(token);

                        String msg = getString(R.string.msg_token_fmt, token);
                        Log.d(TAG, msg);
                    }
                });
    }

    private void saveInstanceId(String token) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("fcm_instance_id", token);
        editor.apply();

        if (isUserSignedIn()) {
            sendInstanceIdToServer(false);
        }
    }

    private void saveCredentials(String uid, String screenName, String token, String secret) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("twitter_id", uid);
        editor.putString("twitter_screen_name", screenName);
        editor.putString("twitter_access_token", token);
        editor.putString("twitter_access_secret", secret);
        editor.apply();

        if (isUserSignedIn()) {
            sendInstanceIdToServer(false);
        }
    }

    private User loadCredentials() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return new User(prefs.getString("twitter_id", ""),
                prefs.getString("twitter_screen_name", ""),
                prefs.getString("twitter_access_token", ""),
                prefs.getString("twitter_access_secret", ""));
    }

    private void clearCredentials() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefs.edit().clear().apply();
    }

    public void sendInstanceIdToServer(boolean isUserRequested) {
        if (lastSyncTime != null && System.currentTimeMillis() - lastSyncTime < SEND_INSTANCE_ID_INTERVAL) {
            long elapsed = SEND_INSTANCE_ID_INTERVAL - (System.currentTimeMillis() - lastSyncTime);
            String message = getString(R.string.sendInstanceIdToServerIntervalTooShortFormat, String.valueOf(elapsed / 1000));
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String instanceId = prefs.getString("fcm_instance_id", "");
        if (instanceId.equals("")) {
            if (isUserRequested) Toast.makeText(this, R.string.fetchInstanceId, Toast.LENGTH_SHORT).show();
            return;
        }

        String twitterUid = prefs.getString("twitter_id", "");
        String accessToken = prefs.getString("twitter_access_token", "");
        String accessSecret = prefs.getString("twitter_access_secret", "");
        if (twitterUid.equals("") || accessToken.equals("") || accessSecret.equals("")) {
            if (isUserRequested) Toast.makeText(this, R.string.fetchCredentials, Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.sendInstanceIdToServer(twitterUid, instanceId, accessToken, accessSecret, MainActivity.this);

        lastSyncTime = System.currentTimeMillis();
//        lastSyncTimeText.setText(getString(R.string.lastSyncTimeFormat, DateFormat.format("hh:mm", new Date())));
    }

    private void saveSummaryItemsList(JSONObject summaryPayload) {
        Log.d(TAG, "saveSummaryItemsList() summaryPayload " + summaryPayload);

        if (!summaryPayload.has("one_sided_friends")) {
            return;
        }

        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("one_sided_friends", summaryPayload.getString("one_sided_friends"));
            editor.putString("one_sided_followers", summaryPayload.getString("one_sided_followers"));
            editor.putString("mutual_friends", summaryPayload.getString("mutual_friends"));
            editor.putString("unfriends", summaryPayload.getString("unfriends"));
            editor.putString("unfollowers", summaryPayload.getString("unfollowers"));
            editor.putString("blocking_or_blocked", summaryPayload.getString("blocking_or_blocked"));
            editor.apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateSummaryItemsList() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (!prefs.contains("one_sided_friends")) {
            return;
        }

        oneSidedFriendsText.setText(getString(R.string.oneSidedFriendsCount, prefs.getString("one_sided_friends", "")));
        oneSidedFollowersText.setText(getString(R.string.oneSidedFollowersCount, prefs.getString("one_sided_followers", "")));
        mutualFriendsText.setText(getString(R.string.mutualFriendsCount, prefs.getString("mutual_friends", "")));
        unfriendsText.setText(getString(R.string.unfriendsCount, prefs.getString("unfriends", "")));
        unfollowersText.setText(getString(R.string.unfollowersCount, prefs.getString("unfollowers", "")));
        blockingOrBlockedText.setText(getString(R.string.blockingOrBlockedCount, prefs.getString("blocking_or_blocked", "")));
    }

    public void onCallback(String result) {
        Log.d(TAG, "HttpTask result " + result);

        try {
            JSONObject json = new JSONObject(result);
            if (json.has("version_code")) {
                int versionCode = json.getInt("version_code");
                if (BuildConfig.VERSION_CODE < versionCode) {
                    new OpenPlayStoreDialogFragment().show(getSupportFragmentManager(), "OpenPlayStore");
                    return;
                }
            }

            if (json.has("one_sided_friends")) {
                saveSummaryItemsList(json);
                updateSummaryItemsList();
            }

            if (json.has("error") && !json.getString("error").equals("")) {
                Toast.makeText(this, R.string.sendInstanceIdFailed, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.sendInstanceIdSucceeded, Toast.LENGTH_LONG).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean isUserSignedIn() {
        return getCurrentUser() != null;
    }

    public User getCurrentUser() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            return null;
        } else {
            return loadCredentials();
        }
    }

    public void signInWithTwitter(boolean forceLogin) {
        Task<AuthResult> pendingResultTask = firebaseAuth.getPendingAuthResult();
        if (pendingResultTask != null) {
            // There's something already here! Finish the sign-in for your user.
            pendingResultTask
                    .addOnSuccessListener(
                            new OnSuccessListener<AuthResult>() {
                                @Override
                                public void onSuccess(AuthResult authResult) {
                                    User user = new User(authResult);
                                    afterSignIn(user);
                                    saveCredentials(user.uid, user.screenName, user.token, user.secret);
                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Handle failure.
                                    Log.e(TAG, "signInWithTwitter() pendingResultTask is failed");
                                    e.printStackTrace();
                                }
                            });
        } else {
            OAuthProvider.Builder provider = OAuthProvider.newBuilder("twitter.com");
            provider.addCustomParameter("lang", "ja");
            if (forceLogin) {
                provider.addCustomParameter("force_login", String.valueOf(forceLogin));
            }

            firebaseAuth
                    .startActivityForSignInWithProvider(MainActivity.this, provider.build())
                    .addOnSuccessListener(
                            new OnSuccessListener<AuthResult>() {
                                @Override
                                public void onSuccess(AuthResult authResult) {
                                    User user = new User(authResult);
                                    afterSignIn(user);
                                    saveCredentials(user.uid, user.screenName, user.token, user.secret);
                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Handle failure.
                                    Log.e(TAG, "signInWithTwitter() signIn is failed");
                                    e.printStackTrace();
                                }
                            });
        }
    }

    public void authenticate(View view){
        signInWithTwitter(false);
    }

    public void reauthenticate(View view){
        signInWithTwitter(true);
    }

    private void afterSignIn(User user) {
        signInButton.setVisibility(View.INVISIBLE);
        currentUserButton.setText(getString(R.string.screenNameFormat, user.screenName));
        currentUserButton.setVisibility(View.VISIBLE);
        notSignedInLayout.setVisibility(View.INVISIBLE);
        signedInLayout.setVisibility(View.VISIBLE);
    }

    public void signOut(View view) {
        new SignOutConfirmationDialogFragment().show(getSupportFragmentManager(), "SignOutConfirmation");
    }

    public void doSignOut() {
        firebaseAuth.signOut();
        afterSignOut();
    }

    public void afterSignOut() {
        currentUserButton.setVisibility(View.INVISIBLE);
        signInButton.setVisibility(View.VISIBLE);
        signedInLayout.setVisibility(View.INVISIBLE);
        notSignedInLayout.setVisibility(View.VISIBLE);
        clearCredentials();
    }

    /*
     * Generates a BIG_TEXT_STYLE Notification that supports both phone/tablet and wear. For devices
     * on API level 16 (4.1.x - Jelly Bean) and after, displays BIG_TEXT_STYLE. Otherwise, displays
     * a basic notification.
     */
    private void generateBigTextStyleNotification() {

        Log.d(TAG, "generateBigTextStyleNotification()");

        // Main steps for building a BIG_TEXT_STYLE notification:
        //      0. Get your data
        //      1. Create/Retrieve Notification Channel for O and beyond devices (26+)
        //      2. Build the BIG_TEXT_STYLE
        //      3. Set up main Intent for notification
        //      4. Create additional Actions for the Notification
        //      5. Build and issue the notification

        // 0. Get your data (everything unique per Notification).
        MockDatabase.BigTextStyleReminderAppData bigTextStyleReminderAppData =
                MockDatabase.getBigTextStyleData();

        // 1. Create/Retrieve Notification Channel for O and beyond devices (26+).
        String notificationChannelId =
                NotificationUtil.createNotificationChannel(this, bigTextStyleReminderAppData);


        // 2. Build the BIG_TEXT_STYLE.
        BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle()
                // Overrides ContentText in the big form of the template.
                .bigText(bigTextStyleReminderAppData.getBigText())
                // Overrides ContentTitle in the big form of the template.
                .setBigContentTitle(bigTextStyleReminderAppData.getBigContentTitle())
                // Summary line after the detail section in the big form of the template.
                // Note: To improve readability, don't overload the user with info. If Summary Text
                // doesn't add critical information, you should skip it.
                .setSummaryText(bigTextStyleReminderAppData.getSummaryText());


        // 3. Set up main Intent for notification.
        Intent notifyIntent = new Intent(this, BigTextMainActivity.class);

        // When creating your Intent, you need to take into account the back state, i.e., what
        // happens after your Activity launches and the user presses the back button.

        // There are two options:
        //      1. Regular activity - You're starting an Activity that's part of the application's
        //      normal workflow.

        //      2. Special activity - The user only sees this Activity if it's started from a
        //      notification. In a sense, the Activity extends the notification by providing
        //      information that would be hard to display in the notification itself.

        // For the BIG_TEXT_STYLE notification, we will consider the activity launched by the main
        // Intent as a special activity, so we will follow option 2.

        // For an example of option 1, check either the MESSAGING_STYLE or BIG_PICTURE_STYLE
        // examples.

        // For more information, check out our dev article:
        // https://developer.android.com/training/notify-user/navigation.html

        // Sets the Activity to start in a new, empty task
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent notifyPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        notifyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        // 4. Create additional Actions (Intents) for the Notification.

        // In our case, we create two additional actions: a Snooze action and a Dismiss action.
        // Snooze Action.
        Intent snoozeIntent = new Intent(this, BigTextIntentService.class);
        snoozeIntent.setAction(BigTextIntentService.ACTION_SNOOZE);

        PendingIntent snoozePendingIntent = PendingIntent.getService(this, 0, snoozeIntent, 0);
        NotificationCompat.Action snoozeAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_alarm_white_48dp,
                        "Snooze",
                        snoozePendingIntent)
                        .build();


        // Dismiss Action.
        Intent dismissIntent = new Intent(this, BigTextIntentService.class);
        dismissIntent.setAction(BigTextIntentService.ACTION_DISMISS);

        PendingIntent dismissPendingIntent = PendingIntent.getService(this, 0, dismissIntent, 0);
        NotificationCompat.Action dismissAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_cancel_white_48dp,
                        "Dismiss",
                        dismissPendingIntent)
                        .build();


        // 5. Build and issue the notification.

        // Because we want this to be a new notification (not updating a previous notification), we
        // create a new Builder. Later, we use the same global builder to get back the notification
        // we built here for the snooze action, that is, canceling the notification and relaunching
        // it several seconds later.

        // Notification Channel Id is ignored for Android pre O (26).
        NotificationCompat.Builder notificationCompatBuilder =
                new NotificationCompat.Builder(
                        getApplicationContext(), notificationChannelId);

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder);

        Notification notification = notificationCompatBuilder
                // BIG_TEXT_STYLE sets title and content for API 16 (4.1 and after).
                .setStyle(bigTextStyle)
                // Title for API <16 (4.0 and below) devices.
                .setContentTitle(bigTextStyleReminderAppData.getContentTitle())
                // Content for API <24 (7.0 and below) devices.
                .setContentText(bigTextStyleReminderAppData.getContentText())
                .setSmallIcon(R.drawable.ic_stat_name)
                .setLargeIcon(BitmapFactory.decodeResource(
                        getResources(),
                        R.drawable.ic_alarm_white_48dp))
                .setContentIntent(notifyPendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                // Set primary color (important for Wear 2.0 Notifications).
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))

                // SIDE NOTE: Auto-bundling is enabled for 4 or more notifications on API 24+ (N+)
                // devices and all Wear devices. If you have more than one notification and
                // you prefer a different summary notification, set a group key and create a
                // summary notification via
                // .setGroupSummary(true)
                // .setGroup(GROUP_KEY_YOUR_NAME_HERE)

                .setCategory(Notification.CATEGORY_REMINDER)

                // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
                // 'importance' which is set in the NotificationChannel. The integers representing
                // 'priority' are different from 'importance', so make sure you don't mix them.
                .setPriority(bigTextStyleReminderAppData.getPriority())

                // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
                // visibility is set in the NotificationChannel.
                .setVisibility(bigTextStyleReminderAppData.getChannelLockscreenVisibility())

                // Adds additional actions specified above.
                .addAction(snoozeAction)
                .addAction(dismissAction)

                .build();

        mNotificationManagerCompat.notify(NOTIFICATION_ID, notification);
    }

    /*
     * Generates a BIG_PICTURE_STYLE Notification that supports both phone/tablet and wear. For
     * devices on API level 16 (4.1.x - Jelly Bean) and after, displays BIG_PICTURE_STYLE.
     * Otherwise, displays a basic notification.
     *
     * This example Notification is a social post. It allows updating the notification with
     * comments/responses via RemoteInput and the BigPictureSocialIntentService on 24+ (N+) and
     * Wear devices.
     */
    private void generateBigPictureStyleNotification() {

        Log.d(TAG, "generateBigPictureStyleNotification()");

        // Main steps for building a BIG_PICTURE_STYLE notification:
        //      0. Get your data
        //      1. Create/Retrieve Notification Channel for O and beyond devices (26+)
        //      2. Build the BIG_PICTURE_STYLE
        //      3. Set up main Intent for notification
        //      4. Set up RemoteInput, so users can input (keyboard and voice) from notification
        //      5. Build and issue the notification

        // 0. Get your data (everything unique per Notification).
        MockDatabase.BigPictureStyleSocialAppData bigPictureStyleSocialAppData =
                MockDatabase.getBigPictureStyleData();

        // 1. Create/Retrieve Notification Channel for O and beyond devices (26+).
        String notificationChannelId =
                NotificationUtil.createNotificationChannel(this, bigPictureStyleSocialAppData);

        // 2. Build the BIG_PICTURE_STYLE.
        BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle()
                // Provides the bitmap for the BigPicture notification.
                .bigPicture(
                        BitmapFactory.decodeResource(
                                getResources(),
                                bigPictureStyleSocialAppData.getBigImage()))
                // Overrides ContentTitle in the big form of the template.
                .setBigContentTitle(bigPictureStyleSocialAppData.getBigContentTitle())
                // Summary line after the detail section in the big form of the template.
                .setSummaryText(bigPictureStyleSocialAppData.getSummaryText());

        // 3. Set up main Intent for notification.
        Intent mainIntent = new Intent(this, BigPictureSocialMainActivity.class);

        // When creating your Intent, you need to take into account the back state, i.e., what
        // happens after your Activity launches and the user presses the back button.

        // There are two options:
        //      1. Regular activity - You're starting an Activity that's part of the application's
        //      normal workflow.

        //      2. Special activity - The user only sees this Activity if it's started from a
        //      notification. In a sense, the Activity extends the notification by providing
        //      information that would be hard to display in the notification itself.

        // Even though this sample's MainActivity doesn't link to the Activity this Notification
        // launches directly, i.e., it isn't part of the normal workflow, a social app generally
        // always links to individual posts as part of the app flow, so we will follow option 1.

        // For an example of option 2, check out the BIG_TEXT_STYLE example.

        // For more information, check out our dev article:
        // https://developer.android.com/training/notify-user/navigation.html

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack.
        stackBuilder.addParentStack(BigPictureSocialMainActivity.class);
        // Adds the Intent to the top of the stack.
        stackBuilder.addNextIntent(mainIntent);
        // Gets a PendingIntent containing the entire back stack.
        PendingIntent mainPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        // 4. Set up RemoteInput, so users can input (keyboard and voice) from notification.

        // Note: For API <24 (M and below) we need to use an Activity, so the lock-screen presents
        // the auth challenge. For API 24+ (N and above), we use a Service (could be a
        // BroadcastReceiver), so the user can input from Notification or lock-screen (they have
        // choice to allow) without leaving the notification.

        // Create the RemoteInput.
        String replyLabel = getString(R.string.reply_label);
        RemoteInput remoteInput =
                new RemoteInput.Builder(BigPictureSocialIntentService.EXTRA_COMMENT)
                        .setLabel(replyLabel)
                        // List of quick response choices for any wearables paired with the phone
                        .setChoices(bigPictureStyleSocialAppData.getPossiblePostResponses())
                        .build();

        // Pending intent =
        //      API <24 (M and below): activity so the lock-screen presents the auth challenge
        //      API 24+ (N and above): this should be a Service or BroadcastReceiver
        PendingIntent replyActionPendingIntent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent intent = new Intent(this, BigPictureSocialIntentService.class);
            intent.setAction(BigPictureSocialIntentService.ACTION_COMMENT);
            replyActionPendingIntent = PendingIntent.getService(this, 0, intent, 0);

        } else {
            replyActionPendingIntent = mainPendingIntent;
        }

        NotificationCompat.Action replyAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_reply_white_18dp,
                        replyLabel,
                        replyActionPendingIntent)
                        .addRemoteInput(remoteInput)
                        .build();

        // 5. Build and issue the notification.

        // Because we want this to be a new notification (not updating a previous notification), we
        // create a new Builder. Later, we use the same global builder to get back the notification
        // we built here for a comment on the post.

        NotificationCompat.Builder notificationCompatBuilder =
                new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder);

        notificationCompatBuilder
                // BIG_PICTURE_STYLE sets title and content for API 16 (4.1 and after).
                .setStyle(bigPictureStyle)
                // Title for API <16 (4.0 and below) devices.
                .setContentTitle(bigPictureStyleSocialAppData.getContentTitle())
                // Content for API <24 (7.0 and below) devices.
                .setContentText(bigPictureStyleSocialAppData.getContentText())
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(
                        getResources(),
                        R.drawable.ic_person_black_48dp))
                .setContentIntent(mainPendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                // Set primary color (important for Wear 2.0 Notifications).
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))

                // SIDE NOTE: Auto-bundling is enabled for 4 or more notifications on API 24+ (N+)
                // devices and all Wear devices. If you have more than one notification and
                // you prefer a different summary notification, set a group key and create a
                // summary notification via
                // .setGroupSummary(true)
                // .setGroup(GROUP_KEY_YOUR_NAME_HERE)

                .setSubText(Integer.toString(1))
                .addAction(replyAction)
                .setCategory(Notification.CATEGORY_SOCIAL)

                // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
                // 'importance' which is set in the NotificationChannel. The integers representing
                // 'priority' are different from 'importance', so make sure you don't mix them.
                .setPriority(bigPictureStyleSocialAppData.getPriority())

                // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
                // visibility is set in the NotificationChannel.
                .setVisibility(bigPictureStyleSocialAppData.getChannelLockscreenVisibility());

        // If the phone is in "Do not disturb mode, the user will still be notified if
        // the sender(s) is starred as a favorite.
        for (String name : bigPictureStyleSocialAppData.getParticipants()) {
            notificationCompatBuilder.addPerson(name);
        }

        Notification notification = notificationCompatBuilder.build();

        mNotificationManagerCompat.notify(NOTIFICATION_ID, notification);
    }

    /*
     * Generates a INBOX_STYLE Notification that supports both phone/tablet and wear. For devices
     * on API level 16 (4.1.x - Jelly Bean) and after, displays INBOX_STYLE. Otherwise, displays a
     * basic notification.
     */
    private void generateInboxStyleNotification() {

        Log.d(TAG, "generateInboxStyleNotification()");


        // Main steps for building a INBOX_STYLE notification:
        //      0. Get your data
        //      1. Create/Retrieve Notification Channel for O and beyond devices (26+)
        //      2. Build the INBOX_STYLE
        //      3. Set up main Intent for notification
        //      4. Build and issue the notification

        // 0. Get your data (everything unique per Notification).
        MockDatabase.InboxStyleEmailAppData inboxStyleEmailAppData =
                MockDatabase.getInboxStyleData();

        // 1. Create/Retrieve Notification Channel for O and beyond devices (26+).
        String notificationChannelId =
                NotificationUtil.createNotificationChannel(this, inboxStyleEmailAppData);

        // 2. Build the INBOX_STYLE.
        InboxStyle inboxStyle = new NotificationCompat.InboxStyle()
                // This title is slightly different than regular title, since I know INBOX_STYLE is
                // available.
                .setBigContentTitle(inboxStyleEmailAppData.getBigContentTitle())
                .setSummaryText(inboxStyleEmailAppData.getSummaryText());

        // Add each summary line of the new emails, you can add up to 5.
        for (String summary : inboxStyleEmailAppData.getIndividualEmailSummary()) {
            inboxStyle.addLine(summary);
        }

        // 3. Set up main Intent for notification.
        Intent mainIntent = new Intent(this, InboxMainActivity.class);

        // When creating your Intent, you need to take into account the back state, i.e., what
        // happens after your Activity launches and the user presses the back button.

        // There are two options:
        //      1. Regular activity - You're starting an Activity that's part of the application's
        //      normal workflow.

        //      2. Special activity - The user only sees this Activity if it's started from a
        //      notification. In a sense, the Activity extends the notification by providing
        //      information that would be hard to display in the notification itself.

        // Even though this sample's MainActivity doesn't link to the Activity this Notification
        // launches directly, i.e., it isn't part of the normal workflow, a eamil app generally
        // always links to individual emails as part of the app flow, so we will follow option 1.

        // For an example of option 2, check out the BIG_TEXT_STYLE example.

        // For more information, check out our dev article:
        // https://developer.android.com/training/notify-user/navigation.html

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack.
        stackBuilder.addParentStack(InboxMainActivity.class);
        // Adds the Intent to the top of the stack.
        stackBuilder.addNextIntent(mainIntent);
        // Gets a PendingIntent containing the entire back stack.
        PendingIntent mainPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        // 4. Build and issue the notification.

        // Because we want this to be a new notification (not updating a previous notification), we
        // create a new Builder. However, we don't need to update this notification later, so we
        // will not need to set a global builder for access to the notification later.

        NotificationCompat.Builder notificationCompatBuilder =
                new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder);

        notificationCompatBuilder

                // INBOX_STYLE sets title and content for API 16+ (4.1 and after) when the
                // notification is expanded.
                .setStyle(inboxStyle)

                // Title for API <16 (4.0 and below) devices and API 16+ (4.1 and after) when the
                // notification is collapsed.
                .setContentTitle(inboxStyleEmailAppData.getContentTitle())

                // Content for API <24 (7.0 and below) devices and API 16+ (4.1 and after) when the
                // notification is collapsed.
                .setContentText(inboxStyleEmailAppData.getContentText())
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(
                        getResources(),
                        R.drawable.ic_person_black_48dp))
                .setContentIntent(mainPendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                // Set primary color (important for Wear 2.0 Notifications).
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))

                // SIDE NOTE: Auto-bundling is enabled for 4 or more notifications on API 24+ (N+)
                // devices and all Wear devices. If you have more than one notification and
                // you prefer a different summary notification, set a group key and create a
                // summary notification via
                // .setGroupSummary(true)
                // .setGroup(GROUP_KEY_YOUR_NAME_HERE)

                // Sets large number at the right-hand side of the notification for API <24 devices.
                .setSubText(Integer.toString(inboxStyleEmailAppData.getNumberOfNewEmails()))

                .setCategory(Notification.CATEGORY_EMAIL)

                // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
                // 'importance' which is set in the NotificationChannel. The integers representing
                // 'priority' are different from 'importance', so make sure you don't mix them.
                .setPriority(inboxStyleEmailAppData.getPriority())

                // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
                // visibility is set in the NotificationChannel.
                .setVisibility(inboxStyleEmailAppData.getChannelLockscreenVisibility());

        // If the phone is in "Do not disturb mode, the user will still be notified if
        // the sender(s) is starred as a favorite.
        for (String name : inboxStyleEmailAppData.getParticipants()) {
            notificationCompatBuilder.addPerson(name);
        }

        Notification notification = notificationCompatBuilder.build();

        mNotificationManagerCompat.notify(NOTIFICATION_ID, notification);
    }

    /*
     * Generates a MESSAGING_STYLE Notification that supports both phone/tablet and wear. For
     * devices on API level 24 (7.0 - Nougat) and after, displays MESSAGING_STYLE. Otherwise,
     * displays a basic BIG_TEXT_STYLE.
     */
    private void generateMessagingStyleNotification() {

        Log.d(TAG, "generateMessagingStyleNotification()");

        // Main steps for building a MESSAGING_STYLE notification:
        //      0. Get your data
        //      1. Create/Retrieve Notification Channel for O and beyond devices (26+)
        //      2. Build the MESSAGING_STYLE
        //      3. Set up main Intent for notification
        //      4. Set up RemoteInput (users can input directly from notification)
        //      5. Build and issue the notification

        // 0. Get your data (everything unique per Notification)
        MockDatabase.MessagingStyleCommsAppData messagingStyleCommsAppData =
                MockDatabase.getMessagingStyleData(getApplicationContext());

        // 1. Create/Retrieve Notification Channel for O and beyond devices (26+).
        String notificationChannelId =
                NotificationUtil.createNotificationChannel(this, messagingStyleCommsAppData);

        // 2. Build the NotificationCompat.Style (MESSAGING_STYLE).
        String contentTitle = messagingStyleCommsAppData.getContentTitle();

        MessagingStyle messagingStyle =
                new MessagingStyle(messagingStyleCommsAppData.getMe())
                        /*
                         * <p>This API's behavior was changed in SDK version
                         * {@link Build.VERSION_CODES#P}. If your application's target version is
                         * less than {@link Build.VERSION_CODES#P}, setting a conversation title to
                         * a non-null value will make {@link #isGroupConversation()} return
                         * {@code true} and passing {@code null} will make it return {@code false}.
                         * This behavior can be overridden by calling
                         * {@link #setGroupConversation(boolean)} regardless of SDK version.
                         * In {@code P} and above, this method does not affect group conversation
                         * settings.
                         *
                         * In our case, we use the same title.
                         */
                        .setConversationTitle(contentTitle);

        // Adds all Messages.
        // Note: Messages include the text, timestamp, and sender.
        for (MessagingStyle.Message message : messagingStyleCommsAppData.getMessages()) {
            messagingStyle.addMessage(message);
        }

        messagingStyle.setGroupConversation(messagingStyleCommsAppData.isGroupConversation());

        // 3. Set up main Intent for notification.
        Intent notifyIntent = new Intent(this, MessagingMainActivity.class);

        // When creating your Intent, you need to take into account the back state, i.e., what
        // happens after your Activity launches and the user presses the back button.

        // There are two options:
        //      1. Regular activity - You're starting an Activity that's part of the application's
        //      normal workflow.

        //      2. Special activity - The user only sees this Activity if it's started from a
        //      notification. In a sense, the Activity extends the notification by providing
        //      information that would be hard to display in the notification itself.

        // Even though this sample's MainActivity doesn't link to the Activity this Notification
        // launches directly, i.e., it isn't part of the normal workflow, a chat app generally
        // always links to individual conversations as part of the app flow, so we will follow
        // option 1.

        // For an example of option 2, check out the BIG_TEXT_STYLE example.

        // For more information, check out our dev article:
        // https://developer.android.com/training/notify-user/navigation.html

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack
        stackBuilder.addParentStack(MessagingMainActivity.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(notifyIntent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent mainPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        notifyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        // 4. Set up RemoteInput, so users can input (keyboard and voice) from notification.

        // Note: For API <24 (M and below) we need to use an Activity, so the lock-screen present
        // the auth challenge. For API 24+ (N and above), we use a Service (could be a
        // BroadcastReceiver), so the user can input from Notification or lock-screen (they have
        // choice to allow) without leaving the notification.

        // Create the RemoteInput specifying this key.
        String replyLabel = getString(R.string.reply_label);
        RemoteInput remoteInput = new RemoteInput.Builder(MessagingIntentService.EXTRA_REPLY)
                .setLabel(replyLabel)
                // Use machine learning to create responses based on previous messages.
                .setChoices(messagingStyleCommsAppData.getReplyChoicesBasedOnLastMessage())
                .build();

        // Pending intent =
        //      API <24 (M and below): activity so the lock-screen presents the auth challenge.
        //      API 24+ (N and above): this should be a Service or BroadcastReceiver.
        PendingIntent replyActionPendingIntent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent intent = new Intent(this, MessagingIntentService.class);
            intent.setAction(MessagingIntentService.ACTION_REPLY);
            replyActionPendingIntent = PendingIntent.getService(this, 0, intent, 0);

        } else {
            replyActionPendingIntent = mainPendingIntent;
        }

        NotificationCompat.Action replyAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_reply_white_18dp,
                        replyLabel,
                        replyActionPendingIntent)
                        .addRemoteInput(remoteInput)
                        // Informs system we aren't bringing up our own custom UI for a reply
                        // action.
                        .setShowsUserInterface(false)
                        // Allows system to generate replies by context of conversation.
                        .setAllowGeneratedReplies(true)
                        .setSemanticAction(Action.SEMANTIC_ACTION_REPLY)
                        .build();


        // 5. Build and issue the notification.

        // Because we want this to be a new notification (not updating current notification), we
        // create a new Builder. Later, we update this same notification, so we need to save this
        // Builder globally (as outlined earlier).

        NotificationCompat.Builder notificationCompatBuilder =
                new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder);

        notificationCompatBuilder
                // MESSAGING_STYLE sets title and content for API 16 and above devices.
                .setStyle(messagingStyle)
                // Title for API < 16 devices.
                .setContentTitle(contentTitle)
                // Content for API < 16 devices.
                .setContentText(messagingStyleCommsAppData.getContentText())
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(
                        getResources(),
                        R.drawable.ic_person_black_48dp))
                .setContentIntent(mainPendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                // Set primary color (important for Wear 2.0 Notifications).
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))

                // SIDE NOTE: Auto-bundling is enabled for 4 or more notifications on API 24+ (N+)
                // devices and all Wear devices. If you have more than one notification and
                // you prefer a different summary notification, set a group key and create a
                // summary notification via
                // .setGroupSummary(true)
                // .setGroup(GROUP_KEY_YOUR_NAME_HERE)

                // Number of new notifications for API <24 (M and below) devices.
                .setSubText(Integer.toString(messagingStyleCommsAppData.getNumberOfNewMessages()))

                .addAction(replyAction)
                .setCategory(Notification.CATEGORY_MESSAGE)

                // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
                // 'importance' which is set in the NotificationChannel. The integers representing
                // 'priority' are different from 'importance', so make sure you don't mix them.
                .setPriority(messagingStyleCommsAppData.getPriority())

                // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
                // visibility is set in the NotificationChannel.
                .setVisibility(messagingStyleCommsAppData.getChannelLockscreenVisibility());

        // If the phone is in "Do not disturb" mode, the user may still be notified if the
        // sender(s) are in a group allowed through "Do not disturb" by the user.
        for (Person name : messagingStyleCommsAppData.getParticipants()) {
            notificationCompatBuilder.addPerson(name.getUri());
        }

        Notification notification = notificationCompatBuilder.build();
        mNotificationManagerCompat.notify(NOTIFICATION_ID, notification);
    }

    /**
     * Helper method for the SnackBar action, i.e., if the user has this application's notifications
     * disabled, this opens up the dialog to turn them back on after the user requests a
     * Notification launch.
     *
     * IMPORTANT NOTE: You should not do this action unless the user takes an action to see your
     * Notifications like this sample demonstrates. Spamming users to re-enable your notifications
     * is a bad idea.
     */
    private void openNotificationSettingsForApp() {
        // Links to this app's notification settings.
        Intent intent = new Intent();
        intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
        intent.putExtra("app_package", getPackageName());
        intent.putExtra("app_uid", getApplicationInfo().uid);
        startActivity(intent);
    }
}

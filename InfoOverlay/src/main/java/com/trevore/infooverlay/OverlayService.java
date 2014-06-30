package com.trevore.infooverlay;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

public class OverlayService extends Service {

    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_LOCATION = "location";
    public static final String KEY_ACTIVITY = "activity";
    public static final String KEY_COLOR = "color";

    /**
     * TextView that is overlayed on the screen
     */
    private TextView displayText;

    /**
     * Whether the background thread is running
     */
    private boolean isRunning = false;

    /**
     * Used to overlay view onto screen
     */
    private WindowManager windowManager;

    /**
     * Used to get the Activity stack
     */
    private ActivityManager activityManager;

    /**
     * User's shared preferences
     */
    @Inject
    SharedPreferences sharedPreferences;

    /**
     * The background thread for processing the info
     */
    private Thread backgroundThread;

    /**
     * Handler to the main thread for posting info
     */
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if(isRunning) {
                Bundle data = message.getData();
                String text = data.getString("activity");
                displayText.setText(text);
            }
        }
    };

    public IBinder onBind(Intent intent) {
        return null; //not implemented
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MainApplication.from(this).getObjectGraph().inject(this);
        activityManager = (ActivityManager) this .getSystemService(ACTIVITY_SERVICE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Bundle bundle = intent != null ? intent.getExtras() : null;
        if(bundle != null) {
            processBundle(bundle);
        }

        return START_STICKY;
    }

    private void processBundle(Bundle bundle) {
        String enabled = bundle.getString(KEY_ENABLED);
        String location = bundle.getString(KEY_LOCATION);
        String color = bundle.getString(KEY_COLOR);

        if(enabled != null) {
            if(enabled.equalsIgnoreCase("true") && !isRunning()) {
                startMonitoring();
            }
            else if(enabled.equalsIgnoreCase("false") && isRunning()) {
                stopMonitoring();
            }
        }

        if(location != null && isRunning()) {
            updateLocation(location);
        }

        if(color != null && isRunning()) {
            updateColor(color);
        }
    }

    private void updateColor(String color) {
        if(displayText != null) {
            int textColor;
            if(!color.startsWith("#")) {
                color = "#" + color;
            }
            try {
                textColor = Color.parseColor(color);
            }
            catch (IllegalArgumentException e) {
                textColor = 0;
            }
            displayText.setTextColor(textColor);
            Log.d("trevor", color + " color set to " + textColor);
        }
    }

    /**
     * Whether the background thread is updating the View.
     * @return
     */
    private boolean isRunning() {
        return isRunning && displayText != null;
    }

    /**
     * Get the LayoutParams for our WindowManager
     * @param gravity
     * @return
     */
    private WindowManager.LayoutParams getLayoutParams(int gravity) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = gravity;
        return params;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        backgroundThread = null;
        windowManager.removeViewImmediate(displayText);
        displayText = null;
    }

    /**
     * Updates the location of the display on the screen
     * @param preferenceValue
     */
    private void updateLocation(String preferenceValue) {
        int gravity = getGravity(preferenceValue);
        WindowManager.LayoutParams params = getLayoutParams(gravity);
        windowManager.updateViewLayout(displayText, params);
    }

    /**
     * Get the gravity for the text display
     * @return
     */
    private int getGravity() {
        String preferenceValue = sharedPreferences.getString(
                getString(R.string.pref_location),
                getString(R.string.location_top_left));
        return getGravity(preferenceValue);
    }

    /**
     * Gets the appropriate gravity value based on the preference selected.
     * @param preferenceValue
     * @return
     */
    private int getGravity(String preferenceValue) {
        int retValue;

        switch(Integer.parseInt(preferenceValue)) {
            case 0:
                retValue = Gravity.TOP | Gravity.LEFT;
                break;
            case 1:
                retValue = Gravity.TOP | Gravity.RIGHT;
                break;
            case 2:
                retValue = Gravity.BOTTOM | Gravity.LEFT;
                break;
            case 3:
                retValue = Gravity.BOTTOM | Gravity.RIGHT;
                break;
            default:
                retValue = Gravity.TOP | Gravity.LEFT;
        }

        return retValue;
    }

    /**
     * Starts the background thread and adds the text display to the screen.
     */
    private void startMonitoring() {
        //Create overlay
        displayText = new TextView(this);
        int gravity = getGravity();
        WindowManager.LayoutParams params = getLayoutParams(gravity);
        windowManager.addView(displayText, params);
        updateColor(sharedPreferences.getString(getString(R.string.pref_color), "#fff"));
        isRunning = true;

        //Create background thread to get info
        Runnable backgroundTask = new Runnable() {
            @Override
            public void run() {
                while(isRunning) {
                    List<ActivityManager.RunningTaskInfo> taskInfo = activityManager.getRunningTasks(1);
                    if(!taskInfo.isEmpty()) {
                        Message message = new Message();
                        Bundle data = new Bundle();
                        data.putString(KEY_ACTIVITY, taskInfo.get(0).topActivity.getClassName());
                        message.setData(data);
                        handler.sendMessage(message);
                    }

                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        isRunning = false;
                    }
                }
            }
        };
        backgroundThread = new Thread(backgroundTask);

        backgroundThread.start();
    }

    /**
     * Stops the background thread and removes the text display from the screen
     */
    private void stopMonitoring() {
        isRunning = false;
        backgroundThread = null;
        windowManager.removeViewImmediate(displayText);
    }
}

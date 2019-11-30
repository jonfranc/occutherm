package bosch.smartcampus.thermalcomfortstudy.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import bosch.smartcampus.thermalcomfortstudy.R;
import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;

/**
 * {@link DataUploadAlarmReceiver} is responsible for:
 *  (1) uploading data from the local storage to Sensor Andrew via {@link SAConnectionService}, and
 *  (2) scheduling the next DataUpload alarm -- depending on the last time that the data were successfully uploaded.
 */
public class DataUploadAlarmReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = DataUploadAlarmReceiver.class.getSimpleName();
    private static final String BASE_NAME = DataUploadAlarmReceiver.class.getSimpleName();

    public static final String DATA_UPLOAD_ALARM_ACTION = BASE_NAME + ".intent.action.DATA_UPLOAD_ALARM";

    // Data upload interval (milliseconds)
    public static final long DATA_UPLOAD_INTERVAL = AlarmManager.INTERVAL_HALF_HOUR;
    public static final long DATA_UPLOAD_DELAY = AlarmManager.INTERVAL_FIFTEEN_MINUTES;

    public DataUploadAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();

            if (action.equals(DATA_UPLOAD_ALARM_ACTION)) {
                handleDataUploadAlarmAction(context);
            } else if (action.equals(SAConnectionService.SA_UPLOAD_DATA_OK_ACTION)) {
                String timestamp = intent.getStringExtra(context.getString(R.string.last_data_upload_time_key));
                Timestamp lastUploadedTimestamp = new Timestamp(timestamp);
                handleUploadDataOKAction(context, lastUploadedTimestamp);
            } else if (action.equals(SAConnectionService.SA_UPLOAD_DATA_BAD_ACTION)) {
                String timestamp = intent.getStringExtra(context.getString(R.string.last_data_upload_time_key));
                Timestamp lastUploadedTimestamp = new Timestamp(timestamp);
                handleUploadDataBadAction(context, lastUploadedTimestamp);
            }
        }
    }

    /**
     * Start {@link SAConnectionService} to upload data, from last data upload time
     * to current time, from local storage to Sensor Andrew.
     */
    private void handleDataUploadAlarmAction(Context context) {
        Timestamp now = new Timestamp();

        SharedPreferences loginSharedPref = context.getSharedPreferences(
                context.getString(R.string.login_storage), Context.MODE_PRIVATE);
        String fromTimestamp = loginSharedPref.getString(
                context.getString(R.string.last_data_upload_time_key), null);
        String toTimestamp = now.toString();

        Intent saConnIntent = new Intent(context, SAConnectionService.class);
        saConnIntent.setAction(SAConnectionService.SA_UPLOAD_DATA_ACTION);
        saConnIntent.putExtra(SAConnectionService.UPLOAD_DATA_FROM_TIMESTAMP_PARAM, fromTimestamp);
        saConnIntent.putExtra(SAConnectionService.UPLOAD_DATA_TO_TIMESTAMP_PARAM, toTimestamp);
        context.startService(saConnIntent);
        Log.i(LOG_TAG, "Started SAConnection service to upload data from "
                + fromTimestamp + " to " + toTimestamp + " from local storage to Sensor Andrew");
    }

    /**
     * Record timestamp of last data item successfully uploaded,
     * and schedule the next DataUpload alarm if user is logged in
     */
    private void handleUploadDataOKAction(Context context, Timestamp lastUploadedTimestamp) {
        Log.i(LOG_TAG, "Uploaded data to Sensor Andrew");

        SharedPreferences loginSharedPref = context.getSharedPreferences(
                context.getString(R.string.login_storage), Context.MODE_PRIVATE);

        if (loginSharedPref.getBoolean(context.getString(R.string.is_logged_in_key), false)) {
            SharedPreferences.Editor editor = loginSharedPref.edit();
            editor.putString(context.getString(R.string.last_data_upload_time_key), lastUploadedTimestamp.toString());
            editor.commit();

            scheduleNextDataUploadAlarm(context, DATA_UPLOAD_INTERVAL);
        }
    }

    /**
     * Record timestamp of last data item successfully uploaded,
     * and try uploading data again soon if user is logged in
     */
    private void handleUploadDataBadAction(Context context, Timestamp lastUploadedTimestamp) {
        Log.e(LOG_TAG, "Failed to upload data to Sensor Andrew");

        SharedPreferences loginSharedPref = context.getSharedPreferences(
                context.getString(R.string.login_storage), Context.MODE_PRIVATE);

        if (loginSharedPref.getBoolean(context.getString(R.string.is_logged_in_key), false)) {
            SharedPreferences.Editor editor = loginSharedPref.edit();
            editor.putString(context.getString(R.string.last_data_upload_time_key), lastUploadedTimestamp.toString());
            editor.commit();

            scheduleNextDataUploadAlarm(context, DATA_UPLOAD_DELAY);
        }
    }

    /**
     * Schedule the next DataUpload alarm
     */
    private void scheduleNextDataUploadAlarm(Context context, long delay) {
        Intent alarmIntent = new Intent(context, DataUploadAlarmReceiver.class);
        alarmIntent.setAction(DataUploadAlarmReceiver.DATA_UPLOAD_ALARM_ACTION);
        PendingIntent pAlarmIntent = PendingIntent.getBroadcast(context, 0,
                alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delay,
                pAlarmIntent);
        Log.i(LOG_TAG, "Scheduled the next DataUpload alarm in " + delay + " milliseconds");
    }
}

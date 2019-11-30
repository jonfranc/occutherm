package bosch.smartcampus.thermalcomfortstudy.ui;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import bosch.smartcampus.thermalcomfortstudy.R;
import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;
import bosch.smartcampus.thermalcomfortstudy.service.BandDataSamplingAlarmReceiver;
import bosch.smartcampus.thermalcomfortstudy.service.BandDataSamplingService;
import bosch.smartcampus.thermalcomfortstudy.service.DataUploadAlarmReceiver;
import bosch.smartcampus.thermalcomfortstudy.service.SAConnectionService;
import bosch.smartcampus.thermalcomfortstudy.service.SurveyNotificationAlarmReceiver;
import bosch.smartcampus.thermalcomfortstudy.service.SurveyNotificationClickReceiver;

/**
 * {@link MainActivity} is the main component of the Experience Sampling.
 * It is responsible for:
 *  (1) starting BandDataSampling via {@link BandDataSamplingService} when the user starts the survey or an action report,
 *  (2) stopping BandDataSampling via {@link BandDataSamplingService} when the user logs out,
 *  (3) canceling all alarms,
 *  (4) uploading the remaining data to Sensor Andrew via {@link SAConnectionService}, and
 *  (5) clearing all user data and disconnecting from Sensor Andrew
 */
public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    /**
     * Receive data upload intent from uploadRemainingDataToSA()
     */
    private BroadcastReceiver mDataUploadAlarmReceiver = new DataUploadAlarmReceiver();

    /**
     * After uploadRemainingDataToSA(), wait to receive result from {@link SAConnectionService}
     */
    private BroadcastReceiver mUploadDataResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            /**
             * After user logged out, if data upload succeeded, clear all user data and disconnect from Sensor Andrew,
             * and go to Login page.
             *
             * Otherwise, prompt user to check Internet connection and try again.
             */
            if (action.equals(SAConnectionService.SA_UPLOAD_DATA_OK_ACTION)) {
                if (isLoggedIn()) {
                    Log.i(LOG_TAG, "Inform user that data were uploaded to Sensor Andrew");
                    Toast.makeText(context, getString(R.string.sa_upload_data_succeeded_prompt), Toast.LENGTH_LONG).show();
                } else {
                    Log.i(LOG_TAG, "Uploaded the remaining data to Sensor Andrew after user logged out");

                    Toast.makeText(context, getString(R.string.sa_upload_data_completed_prompt), Toast.LENGTH_LONG).show();

                    clearUserData();
                    disconnectFromSA();
                    gotoLogin();
                }
            } else if (action.equals(SAConnectionService.SA_UPLOAD_DATA_BAD_ACTION)) {
                if (isLoggedIn()) {
                    Log.i(LOG_TAG, "Inform user that data were not uploaded to Sensor Andrew");
                    Toast.makeText(context, getString(R.string.sa_upload_data_failed_prompt), Toast.LENGTH_LONG).show();
                    Toast.makeText(context, getString(R.string.check_connection_prompt), Toast.LENGTH_LONG).show();
                } else {
                    Log.e(LOG_TAG, "Failed to upload the remaining data to Sensor Andrew after user logged out");

                    Button logoutButton = (Button) findViewById(R.id.logout);
                    logoutButton.setText(R.string.action_logout);
                    logoutButton.setEnabled(true);
                    Toast.makeText(context, getString(R.string.sa_upload_data_failed_prompt), Toast.LENGTH_LONG).show();
                    Toast.makeText(context, getString(R.string.check_connection_retry_prompt), Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter uploadResultIntentFilter = new IntentFilter();
        uploadResultIntentFilter.addAction(SAConnectionService.SA_UPLOAD_DATA_OK_ACTION);
        uploadResultIntentFilter.addAction(SAConnectionService.SA_UPLOAD_DATA_BAD_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mUploadDataResultReceiver, uploadResultIntentFilter);

        IntentFilter uploadAlarmIntentFilter = new IntentFilter();
        uploadAlarmIntentFilter.addAction(DataUploadAlarmReceiver.DATA_UPLOAD_ALARM_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mDataUploadAlarmReceiver, uploadAlarmIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mUploadDataResultReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mDataUploadAlarmReceiver);
    }

    /**
     * Start the survey, and start band data sampling (if not already in progress)
     */
    public void startSurvey(View view) {
        Intent surveyIntent = new Intent(this, SurveyActivity.class);
        startActivity(surveyIntent);
        Log.i(LOG_TAG, "User starts the survey...");

        if (shouldStartBandDataSampling()) {
            startOneOffBandDataSamplingService();
        }
    }

    /**
     * Start action report, and start band data sampling (if not already in progress)
     */
    public void startActionReport(View view) {
        Intent actionReportIntent = new Intent(this, ActionReportActivity.class);
        startActivity(actionReportIntent);
        Log.i(LOG_TAG, "User starts an action report...");

        if (shouldStartBandDataSampling()) {
            startOneOffBandDataSamplingService();
        }
    }

    /**
     * Log out of the study.
     *
     * Stop band data sampling, cancel all alarms and upload the remaining data
     * in local storage to Sensor Andrew.
     */
    public void logOut(View view) {
        // Disable logout button to prevent calling logOut() multiple times
        Button logoutButton = (Button) view;
        logoutButton.setText(R.string.data_upload_on_logout);
        logoutButton.setEnabled(false);

        SharedPreferences loginSharedPref = getSharedPreferences(getString(R.string.login_storage),
                Context.MODE_PRIVATE);
        String username = loginSharedPref.getString(getString(R.string.username_key), null);
        assert username != null;
        SharedPreferences.Editor editor = loginSharedPref.edit();
        editor.putBoolean(getString(R.string.is_logged_in_key), false);
        editor.commit();
        Log.i(LOG_TAG, username + " logged out of the study");

        stopBandDataSampling();
        cancelBandDataSamplingAlarm();
        cancelSurveyNotificationAlarm();
        cancelDataUploadAlarm();
        uploadRemainingDataToSA();
    }

    /**
     * When user starts survey or action report, we should start sampling band data if
     * it has been more than LAST_BAND_STREAMING_THRESHOLD_ELAPSED_TIME since last band data sampling
     */
    private boolean shouldStartBandDataSampling() {
        Timestamp now = new Timestamp();
        SharedPreferences loginSharedPref = getSharedPreferences(getString(R.string.login_storage),
                Context.MODE_PRIVATE);
        String lastBandStreamingTimestamp = loginSharedPref.getString(
                getString(R.string.last_band_streaming_time_key), null);

        if (lastBandStreamingTimestamp == null) {
            return true;
        }

        Timestamp lastBandStreamingTime = new Timestamp(lastBandStreamingTimestamp);
        long elapsedTime = now.getDateTime().getTime() - lastBandStreamingTime.getDateTime().getTime();
        return elapsedTime > SurveyNotificationClickReceiver.LAST_BAND_STREAMING_THRESHOLD_ELAPSED_TIME;
    }

    /**
     * Start a one-off BandDataSampling service
     */
    private void startOneOffBandDataSamplingService() {
        Intent bandDataSamplingIntent = new Intent(this, BandDataSamplingService.class);
        bandDataSamplingIntent.putExtra(BandDataSamplingService.SAMPLING_DURATION_PARAM,
                SurveyNotificationClickReceiver.SURVEY_BAND_DATA_SAMPLING_DURATION);
        bandDataSamplingIntent.putExtra(BandDataSamplingService.IS_PERIODIC_SAMPLING_PARAM,
                false);
        startService(bandDataSamplingIntent);
        Log.i(LOG_TAG, "Started a one-off BandDataSampling service");
    }

    /**
     * Cancel BandDataSampling alarm
     */
    private void cancelBandDataSamplingAlarm() {
        Intent alarmIntent = new Intent(this, BandDataSamplingAlarmReceiver.class);
        alarmIntent.setAction(BandDataSamplingAlarmReceiver.BAND_DATA_SAMPLING_ALARM_ACTION);
        final PendingIntent pAlarmIntent = PendingIntent.getBroadcast(this, 0,
                alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pAlarmIntent);
        Log.i(LOG_TAG, "Canceled BandDataSampling alarm");
    }

    /**
     * Cancel SurveyNotification alarm
     */
    private void cancelSurveyNotificationAlarm() {
        Intent alarmIntent = new Intent(this, SurveyNotificationAlarmReceiver.class);
        alarmIntent.setAction(SurveyNotificationAlarmReceiver.SURVEY_NOTIFICATION_ALARM_ACTION);
        final PendingIntent pAlarmIntent = PendingIntent.getBroadcast(this, 0,
                alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pAlarmIntent);
        Log.i(LOG_TAG, "Canceled SurveyNotification alarm");

        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.cancel(SurveyNotificationAlarmReceiver.SURVEY_NOTIFICATION_ID);
        Log.i(LOG_TAG, "Canceled previously shown SurveyNotification, if any");
    }

    /**
     * Cancel DataUpload alarm
     */
    private void cancelDataUploadAlarm() {
        Intent alarmIntent = new Intent(this, DataUploadAlarmReceiver.class);
        alarmIntent.setAction(DataUploadAlarmReceiver.DATA_UPLOAD_ALARM_ACTION);
        final PendingIntent pAlarmIntent = PendingIntent.getBroadcast(this, 0,
                alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pAlarmIntent);
        Log.i(LOG_TAG, "Canceled DataUpload alarm");
    }

    /**
     * Upload the remaining data in local storage to Sensor Andrew
     */
    private void uploadRemainingDataToSA() {
        Intent dataUploadIntent = new Intent(DataUploadAlarmReceiver.DATA_UPLOAD_ALARM_ACTION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(dataUploadIntent);
    }

    /**
     * Stop band data sampling
     */
    private void stopBandDataSampling() {
        Intent stopSamplingIntent = new Intent(BandDataSamplingService.STOP_BAND_DATA_SAMPLING_ACTION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(stopSamplingIntent);
    }

    /**
     * Clear username, password, and user's survey response from shared preferences
     */
    private void clearUserData() {
        // Clear username and password
        SharedPreferences loginSharedPref = getSharedPreferences(getString(R.string.login_storage),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor loginEditor = loginSharedPref.edit();
        loginEditor.clear();
        loginEditor.commit();

        // Clear survey response
        SharedPreferences responseSharedPref = getSharedPreferences(getString(R.string.survey_response_storage),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor responseEditor = responseSharedPref.edit();
        responseEditor.clear();
        responseEditor.commit();
        Log.i(LOG_TAG, "Cleared all user data");
    }

    /**
     * Disconnect from Sensor Andrew (if the global Sensor Andrew Connection object still exists)
     */
    private void disconnectFromSA() {
        Intent saDisconnectIntent = new Intent(this, SAConnectionService.class);
        saDisconnectIntent.setAction(SAConnectionService.SA_DISCONNECT_ACTION);
        startService(saDisconnectIntent);
    }

    /**
     * Go to Login page
     */
    private void gotoLogin() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        startActivity(loginIntent);
    }

    /**
     * @return Whether user has logged in to the study
     */
    private boolean isLoggedIn() {
        SharedPreferences loginSharedPref = getSharedPreferences(getString(R.string.login_storage),
                Context.MODE_PRIVATE);
        return loginSharedPref.getBoolean(getString(R.string.is_logged_in_key), false);
    }
}

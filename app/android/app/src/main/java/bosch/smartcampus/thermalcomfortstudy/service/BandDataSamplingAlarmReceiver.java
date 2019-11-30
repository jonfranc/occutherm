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
 * {@link BandDataSamplingAlarmReceiver} is responsible for:
 *  (1) starting sampling data from the band via {@link BandDataSamplingService}, and
 *  (2) scheduling survey notification and the next band data sampling alarm -- depending on the last
 *  survey notification time and the last survey response time.
 */
public class BandDataSamplingAlarmReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = BandDataSamplingAlarmReceiver.class.getSimpleName();
    private static final String BASE_NAME = BandDataSamplingAlarmReceiver.class.getSimpleName();

    public static final String BAND_DATA_SAMPLING_ALARM_ACTION = BASE_NAME + ".intent.action.BAND_DATA_SAMPLING_ALARM";

    // Band data sampling interval (milliseconds)
    public static final long BAND_DATA_SAMPLING_INTERVAL = 1200000L;
    public static final long BAND_DATA_SAMPLING_DELAY = 300000L;
    public static final long BAND_DATA_SAMPLING_WINDOW = 180000L;

    // Band data sampling duration (milliseconds) -- if started by alarm
    public static final long ALARM_BAND_DATA_SAMPLING_DURATION = 600000L;

    // Survey notification delay (milliseconds)
    public static final long SURVEY_NOTIFICATION_DELAY = 600000L;
    public static final long SURVEY_NOTIFICATION_WINDOW = 180000L;

    // Threshold elapsed time since last survey notification and survey response (milliseconds)
    public static final long LAST_SURVEY_NOTIFICATION_THRESHOLD_ELAPSED_TIME = 3000000L;
    public static final long LAST_SURVEY_RESPONSE_THRESHOLD_ELAPSED_TIME = 1800000L;

    public BandDataSamplingAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();

            if (action.equals(BAND_DATA_SAMPLING_ALARM_ACTION)) {
                startPeriodicBandDataSamplingService(context);
            } else if (action.equals(BandDataSamplingService.BAND_DATA_SAMPLING_OK_ACTION)) {
                boolean isPeriodicSampling = intent.getBooleanExtra(BandDataSamplingService.IS_PERIODIC_SAMPLING_PARAM,
                        false);

                if (isPeriodicSampling) {
                    if (shouldScheduleSurveyNotificationAlarm(context)) {
                        scheduleSurveyNotificationAlarm(context);
                    }

                    scheduleNextBandDataSamplingAlarm(context, BAND_DATA_SAMPLING_INTERVAL);
                }
            } else if (action.equals(BandDataSamplingService.BAND_DATA_SAMPLING_BAD_ACTION)) {
                boolean isPeriodicSampling = intent.getBooleanExtra(BandDataSamplingService.IS_PERIODIC_SAMPLING_PARAM,
                        false);

                if (isPeriodicSampling) {
                    scheduleNextBandDataSamplingAlarm(context, BAND_DATA_SAMPLING_DELAY);
                }
            }
        }
    }

    /**
     * Start a periodic BandDataSampling service
     */
    private void startPeriodicBandDataSamplingService(Context context) {
        Intent bandDataSamplingIntent = new Intent(context, BandDataSamplingService.class);
        bandDataSamplingIntent.putExtra(BandDataSamplingService.SAMPLING_DURATION_PARAM,
                ALARM_BAND_DATA_SAMPLING_DURATION);
        bandDataSamplingIntent.putExtra(BandDataSamplingService.IS_PERIODIC_SAMPLING_PARAM,
                true);
        context.startService(bandDataSamplingIntent);
        Log.i(LOG_TAG, "Started a periodic BandDataSampling service");
    }

    /**
     * Schedule the next BandDataSampling alarm
     */
    private void scheduleNextBandDataSamplingAlarm(Context context, long delay) {
        Intent alarmIntent = new Intent(context, BandDataSamplingAlarmReceiver.class);
        alarmIntent.setAction(BandDataSamplingAlarmReceiver.BAND_DATA_SAMPLING_ALARM_ACTION);
        PendingIntent pAlarmIntent = PendingIntent.getBroadcast(context, 0,
                alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.setWindow(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delay,
                BAND_DATA_SAMPLING_WINDOW,
                pAlarmIntent);
        Log.i(LOG_TAG, "Scheduled the next BandDataSampling alarm in " + delay + " milliseconds");
    }

    /**
     * We should schedule a survey notification alarm if it will be more than
     * LAST_SURVEY_NOTIFICATION_THRESHOLD_ELAPSED_TIME since last survey notification and
     * more than LAST_SURVEY_RESPONSE_THRESHOLD_ELAPSED_TIME since last survey response
     * at the time the alarm fires
     */
    private boolean shouldScheduleSurveyNotificationAlarm(Context context) {
        Timestamp now = new Timestamp();
        SharedPreferences loginSharedPref = context.getSharedPreferences(
                context.getString(R.string.login_storage), Context.MODE_PRIVATE);
        String lastSurveyNotificationTimestamp = loginSharedPref.getString(
                context.getString(R.string.last_survey_notification_time_key), null);
        String lastSurveyResponseTimestamp = loginSharedPref.getString(
                context.getString(R.string.last_survey_response_time_key), null);

        long responseElapsedTime = Long.MAX_VALUE;
        long notificationElapsedTime = Long.MAX_VALUE;
        long responseThreshold = LAST_SURVEY_RESPONSE_THRESHOLD_ELAPSED_TIME - SURVEY_NOTIFICATION_DELAY;
        long notificationThreshold = LAST_SURVEY_NOTIFICATION_THRESHOLD_ELAPSED_TIME - SURVEY_NOTIFICATION_DELAY;

        if (lastSurveyResponseTimestamp != null) {
            Timestamp lastSurveyResponseTime = new Timestamp(lastSurveyResponseTimestamp);
            responseElapsedTime = now.getDateTime().getTime() - lastSurveyResponseTime.getDateTime().getTime();
        }

        if (lastSurveyNotificationTimestamp != null) {
            Timestamp lastSurveyNotificationTime = new Timestamp(lastSurveyNotificationTimestamp);
            notificationElapsedTime = now.getDateTime().getTime() - lastSurveyNotificationTime.getDateTime().getTime();
        }

        return responseElapsedTime > responseThreshold
                && notificationElapsedTime > notificationThreshold;
    }

    /**
     * Schedule SurveyNotification alarm
     */
    private void scheduleSurveyNotificationAlarm(Context context) {
        Intent alarmIntent = new Intent(context, SurveyNotificationAlarmReceiver.class);
        alarmIntent.setAction(SurveyNotificationAlarmReceiver.SURVEY_NOTIFICATION_ALARM_ACTION);
        PendingIntent pAlarmIntent = PendingIntent.getBroadcast(context, 0,
                alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.setWindow(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + SURVEY_NOTIFICATION_DELAY,
                SURVEY_NOTIFICATION_WINDOW,
                pAlarmIntent);
        Log.i(LOG_TAG, "Scheduled SurveyNotification alarm in "
                + SURVEY_NOTIFICATION_DELAY + " milliseconds");
    }
}

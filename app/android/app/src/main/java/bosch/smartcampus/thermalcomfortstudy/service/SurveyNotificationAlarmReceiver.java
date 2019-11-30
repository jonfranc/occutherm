package bosch.smartcampus.thermalcomfortstudy.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import bosch.smartcampus.thermalcomfortstudy.R;
import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;

/**
 * {@link SurveyNotificationAlarmReceiver} is responsible for:
 *  (1) sending a survey notification to the user, and
 *  (2) recording the last time that it sends a survey notification to the user.
 */
public class SurveyNotificationAlarmReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = SurveyNotificationAlarmReceiver.class.getSimpleName();
    private static final String BASE_NAME = SurveyNotificationAlarmReceiver.class.getSimpleName();

    // Survey notification id
    public static final int SURVEY_NOTIFICATION_ID = 2555;
    public static final String SURVEY_NOTIFICATION_ALARM_ACTION = BASE_NAME + ".intent.action.SURVEY_NOTIFICATION_ALARM";

    public SurveyNotificationAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (shouldSendSurveyNotification(context)) {
            sendSurveyNotification(context);
            recordLastSurveyNotificationTime(context);
        } else {
            Log.i(LOG_TAG, "User has recently submitted a survey response -- defer the survey notification");
        }
    }

    /**
     * Send a survey notification to user
     */
    private void sendSurveyNotification(Context context) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Thermal Comfort Study Notification")
                .setContentText("Thermal Comfort Study Survey");
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mBuilder.setSound(alarmSound);
        mBuilder.setAutoCancel(true);

        Intent surveyClickIntent = new Intent(SurveyNotificationClickReceiver.SURVEY_NOTIFICATION_CLICK_ACTION);
        PendingIntent pSurveyClickIntent = PendingIntent.getBroadcast(context, 0, surveyClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pSurveyClickIntent);

        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.notify(SURVEY_NOTIFICATION_ID, mBuilder.build());
        Log.i(LOG_TAG, "Notified user to take survey");
    }

    /**
     * We should send a survey notification if it has been more than
     * LAST_SURVEY_RESPONSE_THRESHOLD_ELAPSED_TIME since last survey response
     * (note: we check this again after scheduling SurveyNotification alarm because
     * user may submit a survey response after the alarm was scheduled)
     */
    private boolean shouldSendSurveyNotification(Context context) {
        Timestamp now = new Timestamp();
        SharedPreferences loginSharedPref = context.getSharedPreferences(
                context.getString(R.string.login_storage), Context.MODE_PRIVATE);
        String lastSurveyResponseTimestamp = loginSharedPref.getString(
                context.getString(R.string.last_survey_response_time_key), null);

        long responseElapsedTime = Long.MAX_VALUE;
        long responseThreshold = BandDataSamplingAlarmReceiver.LAST_SURVEY_RESPONSE_THRESHOLD_ELAPSED_TIME;

        if (lastSurveyResponseTimestamp != null) {
            Timestamp lastSurveyResponseTime = new Timestamp(lastSurveyResponseTimestamp);
            responseElapsedTime = now.getDateTime().getTime() - lastSurveyResponseTime.getDateTime().getTime();
        }

        return responseElapsedTime > responseThreshold;
    }

    /**
     * Record the most recent time we send survey notification to user
     */
    private void recordLastSurveyNotificationTime(Context context) {
        Timestamp now = new Timestamp();
        SharedPreferences loginSharedPref = context.getSharedPreferences(
                context.getString(R.string.login_storage), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = loginSharedPref.edit();
        editor.putString(context.getString(R.string.last_survey_notification_time_key), now.toString());
        editor.commit();
    }
}

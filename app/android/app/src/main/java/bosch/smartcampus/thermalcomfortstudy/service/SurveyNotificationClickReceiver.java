package bosch.smartcampus.thermalcomfortstudy.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import bosch.smartcampus.thermalcomfortstudy.R;
import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;
import bosch.smartcampus.thermalcomfortstudy.ui.SurveyActivity;

/**
 * {@link SurveyNotificationClickReceiver} is responsible for:
 *  (1) starting the survey, and
 *  (2) starting sampling data from the band via {@link BandDataSamplingService} if necessary.
 */
public class SurveyNotificationClickReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = SurveyNotificationClickReceiver.class.getSimpleName();
    private static final String BASE_NAME = SurveyNotificationClickReceiver.class.getSimpleName();

    public static final String SURVEY_NOTIFICATION_CLICK_ACTION = BASE_NAME + ".intent.action.SURVEY_NOTIFICATION_CLICK";

    // Band data sampling duration (milliseconds) -- if started by survey
    public static final long SURVEY_BAND_DATA_SAMPLING_DURATION = 300000L;

    // Threshold elapsed time since last band data sampling (milliseconds)
    public static final long LAST_BAND_STREAMING_THRESHOLD_ELAPSED_TIME = 300000L;

    public SurveyNotificationClickReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (shouldStartBandDataSampling(context)) {
            startOneOffBandDataSamplingService(context);
        }

        Intent surveyIntent = new Intent(context, SurveyActivity.class);
        surveyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(surveyIntent);
        Log.i(LOG_TAG, "User starts the survey...");
    }

    /**
     * When user starts survey or action report, we should start sampling band data if
     * it has been more than LAST_BAND_STREAMING_THRESHOLD_ELAPSED_TIME since last band data sampling
     */
    private boolean shouldStartBandDataSampling(Context context) {
        Timestamp now = new Timestamp();
        SharedPreferences loginSharedPref = context.getSharedPreferences(
                context.getString(R.string.login_storage), Context.MODE_PRIVATE);
        String lastBandStreamingTimestamp = loginSharedPref.getString(
                context.getString(R.string.last_band_streaming_time_key), null);

        if (lastBandStreamingTimestamp == null) {
            return true;
        }

        Timestamp lastBandStreamingTime = new Timestamp(lastBandStreamingTimestamp);
        long elapsedTime = now.getDateTime().getTime() - lastBandStreamingTime.getDateTime().getTime();
        return elapsedTime > LAST_BAND_STREAMING_THRESHOLD_ELAPSED_TIME;
    }

    /**
     * Start a one-off BandDataSampling service
     */
    private void startOneOffBandDataSamplingService(Context context) {
        Intent bandDataSamplingIntent = new Intent(context, BandDataSamplingService.class);
        bandDataSamplingIntent.putExtra(BandDataSamplingService.SAMPLING_DURATION_PARAM,
                SurveyNotificationClickReceiver.SURVEY_BAND_DATA_SAMPLING_DURATION);
        bandDataSamplingIntent.putExtra(BandDataSamplingService.IS_PERIODIC_SAMPLING_PARAM,
                false);
        context.startService(bandDataSamplingIntent);
        Log.i(LOG_TAG, "Started a one-off BandDataSampling service");
    }
}

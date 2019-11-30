package bosch.smartcampus.thermalcomfortstudy.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import bosch.smartcampus.thermalcomfortstudy.R;
import bosch.smartcampus.thermalcomfortstudy.lib.BandDataAggregate;
import bosch.smartcampus.thermalcomfortstudy.lib.LocalDataStorage;
import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;

/**
 * {@link BandDataSamplingService} is the band data sampling component of the Experience Sampling.
 * It is responsible for:
 *  (1) streaming data from the band via {@link BandStreamingService} for a given duration,
 *  (2) periodically sampling its data, and
 *  (3) saving the sampled data to the local storage via {@link LocalDataStorage}.
 */
public class BandDataSamplingService extends Service {
    private static final String LOG_TAG = BandDataSamplingService.class.getSimpleName();
    private static final String BASE_NAME = BandDataSamplingService.class.getSimpleName();

    public static final String BAND_DATA_SAMPLING_OK_ACTION = BASE_NAME + ".intent.action.BAND_DATA_SAMPLING_OK";
    public static final String BAND_DATA_SAMPLING_BAD_ACTION = BASE_NAME + ".intent.action.BAND_DATA_SAMPLING_BAD";
    public static final String STOP_BAND_DATA_SAMPLING_ACTION = BASE_NAME + ".intent.action.STOP_BAND_DATA_SAMPLING";

    // Sampling duration parameter
    public static final String SAMPLING_DURATION_PARAM = "SamplingDuration";

    // Parameter indicating whether this is a periodic sampling
    public static final String IS_PERIODIC_SAMPLING_PARAM = "IsPeriodicSampling";

    // Default sampling duration (milliseconds)
    private static final long DEFAULT_SAMPLING_DURATION = 300000L;

    // Band data sampling interval (milliseconds)
    private static final long BAND_DATA_SAMPLING_INTERVAL = 60000L;

    // Note: Timer runs on its own thread
    private Timer mTimer = new Timer();
    private TimerTask mSamplingTimerTask;
    private TimerTask mEndSamplingTimerTask;

    // BandStreaming service and service connection
    private BandStreamingService mBandStreamingService;
    private boolean mBandStreamingServiceBound;
    private ServiceConnection mBandStreamingServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BandStreamingService.BandStreamingBinder binder = (BandStreamingService.BandStreamingBinder) service;
            mBandStreamingService = binder.getService();
            mBandStreamingServiceBound = true;
            startSampling();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBandStreamingServiceBound = false;
        }
    };

    /**
     * After startSampling(), wait to receive band connection status from {@link BandStreamingService}.
     *
     * After executing EndSampling task, wait for {@link BandStreamingService} to disconnect from band.
     */
    private BroadcastReceiver mBandConnStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            /**
             * If {@link BandStreamingService} successfully connected and subscribed to band
             * (i.e., band streaming has started), schedule a repeating Sampling task
             * and EndSampling task.
             *
             * Otherwise, stop this service.
             */
            if (action.equals(BandStreamingService.BAND_CONN_OK_ACTION)) {
                Log.i(LOG_TAG, "BandStreamingService is connected to band");

                scheduleRepeatingSamplingTask();
                scheduleEndSamplingTask();

                broadcastBandDataSamplingStatus(BAND_DATA_SAMPLING_OK_ACTION, mIsPeriodicSampling);
            } else if (action.equals(BandStreamingService.BAND_CONN_BAD_ACTION)) {
                Log.e(LOG_TAG, "BandStreamingService failed to connect to band");

                mHasStarted = false;
                stopSelfResult(mStartId);

                broadcastBandDataSamplingStatus(BAND_DATA_SAMPLING_BAD_ACTION, mIsPeriodicSampling);
            }
            /**
             * Stop this service after {@link BandStreamingService} disconnected from band.
             */
            else if (action.equals(BandStreamingService.BAND_DISCONN_OK_ACTION)) {
                Log.i(LOG_TAG, "BandStreamingService disconnected from band");

                mHasStarted = false;
                stopSelfResult(mStartId);
            }
        }
    };

    /**
     * Stop sampling band data as triggered by external entities (e.g., user logs out)
     */
    private BroadcastReceiver mStopSamplingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(STOP_BAND_DATA_SAMPLING_ACTION)) {
                Log.i(LOG_TAG, "Stop sampling band data");
                mSamplingTimerTask.cancel();
                mEndSamplingTimerTask.cancel();
                mBandStreamingService.stopStreaming();
                recordLastBandStreamingTime();
            }
        }
    };

    private BroadcastReceiver mSamplingAlarmReceiver = new BandDataSamplingAlarmReceiver();

    private boolean mHasStarted = false;
    private int mStartId;
    private long mSamplingDuration;
    private boolean mIsPeriodicSampling = false;

    public BandDataSamplingService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter bandConnStatusIntentFilter = new IntentFilter();
        bandConnStatusIntentFilter.addAction(BandStreamingService.BAND_CONN_OK_ACTION);
        bandConnStatusIntentFilter.addAction(BandStreamingService.BAND_CONN_BAD_ACTION);
        bandConnStatusIntentFilter.addAction(BandStreamingService.BAND_DISCONN_OK_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBandConnStatusReceiver, bandConnStatusIntentFilter);

        IntentFilter stopSamplingIntentFilter = new IntentFilter();
        stopSamplingIntentFilter.addAction(STOP_BAND_DATA_SAMPLING_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mStopSamplingReceiver, stopSamplingIntentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mStartId = startId;
        long samplingDuration = intent.getLongExtra(SAMPLING_DURATION_PARAM, DEFAULT_SAMPLING_DURATION);
        boolean isPeriodicSampling = intent.getBooleanExtra(IS_PERIODIC_SAMPLING_PARAM, false);

        /**
         * Register alarm receiver to schedule the next band data sampling alarm if this is a periodic sampling
         */
        if (isPeriodicSampling) {
            registerBandDataSamplingAlarmReceiver();
        }

        if (mHasStarted) {
            if (isPeriodicSampling) {
                /**
                 * If sampling is already in progress, and this is a periodic sampling
                 * (e.g., the previous sampling was started by user starting the survey),
                 * change the end time of the sampling task to be samplingDuration from now
                 */
                Log.i(LOG_TAG, "Band data sampling is already in progress");
                Log.i(LOG_TAG, "Extend duration of sampling for " + samplingDuration + " ms...");
                mEndSamplingTimerTask.cancel();
                mSamplingDuration = samplingDuration;
                mIsPeriodicSampling = isPeriodicSampling;
                scheduleEndSamplingTask();
            } else {
                /**
                 * If sampling is already in progress, and this is not a periodic sampling, do nothing
                 * (including not overriding the samplingDuration and isPeriodicSampling fields)
                 */
                Log.i(LOG_TAG, "Band data sampling is already in progress");
            }

            /**
             * Notifying others that sampling begins (or has already begun)
             */
            broadcastBandDataSamplingStatus(BAND_DATA_SAMPLING_OK_ACTION, isPeriodicSampling);
        } else {
            Log.i(LOG_TAG, "Start band data sampling process for " + samplingDuration + " ms...");
            mHasStarted = true;
            mSamplingDuration = samplingDuration;
            mIsPeriodicSampling = isPeriodicSampling;
            Intent bandStreamingIntent = new Intent(this, BandStreamingService.class);
            bindService(bandStreamingIntent, mBandStreamingServiceConn, Context.BIND_AUTO_CREATE);
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mTimer.cancel();

        if (mBandStreamingServiceBound) {
            unbindService(mBandStreamingServiceConn);
            mBandStreamingServiceBound = false;
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBandConnStatusReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mStopSamplingReceiver);

        if (mIsPeriodicSampling) {
            unregisterBandDataSamplingAlarmReceiver();
        }
    }

    /**
     * Start sampling band data as soon as {@link BandStreamingService} is connected to this service
     */
    private void startSampling() {
        if (mHasStarted && mBandStreamingServiceBound) {
            mBandStreamingService.startStreaming();
        }
    }

    /**
     * Average band data every 1 minute and save data to local storage
     *
     * @pre: {@link BandStreamingService} is connected to band
     */
    private void scheduleRepeatingSamplingTask() {
        // Create a new instance of SamplingTimerTask every time we (re)schedule it
        // because TimerTask cannot be rescheduled
        mSamplingTimerTask = new TimerTask() {
            /**
             * Run Sampling task only if there are raw data from band's sensors in the BandDataAggregate
             * that have not been processed yet.
             *
             * Otherwise, skip this round of Sampling task.
             */
            @Override
            public void run() {
                BandDataAggregate aggregate = mBandStreamingService.getBandDataAggregate();
                if (aggregate.hasRawTemperatureData() && aggregate.hasRawTotalCaloriesData()
                        && aggregate.hasRawGsrData() && aggregate.hasRawSkinTemperatureData()) {
                    averageBandData(aggregate);
                    writeBandDataToLocalStorage(aggregate);
                } else {
                    Log.e(LOG_TAG, "No raw data has been received from band in this sampling interval");
                }
            }
        };

        mTimer.scheduleAtFixedRate(mSamplingTimerTask,
                BAND_DATA_SAMPLING_INTERVAL,
                BAND_DATA_SAMPLING_INTERVAL);
        Log.i(LOG_TAG, "Start sampling band data...");
    }

    /**
     * End the repeating sampling task after a fixed period of time
     */
    private void scheduleEndSamplingTask() {
        // Create a new instance of EndSamplingTimerTask every time we (re)schedule it
        // because TimerTask cannot be rescheduled
        mEndSamplingTimerTask = new TimerTask() {
            /**
             * Stop sampling band data by canceling the repeating Sampling task and stopping streaming band
             */
            @Override
            public void run() {
                mSamplingTimerTask.cancel();
                mBandStreamingService.stopStreaming();
                recordLastBandStreamingTime();
                Log.i(LOG_TAG, "Stop sampling band data");
            }
        };

        mTimer.schedule(mEndSamplingTimerTask, mSamplingDuration);
    }

    /**
     * Average band data
     */
    private void averageBandData(BandDataAggregate aggregate) {
        aggregate.averageTemperature();
        aggregate.averageCalories();
        aggregate.averageGsr();
        aggregate.averageSkinTemperature();
    }

    /**
     * Write band data to local storage
     */
    private void writeBandDataToLocalStorage(BandDataAggregate aggregate) {
        String username = getUsername();
        Timestamp timestamp = new Timestamp();

        double lastAvgTemperature = aggregate.getLastAverageTemperature();
        double lastAvgCalories = aggregate.getLastAverageCalories();
        // Rate of Total Calories reading is 1/sec
        double lastMinuteCalories = BAND_DATA_SAMPLING_INTERVAL / 1000 * lastAvgCalories;
        double lastAvgGsr = aggregate.getLastAverageGsr();
        double lastAvgSkinTemperature = aggregate.getLastAverageSkinTemperature();

        try {
            LocalDataStorage storage = new LocalDataStorage(true);
            storage.writeBandData(username, timestamp,
                    lastAvgTemperature,
                    lastMinuteCalories,
                    lastAvgGsr,
                    lastAvgSkinTemperature);
            storage.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Record the most recent time we stream data from band
     */
    private void recordLastBandStreamingTime() {
        Timestamp now = new Timestamp();
        SharedPreferences loginSharedPref = getSharedPreferences(getString(R.string.login_storage),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = loginSharedPref.edit();
        editor.putString(getString(R.string.last_band_streaming_time_key), now.toString());
        editor.commit();
    }

    /**
     * Register {@link BandDataSamplingAlarmReceiver} to schedule the next sampling alarm
     * if this is a periodic sampling
     */
    private void registerBandDataSamplingAlarmReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BandDataSamplingService.BAND_DATA_SAMPLING_OK_ACTION);
        intentFilter.addAction(BandDataSamplingService.BAND_DATA_SAMPLING_BAD_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mSamplingAlarmReceiver, intentFilter);
    }

    private void unregisterBandDataSamplingAlarmReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSamplingAlarmReceiver);
    }

    /**
     * Broadcast band data sampling status, with a flag indicating whether this is a periodic sampling
     */
    private void broadcastBandDataSamplingStatus(String action, boolean isPeriodicSampling) {
        Intent samplingStatusIntent = new Intent(action);
        samplingStatusIntent.putExtra(IS_PERIODIC_SAMPLING_PARAM, isPeriodicSampling);
        LocalBroadcastManager.getInstance(BandDataSamplingService.this).sendBroadcast(samplingStatusIntent);
    }

    private String getUsername() {
        SharedPreferences loginSharedPref = getSharedPreferences(getString(R.string.login_storage),
                Context.MODE_PRIVATE);
        String username = loginSharedPref.getString(getString(R.string.username_key), null);
        assert username != null;
        return username;
    }
}

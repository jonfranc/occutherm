package bosch.smartcampus.thermalcomfortstudy.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandPendingResult;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.InvalidBandVersionException;
import com.microsoft.band.sensors.BandBarometerEvent;
import com.microsoft.band.sensors.BandBarometerEventListener;
import com.microsoft.band.sensors.BandCaloriesEvent;
import com.microsoft.band.sensors.BandCaloriesEventListener;
import com.microsoft.band.sensors.BandContactEvent;
import com.microsoft.band.sensors.BandContactEventListener;
import com.microsoft.band.sensors.BandContactState;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;
import com.microsoft.band.sensors.GsrSampleRate;

import bosch.smartcampus.thermalcomfortstudy.R;
import bosch.smartcampus.thermalcomfortstudy.lib.BandDataAggregate;

/**
 * {@link BandStreamingService} is the band streaming component of the Experience Sampling.
 * It is responsible for:
 *  (1) creating a connection to the band and subscribing to the band's sensors (i.e., streaming data from the band), and
 *  (2) unsubscribing from the band's sensors and disconnecting from the band (i.e., stop streaming data from the band).
 */
public class BandStreamingService extends Service {
    private static final String LOG_TAG = BandStreamingService.class.getSimpleName();
    private static final String BASE_NAME = BandStreamingService.class.getSimpleName();

    // Intent actions to be sent
    public static final String BAND_CONN_OK_ACTION = BASE_NAME + ".intent.action.BAND_CONN_OK";
    public static final String BAND_CONN_BAD_ACTION = BASE_NAME + ".intent.action.BAND_CONN_BAD";
    public static final String BAND_DISCONN_OK_ACTION = BASE_NAME + ".intent.action.BAND_DISCONN_OK";

    // Band connection notification id
    public static final int BAND_CONNECTION_NOTIFICATION_ID = 5222;

    // Band contact notification id
    public static final int BAND_CONTACT_NOTIFICATION_ID = 5223;

    // New thread for band streaming service
    private static final String SERVICE_THREAD_NAME = BASE_NAME + "WorkerThread";
    private static final int START_STREAMING_CODE = 001;
    private static final int STOP_STREAMING_CODE = 002;

    public class BandStreamingBinder extends Binder {
        public BandStreamingService getService() {
            return BandStreamingService.this;
        }
    }

    // Service binder providing interface to BandStreaming service
    private final IBinder mServiceBinder = new BandStreamingBinder();

    // Service thread and handler
    private HandlerThread mServiceThread;
    private BandStreamingServiceHandler mServiceHandler;

    private class BandStreamingServiceHandler extends Handler {
        public BandStreamingServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == START_STREAMING_CODE) {
                handleStartBandStreaming();
            } else if (message.what == STOP_STREAMING_CODE) {
                handleStopBandStreaming();
            }
        }

        /**
         * Connect and subscribe to band's sensors (start band streaming)
         */
        private void handleStartBandStreaming() {
            String bandConnMessage;
            boolean connOK = false;

            if (!createBandClient()) {
                String pairMessage = "No band is paired with this phone";
                bandConnMessage = pairMessage;
                Log.e(LOG_TAG, pairMessage);

                // Notify user to check band connection
                sendBandConnectionNotification();
            } else if (!connectToBand()) {
                String connMessage = "Failed to connect to band";
                bandConnMessage = connMessage;
                Log.e(LOG_TAG, connMessage);

                // Notify user to check band connection
                sendBandConnectionNotification();
                disconnectFromBand(); // to prevent ServiceConnection leak
            } else {
                String connMessage = "Connected to band";
                Log.i(LOG_TAG, connMessage);

                connOK = subscribeToBandSensors();
                String subsMessage;
                if (connOK) {
                    subsMessage = "Registered Barometer, Calories, GSR, and Skin Temperature EventListeners";
                } else {
                    subsMessage = "Failed to register some EventListener";

                    // Notify user to check band connection
                    sendBandConnectionNotification();
                }

                bandConnMessage = connMessage + "\n" + subsMessage;
            }

            String action = connOK ? BAND_CONN_OK_ACTION : BAND_CONN_BAD_ACTION;
            broadcastBandConnectionStatus(action, bandConnMessage);

            mBandConnMessage = bandConnMessage;
        }

        /**
         * Unsubscribe from band's sensors and disconnect from band (stop band streaming)
         */
        private void handleStopBandStreaming() {
            if (mBandClient != null) {
                unsubscribeFromBandSensors();
                disconnectFromBand();
            }

            String bandDisConnMessage = "Disconnected from band";
            broadcastBandConnectionStatus(BAND_DISCONN_OK_ACTION, bandDisConnMessage);

            mBandConnMessage = bandDisConnMessage;
        }
    }

    private BandClient mBandClient;

    private BandBarometerEventListener mBarometerEventListener = new BandBarometerEventListener() {
        @Override
        public void onBandBarometerChanged(BandBarometerEvent event) {
            if (event != null) {
                double temperature = event.getTemperature();
                mBandDataAggregate.putTemperature(temperature);
                Log.i(LOG_TAG, "temperature = " + temperature);
            }
        }
    };

    private BandCaloriesEventListener mCaloriesEventListener = new BandCaloriesEventListener() {
        @Override
        public void onBandCaloriesChanged(BandCaloriesEvent event) {
            if (event != null) {
                long totalCalories = event.getCalories();
                mBandDataAggregate.putTotalCalories(totalCalories);
                Log.i(LOG_TAG, "total calories = " + totalCalories);
            }
        }
    };

    private BandGsrEventListener mGsrEventListener = new BandGsrEventListener() {
        @Override
        public void onBandGsrChanged(BandGsrEvent event) {
            if (event != null) {
                int resistance = event.getResistance();
                mBandDataAggregate.putGsr(resistance);
                Log.i(LOG_TAG, "GSR = " + resistance);
            }
        }
    };

    private BandSkinTemperatureEventListener
            mSkinTempEventListener = new BandSkinTemperatureEventListener() {
        @Override
        public void onBandSkinTemperatureChanged(BandSkinTemperatureEvent event) {
            if (event != null) {
                float skinTemperature = event.getTemperature();
                mBandDataAggregate.putSkinTemperature(skinTemperature);
                Log.i(LOG_TAG, "skin temperature = " + skinTemperature);
            }
        }
    };

    private BandContactEventListener mContactEventListener = new BandContactEventListener() {
        @Override
        public void onBandContactChanged(BandContactEvent event) {
            if (event != null) {
                BandContactState contactState = event.getContactState();
                mBandDataAggregate.setBandContactStatus(contactState == BandContactState.WORN);

                if (contactState == BandContactState.WORN) {
                    Log.i(LOG_TAG, "User is wearing the band");
                    Log.i(LOG_TAG, "Start or resume band data collection");
                } else {
                    Log.e(LOG_TAG, "User is not wearing the band");
                    Log.e(LOG_TAG, "Pause band data collection");
                    // Notify user to check band contact
                    sendBandContactNotification();
                }
            }
        }
    };

    private String mBandConnMessage = "";
    private BandDataAggregate mBandDataAggregate = new BandDataAggregate();

    @Override
    public void onCreate() {
        mServiceThread = new HandlerThread(SERVICE_THREAD_NAME, Process.THREAD_PRIORITY_BACKGROUND);
        mServiceThread.start();
        mServiceHandler = new BandStreamingServiceHandler(mServiceThread.getLooper());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mServiceBinder;
    }

    @Override
    public void onDestroy() {
        mServiceThread.quitSafely();
    }

    /**
     * Start streaming band
     */
    public void startStreaming() {
        Message startMessage = mServiceHandler.obtainMessage();
        startMessage.what = START_STREAMING_CODE;
        mServiceHandler.sendMessage(startMessage);
        Log.i(LOG_TAG, "Start streaming process...");
    }

    /**
     * Stop streaming band
     */
    public void stopStreaming() {
        Message stopMessage = mServiceHandler.obtainMessage();
        stopMessage.what = STOP_STREAMING_CODE;
        mServiceHandler.sendMessage(stopMessage);
        Log.i(LOG_TAG, "Stop streaming process...");
    }

    public BandDataAggregate getBandDataAggregate() {
        return mBandDataAggregate;
    }

    /**
     * Check if this service is connected to band
     */
    public boolean isConnectedToBand() {
        return mBandClient != null && mBandClient.getConnectionState() == ConnectionState.CONNECTED;
    }

    /**
     * Get the latest band connection message
     */
    public String getBandConnectionMessage() {
        return mBandConnMessage;
    }

    /**
     * Create band client
     * @return true iff there is a paired band
     */
    private boolean createBandClient() {
        Log.i(LOG_TAG, "Creating BandClient...");

        BandInfo[] pairedBands = BandClientManager.getInstance().getPairedBands();
        if (pairedBands.length == 0) {
            return false;
        }

        mBandClient = BandClientManager.getInstance().create(getBaseContext(), pairedBands[0]);
        return true;
    }

    /**
     * Connect to band
     * @return true iff band is connected
     */
    private boolean connectToBand() {
        Log.i(LOG_TAG, "Connecting to band...");

        if (mBandClient.getConnectionState() == ConnectionState.CONNECTED) {
            return true;
        }

        BandPendingResult<ConnectionState> pendingResult = mBandClient.connect();
        ConnectionState connState = null;
        try {
            connState = pendingResult.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BandException e) {
            e.printStackTrace();
        }
        return connState == ConnectionState.CONNECTED;
    }

    /**
     * Disconnect from band
     */
    private void disconnectFromBand() {
        Log.i(LOG_TAG, "Disconnecting from band...");

        try {
            mBandClient.disconnect().await();
            Log.i(LOG_TAG, "Disconnected from band");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BandException e) {
            Log.e(LOG_TAG, "Exception occurred when disconnected from band");
            e.printStackTrace();
        }
    }

    /**
     * Subscribe to band's sensors
     */
    private boolean subscribeToBandSensors() {
        Log.i(LOG_TAG, "Subscribing to band's sensors...");
        
        try {
            mBandClient.getSensorManager().registerBarometerEventListener(mBarometerEventListener);
            Log.i(LOG_TAG, "Registered Barometer EventListener");
            mBandClient.getSensorManager().registerCaloriesEventListener(mCaloriesEventListener);
            Log.i(LOG_TAG, "Registered Calories EventListener");
            mBandClient.getSensorManager().registerGsrEventListener(mGsrEventListener, GsrSampleRate.MS5000);
            Log.i(LOG_TAG, "Registered GSR EventListener");
            mBandClient.getSensorManager().registerSkinTemperatureEventListener(mSkinTempEventListener);
            Log.i(LOG_TAG, "Registered Skin Temperature EventListener");
            mBandClient.getSensorManager().registerContactEventListener(mContactEventListener);
            Log.i(LOG_TAG, "Registered Band Contact EventListener");
            return true;
        } catch (BandIOException | InvalidBandVersionException e) {
            Log.e(LOG_TAG, "Failed to register some EventListener");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Unsubscribe from band's sensors
     */
    private void unsubscribeFromBandSensors() {
        Log.i(LOG_TAG, "Unsubscribing from band's sensors...");

        try {
            mBandClient.getSensorManager().unregisterBarometerEventListener(mBarometerEventListener);
            Log.i(LOG_TAG, "Unregistered Barometer EventListener");
            mBandClient.getSensorManager().unregisterCaloriesEventListener(mCaloriesEventListener);
            Log.i(LOG_TAG, "Unregistered Calories EventListener");
            mBandClient.getSensorManager().unregisterGsrEventListener(mGsrEventListener);
            Log.i(LOG_TAG, "Unregistered GSR EventListener");
            mBandClient.getSensorManager().unregisterSkinTemperatureEventListener(mSkinTempEventListener);
            Log.i(LOG_TAG, "Unregistered Skin Temperature EventListener");
            mBandClient.getSensorManager().unregisterContactEventListener(mContactEventListener);
            Log.i(LOG_TAG, "Unregistered Band Contact EventListener");
        } catch (BandIOException e) {
            Log.e(LOG_TAG, "Exception occurred when unregistered some EventListener");
            e.printStackTrace();
        }
    }

    /**
     * Broadcast band connection status
     */
    private void broadcastBandConnectionStatus(String action, String message) {
        Intent connStatusIntent = new Intent(action);
        connStatusIntent.putExtra(getString(R.string.band_conn_message), message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(connStatusIntent);
    }

    /**
     * Send a band connection notification to user
     */
    private void sendBandConnectionNotification() {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Thermal Comfort Study Notification")
                .setContentText("Please check the band's Bluetooth connection to your phone");
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mBuilder.setSound(alarmSound);
        mBuilder.setAutoCancel(true);

        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.notify(BAND_CONNECTION_NOTIFICATION_ID, mBuilder.build());
        Log.i(LOG_TAG, "Notified user to check band's Bluetooth connection");
    }

    /**
     * Send a band contact notification to user
     */
    private void sendBandContactNotification() {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Thermal Comfort Study Notification")
                .setContentText("Please make sure you are wearing the band properly");
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mBuilder.setSound(alarmSound);
        mBuilder.setAutoCancel(true);

        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.notify(BAND_CONTACT_NOTIFICATION_ID, mBuilder.build());
        Log.i(LOG_TAG, "Notified user to check band's skin contact");
    }
}

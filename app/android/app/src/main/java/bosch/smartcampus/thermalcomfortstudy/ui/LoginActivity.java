package bosch.smartcampus.thermalcomfortstudy.ui;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import bosch.smartcampus.thermalcomfortstudy.R;
import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;
import bosch.smartcampus.thermalcomfortstudy.service.BandDataSamplingAlarmReceiver;
import bosch.smartcampus.thermalcomfortstudy.service.BandDataSamplingService;
import bosch.smartcampus.thermalcomfortstudy.service.DataUploadAlarmReceiver;
import bosch.smartcampus.thermalcomfortstudy.service.SAConnectionService;

/**
 * {@link LoginActivity} is the login component of the application.
 * It is responsible for:
 *  (1) verifying the user's credentials and creating a connection to Sensor Andrew via {@link SAConnectionService},
 *  (2) saving the user's credentials,
 *  (3) starting the first BandDataSampling via {@link BandDataSamplingService} for the Experience Sampling --
 *  which will schedule the SurveyNotification alarm and the next BandDataSampling alarm, and
 *  (4) scheduling the first DataUpload alarm.
 */
public class LoginActivity extends AppCompatActivity {
    private static final String LOG_TAG = LoginActivity.class.getSimpleName();

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 001;

    /**
     * After connectToSA(), wait to receive Sensor Andrew connection status and message from {@link SAConnectionService}
     */
    private BroadcastReceiver mSAConnStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String saConnMessage = intent.getStringExtra(getString(R.string.sa_conn_status_message));

            /**
             * If connected to Sensor Andrew, save username and password,
             * start the first BandDataSampling (which will schedule
             * SurveyNotification alarm and the next BandDataSampling alarm),
             * schedule DataUpload alarm, and go to Main page.
             *
             * Otherwise, prompt user to try again.
             */
            if (action.equals(SAConnectionService.SA_CONNECT_OK_ACTION)) {
                Log.i(LOG_TAG, "Sensor Andrew connection established");

                Toast.makeText(context, saConnMessage, Toast.LENGTH_LONG).show();

                saveUsernameAndPassword();
                startPeriodicBandDataSamplingService();
                scheduleDataUploadAlarm();
                gotoMain();
            } else if (action.equals(SAConnectionService.SA_CONNECT_BAD_ACTION)) {
                Log.e(LOG_TAG, "Sensor Andrew connection failed");

                // Enable login button to let user try logging in again
                Button loginButton = (Button) findViewById(R.id.login);
                loginButton.setEnabled(true);
                Toast.makeText(context, saConnMessage, Toast.LENGTH_LONG).show();

                String promptText = "Please make sure you enter the correct username and password";
                promptText += " and you are connected to the Internet";
                Toast.makeText(context, promptText, Toast.LENGTH_LONG).show();
            }
        }
    };

    private String mUsername;
    private String mPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * If user has already logged in, go to Main page
         */
        if (isLoggedIn()) {
            Log.i(LOG_TAG, "User has already logged in -- go to Main page");
            gotoMain();
        } else {
            setContentView(R.layout.activity_login);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestPermissions();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SAConnectionService.SA_CONNECT_OK_ACTION);
        intentFilter.addAction(SAConnectionService.SA_CONNECT_BAD_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mSAConnStatusReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSAConnStatusReceiver);
    }

    /**
     * Log in to the study.
     *
     * Connect to Sensor Andrew to verify username and password.
     */
    public void logIn(View view) {
        // Disable login button to prevent calling logIn() multiple times
        Button loginButton = (Button) view;
        loginButton.setEnabled(false);

        EditText usernameInput = (EditText) findViewById(R.id.username);
        EditText passwordInput = (EditText) findViewById(R.id.password);
        mUsername = usernameInput.getText().toString();
        mPassword = passwordInput.getText().toString();

        // Connect to Sensor Andrew
        connectToSA(mUsername, mPassword);
    }

    /**
     * Request WRITE_EXTERNAL_STORAGE permission from user
     */
    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(LOG_TAG, "User granted WRITE_EXTERNAL_STORAGE permission");
                } else {
                    Log.e(LOG_TAG, "User did not grant WRITE_EXTERNAL_STORAGE permission");
                }
            }
        }
    }

    /**
     * @return Whether user has logged in to the study
     */
    private boolean isLoggedIn() {
        SharedPreferences loginSharedPref = getSharedPreferences(getString(R.string.login_storage),
                Context.MODE_PRIVATE);
        return loginSharedPref.getBoolean(getString(R.string.is_logged_in_key), false);
    }

    /**
     * Open a connection to Sensor Andrew
     */
    private void connectToSA(String username, String password) {
        Intent saConnectIntent = new Intent(this, SAConnectionService.class);
        saConnectIntent.setAction(SAConnectionService.SA_CONNECT_ACTION);
        saConnectIntent.putExtra(SAConnectionService.USERNAME_PARAM, username);
        saConnectIntent.putExtra(SAConnectionService.PASSWORD_PARAM, password);
        startService(saConnectIntent);
    }

    /**
     * Save username and password to shared preferences
     *
     * @pre {@link SAConnectionService} has established a connection to Sensor Andrew
     */
    private void saveUsernameAndPassword() {
        SharedPreferences loginSharedPref = getSharedPreferences(getString(R.string.login_storage),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = loginSharedPref.edit();
        editor.putBoolean(getString(R.string.is_logged_in_key), true);
        editor.putString(getString(R.string.username_key), mUsername);
        editor.putString(getString(R.string.password_key), mPassword);
        editor.commit();
        Log.i(LOG_TAG, "Logged in to the study as " + mUsername);
    }

    /**
     * Schedule the first DataUpload alarm
     */
    private void scheduleDataUploadAlarm() {
        // Record current time as "last data upload time"
        Timestamp now = new Timestamp();
        SharedPreferences loginSharedPref = getSharedPreferences(getString(R.string.login_storage),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = loginSharedPref.edit();
        editor.putString(getString(R.string.last_data_upload_time_key), now.toString());
        editor.commit();

        // Schedule DataUpload alarm
        Intent alarmIntent = new Intent(this, DataUploadAlarmReceiver.class);
        alarmIntent.setAction(DataUploadAlarmReceiver.DATA_UPLOAD_ALARM_ACTION);
        PendingIntent pAlarmIntent = PendingIntent.getBroadcast(this, 0,
                alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + DataUploadAlarmReceiver.DATA_UPLOAD_INTERVAL,
                pAlarmIntent);
        Log.i(LOG_TAG, "Scheduled the first DataUpload alarm in "
                + DataUploadAlarmReceiver.DATA_UPLOAD_INTERVAL + " milliseconds");
    }

    /**
     * Start a periodic BandDataSampling service
     */
    private void startPeriodicBandDataSamplingService() {
        Intent bandDataSamplingIntent = new Intent(this, BandDataSamplingService.class);
        bandDataSamplingIntent.putExtra(BandDataSamplingService.SAMPLING_DURATION_PARAM,
                BandDataSamplingAlarmReceiver.ALARM_BAND_DATA_SAMPLING_DURATION);
        bandDataSamplingIntent.putExtra(BandDataSamplingService.IS_PERIODIC_SAMPLING_PARAM,
                true);
        startService(bandDataSamplingIntent);
        Log.i(LOG_TAG, "Started a periodic BandDataSampling service");
    }

    /**
     * Go to Main page
     *
     * @pre: {@link SAConnectionService} has established a connection to Sensor Andrew
     */
    private void gotoMain() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        startActivity(mainIntent);
    }
}

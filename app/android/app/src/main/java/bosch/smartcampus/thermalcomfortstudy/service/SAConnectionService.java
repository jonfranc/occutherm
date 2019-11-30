package bosch.smartcampus.thermalcomfortstudy.service;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PubSubManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import bosch.smartcampus.thermalcomfortstudy.R;
import bosch.smartcampus.thermalcomfortstudy.ThermalComfortStudyApplication;
import bosch.smartcampus.thermalcomfortstudy.lib.LocalDataStorage;
import bosch.smartcampus.thermalcomfortstudy.lib.Timestamp;
import bosch.smartcampus.thermalcomfortstudy.packet.TCSDataItem;

/**
 * {@link SAConnectionService} is the Sensor Andrew connection component of the application.
 * It is responsible for:
 *  (1) creating and closing a connection to Sensor Andrew,
 *  (2) publishing the user's data to the OccupantThermalComfort node, and
 *  (3) broadcasting to {@link DataUploadAlarmReceiver} the last time that a data item is successfully uploaded to Sensor Andrew.
 */
public class SAConnectionService extends IntentService {
    private static final String LOG_TAG = SAConnectionService.class.getSimpleName();
    private static final String BASE_NAME = SAConnectionService.class.getSimpleName();

    // Intent actions to be received
    public static final String SA_CONNECT_ACTION = BASE_NAME + ".intent.action.SA_CONNECT";
    public static final String SA_DISCONNECT_ACTION = BASE_NAME + ".intent.action.SA_DISCONNECT";
    public static final String SA_UPLOAD_DATA_ACTION = BASE_NAME + ".intent.action.SA_UPLOAD_DATA";

    // Intent actions to be sent
    public static final String SA_CONNECT_OK_ACTION = BASE_NAME + ".intent.action.SA_CONNECT_OK";
    public static final String SA_CONNECT_BAD_ACTION = BASE_NAME + ".intent.action.SA_CONNECT_BAD";
    public static final String SA_UPLOAD_DATA_OK_ACTION = BASE_NAME + ".intent.action.SA_UPLOAD_DATA_OK";
    public static final String SA_UPLOAD_DATA_BAD_ACTION = BASE_NAME + ".intent.action.SA_UPLOAD_DATA_BAD";

    // Received intent extra keys
    public static final String USERNAME_PARAM = "username";
    public static final String PASSWORD_PARAM = "password";
    public static final String UPLOAD_DATA_FROM_TIMESTAMP_PARAM = "fromTimestamp";
    public static final String UPLOAD_DATA_TO_TIMESTAMP_PARAM = "toTimestamp";

    // Sensor Andrew connection info
    private static final String SA_SERVICE = "sensor.andrew.cmu.edu";
    private static final String HOST = "sensor.andrew.cmu.edu";
    private static final int PORT = 5222;
    private static final String PUBSUB_PATH = "pubsub." + SA_SERVICE;
    private static final String NODE_ID = "occthermcomf_bosch";

    private BroadcastReceiver mDataUploadAlarmReceiver = new DataUploadAlarmReceiver();

    public SAConnectionService() {
        super(BASE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter uploadResultIntentFilter = new IntentFilter();
        uploadResultIntentFilter.addAction(SA_UPLOAD_DATA_OK_ACTION);
        uploadResultIntentFilter.addAction(SA_UPLOAD_DATA_BAD_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mDataUploadAlarmReceiver, uploadResultIntentFilter);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();

            if (action.equals(SA_CONNECT_ACTION)) {
                String username = intent.getStringExtra(USERNAME_PARAM);
                String password = intent.getStringExtra(PASSWORD_PARAM);
                handleSAConnectAction(username, password);
            } else if (action.equals(SA_DISCONNECT_ACTION)) {
                disconnectFromSA();
            } else if (action.equals(SA_UPLOAD_DATA_ACTION)) {
                String fromTimestamp = intent.getStringExtra(UPLOAD_DATA_FROM_TIMESTAMP_PARAM);
                String toTimestamp = intent.getStringExtra(UPLOAD_DATA_TO_TIMESTAMP_PARAM);
                handleSAUploadDataAction(fromTimestamp, toTimestamp);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mDataUploadAlarmReceiver);
    }

    /**
     * Handle connect action
     */
    private void handleSAConnectAction(String username, String password) {
        Pair<Boolean, String> connResult = connectToSA(username, password);
        boolean connected = connResult.first;
        String connMessage = connResult.second;

        if (connected) {
            broadcastSAConnectionStatus(SA_CONNECT_OK_ACTION, connMessage);
        } else {
            broadcastSAConnectionStatus(SA_CONNECT_BAD_ACTION, connMessage);
        }
    }

    /**
     * Handle upload data action
     */
    private void handleSAUploadDataAction(String fromTimestamp, String toTimestamp) {
        Timestamp from = new Timestamp(fromTimestamp);
        Timestamp to = new Timestamp(toTimestamp);

        Pair<Boolean, String> reconnResult = reconnectToSA();
        boolean connected = reconnResult.first;
        String reconnMessage = reconnResult.second;

        if (!connected) {
            broadcastSAUploadDataResult(SA_UPLOAD_DATA_BAD_ACTION, reconnMessage, from);
            return;
        }

        LocalDataStorage storage = null;
        try {
            storage = new LocalDataStorage(false);
            List<TCSDataItem> dataItems = storage.readDataItems(from, to);
            sendDataItemsToSA(dataItems);
        } catch (FileNotFoundException e) {
            broadcastSAUploadDataResult(SA_UPLOAD_DATA_BAD_ACTION, "FileNotFoundException", from);
            e.printStackTrace();
        } catch (IOException e) {
            broadcastSAUploadDataResult(SA_UPLOAD_DATA_BAD_ACTION, "IOException", from);
            e.printStackTrace();
        }

        if (storage != null) {
            try {
                storage.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Create a global connection to sensor.andrew.cmu.edu
     */
    private Pair<Boolean, String> connectToSA(String username, String password) {
        XMPPTCPConnectionConfiguration connConfig = XMPPTCPConnectionConfiguration.builder()
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .setServiceName(SA_SERVICE)
                .setHost(HOST)
                .setPort(PORT)
                .setUsernameAndPassword(username, password)
                .build();

        XMPPTCPConnection connection = new XMPPTCPConnection(connConfig);
        String connMessage;

        try {
            connection.connect();
            connMessage = "Connected to " + connection.getHost();
            Log.i(LOG_TAG, connMessage);
        } catch (XMPPException | SmackException | IOException e) {
            connMessage = "Failed to connect to " + HOST;
            Log.e(LOG_TAG, connMessage);
            e.printStackTrace();
            return Pair.create(Boolean.FALSE, connMessage);
        }

        try {
            connection.login();
            String loginMessage = "Logged in as " + connection.getUser();
            connMessage += "\n" + loginMessage;
            Log.i(LOG_TAG, loginMessage);
        } catch (XMPPException | SmackException | IOException e) {
            String loginMessage = "Failed to log in as " + username;
            connMessage += "\n" + loginMessage;
            Log.e(LOG_TAG, loginMessage);
            e.printStackTrace();
            return Pair.create(Boolean.FALSE, connMessage);
        }

        ThermalComfortStudyApplication application = (ThermalComfortStudyApplication) getApplication();
        application.setGlobalSAConnection(connection);
        return Pair.create(Boolean.TRUE, connMessage);
    }

    /**
     * Check if the global Sensor Andrew Connection object still exists,
     * if not, connect to Sensor Andrew before sending data
     */
    private Pair<Boolean, String> reconnectToSA() {
        ThermalComfortStudyApplication application = (ThermalComfortStudyApplication) getApplication();

        if (!application.hasGlobalSAConnection()) {
            Log.i(LOG_TAG, "Attempting to establish connection to Sensor Andrew...");

            Pair<Boolean, String> connResult = connectToSA(getUsername(), getPassword());
            boolean connected = connResult.first;
            String connMessage = connResult.second;

            if (connected) {
                Log.i(LOG_TAG, "Established connection to Sensor Andrew");
                return Pair.create(Boolean.TRUE, connMessage);
            } else {
                Log.e(LOG_TAG, "Cannot establish connection to Sensor Andrew");
                return Pair.create(Boolean.FALSE, connMessage);
            }
        }

        return Pair.create(Boolean.TRUE, "Already connected to " + application.getGlobalSAConnection().getHost());
    }

    /**
     * Disconnect from sensor.andrew.cmu.edu
     */
    private void disconnectFromSA() {
        ThermalComfortStudyApplication application = (ThermalComfortStudyApplication) getApplication();

        if (application.hasGlobalSAConnection()) {
            XMPPTCPConnection globalConnection = application.getGlobalSAConnection();
            globalConnection.disconnect();
            Log.i(LOG_TAG, "Disconnected from " + globalConnection.getHost());
        } else {
            Log.i(LOG_TAG, "Already disconnected from " + HOST);
        }
    }

    /**
     * Broadcast the SA connection status after connectToSA() and disconnectFromSA()
     */
    private void broadcastSAConnectionStatus(String action, String message) {
        Intent connStatusIntent = new Intent(action);
        connStatusIntent.putExtra(getString(R.string.sa_conn_status_message), message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(connStatusIntent);
    }

    /**
     * Send data items to Sensor Andrew
     */
    private void sendDataItemsToSA(List<TCSDataItem> dataItems) {
        Timestamp lastUploadedTimestamp = getLastDataUploadTimestamp();

        for (TCSDataItem dataItem : dataItems) {
            Pair<Boolean, String> sendResult = sendDataItemToSA(dataItem);
            boolean sent = sendResult.first;
            String sendMessage = sendResult.second;

            if (sent) {
                lastUploadedTimestamp = dataItem.getTimestamp();
            } else {
                broadcastSAUploadDataResult(SA_UPLOAD_DATA_BAD_ACTION, sendMessage, lastUploadedTimestamp);
                return;
            }
        }

        broadcastSAUploadDataResult(SA_UPLOAD_DATA_OK_ACTION, "Sent all data to " + NODE_ID, lastUploadedTimestamp);
    }

    /**
     * Send data item to Sensor Andrew's OccupantThermalComfort node
     */
    private Pair<Boolean, String> sendDataItemToSA(Item item) {
        ThermalComfortStudyApplication application = (ThermalComfortStudyApplication) getApplication();
        XMPPTCPConnection globalConnection = application.getGlobalSAConnection();
        assert globalConnection != null;
        PubSubManager pubSubManager = new PubSubManager(globalConnection, PUBSUB_PATH);

        try {
            LeafNode node = pubSubManager.getNode(NODE_ID);
            node.send(item);
            Log.i(LOG_TAG, "Sent data to " + node.getId());
            Log.i(LOG_TAG, "Data: " + item.toXML());
            return Pair.create(Boolean.TRUE, "Sent data to " + node.getId());
        } catch (XMPPException | SmackException e) {
            Log.e(LOG_TAG, "Failed to send data to " + NODE_ID);
            Log.e(LOG_TAG, "Data: " + item.toXML());
            e.printStackTrace();
            return Pair.create(Boolean.FALSE, "Failed to send data to " + NODE_ID);
        }
    }

    private Timestamp getLastDataUploadTimestamp() {
        SharedPreferences loginSharedPref = getSharedPreferences(getString(R.string.login_storage),
                Context.MODE_PRIVATE);
        String timestamp = loginSharedPref.getString(getString(R.string.last_data_upload_time_key), null);
        Timestamp lastUploadedTimestamp = new Timestamp(timestamp);
        return lastUploadedTimestamp;
    }

    /**
     * Broadcast the result after sendDataItemsToSA()
     */
    private void broadcastSAUploadDataResult(String action, String message, Timestamp lastUploadedTimestamp) {
        Intent sendDataResultIntent = new Intent(action);
        sendDataResultIntent.putExtra(getString(R.string.sa_upload_data_result_message), message);
        sendDataResultIntent.putExtra(getString(R.string.last_data_upload_time_key), lastUploadedTimestamp.toString());
        LocalBroadcastManager.getInstance(this).sendBroadcast(sendDataResultIntent);
    }

    private String getUsername() {
        SharedPreferences loginSharedPref = getSharedPreferences(getString(R.string.login_storage),
                Context.MODE_PRIVATE);
        String username = loginSharedPref.getString(getString(R.string.username_key), null);
        assert username != null;
        return username;
    }

    private String getPassword() {
        SharedPreferences loginSharedPref = getSharedPreferences(getString(R.string.login_storage),
                Context.MODE_PRIVATE);
        String password = loginSharedPref.getString(getString(R.string.password_key), null);
        assert password != null;
        return password;
    }
}

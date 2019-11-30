package bosch.smartcampus.thermalcomfortstudy;

import android.app.Application;

import org.jivesoftware.smack.tcp.XMPPTCPConnection;

/**
 * {@link ThermalComfortStudyApplication} holds Sensor Andrew connection global variable.
 * This is just so that {@link bosch.smartcampus.thermalcomfortstudy.service.SAConnectionService} doesn't have to
 * create a new connection every time it needs to send data to Sensor Andrew.
 */
public class ThermalComfortStudyApplication extends Application {

    // This global Connection object will be destroyed when the process of this application is killed
    private XMPPTCPConnection mGlobalSAConnection;

    public void setGlobalSAConnection(XMPPTCPConnection connection) {
        mGlobalSAConnection = connection;
    }

    public XMPPTCPConnection getGlobalSAConnection() {
        return mGlobalSAConnection;
    }

    public boolean hasGlobalSAConnection() {
        return mGlobalSAConnection != null && mGlobalSAConnection.isConnected()
                && mGlobalSAConnection.isAuthenticated();
    }
}

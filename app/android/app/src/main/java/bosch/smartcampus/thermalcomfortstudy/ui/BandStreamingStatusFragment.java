package bosch.smartcampus.thermalcomfortstudy.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import bosch.smartcampus.thermalcomfortstudy.R;
import bosch.smartcampus.thermalcomfortstudy.service.BandStreamingService;

/**
 * {@link BandStreamingStatusFragment} is the visualization component for the status of band streaming.
 * It is responsible for showing the band streaming status according to {@link bosch.smartcampus.thermalcomfortstudy.service.BandStreamingService}.
 */
public class BandStreamingStatusFragment extends Fragment {

    // BandStreaming service and service connection
    private BandStreamingService mBandStreamingService;
    private boolean mBandStreamingServiceBound;
    private ServiceConnection mBandStreamingServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BandStreamingService.BandStreamingBinder binder = (BandStreamingService.BandStreamingBinder) service;
            mBandStreamingService = binder.getService();
            mBandStreamingServiceBound = true;
            displayCurrentBandConnectionStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBandStreamingServiceBound = false;
        }
    };

    private BroadcastReceiver mBandStreamingStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String bandConnMessage = intent.getStringExtra(context.getString(R.string.band_conn_message));

            if (isAdded()) {
                TextView bandStreamingStatusTV = (TextView) getActivity().findViewById(R.id.band_streaming_status);
                TextView bandConnMessageTV = (TextView) getActivity().findViewById(R.id.band_conn_message);

                if (action.equals(BandStreamingService.BAND_CONN_OK_ACTION)) {
                    bandStreamingStatusTV.setText(getString(R.string.band_streaming));
                } else {
                    bandStreamingStatusTV.setText(getString(R.string.band_not_streaming));
                }

                bandConnMessageTV.setText(bandConnMessage);
            }
        }
    };

    public BandStreamingStatusFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_band_streaming_status, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (isAdded()) {
            Intent bandStreamingIntent = new Intent(getActivity(), BandStreamingService.class);
            getActivity().bindService(bandStreamingIntent, mBandStreamingServiceConn, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter bandStreamingStatusIntentFilter = new IntentFilter();
        bandStreamingStatusIntentFilter.addAction(BandStreamingService.BAND_CONN_OK_ACTION);
        bandStreamingStatusIntentFilter.addAction(BandStreamingService.BAND_CONN_BAD_ACTION);
        bandStreamingStatusIntentFilter.addAction(BandStreamingService.BAND_DISCONN_OK_ACTION);
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(mBandStreamingStatusReceiver, bandStreamingStatusIntentFilter);

        displayCurrentBandConnectionStatus();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mBandStreamingStatusReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mBandStreamingServiceBound && isAdded()) {
            getActivity().unbindService(mBandStreamingServiceConn);
            mBandStreamingServiceBound = false;
        }
    }

    private void displayCurrentBandConnectionStatus() {
        if (mBandStreamingServiceBound && isAdded()) {
            TextView bandStreamingStatus = (TextView) getActivity().findViewById(R.id.band_streaming_status);
            TextView bandConnMessageTV = (TextView) getActivity().findViewById(R.id.band_conn_message);

            if (mBandStreamingService.isConnectedToBand()) {
                bandStreamingStatus.setText(getString(R.string.band_streaming));
            } else {
                bandStreamingStatus.setText(getString(R.string.band_not_streaming));
            }

            bandConnMessageTV.setText(mBandStreamingService.getBandConnectionMessage());
        }
    }
}

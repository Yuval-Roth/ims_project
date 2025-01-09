

package com.imsproject.watch.sensors;

import android.util.Log;

import androidx.annotation.NonNull;

import com.samsung.android.service.health.tracking.HealthTracker;
import com.samsung.android.service.health.tracking.data.DataPoint;
import com.samsung.android.service.health.tracking.data.ValueKey;
import com.imsproject.watch.R;

import java.util.List;

public class SpO2Listener extends BaseListener {
    private final static String APP_TAG = "SpO2Listener";

    SpO2Listener() {
        final HealthTracker.TrackerEventListener trackerEventListener = new HealthTracker.TrackerEventListener() {
            @Override
            public void onDataReceived(@NonNull List<DataPoint> list) {
                for (DataPoint data : list) {
                    updateSpo2(data);
                }
            }

            @Override
            public void onFlushCompleted() {
                Log.i(APP_TAG, " onFlushCompleted called");
            }

            @Override
            public void onError(HealthTracker.TrackerError trackerError) {
                Log.e(APP_TAG, " onError called: " + trackerError);
                setHandlerRunning(false);
                if (trackerError == HealthTracker.TrackerError.PERMISSION_ERROR) {
                    TrackerDataNotifier.getInstance().notifyError(R.string.NoPermission);
                }
                if (trackerError == HealthTracker.TrackerError.SDK_POLICY_ERROR) {
                    TrackerDataNotifier.getInstance().notifyError(R.string.SdkPolicyError);
                }
            }
        };
        setTrackerEventListener(trackerEventListener);
    }

    /*******************************************************************************************
     * [Practice 5] Read values from DataPoint object
     *  - Get blood oxygen level status
     *  - Get blood oxygen level value
     -------------------------------------------------------------------------------------------
     *  - (Hint) Replace TODO 5 with parts of code
     *      (1) remove SpO2Status.CALCULATING and
     *          set status from 'dataPoint' object using dataPoint.getValue(ValueKey.SpO2Set.STATUS)
     *      (2) set spo2Value from 'dataPoint' object using dataPoint.getValue(ValueKey.SpO2Set.SPO2)
     *          if status is 'SpO2Status.MEASUREMENT_COMPLETED'
     ******************************************************************************************/

    public void updateSpo2(DataPoint dataPoint) {

        //int status = SpO2Status.CALCULATING; //"TODO 5 (1)"
        int status = dataPoint.getValue(ValueKey.SpO2Set.STATUS);
        int spo2Value = 0;
        if(status == SpO2Status.MEASUREMENT_COMPLETED) {
            spo2Value = dataPoint.getValue(ValueKey.SpO2Set.SPO2);
        }
        //"TODO 5 (2)"

        TrackerDataNotifier.getInstance().notifySpO2TrackerObservers(status, spo2Value);
        Log.d(APP_TAG, dataPoint.toString());
    }

}

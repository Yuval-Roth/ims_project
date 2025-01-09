

package com.imsproject.watch.sensors;

import android.util.Log;

import androidx.annotation.NonNull;

import com.samsung.android.service.health.tracking.HealthTracker;
import com.samsung.android.service.health.tracking.data.DataPoint;
import com.samsung.android.service.health.tracking.data.ValueKey;
import com.imsproject.watch.R;

import java.util.List;

public class HeartRateListener extends BaseListener {
    private final static String APP_TAG = "HeartRateListener";

    HeartRateListener() {
        final HealthTracker.TrackerEventListener trackerEventListener = new HealthTracker.TrackerEventListener() {
            @Override
            public void onDataReceived(@NonNull List<DataPoint> list) {
                for (DataPoint dataPoint : list) {
                    readValuesFromDataPoint(dataPoint);
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

    public void readValuesFromDataPoint(DataPoint dataPoint) {
        final HeartRateData hrData = new HeartRateData();
        //"TODO 6 (1)"
        hrData.status = dataPoint.getValue(ValueKey.HeartRateSet.HEART_RATE_STATUS);
        //"TODO 6 (2)"
        hrData.hr = dataPoint.getValue(ValueKey.HeartRateSet.HEART_RATE);
        //"TODO 6 (3)"
        List<Integer> hrIbiList = dataPoint.getValue(ValueKey.HeartRateSet.IBI_LIST);
        //"TODO 6 (4)"
        final List<Integer> hrIbiStatus = dataPoint.getValue(ValueKey.HeartRateSet.IBI_STATUS_LIST);
        //"TODO 6 (5)"
        hrData.ibi = hrIbiList.get(hrIbiList.size()-1);
        //"TODO 6 (6)"
        hrData.qIbi = hrIbiStatus.get(hrIbiList.size()-1);
        TrackerDataNotifier.getInstance().notifyHeartRateTrackerObservers(hrData);
        Log.d(APP_TAG, dataPoint.toString());
    }

}

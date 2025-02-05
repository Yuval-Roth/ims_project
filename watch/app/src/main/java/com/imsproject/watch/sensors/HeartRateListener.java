

package com.imsproject.watch.sensors;

import android.util.Log;

import androidx.annotation.NonNull;

import com.imsproject.watch.R;
import com.samsung.android.service.health.tracking.HealthTracker;
import com.samsung.android.service.health.tracking.data.DataPoint;
import com.samsung.android.service.health.tracking.data.ValueKey;

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
        hrData.status = dataPoint.getValue(ValueKey.HeartRateSet.HEART_RATE_STATUS);
        hrData.hr = dataPoint.getValue(ValueKey.HeartRateSet.HEART_RATE);
        List<Integer> hrIbiList = dataPoint.getValue(ValueKey.HeartRateSet.IBI_LIST);
        final List<Integer> hrIbiStatus = dataPoint.getValue(ValueKey.HeartRateSet.IBI_STATUS_LIST);
        if (!hrIbiList.isEmpty()) {
            hrData.ibi = hrIbiList.get(hrIbiList.size()-1);
            hrData.qIbi = hrIbiStatus.get(hrIbiList.size() - 1);
        }
        else {
            hrData.qIbi = -1;
        }
        TrackerDataNotifier.getInstance().notifyHeartRateTrackerObservers(hrData);
//        Log.d(APP_TAG, dataPoint.toString());
    }

}

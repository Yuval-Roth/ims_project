
package com.imsproject.watch.sensors;

public interface TrackerDataObserver {
    void onHeartRateTrackerDataChanged(HeartRateData hrData);

    void onError(int errorResourceId);
}



package com.imsproject.watch.sensors;

import java.util.ArrayList;
import java.util.List;

public class TrackerDataNotifier {
    private static TrackerDataNotifier instance;

    private final List<TrackerDataObserver> observers = new ArrayList<>();

    public static TrackerDataNotifier getInstance() {
        if (instance == null) {
            instance = new TrackerDataNotifier();
        }
        return instance;
    }

    public void addObserver(TrackerDataObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(TrackerDataObserver observer) {
        observers.remove(observer);
    }

    public void notifyHeartRateTrackerObservers(HeartRateData hrData) {
        observers.forEach(observer -> observer.onHeartRateTrackerDataChanged(hrData));
    }

    public void notifySpO2TrackerObservers(int status, int spO2Value) {
        observers.forEach(observer -> observer.onSpO2TrackerDataChanged(status, spO2Value));
    }

    public void notifyError(int errorResourceId) {
        observers.forEach(observer -> observer.onError(errorResourceId));
    }
}



package com.imsproject.watch.sensors;

public class HeartRateData {
    public final static int IBI_QUALITY_SHIFT = 15;
    public final static int IBI_MASK = 0x1;
    public final static int IBI_QUALITY_MASK = 0x7FFF;

    int status = HeartRateStatus.HR_STATUS_NONE;
    int hr = 0;
    int ibi = 0;
    int qIbi = 1;

    HeartRateData() {

    }

    HeartRateData(int status, int hr, int ibi, int qIbi) {
        this.status = status;
        this.hr = hr;
        this.ibi = ibi;
        this.qIbi = qIbi;
    }

    int getHrIbi() {
        return (qIbi << IBI_QUALITY_SHIFT) | ibi;
    }
}

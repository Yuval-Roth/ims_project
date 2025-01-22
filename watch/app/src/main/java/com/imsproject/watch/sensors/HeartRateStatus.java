

package com.imsproject.watch.sensors;

public class HeartRateStatus {
    public static final int HR_STATUS_NONE = 0;
    public static final int HR_STATUS_FIND_HR = 1;
    public static final int HR_STATUS_ATTACHED = -1;
    public static final int HR_STATUS_DETECT_MOVE = -2;
    public static final int HR_STATUS_DETACHED = -3;
    public static final int HR_STATUS_LOW_RELIABILITY = -8;
    public static final int HR_STATUS_VERY_LOW_RELIABILITY = -10;
    public static final int HR_STATUS_NO_DATA_FLUSH = -99;
}

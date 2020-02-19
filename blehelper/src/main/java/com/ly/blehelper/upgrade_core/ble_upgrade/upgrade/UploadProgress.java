/**************************************************************************************************
 * Copyright 2016 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.ly.blehelper.upgrade_core.ble_upgrade.upgrade;

/**
 * <p>This class encapsulates the progress during the file upload.</p>
 * <p>It provides the current percentage and the corresponding elapsed time.</p>
 */
public class UploadProgress {

    /**
     * Represents the percentage of the file which has been uploaded on the Device.
     */
    private final double mPercentage;
    /**
     * Represents the time related to this progress.
     */
    private long mTime;

    /**
     * <p>Constructor to build an instance of this class.</p>
     *
     * @param percentage
     *        The percentage of the file which has already been uploaded on the Device. This value has to be between
     *        0 and 100 included.
     * @param time
     *        The time information corresponding to this progress. By default this time is understood as the elapsed
     *        time.
     */
    UploadProgress (double percentage, long time) {
        mPercentage = (percentage < 0) ? 0 : (percentage > 100) ? 100 : percentage;
        mTime = time < 0 ? 0 : time;
    }

    /**
     * To get the exact percentage of the file which has been uploaded on the Device.
     *
     * @return a value between 0 and 100 included.
     */
    public double getPercentage() {
        return mPercentage;
    }

    /**
     * To get the exact value of the remaining time.
     *
     * @return The exact value of the remaining time in ms.
     *
     * @deprecated As of 1.1.2, remaining time is not anymore part of the upgrade and replaced by the elapsed time.
     */
    public long getRemainingTime() {
        return mTime;
    }

    /**
     * To get the time value corresponding to this progress object.
     *
     * @return The exact value of the time in ms.
     */
    public long getTime() {
        return mTime;
    }

    /**
     * To set up the time value. This might be used to update this object if the time has changed.
     *
     * @param time
     *          The time to set up for time value.
     *
     * @since 1.1.2
     */
    public void setTime(long time) {
        mTime = time;
    }

}

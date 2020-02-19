package com.ly.blehelper.upgrade_core.ble_upgrade.annotation;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/10/25 15:06
 * version: 1.0
 */

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>The enumeration of all resume points.</p>
 */
@IntDef(flag = true, value = { Enums.DATA_TRANSFER, Enums.VALIDATION, Enums.TRANSFER_COMPLETE, Enums.IN_PROGRESS,
        Enums.COMMIT })
@Retention(RetentionPolicy.SOURCE)
@SuppressLint("ShiftFlags") // values are more readable this way
public @interface Enums {
    /**
     * This is the resume point "0", that means the upgrade will start from the beginning, the UPGRADE_START_DATA_REQ
     * request.
     */
    byte DATA_TRANSFER = 0x00;
    /**
     * This is the 1st resume point, that means the upgrade should resume from the UPGRADE_IS_CSR_VALID_DONE_REQ
     * request.
     */
    byte VALIDATION = 0x01;
    /**
     * This is the 2nd resume point, that means the upgrade should resume from the UPGRADE_TRANSFER_COMPLETE_RES request.
     */
    byte TRANSFER_COMPLETE = 0x02;
    /**
     * This is the 3rd resume point, that means the upgrade should resume from the UPGRADE_IN_PROGRESS_RES request.
     */
    byte IN_PROGRESS = 0x03;
    /**
     * This is the 4th resume point, that means the upgrade should resume from the UPGRADE_COMMIT_CFM confirmation request.
     */
    byte COMMIT = 0x04;
}

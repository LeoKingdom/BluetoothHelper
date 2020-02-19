package com.ly.blehelper.upgrade_core.ble_upgrade.annotation;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/10/25 15:12
 * version: 1.0
 */

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>All different errors which can occur when using the vumpgrade library.</p>
 */
@IntDef(flag = true, value = {ErrorTypes.ERROR_BOARD_NOT_READY, ErrorTypes.WRONG_DATA_PARAMETER, ErrorTypes
        .RECEIVED_ERROR_FROM_BOARD, ErrorTypes.EXCEPTION, ErrorTypes.AN_UPGRADE_IS_ALREADY_PROCESSING,
        ErrorTypes.NO_FILE })
@Retention(RetentionPolicy.SOURCE)
@SuppressLint("ShiftFlags") // values are more readable this way
public @interface ErrorTypes {

    /**
     * <p>This error occurs when there is an attempt to start the upgrade but the board is not ready to process.</p>
     */
    int ERROR_BOARD_NOT_READY = 1;
    /**
     * <p>This error occurs when a received VMU packet from the board does not match the expected data: too much
     * information, not enough, etc.</p>
     */
    int WRONG_DATA_PARAMETER = 2;
    /**
     * <p>This error occurs when the board notifies that an error or a warning occurs internally during its upgrade
     * process.</p>
     */
    int RECEIVED_ERROR_FROM_BOARD = 3;
    /**
     * <p>This error is reported when a {@link com.ly.bluetoothhelper.oat.upgrade.packet.VMUException VMUException} occurs during the process.</p>
     */
    int EXCEPTION = 4;
    /**
     * <p>This error occurs when there is an attempt to start the upgrade but the VMUManager is already processing an upgrade
     * .</p>
     */
    int AN_UPGRADE_IS_ALREADY_PROCESSING = 5;
    /**
     * <p>This error occurs when the file to upload is empty or does not exist.</p>
     */
    int NO_FILE = 6;
}

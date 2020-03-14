package com.ly.qcommesim.qcomm.annotation;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/10/25 15:04
 * version: 1.0
 */

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>All the types of confirmation this manager could request from the listener depending on the messages
 * received from the board.</p>
 */
@IntDef(flag = true, value = { ConfirmationType.TRANSFER_COMPLETE, ConfirmationType.COMMIT,  ConfirmationType
        .IN_PROGRESS, ConfirmationType.BATTERY_LOW_ON_DEVICE, ConfirmationType.WARNING_FILE_IS_DIFFERENT })
@Retention(RetentionPolicy.SOURCE)
@SuppressLint("ShiftFlags") // values are more readable this way
public @interface ConfirmationType {
    /**
     * <p>When the manager receives the
     * {@link com.ly.bluetoothhelper.oat.upgrade.codes.OpCodes.Enum#UPGRADE_TRANSFER_COMPLETE_IND
     * UPGRADE_TRANSFER_COMPLETE_IND} message, the board is asking for a confirmation to
     * {@link com.ly.bluetoothhelper.oat.upgrade.codes.OpCodes.UpgradeTransferCompleteRES.Action#CONTINUE CONTINUE}
     * or {@link com.ly.bluetoothhelper.oat.upgrade.codes.OpCodes.UpgradeTransferCompleteRES.Action#ABORT ABORT}  the
     * process.</p>
     */
    int TRANSFER_COMPLETE = 1;
    /**
     * <p>When the manager receives the
     * {@link com.ly.bluetoothhelper.oat.upgrade.codes.OpCodes.Enum#UPGRADE_COMMIT_REQ UPGRADE_COMMIT_REQ} message, the
     * board is asking for a confirmation to
     * {@link com.ly.bluetoothhelper.oat.upgrade.codes.OpCodes.UpgradeCommitCFM.Action#CONTINUE CONTINUE}
     * or {@link com.ly.bluetoothhelper.oat.upgrade.codes.OpCodes.UpgradeCommitCFM.Action#ABORT ABORT}  the process.</p>
     */
    int COMMIT = 2;
    /**
     * <p>When the resume point
     * {@link com.ly.bluetoothhelper.oat.upgrade.codes.ResumePoints.Enum#IN_PROGRESS IN_PROGRESS} is reached, the board
     * is expecting to receive a confirmation to
     * {@link com.ly.bluetoothhelper.oat.upgrade.codes.OpCodes.UpgradeInProgressRES.Action#CONTINUE CONTINUE}
     * or {@link com.ly.bluetoothhelper.oat.upgrade.codes.OpCodes.UpgradeInProgressRES.Action#ABORT ABORT} the process.</p>
     */
    int IN_PROGRESS = 3;
    /**
     * <p>When the Host receives
     * {@link com.ly.bluetoothhelper.oat.upgrade.codes.ReturnCodes.Enum#WARN_SYNC_ID_IS_DIFFERENT WARN_SYNC_ID_IS_DIFFERENT},
     * the listener has to ask if the upgrade should continue or not.</p>
     */
    int WARNING_FILE_IS_DIFFERENT = 4;
    /**
     * <p>>When the Host receives
     * {@link com.ly.bluetoothhelper.oat.upgrade.codes.ReturnCodes.Enum#ERROR_BATTERY_LOW ERROR_BATTERY_LOW},the
     * listener has to ask if the upgrade should continue or not.</p>
     */
    int BATTERY_LOW_ON_DEVICE = 5;
}

package com.ly.blehelper.upgrade_core.ble_upgrade.annotation;

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/10/25 14:39
 * version: 1.0
 */

    /**
     * <p>All types of messages which can be sent to an app linked to this service.</p>
     */
    @IntDef(flag = true, value = {MessageType.CONNECTION_STATE_HAS_CHANGED, MessageType.GAIA_SUPPORT,
            MessageType.UPGRADE_FINISHED, MessageType.UPGRADE_REQUEST_CONFIRMATION, MessageType.UPGRADE_SUPPORT,
            MessageType.UPGRADE_STEP_HAS_CHANGED, MessageType.UPGRADE_ERROR, MessageType.UPGRADE_UPLOAD_PROGRESS,
            MessageType.DEVICE_BOND_STATE_HAS_CHANGED, MessageType.RWCP_ENABLED, MessageType.TRANSFER_FAILED,
            MessageType.RWCP_SUPPORTED, MessageType.MTU_SUPPORTED, MessageType.MTU_UPDATED})
    @Retention(RetentionPolicy.SOURCE)
    @SuppressLint("ShiftFlags") // values are more readable this way
    public @interface MessageType {
        /**
         * <p>To inform that the connection state with the selected device has changed.</p>
         * <p>This type of {@link android.os.Message Message} also contains:</p>
         * <ul>
         * <li>The connection state of the device as: {@link State#CONNECTED
         * CONNECTED}, {@link State#CONNECTING CONNECTING},
         * {@link State#DISCONNECTING DISCONNECTING} or
         * {@link State#DISCONNECTED DISCONNECTED}. This information is contained
         * in <code>{@link android.os.Message#obj obj}</code>.</li>
         * </ul>
         */
        int CONNECTION_STATE_HAS_CHANGED = 0;
        /**
         * <p>To inform if the GAIA Service and required characteristics are supported by the device.</p>
         * <p>This type of {@link android.os.Message Message} also contains:</p>
         * <ul>
         * <li>The support of the feature as a value of {@link com.ly.bluetoothhelper.oat.annotation.Supports Support}.<br/>
         * This information is contained in <code>{@link android.os.Message#obj obj}</code>.</li>
         * </ul>
         */
        int GAIA_SUPPORT = 1;
        /**
         * <p>To inform if the GAIA commands for upgrading a Device are supported by the device.</p>
         * <p>This type of {@link android.os.Message Message} also contains:</p>
         * <ul>
         * <li>The support of the feature as a value of {@link com.ly.bluetoothhelper.oat.annotation.Supports Support}.<br/>
         * This information is contained in <code>{@link android.os.Message#obj obj}</code>.</li>
         * </ul>
         */
        int UPGRADE_SUPPORT = 2;
        /**
         * To inform that the upgrade process has successfully ended.
         */
        int UPGRADE_FINISHED = 3;
        /**
         * <p>To inform that the upgrade process needs he user to confirm the process to continue.</p>
         * <p>This type of {@link android.os.Message Message} also contains:</p>
         * <ul>
         * <li>{@link com.ly.bluetoothhelper.oat.upgrade.UpgradeManager.ConfirmationType ConfirmationType}
         * information contained in <code>{@link android.os.Message#obj obj}</code>.</li>
         * </ul>
         */
        int UPGRADE_REQUEST_CONFIRMATION = 4;
        /**
         * <p>To inform that a new step has been reached during the process.</p>
         * <p>This type of {@link android.os.Message Message} also contains:</p>
         * <ul>
         * <li>{@link com.ly.bluetoothhelper.oat.service.OtauBleService ResumePoints ResumePoints}
         * information contained in <code>{@link android.os.Message#obj obj}</code>.</li>
         * </ul>
         */
        int UPGRADE_STEP_HAS_CHANGED = 5;
        /**
         * <p>To inform that an error occurs during the upgrade process.</p>
         * <p>This type of {@link android.os.Message Message} also contains:</p>
         * <ul>
         * <li>{@link com.ly.bluetoothhelper.oat.upgrade.UpgradeError UpgradeError}
         * information contained in <code>{@link android.os.Message#obj obj}</code>.</li>
         * </ul>
         */
        int UPGRADE_ERROR = 6;
        /**
         * <p>To inform on the progress of the file upload.</p>
         * <p>This type of {@link android.os.Message Message} also contains:</p>
         * <ul>
         * <li>{@link com.ly.bluetoothhelper.oat.upgrade.UploadProgress UploadProgress}
         * information contained in <code>{@link android.os.Message#obj obj}</code>.</li>
         * </ul>
         */
        int UPGRADE_UPLOAD_PROGRESS = 7;
        /**
         * <p>To inform that the device bond state has changed.</p>
         * <p>This type of {@link android.os.Message Message} also contains:</p>
         * <ul>
         * <li>the bond state value which can be one of the following:
         * {@link android.bluetooth.BluetoothDevice#BOND_BONDED BOND_BONDED}, {@link android.bluetooth.BluetoothDevice#BOND_BONDING BOND_BONDING} and
         * {@link android.bluetooth.BluetoothDevice#BOND_NONE BOND_NONE}.<br/>This information is contained in
         * <code>{@link android.os.Message#obj obj}</code>.</li>
         * </ul>
         */
        int DEVICE_BOND_STATE_HAS_CHANGED = 8;
        /**
         * <p>To inform if RWCP mode is supported by the device.</p>
         * <p>This type of {@link android.os.Message Message} also contains:</p>
         * <ul>
         * <li>The support of the feature as a boolean: true if the feature is supported, false otherwise.
         * This information is contained in <code>{@link android.os.Message#obj obj}</code>.</li>
         * </ul>
         */
        int RWCP_SUPPORTED = 9;
        /**
         * <p>To inform if RWCP mode is enabled by the device.</p>
         * <p>This type of {@link android.os.Message Message} also contains:</p>
         * <ul>
         * <li>The support of the feature as a boolean: true if the feature is enabled, false otherwise.
         * This information is contained in <code>{@link android.os.Message#obj obj}</code>.</li>
         * </ul>
         */
        int RWCP_ENABLED = 10;
        /**
         * <p>To inform that the transfer of bytes during the transfer has failed.</p>
         * <p>The upgrade is aborted.</p>
         */
        int TRANSFER_FAILED = 11;
        /**
         * <p>To inform if MTU size is supported by the device.</p>
         * <p>This type of {@link android.os.Message Message} also contains:</p>
         * <ul>
         * <li>The support of the feature as a boolean: true if the feature is supported, false otherwise.
         * This information is contained in <code>{@link android.os.Message#obj obj}</code>.</li>
         * </ul>
         */
        int MTU_SUPPORTED = 12;
        /**
         * <p>To inform about the new MTU size which had been negotiated with the device..</p>
         * <p>This type of {@link android.os.Message Message} also contains:</p>
         * <ul>
         * <li>The value of the new size as an <code>int</code>.
         * This information is contained in <code>{@link android.os.Message#obj obj}</code>.</li>
         * </ul>
         */
        int MTU_UPDATED = 13;
    }


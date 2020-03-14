/**************************************************************************************************
 * Copyright 2017 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.ly.qcommesim.qcomm.gaia;

import android.util.Log;

import com.ly.qcommesim.qcomm.annotation.ConfirmationType;
import com.ly.qcommesim.qcomm.annotation.Enums;
import com.ly.qcommesim.qcomm.annotation.ErrorTypes;
import com.ly.qcommesim.qcomm.gaia.exceptions.GaiaException;
import com.ly.qcommesim.qcomm.gaia.packets.GaiaPacket;
import com.ly.qcommesim.qcomm.gaia.packets.GaiaPacketBLE;
import com.ly.qcommesim.qcomm.rwcp.RWCP;
import com.ly.qcommesim.qcomm.upgrade.UpgradeError;
import com.ly.qcommesim.qcomm.upgrade.UpgradeManager;
import com.ly.qcommesim.qcomm.upgrade.UploadProgress;
import com.ly.qcommesim.qcomm.utils.Consts;

import java.io.File;

/**
 * <p>This class follows the GAIA protocol. It manages all messages which are sent and received over the protocol.</p>
 * <p>For all GAIA commands used in this class, the Vendor ID is always
 * {@link GAIA#VENDOR_QUALCOMM}.</p>
 */
public class GaiaUpgradeManager extends GaiaManager implements UpgradeManager.UpgradeManagerListener {

    // ====== PRIVATE FIELDS =======================================================================

    /**
     * <p>The tag to display for logs.</p>
     */
    private final String TAG = "GaiaUpgradeManager";
    /**
     * <p>The listener which implements the GaiaManagerListener interface to allow this manager to communicate with a
     * device.</p>
     */
    private final GaiaManagerListener mListener;
    /**
     * <p>The manager for the upgrade process.</p>
     */
    private final UpgradeManager mUpgradeManager = new UpgradeManager(this);
    /**
     * <p>To now if the sent commands were to know if the command is available.</p>
     */
    private boolean isTestingCommands = false;
    /**
     * To know if RWCP is enabled.
     */
    private boolean mIsRWCPEnabled = false;
    /**
     * To get the maximum size a payload can be.
     */
    private int mPayloadSizeMax = GaiaPacketBLE.MAX_PAYLOAD;


    // ====== CONSTRUCTOR ==========================================================================

    /**
     * <p>Main constructor of this class which allows to initialise a listener to send messages to a device or dispatch
     * any GAIA received messages.</p>
     *
     * @param myListener
     *         An object which implements the {@link GaiaManagerListener MyGaiaManagerListener} interface.
     */
    public GaiaUpgradeManager(GaiaManagerListener myListener) {
        super(GAIA.Transport.BLE);
        this.mListener = myListener;
        this.showDebugLogs(Consts.DEBUG);
        mUpgradeManager.showDebugLogs(Consts.DEBUG);
    }


    // ====== PUBLIC METHODS =======================================================================

    /**
     * <p>To start the VM Upgrade process by sending the {@link GAIA#COMMAND_VM_UPGRADE_CONNECT
     * COMMAND_VM_UPGRADE_CONNECT} command and registering for the
     * {@link GAIA.NotificationEvents#VMU_PACKET VMU_PACKET} event. Once this step has be done
     * and acknowledged, this manager asks to the VMU Manager to start its process.</p>
     *
     */
    public void startUpgrade(File file) {
        if (!mUpgradeManager.isUpgrading()) {
            mUpgradeManager.setFile(file);
            sendUpgradeConnect();
        }
    }

    /**
     * <p>Once the GAIA connection over Bluetooth is ready, this method has to be called to inform this manager it
     * can start to communicate with a GAIA device.</p>
     * <p>This method will resume any VMU process already started.</p>
     */
    public void onGAIAConnectionReady() {
        if (mUpgradeManager.isUpgrading()) {
            if (mIsRWCPEnabled) {
                setRWCPMode(true);
            }
            registerNotification(GAIA.NotificationEvents.VMU_PACKET);
            sendUpgradeConnect();
        }
        else {
            isTestingCommands = true;
            sendUpgradeConnect();
        }
    }

    /**
     * <p>To abort an ongoing upgrade.</p>
     */
    public void abortUpgrade() {
        mUpgradeManager.abortUpgrade();
    }

    /**
     * <p>To get the current {@link ResumePoints ResumePoint} of the upgrade.</p>
     *
     * @return The corresponding ResumePoint. If there is no ongoing upgrade the given ResumePoint is not accurate.
     */
    public @Enums
    int getResumePoint() {
        return mUpgradeManager.getResumePoint();
    }

    /**
     * <p>To give an answer to the {@link UpgradeManager UpgradeManager} about a confirmation it is waiting for to
     * go on the upgrade.</p>
     *
     * @param type
     *              The type of confirmation the UpgradeManager asked for.
     * @param confirmation
     *              To know if the UpgradeManager should confirm the process to continue.
     */
    public void sendConfirmation(@ConfirmationType int type, boolean confirmation) {
        if (mUpgradeManager.isUpgrading()) {
            mUpgradeManager.sendConfirmation(type, confirmation);
        }
    }

    /**
     * <p>To turn on or off the RWCP mode on the remote device. This is done using the GAIA command
     * {@link GAIA#COMMAND_SET_DATA_ENDPOINT_MODE COMMAND_SET_DATA_ENDPOINT_MODE}.</p>
     * <p>If the use of the command is not successful, the listener get an information through the method
     * {@link GaiaManagerListener#onRWCPNotSupported() onRWCPNotSupported}.</p>
     *
     * @param enabled
     *          True to enable the RWCP mode, false to disable it.
     */
    public void setRWCPMode(boolean enabled) {
        byte mode = enabled ? TransferModes.MODE_RWCP : TransferModes.MODE_NONE;
        mIsRWCPEnabled = enabled;
        byte[] RWCPMode = {mode};
        sendSetDataEndPointMode(RWCPMode);
    }

    /**
     * <p>To get the current status of RWCP: is it enabled or disabled. This is done using the GAIA command
     * {@link GAIA#COMMAND_GET_DATA_ENDPOINT_MODE COMMAND_GET_DATA_ENDPOINT_MODE}.</p>
     * <p>If the use of the command is successful, the listener gets the result of this request through the method
     * {@link GaiaManagerListener#onRWCPEnabled(boolean) onRWCPEnabled}.</p>
     * <p>If the use of the command is not successful, the listener get an information through the method
     * {@link GaiaManagerListener#onRWCPNotSupported() onRWCPNotSupported}.</p>
     */
    public void getRWCPStatus() {
        sendGetDataEndPointMode();
    }

    /**
     * <p>This method gives the current known activation of RWCP.</p>
     *
     * @return true if RWCP is known as enabled.
     */
    public boolean isRWCPEnabled() {
        return mIsRWCPEnabled;
    }

    /**
     * <p>Called when the RWCP transfer mode had been set up using the GAIA commands but the transport layer cannot get
     * it successfully activated.</p>
     */
    public void onRWCPNotSupported() {
        mIsRWCPEnabled = false;
    }

    /**
     * <p>To know if there is an upgrade which is going on.</p>
     *
     * @return true if there is an upgrade working, false otherwise.
     */
    public boolean isUpdating() {
        return mUpgradeManager.isUpgrading();
    }

    /**
     * <p>This method must be called when the transmission of the file transfer packets are transported through another
     * protocol instead of the default GAIA transport. Here the default GAIA transport is BLE with the GAIA Service.</p>
     */
    public void onTransferFinished() {
        mUpgradeManager.onSuccessfulTransmission();
    }


    // ====== IMPLEMENTED METHODS =======================================================================

    /**
     * implements, {@link UpgradeManager}
     * @see com.ly.bluetoothhelper.oat.upgrade.UpgradeManager.UpgradeManagerListener
     * */
    @Override
    public void sendUpgradePacket(byte[] bytes, boolean isTransferringData) {
        sendUpgradeControl(bytes, isTransferringData);
    }

    // implements UpgradeManager.UpgradeManagerListener
    @Override
    public void onUpgradeProcessError(UpgradeError error) {
        mListener.onUpgradeError(error);
        switch (error.getError()) {
            case ErrorTypes.AN_UPGRADE_IS_ALREADY_PROCESSING:
            case ErrorTypes.NO_FILE:
                break;
            case ErrorTypes.ERROR_BOARD_NOT_READY:
            case ErrorTypes.EXCEPTION:
            case ErrorTypes.RECEIVED_ERROR_FROM_BOARD:
            case ErrorTypes.WRONG_DATA_PARAMETER:
                mUpgradeManager.abortUpgrade();
                break;
        }
    }

    // implements UpgradeManager.UpgradeManagerListener
    @Override
    public void onResumePointChanged(@Enums int point) {
        mListener.onResumePointChanged(point);
    }

    // implements UpgradeManager.UpgradeManagerListener
    @Override
    public void onUpgradeFinished() {
        mListener.onUpgradeFinish();
        disconnectUpgrade();
    }

    // implements UpgradeManager.UpgradeManagerListener
    @Override
    public void onFileUploadProgress(UploadProgress progress) {
        mListener.onUploadProgress(progress);
    }

    // implements UpgradeManager.UpgradeManagerListener
    @Override
    public void askConfirmationFor(@ConfirmationType int type) {
        mListener.askConfirmationFor(type);
    }

    // implements UpgradeManager.UpgradeManagerListener
    @Override
    public void disconnectUpgrade() {
        cancelNotification(GAIA.NotificationEvents.VMU_PACKET);
        sendUpgradeDisconnect();
    }

    @Override // extends GaiaManager
    public void reset() {
        super.reset();
        mIsRWCPEnabled = false;
    }


    // ====== PRIVATE METHODS - SENDING =============================================================

    /**
     * <p>To send a {@link GAIA#COMMAND_VM_UPGRADE_CONNECT COMMAND_VM_UPGRADE_CONNECT} packet.</p>
     */
    private void sendUpgradeConnect() {
        GaiaPacket packet = new GaiaPacketBLE(GAIA.VENDOR_QUALCOMM, GAIA.COMMAND_VM_UPGRADE_CONNECT);
        createRequest(packet);
    }

    /**
     * <p>To send a {@link GAIA#COMMAND_VM_UPGRADE_DISCONNECT COMMAND_VM_UPGRADE_DISCONNECT} packet.</p>
     */
    private void sendUpgradeDisconnect() {
        GaiaPacket packet = new GaiaPacketBLE(GAIA.VENDOR_QUALCOMM, GAIA.COMMAND_VM_UPGRADE_DISCONNECT);
        createRequest(packet);
    }

    /**
     * <p>To send a {@link GAIA#COMMAND_VM_UPGRADE_CONTROL COMMAND_VM_UPGRADE_CONTROL} packet. That packet contains
     * the bytes of a VM Upgrade packet.</p>
     *
     * @param payload
     *              The bytes which corresponds to a {@link com.ly.bluetoothhelper.oat.upgrade.packet.VMUPacket VMUPacket}.
     * @param isTransferringData
     *              True if the packet corresponds to the transfer of the file data.
     *
     */
    private void sendUpgradeControl(byte[] payload, boolean isTransferringData) {
        if (isTransferringData && mIsRWCPEnabled) {
            GaiaPacket packet = new GaiaPacketBLE(GAIA.VENDOR_QUALCOMM, GAIA.COMMAND_VM_UPGRADE_CONTROL, payload);
            try {
                byte[] bytes = packet.getBytes();
                if (!mListener.sendGAIAPacket(bytes, true)) {
                    Log.w(TAG, "Fail to send GAIA packet for GAIA command: " + packet.getCommandId());
                    onSendingFailed(packet);
                }
            }
            catch (GaiaException e) {
                Log.w(TAG, "Exception when attempting to create GAIA packet: " + e.toString());
            }
        }
        else {
            GaiaPacket packet = new GaiaPacketBLE(GAIA.VENDOR_QUALCOMM, GAIA.COMMAND_VM_UPGRADE_CONTROL, payload);
            createRequest(packet);
        }
    }

    /**
     * <p>To send a {@link GAIA#COMMAND_SET_DATA_ENDPOINT_MODE COMMAND_SET_DATA_ENDPOINT_MODE} packet with the given
     * payload.</p>
     * <p>This command is used to set up the transfer mode to use with the GAIA DATA ENDPOINT GATT characteristic
     * when connected through a GATT connection.</p>
     *
     * @param payload
     *          The payload to send with the command SET DATA ENDPOINT MODE.
     */
    private void sendSetDataEndPointMode(byte[] payload) {
        GaiaPacket packet = new GaiaPacketBLE(GAIA.VENDOR_QUALCOMM, GAIA.COMMAND_SET_DATA_ENDPOINT_MODE, payload);
        createRequest(packet);
    }

    /**
     * <p>To send a {@link GAIA#COMMAND_GET_DATA_ENDPOINT_MODE COMMAND_GET_DATA_ENDPOINT_MODE} packet.</p>
     * <p>This command is used to get the transfer mode used with the GAIA DATA ENDPOINT GATT characteristic
     * when connected through a GATT connection.</p>
     */
    private void sendGetDataEndPointMode() {
        GaiaPacket packet = new GaiaPacketBLE(GAIA.VENDOR_QUALCOMM, GAIA.COMMAND_GET_DATA_ENDPOINT_MODE);
        createRequest(packet);
    }

    /**
     * <p>To register for a {@link GAIA.NotificationEvents GAIA event notification}.</p>
     *
     * @param event
     *              The event to register with.
     */
    @SuppressWarnings("SameParameterValue") // the parameter is always VMU_PACKET in this application
    private void registerNotification (@GAIA.NotificationEvents int event) {
        try {
            GaiaPacket packet = GaiaPacketBLE.buildGaiaNotificationPacket(GAIA.VENDOR_QUALCOMM, GAIA
                    .COMMAND_REGISTER_NOTIFICATION, event, null, GAIA.Transport.BLE);
            createRequest(packet);
        } catch (GaiaException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * <p>To cancel a {@link GAIA.NotificationEvents GAIA event notification}.</p>
     *
     * @param event
     *              The notification event to cancel.
     */
    @SuppressWarnings("SameParameterValue") // the parameter is always VMU_PACKET in this application
    private void cancelNotification (@GAIA.NotificationEvents int event) {
        try {
            GaiaPacket packet = GaiaPacketBLE.buildGaiaNotificationPacket(GAIA.VENDOR_QUALCOMM, GAIA
                    .COMMAND_CANCEL_NOTIFICATION, event, null, GAIA.Transport.BLE);
            createRequest(packet);
        } catch (GaiaException e) {
            Log.e(TAG, e.getMessage());
        }
    }


    // ====== PRIVATE METHODS - RECEIVING =============================================================

    /**
     * <p>To manage a received {@link GaiaPacket} which has {@link GAIA#COMMAND_EVENT_NOTIFICATION} for command.</p>
     * <p>This manager is only interested by the
     * {@link GAIA.NotificationEvents#VMU_PACKET VMU_PACKET} event to manage a VM Upgrade.</p>
     *
     * @param packet
     *              The receive notification event packet.
     *
     * @return
     *          true if an acknowledgement has been sent.
     */
    private boolean receiveEventNotification(GaiaPacket packet) {
        byte[] payload = packet.getPayload();

        if (payload.length > 0) {
            @GAIA.NotificationEvents int event = packet.getEvent();
            if (event == GAIA.NotificationEvents.VMU_PACKET && mUpgradeManager != null) {
                createAcknowledgmentRequest(packet, GAIA.Status.SUCCESS, null);
                byte[] data = new byte[payload.length - 1];
                System.arraycopy(payload, 1, data, 0, payload.length - 1);
                mUpgradeManager.receiveVMUPacket(data);
                return true;
            }
            else {
                // not supported
                return false;
            }
        }
        else {
            createAcknowledgmentRequest(packet, GAIA.Status.INVALID_PARAMETER, null);
            return true;
        }
    }


    // ====== PROTECTED METHODS ====================================================================

    // extends GaiaManager
    @Override
    protected void receiveSuccessfulAcknowledgement(GaiaPacket packet) {
        switch (packet.getCommand()) {
            case GAIA.COMMAND_VM_UPGRADE_CONNECT:
                if (isTestingCommands) {
                    sendUpgradePacket(new byte[0], false);
                }
                else {
                    if (mUpgradeManager.isUpgrading()) {
                        mUpgradeManager.resumeUpgrade();
                    }
                    else {
                        // The size of the Upgrade packets depends on the support of RWCP which uses some bytes
                        // For the data transfer it is better to have an even number of bytes sent.
                        int size = mPayloadSizeMax;
                        if (mIsRWCPEnabled) {
                            size = mPayloadSizeMax - RWCP.Segment.REQUIRED_INFORMATION_LENGTH;
                            size = (size%2 == 0) ? size : size-1;
                        }
                        mUpgradeManager.startUpgrade(size, isRWCPEnabled());
                    }
                }
                break;
            case GAIA.COMMAND_REGISTER_NOTIFICATION:
            case GAIA.COMMAND_CANCEL_NOTIFICATION:
            case GAIA.COMMAND_EVENT_NOTIFICATION:
                /* we assume that if the VM_UPGRADE commands are supported the NOTIFICATION ones also are as there
                are necessary for the Device to communicate with the Host.*/
                break;
            case GAIA.COMMAND_VM_UPGRADE_DISCONNECT:
                if (isTestingCommands) {
                    isTestingCommands = false;
                    mListener.onUpgradeSupported(true);
                }
                else {
                    mUpgradeManager.onUpgradeDisconnected();
                    mListener.onVMUpgradeDisconnected();
                }
                break;
            case GAIA.COMMAND_VM_UPGRADE_CONTROL:
                mUpgradeManager.onSuccessfulTransmission();
                if (isTestingCommands) {
                    sendUpgradeDisconnect();
                }
                break;
            case GAIA.COMMAND_SET_DATA_ENDPOINT_MODE:
                mListener.onRWCPEnabled(mIsRWCPEnabled);
                break;
            case GAIA.COMMAND_GET_DATA_ENDPOINT_MODE:
                final int MODE_OFFSET = 1;
                byte mode = packet.getPayload()[MODE_OFFSET];
                mIsRWCPEnabled = mode == TransferModes.MODE_RWCP;
                mListener.onRWCPEnabled(mIsRWCPEnabled);
                break;
        }

    }

    // extends GaiaManager
    @Override
    protected void receiveUnsuccessfulAcknowledgement(GaiaPacket packet) {
        if (isTestingCommands && packet.getStatus() == GAIA.Status.NOT_SUPPORTED) {
            isTestingCommands = false;
            mListener.onUpgradeSupported(false);
        }
        else if (isTestingCommands && packet.getCommand() == GAIA.COMMAND_VM_UPGRADE_CONNECT) {
            sendUpgradePacket(new byte[0], false);
        }
        else if (packet.getCommand() == GAIA.COMMAND_VM_UPGRADE_CONNECT
                || packet.getCommand() == GAIA.COMMAND_VM_UPGRADE_CONTROL) {
            sendUpgradeDisconnect();
        }
        else if (isTestingCommands && packet.getCommand() == GAIA.COMMAND_VM_UPGRADE_DISCONNECT) {
            isTestingCommands = false;
            mListener.onUpgradeSupported(true);
        }
        else if (packet.getCommand() == GAIA.COMMAND_SET_DATA_ENDPOINT_MODE
                || packet.getCommand() == GAIA.COMMAND_GET_DATA_ENDPOINT_MODE) {
            mIsRWCPEnabled = false;
            mListener.onRWCPNotSupported();
        }
    }

    // extends GaiaManager
    @Override
    protected void hasNotReceivedAcknowledgementPacket(GaiaPacket packet) {
        if (isTestingCommands) {
            isTestingCommands = false;
            mListener.onUpgradeSupported(false);
        }
        else if (packet.getCommand() == GAIA.COMMAND_DISCONNECT) {
            mListener.onVMUpgradeDisconnected();
        }
    }

    // extends GaiaManager
    @Override
    protected boolean manageReceivedPacket(GaiaPacket packet) {
        switch (packet.getCommand()) {
            case GAIA.COMMAND_EVENT_NOTIFICATION:
                return receiveEventNotification(packet);
        }

        return false;
    }

    // extends GaiaManager
    @Override
    protected boolean sendGAIAPacket(byte[] packet) {
        return mListener.sendGAIAPacket(packet, false);
    }

    // extends GaiaManager
    @Override
    protected void onSendingFailed(GaiaPacket packet) {
    }

    /**
     * <p>To set up the maximum size of a GAIA packet.</p>
     *
     * @param packetSize The maximum size.
     */
    public void setPacketMaximumSize(int packetSize) {
        this.mPayloadSizeMax = packetSize - GaiaPacketBLE.MIN_PACKET_LENGTH;
    }


    // ====== INTERFACES ===========================================================================

    /**
     * <p>This interface allows this manager to dispatch messages or event to a listener.</p>
     */
    public interface GaiaManagerListener {

        /**
         * <p>This method is called to inform the listener that the VM Upgrade process has been disconnected.</p>
         * <p>This method is called if a {@link GAIA#COMMAND_DISCONNECT COMMAND_DISCONNECT} packet has been sent to
         * the device. Before to be called, the manager waits for any ACk to the command or a time out.</p>
         */
        @SuppressWarnings("EmptyMethod") // this method is empty for some of its implementation
        void onVMUpgradeDisconnected();

        /**
         * <p>This method is called when there is a progress on the steps during the upgrade process.</p>
         *
         * @see ResumePoints
         *
         * @param point
         *              the new step reaches by the process.
         */
        void onResumePointChanged(@Enums int point);

        /**
         * <p>This method informs the listener that an error occurs during the upgrade process.</p>
         * <p>For the following error types, the upgrade is automatically aborted:
         * <ul>
         *     <li>{@link ErrorTypes#EXCEPTION EXCEPTION}</li>
         *     <li>{@link ErrorTypes#RECEIVED_ERROR_FROM_BOARD RECEIVED_ERROR_FROM_BOARD}</li>
         *     <li>{@link ErrorTypes#WRONG_DATA_PARAMETER WRONG_DATA_PARAMETER}</li>
         * </ul></p>
         *
         * @param error
         *          All the information relative to the occurred error.
         */
        void onUpgradeError(UpgradeError error);

        /**
         * <p>This method is called when there is a progress during the upload of the file on the device.</p>
         * <p>This method is used when the actual step is
         * {@link Enums#DATA_TRANSFER DATA_TRANSFER}.</p>
         *
         * @param progress
         *          A progress object which contains the percentage of how many bytes of the file have been sent to
         *          the Board and an estimation of how long this manager will need to upload the rest of the file.
         */
        void onUploadProgress(UploadProgress progress);

        /**
         * <p>To transmit the bytes of a GAIA packet to a connected device.</p>
         *
         * @param packet
         *          The byte array to send to a device.
         *
         * @return
         *          true if the sending could be done.
         */
        boolean sendGAIAPacket(byte[] packet, boolean isTransferringData);

        /**
         * <p>To inform any listener that the upgrade has ended successfully. This method is called after the last
         * Upgrade packet has been sent by the Device to inform that it has been successfully upgraded.</p>
         */
        void onUpgradeFinish();

        /**
         * <p>This method is called when the manager needs the listener to confirm any action before to continue the
         * process.</p>  当manager需要侦听器在继续进程之前确认任何操作时，将调用此方法。(调用sendConfirmation(int, boolean)后的回调方法)
         * <p>To inform the manager about its decision, the listener has to call the
         * {@link GaiaUpgradeManager#sendConfirmation(int, boolean) sendConfirmation} method.</p>
         */
        void askConfirmationFor(@ConfirmationType int type);

        /**
         * <p>This method is called when the {@link GaiaUpgradeManager GaiaUpgradeManager} knows that all commands
         * related to the upgrade process are supported by the Device.</p>
         *
         * @param supported
         *                  Return trues if all commands are supported, returns false if not all commands are supported.
         *
         *设备支持升级所需要的所有命令时调用这个方法
         */
        void onUpgradeSupported(boolean supported);

        /**
         * <p>This method is called when this manager knows if the RWCP mode is enabled - and supported - on the
         * connected device.</p>
         * 蓝牙设备是否支持并开启RWCP模式时调用此方法
         * <p>This method is called when one of the following happens:
         * 当设置了COMMAND_GET_DATA_ENDPOINT_MODE 或者COMMAND_SET_DATA_ENDPOINT_MODE时被认为支持RWCP,否则不支持,即返回true,否则false
         * <ul>
         *     <li>The commands {@link GAIA#COMMAND_GET_DATA_ENDPOINT_MODE COMMAND_GET_DATA_ENDPOINT_MODE} and
         *      {@link GAIA#COMMAND_SET_DATA_ENDPOINT_MODE COMMAND_SET_DATA_ENDPOINT_MODE} are not supported, RWCP
         *      is assumed as not being supported by the device.</li>
         *
         *      <li>When this manager gets a successful acknowledgement to the command
         *      {@link GAIA#COMMAND_GET_DATA_ENDPOINT_MODE COMMAND_GET_DATA_ENDPOINT_MODE} which gives the
         *      current enabled transfer mode. If it is {@link TransferModes#MODE_RWCP MODE_RWCP}, this method
         *      returns true, false otherwise.</li>
         * </ul></p>
         *
         * @param enabled
         *          True if RWCP transfer mode is assumed supported and enabled by the device. False otherwise.
         */
        void onRWCPEnabled(boolean enabled);

        /**
         * <p>Called when the RWCP transfer mode couldn't be successfully set up through the command
         * {@link GAIA#COMMAND_SET_DATA_ENDPOINT_MODE COMMAND_SET_DATA_ENDPOINT_MODE}.</p>
         *
         * 设置COMMAND_SET_DATA_ENDPOINT_MODE失败时调用此方法
         */
        void onRWCPNotSupported();
    }


    // ====== INNER CLASSES ===========================================================================

    /**
     * <p>This class of constants represents all data transfer modes values which can be sent and received through the
     * commands : {@link GAIA#COMMAND_GET_DATA_ENDPOINT_MODE COMMAND_GET_DATA_ENDPOINT_MODE} and
     * {@link GAIA#COMMAND_SET_DATA_ENDPOINT_MODE COMMAND_SET_DATA_ENDPOINT_MODE}.</p>
     */
    private static final class TransferModes {
        /**
         * <p>Sending this value enables the RWCP transfer mode. Receiving it means it is activated.</p>
         */
        private static final byte MODE_RWCP = (byte) 0x01;
        /**
         * <p>Sending this value disables all transfer mode and activates the default one for data transfer.</p>
         */
        private static final byte MODE_NONE = (byte) 0x00;
    }

}

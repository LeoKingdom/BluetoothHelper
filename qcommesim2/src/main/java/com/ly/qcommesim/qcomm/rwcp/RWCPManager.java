/**************************************************************************************************
 * Copyright 2017 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.ly.qcommesim.qcomm.rwcp;

import android.os.Handler;
import android.util.Log;

import com.ly.qcommesim.qcomm.utils.Utils;

import java.util.LinkedList;

/**
 * <p>This class implements the Reliable Write Command Protocol - RWCP. It manages RWCP segments and the
 * encapsulation of data into a RWCP segment.</p>
 * <p>For any byte array known as a potential RWCP segment, it is passed to this manager using
 * {@link #onReceiveRWCPSegment(byte[]) onReceiveRWCPSegment}. This method will then analyze the array in order to
 * get a {@link Segment Segment}. Then depending on the {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.OpCode opration code}
 * and the current {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.State state} it handles the received segment.</p>
 * <p>This manager provides a {@link #sendData(byte[]) sendData} method in order to send a byte array to a connected
 * device using RWCP. If a session is already running, this method queues up the messages.</p>
 * <p>The transfer should be cancelled using {@link #cancelTransfer() cancelTransfer} when the device is
 * disconnected.</p>
 *
 * @since 2.0.3
 */
public class RWCPManager {

    // ====== FIELDS ====================================================================

    /**
     * <p>The tag to display for logs.</p>
     */
    private final String TAG = "RWCPManager";
    /**
     * <p>The listener to communicate with the application and send packets.</p>
     */
    private final RWCPListener mListener;
    /**
     * The sequence number of the last sequence which had been acknowledged by the device.
     */
    private int mLastAckSequence = -1;
    /**
     * The next sequence number which will be send.
     */
    private int mNextSequence = 0;
    /**
     * The window represents the maximum number of segments which can be sent simultaneously.
     */
    private int mWindow = RWCP.WINDOW_MAX;
    /**
     * The credit number represents the number of segments which can still be send to fill the current window.
     */
    private int mCredits = 0;
    /**
     * When receiving a GAP or when an operation is timed out, this manager resends the unacknowledged data and stops
     * any other running operation.
     */
    private boolean mIsResendingSegments = false;
    /**
     * The state of the manager.
     */
    private @RWCP.State int mState = RWCP.State.LISTEN;
    /**
     * The queue of data which are waiting to be sent.
     */
    private LinkedList<byte[]> mPendingData = new LinkedList<>();
    /**
     * The queue of segments which have been sent but have not been acknowledged yet.
     */
    private final LinkedList<Segment> mUnacknowledgedSentSegments = new LinkedList<>();
    /**
     * The runnable used to time out the segments which have been sent but not acknowledged.
     */
    private TimeOutRunnable mTimeOutRunnable = new TimeOutRunnable();
    /**
     * To know if a time out is running.
     */
    private boolean isTimeOutRunning = false;
    /**
     * <p>The main handler to run tasks.</p>>
     */
    private final Handler mHandler = new Handler();
    /**
     * The time used to time out the DATA segments.
     */
    private int mDataTimeOutMs = RWCP.DATA_TIMEOUT_MS_DEFAULT;
    /**
     * <p>To show the debug logs indicating when a method had been reached.</p>
     */
    private boolean mShowDebugLogs = false;
    /**
     * To know the number of segments which had been acknowledged in a row with DATA_ACK.
     */
    private int mAcknowledgedSegments = 0;


    // ====== CONSTRUCTORS ====================================================================

    /**
     * <p>Main constructor of this class which allows initialisation of a listener to send messages to a device or
     * dispatch any received RWCP messages.</p>
     *
     * @param listener
     *            An object which implements the {@link RWCPListener} interface.
     */
    public RWCPManager(RWCPListener listener) {
        mListener = listener;
    }


    // ====== PUBLIC METHODS ====================================================================

    /**
     * <p>To know if the manager is currently processing a data transfer.</p>
     *
     * @return True if a session is currently running, false otherwise.
     */
    public boolean isInProgress() {
        return mState != RWCP.State.LISTEN;
    }

    /**
     * <p>To allow the display of the debug logs.</p>
     * <p>They give complementary information on any call of a method.
     * They can indicate that a method is reached but also some action the method does.</p>
     *
     * @param show
     *          True to show the debug logs, false otherwise.
     */
    public void showDebugLogs(boolean show) {
        mShowDebugLogs = show;
        Log.i(TAG, "Debug logs are now " + (show ? "activated" : "deactivated") + ".");
    }

    /**
     * <p>To send a message to a device using RWCP.</p>
     * <p>This method queues up the data and depending on the presence of a current session it starts a session or
     * requests the sending of the data if there is no activity at this moment.</p>
     *
     * @param bytes The array byte to send in a RWCP segment.
     *
     * @return True if the manager could start the session or initiate the sending of the given data. This method
     * returns false if any of the previous fails.
     */
    public boolean sendData(byte[] bytes) {
        mPendingData.add(bytes);

        if (mState == RWCP.State.LISTEN) {
            return startSession();
        }
        else if (mState == RWCP.State.ESTABLISHED && !isTimeOutRunning) {
            sendDataSegment();
            return true;
        }

        return true;
    }

    /**
     * <p>To cancel any ongoing session.</p>
     * <p>This method should be called if the system cannot communicate with the device anymore, if the transfer had
     * been aborted, etc.</p>
     */
    public void cancelTransfer() {
        reset(true);
        if (!sendRSTSegment()) {
            Log.w(TAG, "Sending of RST segment has failed, terminating session.");
            terminateSession(true);
        }
    }

    /**
     * <p>This method is called by the application when it receives a possible RWCP segment for this manager to
     * handle it.</p>
     * <p>This method will act depending on this manager current state and the segment operation code.</p>
     *
     * @param bytes The bytes which correspond to a potential RWCP packet.
     *
     * @return True if the bytes had successfully handled as an expected RWCP segment.
     */
    public boolean onReceiveRWCPSegment(byte[] bytes) {
        if (bytes == null) {
            Log.w(TAG, "onReceiveRWCPSegment called with a null bytes array.");
            return false;
        }

        if (mShowDebugLogs) {
            Log.d(TAG, "Received potential RWCP segment: " + Utils.getStringFromBytes(bytes));
        }

        if (bytes.length < RWCP.Segment.REQUIRED_INFORMATION_LENGTH) {
            String message = "Analyse of RWCP Segment failed: the byte array does not contain the minimum " +
                    "required information.";
            Log.w(TAG, message);
            return false;
        }

        // getting the segment information from the bytes
        Segment segment = new Segment(bytes);
        int code = segment.getOperationCode();
        if (code == RWCP.OpCode.NONE) {
            Log.w(TAG, "onReceivedRWCPSegment failed to get a RWCP segement from given bytes: "
                    + Utils.getStringFromBytes(bytes));
            return false;
        }

        // handling packet depending on the operation code.
        switch (code) {
            case RWCP.OpCode.SYN_ACK:
                return receiveSynAck(segment);
            case RWCP.OpCode.DATA_ACK:
                return receiveDataAck(segment);
            case RWCP.OpCode.RST:
            /*case RWCP.OpCode.RST_ACK:*/
                return receiveRST(segment);
            case RWCP.OpCode.GAP:
                return receiveGAP(segment);
            default:
                Log.w(TAG, "Received unknown operation code: " + code);
                return false;
        }
    }


    // ====== PRIVATE METHODS ====================================================================

    /**
     * <p>This method starts a transfer session by sending a RST segment and a SYn segment. Then this manager waits
     * for these to be timed out or acknowledged by the device.</p>
     *
     * @return True if the start of the session had successfully been initiated.
     */
    private boolean startSession() {
        if (mShowDebugLogs) {
            Log.d(TAG, "Starting session of RWCP data transfer.");
        }

        if (sendRSTSegment() && sendSYNSegment()) {
            return true;
        }
        else {
            Log.w(TAG, "Start session of RWCP data transfer failed.");
            terminateSession(true);
            return false;
        }
    }

    /**
     * <p>To reset this manager when a session is ending. This can be called to reset the session.</p>
     *
     * @param hasFailed True if the session is ended due to a failure.
     */
    private void terminateSession(boolean hasFailed) {
        if (mShowDebugLogs) {
            Log.d(TAG, "Terminate session of RWCP data transfer.");
        }

        mState = RWCP.State.LISTEN;

        if (hasFailed) {
            mListener.onTransferFailed();
            reset(true);
        }
    }

    /**
     * <p>This method is called by {@link #onReceiveRWCPSegment(byte[]) onReceiveRWCPSegment} when it has received a
     * {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.OpCode#SYN_ACK SYN_ACK} segment.</p>
     * <p>A SYN_ACK segment can be expected on the following cases:
     * <ul>
     *     <li>state {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.State#SYN_SENT SYN_SENT}: default behaviour, the
     *     manager can start the data transfer.</li>
     *     <li>state {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.State#ESTABLISHED ESTABLISHED}: the device has not
     *     received any data yet and is expecting some. This manager resent any unacknowledged data and the
     *     following segments if there is any credit.</li>
     * </ul></p>
     *
     * @param segment
     *          The received segment with the operation code
     *          {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.OpCode#SYN_ACK SYN_ACK}.
     *
     * @return True if the segment has successfully been handled.
     */
    private boolean receiveSynAck(Segment segment) {
        if (mShowDebugLogs) {
            Log.d(TAG, "Receive SYN_ACK for sequence " + segment.getSequenceNumber());
        }

        switch (mState) {

            case RWCP.State.SYN_SENT:
                cancelTimeOut();
                int validated = validateAckSequence(segment.getSequenceNumber());
                if (validated >= 0) {
                    mState = RWCP.State.ESTABLISHED;
                    if (mPendingData.size() > 0) {
                        sendDataSegment();
                    }
                }
                else {
                    sendRSTSegment();
                }
                return true;

            case RWCP.State.ESTABLISHED:
                cancelTimeOut();
                if (mUnacknowledgedSentSegments.size() > 0) {
                    resendDataSegment();
                }
                return true;

            case RWCP.State.CLOSING:
            case RWCP.State.LISTEN:
            default:
                Log.w(TAG, "Received unexpected SYN_ACK segment with header " + segment.getHeader()
                        + " while in state " + RWCP.getStateLabel(mState));
                return false;
        }
    }

    /**
     * <p>This method is called by {@link #onReceiveRWCPSegment(byte[]) onReceiveRWCPSegment} when it has received a
     * {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.OpCode#DATA_ACK DATA_ACK} segment.</p>
     * <p>A DATA_ACK segment can be expected on the following cases:
     * <ul>
     *     <li>state {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.State#ESTABLISHED ESTABLISHED}: the device acknowledges
     *     the data it has received. This manager then validates the acknowledged data and send more if it has free
     *     credits.</li>
     *     <li>state {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.State#CLOSING CLOSING}: when this manager has sent a
     *     {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.OpCode#RST RST} segment but it has not been fetched yet by the
     *     device.</li>
     * </ul></p>
     *
     * @param segment
     *          The received segment with the operation code
     *          {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.OpCode#DATA_ACK DATA_ACK}.
     *
     * @return True if the segment has successfully been handled.
     */
    private boolean receiveDataAck(Segment segment) {
        if (mShowDebugLogs) {
            Log.d(TAG, "Receive DATA_ACK for sequence " + segment.getSequenceNumber());
        }

        switch (mState) {
            case RWCP.State.ESTABLISHED:
                cancelTimeOut();
                int sequence = segment.getSequenceNumber();
                int validated = validateAckSequence(sequence);
                if (validated >= 0) {
                    if (mCredits > 0) {
                        sendDataSegment();
                    }

                    if (mPendingData.isEmpty() && mUnacknowledgedSentSegments.isEmpty()) {
                        sendRSTSegment();
                        mListener.onTransferFinished();
                    }
                    else {
                        mListener.onTransferProgress(validated);
                    }
                }
                return true;

            case RWCP.State.CLOSING:
                cancelTimeOut();
                sendRSTSegment();
                return true;

            case RWCP.State.SYN_SENT:
            case RWCP.State.LISTEN:
            default:
                Log.w(TAG, "Received unexpected DATA_ACK segment with header " + segment.getHeader()
                        + " while in state " + RWCP.getStateLabel(mState));
                return false;
        }
    }

    /**
     * <p>This method is called by {@link #onReceiveRWCPSegment(byte[]) onReceiveRWCPSegment} when it has received a
     * {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.OpCode#RST RST} or a
     * {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.OpCode#RST_ACK RST_ACK} segment.</p>
     * <p>A RST segment can be expected on the following cases:
     * <ul>
     *     <li>state {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.State#ESTABLISHED ESTABLISHED}: the device requests a
     *     reset of the session, this cancels the transfer.</li>
     *     <li>state {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.State#CLOSING CLOSING}: This might be a
     *     {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.OpCode#RST_ACK RST_ACK} - the
     *     device acknowledges a RST sent by this manager. The session is ended.</li>
     *     <li>state {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.State#SYN_SENT SYN_SENT}: This might be a
     *     {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.OpCode#RST_ACK RST_ACK} - the
     *     device acknowledges a RST sent by this manager. A RST message is always sent before a SYN message. There
     *     is nothing to do here.</li>
     * </ul></p>
     *
     * @param segment
     *          The received segment with the operation code
     *          {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.OpCode#RST RST} or
     *          {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.OpCode#RST_ACK RST_ACK}.
     *
     * @return True if the segment has successfully been handled.
     */
    private boolean receiveRST(Segment segment) {
        if (mShowDebugLogs) {
            Log.d(TAG, "Receive RST or RST_ACK for sequence " + segment.getSequenceNumber());
        }

        switch (mState) {
            case RWCP.State.ESTABLISHED:
                // received RST
                Log.w(TAG, "Received RST (sequence " + segment.getSequenceNumber() + ") in ESTABLISHED state, " +
                        "terminating session, transfer failed.");
                terminateSession(true);
                return true;


            case RWCP.State.CLOSING:
                // received RST_ACK
                cancelTimeOut();
                validateAckSequence(segment.getSequenceNumber());
                terminateSession(false);
                if (!mPendingData.isEmpty()) {
                    startSession();
                }
                return true;

            case RWCP.State.SYN_SENT:
                // received RST_ACK
                validateAckSequence(segment.getSequenceNumber());
                Log.i(TAG, "Received RST segment while in SYN_SENT state.");
                return true;

            case RWCP.State.LISTEN:
            default:
                Log.w(TAG, "Received unexpected RST segment with header " + segment.getHeader()
                        + " while in state " + RWCP.getStateLabel(mState));
                return false;
        }
    }

    /**
     * <p>This method is called by {@link #onReceiveRWCPSegment(byte[]) onReceiveRWCPSegment} when it has received a
     * {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.OpCode#GAP GAP} segment.</p>
     * <p>A DATA_ACK segment can be expected on the following cases:
     * <ul>
     *     <li>state {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.State#ESTABLISHED ESTABLISHED}: the device acknowledges
     *     the data it has received and that it misses segments after. This manager then validates the acknowledged
     *     data and resends the next ones.</li>
     *     <li>state {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.State#CLOSING CLOSING}: when this manager has sent a
     *     {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.OpCode#RST RST} segment but it has not been fetched yet by the
     *     device.</li>
     * </ul></p>
     *
     * @param segment
     *          The received segment with the operation code {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.OpCode#GAP GAP}.
     *
     * @return True if the segment has successfully been handled.
     */
    private boolean receiveGAP(Segment segment) {
        if (mShowDebugLogs) {
            Log.d(TAG, "Receive GAP for sequence " + segment.getSequenceNumber());
        }

        switch (mState) {
            case RWCP.State.ESTABLISHED:
                if (mLastAckSequence > segment.getSequenceNumber()) {
                    Log.i(TAG, "Received GAP (" + segment.getSequenceNumber() + ") and last ack sequence is "
                            + mLastAckSequence + " - ignoring GAP.");
                    return true;
                }
                if (mLastAckSequence <= segment.getSequenceNumber()) {
                    // Sequence number in GAP implies lost DATA_ACKs
                    if (mShowDebugLogs) {
                        Log.i(TAG, "Received GAP with DATA_ACK ahead of known one.");
                    }

                    // adjust window
                    decreaseWindow();
                    // validate the acknowledged packets if not known.
                    validateAckSequence(segment.getSequenceNumber());
                }

                cancelTimeOut();
                resendDataSegment();
                return true;


            case RWCP.State.CLOSING:
                cancelTimeOut();
                sendRSTSegment();
                return true;

            case RWCP.State.SYN_SENT:
            case RWCP.State.LISTEN:
            default:
                Log.w(TAG, "Received unexpected GAP segment with header " + segment.getHeader()
                        + " while in state " + RWCP.getStateLabel(mState));
                return false;
        }
    }

    /**
     * <p>This method is called when segments are timed out: this manager didn't receive any acknowledgement for
     * them.</p>
     * <p>This manager resends the segments from the last known acknowledged sequence number.</p>
     */
    private void onTimeOut() {
        if (isTimeOutRunning) {
            isTimeOutRunning = false;
            mIsResendingSegments = true;

            if (mShowDebugLogs) {
                Log.i(TAG, "Segments had been timed out, re sending segment and followings.");
            }

            // if the first missing segment is a DATA segment: increasing data time out value
            if (mUnacknowledgedSentSegments.getFirst().getOperationCode() == RWCP.OpCode.DATA) {
                mDataTimeOutMs *= 2;
                if (mDataTimeOutMs > RWCP.DATA_TIMEOUT_MS_MAX) {
                    mDataTimeOutMs = RWCP.DATA_TIMEOUT_MS_MAX;
                }
            }

            resendDataSegment();
        }
    }

    /**
     * <p>This method validates the segments which had been acknowledged by the device and returns the number of
     * segments which had been acknowledged.</p>
     * <p>If this method couldn't validate the sequence it returns <code>-1</code>.</p>
     *
     * @param sequence
     *          The sequence number which acknowledges the corresponding segment and previous ones.
     *
     * @return The number of segments acknowledged by the given segment or -1 if it couldn't validate the given
     * sequence.
     */
    private int validateAckSequence(int sequence) {
        final int NOT_VALIDATED = -1;

        if (sequence < 0) {
            Log.w(TAG, "Received ACK sequence (" + sequence + ") is less than 0.");
            return NOT_VALIDATED;
        }

        if (sequence > RWCP.SEQUENCE_NUMBER_MAX) {
            Log.w(TAG, "Received ACK sequence (" + sequence + ") is bigger than its maximum value ("
                    + RWCP.SEQUENCE_NUMBER_MAX + ").");
            return NOT_VALIDATED;
        }

        if (mLastAckSequence<mNextSequence && (sequence<mLastAckSequence || sequence>mNextSequence)) {
            Log.w(TAG, "Received ACK sequence (" + sequence + ") is out of interval: last received is " +
                    mLastAckSequence + " and next will be " + mNextSequence);
            return NOT_VALIDATED;
        }

        if (mLastAckSequence>mNextSequence && sequence<mLastAckSequence && sequence>mNextSequence) {

            Log.w(TAG, "Received ACK sequence (" + sequence + ") is out of interval: last received is " +
                    mLastAckSequence + " and next will be " + mNextSequence);
            return NOT_VALIDATED;
        }

        int acknowledged = 0;

        synchronized (mUnacknowledgedSentSegments) {
            while (mLastAckSequence != sequence) {
                mLastAckSequence = increaseSequenceNumber(mLastAckSequence);
                mUnacknowledgedSentSegments.removeFirst();
                if (mCredits < mWindow) {
                    mCredits++;
                }
                acknowledged++;
            }
        }

        // increase the window size if qualified.
        increaseWindow(acknowledged);

        return acknowledged;
    }

    /**
     * <p>Builds and sends a RST segment. This method also add the segment to the list of unacknowledged segments.</p>
     *
     * @return True if the segment had been sent.
     */
    private boolean sendRSTSegment() {
        boolean done;
        reset(false);
        synchronized (mUnacknowledgedSentSegments) {
            mState = RWCP.State.CLOSING;
            Segment segment = new Segment(RWCP.OpCode.RST, mNextSequence);
            done = sendSegment(segment, RWCP.RST_TIMEOUT_MS);
            if (done) {
                mUnacknowledgedSentSegments.add(segment);
                mNextSequence = increaseSequenceNumber(mNextSequence);
                mCredits--;
            }
        }
        return done;
    }

    /**
     * <p>Builds and sends a SYN segment. This method also adds the segment to the list of unacknowledged segments.</p>
     *
     * @return True if the segment had been sent.
     */
    private boolean sendSYNSegment() {
        boolean done;
        reset(false);
        synchronized (mUnacknowledgedSentSegments) {
            mState = RWCP.State.SYN_SENT;
            Segment segment = new Segment(RWCP.OpCode.SYN, mNextSequence);
            done = sendSegment(segment, RWCP.SYN_TIMEOUT_MS);
            if (done) {
                mUnacknowledgedSentSegments.add(segment);
                mNextSequence = increaseSequenceNumber(mNextSequence);
                mCredits--;
            }
        }
        return done;
    }

    /**
     * <p>Sends the pending DATA segments depending on available credits. If this manager needs to resend
     * unacknowledged segments, this method stops its process.</p>
     */
    private void sendDataSegment() {
        while (mCredits > 0 && !mPendingData.isEmpty() && !mIsResendingSegments && mState == RWCP.State.ESTABLISHED) {
            synchronized (mUnacknowledgedSentSegments) {
                byte[] data = mPendingData.poll();
                Segment segment = new Segment(RWCP.OpCode.DATA, mNextSequence, data);
                sendSegment(segment, mDataTimeOutMs);
                mUnacknowledgedSentSegments.add(segment);
                mNextSequence = increaseSequenceNumber(mNextSequence);
                mCredits--;
            }
        }
    }

    /**
     * <p>This method increases the given sequence number by 1. As a sequence number is always from 0 to
     * {@link RWCP#SEQUENCE_NUMBER_MAX SEQUENCE_NUMBER_MAX}, the method restarts to 0 if the new value is greater than
     * {@link RWCP#SEQUENCE_NUMBER_MAX SEQUENCE_NUMBER_MAX}.</p>
     *
     * @param sequence
     *          The sequence number to increase.
     *
     * @return The increased value of the sequence number.
     */
    private int increaseSequenceNumber(int sequence) {
        return (sequence+1) % (RWCP.SEQUENCE_NUMBER_MAX+1);
    }

    /**
     * <p>This method resends the DATA segments which are still unacknowledged.</p>
     */
    private void resendDataSegment() {
        if (mShowDebugLogs) {
            Log.d(TAG, "Resending unacknowledged segments.");
        }

        mIsResendingSegments = true;
        mCredits = mWindow;

        synchronized (mUnacknowledgedSentSegments) {

            // if they are more unacknowledged segments than available credits, these extra segments are not anymore
            // unacknowledged but pending
            while (mUnacknowledgedSentSegments.size() > mCredits) {
                mPendingData.addFirst(mUnacknowledgedSentSegments.removeLast().getPayload());
            }

            // if some segments have been moved to the pending state, the next sequence number has changed.
            mNextSequence = increaseSequenceNumber(mUnacknowledgedSentSegments.getLast().getSequenceNumber());

            // resend the unacknowledged segments corresponding to the window
            for (Segment segment : mUnacknowledgedSentSegments) {
                int delay = (segment.getOperationCode() == RWCP.OpCode.SYN) ? RWCP.SYN_TIMEOUT_MS :
                        (segment.getOperationCode() == RWCP.OpCode.RST) ? RWCP.RST_TIMEOUT_MS : mDataTimeOutMs;

                sendSegment(segment, delay);
                mCredits--;
            }
        }

        mIsResendingSegments = false;

        if (mCredits > 0) {
            sendDataSegment();
        }
    }

    /**
     * <p>This method transmits the bytes of a segment to a listener in order to send them to the device.</p>
     * <p>This method also starts the time out for the segment.</p>
     *
     * @param segment
     *          The segment to send to a device.
     * @param timeout
     *          The timeout in ms to use for the sending of this segment.
     *
     * @return True if the segment could be sent to the device.
     */
    private boolean sendSegment(Segment segment, int timeout) {
        byte[] bytes = segment.getBytes();
        if (mShowDebugLogs) {
            Log.d(TAG, "Sending segment " + segment.toString(true));
        }
        if (mListener.sendRWCPSegment(bytes)) {
            startTimeOut(timeout);
            return true;
        }

        return false;
    }

    /**
     * <p>To reset all fields at their default state for a new session. Setting <code>complete</code> to True empty the
     * queue of pending data.</p>
     *
     * @param complete True to completely reset this manager. False to only reset a session.
     */
    private void reset(boolean complete) {
        synchronized (mUnacknowledgedSentSegments) {
            mLastAckSequence = -1;
            mNextSequence = 0;
            mState = RWCP.State.LISTEN;
            mUnacknowledgedSentSegments.clear();
            resetWindow();
            cancelTimeOut();
        }
        if (complete) {
            mPendingData.clear();
        }
    }

    /**
     * <p>This method keeps a record of how many data segments had been acknowledged successfully in a row. If this
     * number is greater than the current window this method increases the window size.</p>
     *
     * @param acknowledged the number of segments which had been acknowledged by a received acknowledgement.
     */
    private void increaseWindow(int acknowledged) {
        mAcknowledgedSegments += acknowledged;
        if (mAcknowledgedSegments > mWindow && mWindow != RWCP.WINDOW_MAX) {
            mAcknowledgedSegments = 0;
            mWindow++;
            mCredits++;
            if (mShowDebugLogs) {
                Log.d(TAG, "Increasing windows size to " + mWindow);
            }
        }
    }

    /**
     * <p>To decrease the window size if the current one is too big and leads to the reception of GAP segments.</p>
     */
    private void decreaseWindow() {
        mWindow = ((mWindow - 1) / 2) + 1;
        if (mWindow > RWCP.WINDOW_MAX) {
            mWindow = 1;
        }

        mAcknowledgedSegments = 0;
        mCredits = mWindow;

        if (mShowDebugLogs) {
            Log.d(TAG, "Decreasing windows size to " + mWindow);
        }
    }

    /**
     * <p>To reset the window to its default value as well as related fields.</p>
     */
    private void resetWindow() {
        mWindow = RWCP.WINDOW_MAX;
        mAcknowledgedSegments = 0;
        mCredits = mWindow;
    }


    // ====== TIMEOUT PROCESS ===============================================================

    /**
     * <p>To start a Runnable which will be thrown after the given delay. This Runnable deals with segments which
     * had not been acknowledged anf might have not been received.</p>
     */
    private void startTimeOut(long delay) {
        if (isTimeOutRunning) {
            mHandler.removeCallbacks(mTimeOutRunnable);
        }

        isTimeOutRunning = true;
        mHandler.postDelayed(mTimeOutRunnable, delay);
    }

    /**
     * <p>To cancel the time out by cancelling the Runnable waiting to be thrown when its delay had passed.</p>
     */
    private void cancelTimeOut() {
        if (isTimeOutRunning) {
            mHandler.removeCallbacks(mTimeOutRunnable);
            isTimeOutRunning = false;
        }
    }


    // ====== INNER CLASSES ====================================================================

    /**
     * <p>This interface allows this manager to dispatch messages or events to a listener.</p>
     */
    public interface RWCPListener {

        /**
         * <p>To send the bytes of a RWCP segment to a connected device.</p>
         *
         * @param bytes
         *          The bytes to send.
         *
         * @return True if the sending could be managed.
         */
        boolean sendRWCPSegment(byte[] bytes);

        /**
         * <p>Called when the transfer with RWCP has failed. The transfer fails in the following cases:
         * <ul>
         *     <li>The sending of a segment fails at the transport layer.</li>
         *     <li>The device sent a {@link com.ly.bluetoothhelper.oat.rwcp.RWCP.OpCode#RST RST} segment.</li>
         * </ul></p>
         */
        void onTransferFailed();

        /**
         * <p>Called when the transfer of all the data given to this manager had been successfully sent and
         * acknowledged.</p>
         */
        void onTransferFinished();

        /**
         * <p>Called when some new packets had been acknowledged to inform the listener.</p>
         *
         * @param acknowledged
         *              The number of packets which had been acknowledged.
         */
        void onTransferProgress(int acknowledged);
    }

    /**
     * <p>A Runnable to define what should be done when a segment is timed out.</p>
     * <p>A segment is considered as being timed out if this manager has not received a corresponding acknowledgement
     * when this runnable is triggered.</p>
     * <p>RWCP uses unreliable messages leading to some segments which might have not been received by a connected
     * device. This Runnable helps to deal with these messages by calling {@link #onTimeOut() onTimeOut}.</p>
     */
    private class TimeOutRunnable implements Runnable {
        @Override
        public void run() {
            onTimeOut();
        }
    }
}

/**************************************************************************************************
 * Copyright 2017 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.ly.blehelper.upgrade_core.ble_upgrade.rwcp;

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>This class contains all characteristics constants for RWCP.</p>
 * <p>The Reliable Write Command Protocol (RWCP) defines a reliable method using BLE Write Commands and Handle Value
 * Notifications to transmit messages. It is designed to increase the transfer rate of messages. This method provides
 * in-sequence reception, retransmission of lost messages, and duplicate message detection from a GATT Client to a
 * GATT Server.</p>
 * <p>The messages send over RWCP are called {@link com.ly.bluetoothhelper.oat.rwcp.Segment Segment} and contains an
 * operation code, a sequence number and the data to transmit.</p>
 * <p>The number of segments which can be sent at once is called the window.</p>
 *
 * @since 2.0.3
 */
public final class RWCP {

    /**
     * <p>The maximum size of the window.</p>
     */
    static final int WINDOW_MAX = 15;

    /**
     * <p>The delay in millisecond to time out a SYN operation.</p>
     */
    static final int SYN_TIMEOUT_MS = 1000;
    /**
     * <p>The delay in millisecond to time out a RST operation.</p>
     */
    static final int RST_TIMEOUT_MS = 1000;
    /**
     * <p>The default delay in millisecond to time out a DATA operation.</p>
     */
    static final int DATA_TIMEOUT_MS_DEFAULT = 100;
    /**
     * <p>The maximum delay in millisecond to time out a DATA operation.</p>
     */
    static final int DATA_TIMEOUT_MS_MAX = 1000;

    /**
     * <p>The maximum number of a sequence is 63 which correspond to the maximum value represented by 6 bits.</p>
     */
    static final int SEQUENCE_NUMBER_MAX = 63;

    /**
     * <p>During a RWCP session, the device is going through a series of states. These states are defined in this
     * enumeration.</p>
     * <p>An RWCP session progresses from one state to another in response to events. The events are: START,
     * CLOSE, the incoming segments, and timeouts.</p>
     */
    @IntDef(flag = true, value = { State.LISTEN, State.SYN_SENT, State.ESTABLISHED, State.CLOSING })
    @Retention(RetentionPolicy.SOURCE)
    @SuppressLint("ShiftFlags") // values are more readable this way
    @interface State {
        /**
         * The Client is ready for the application to request that a Write Command(s) be sent to the Server.
         */
        int LISTEN = 0;
        /**
         * The Client has started a session and is waiting for the Server to acknowledge the start.
         */
        int SYN_SENT = 1;
        /**
         * The Client sends data to the Server.
         */
        int ESTABLISHED = 2;
        /**
         * The Client has terminated the connection the connection and the Client is waiting for the Server to
         * acknowledge the termination request.
         */
        int CLOSING = 3;
    }

    /**
     * <p>This class contains all the operation codes which are sent and received by both the server and the
     * clients.</p>
     * <p>This codes define the type of the segment.</p>
     * <p>The operation codes are contained in 2 bits so their values are from 0 to 4.</p>
     */
    static class OpCode {
        /**
         * Data sent to the server by the client.
         */
        static final byte DATA = 0;
        /**
         * Used by the server to acknowledge the data sent to the server.
         */
        static final byte DATA_ACK = 0;
        /**
         * Used to synchronise and start a session by the client.
         */
        static final byte SYN = 1;
        /**
         * Used by the server to acknowledge the SYN segment.
         */
        static final byte SYN_ACK = 1;
        /**
         * RST is used to terminate a session by the client.
         * RST_ACK is used by the server to acknowledge the Client’s request to terminate a session.
         */
        static final byte RST = 2;
        static final byte RST_ACK = 2;
        /**
         * Used by the server to indicate that the Server received a DATA segment that was out-of-sequence.
         */
        static final byte GAP = 3;
        /**
         * Used by the application to indicate an unknown operation code.
         */
        static final byte NONE = -1;
    }

    /**
     * <p>This class contains all the constants which define the frame of a segment. The frame is as follows:</p>
     * <blockquote><pre>
     * 0 byte     1         ...         n
     * +----------+----------+----------+
     * |  HEADER  |       PAYLOAD       |
     * +----------+----------+----------+
     * </pre></blockquote>
     *
     */
    public static class Segment {
        /**
         * The offset for the header information.
         */
        static final int HEADER_OFFSET = 0;
        /**
         * The number of bytes which contain the header.
         */
        static final int HEADER_LENGTH = 1;
        /**
         * The offset for the payload information.
         */
        static final int PAYLOAD_OFFSET = HEADER_OFFSET + HEADER_LENGTH;
        /**
         * The minimum length of a segment.
         */
        public static final int REQUIRED_INFORMATION_LENGTH = HEADER_LENGTH;

        /**
         * <p>The header of a RWCP segment contains the information to identify the segment: a sequence number and an
         * operation code. The header is contained in one byte for which the bits are allocated as follows:</p>
         * <blockquote><pre>
         * 0 bit     ...         6          7          8
         * +----------+----------+----------+----------+
         * |   SEQUENCE NUMBER   |   OPERATION CODE    |
         * +----------+----------+----------+----------+
         * </pre></blockquote>
         */
        static class Header {
            /**
             * The bit offset for the sequence number.
             */
            static final int SEQUENCE_NUMBER_BIT_OFFSET = 0;
            /**
             * The number of bits which contain the sequence number information.
             */
            static final int SEQUENCE_NUMBER_BITS_LENGTH = 6;
            /**
             * The bit offset for the operation code.
             */
            static final int OPERATION_CODE_BIT_OFFSET = SEQUENCE_NUMBER_BIT_OFFSET +
                    SEQUENCE_NUMBER_BITS_LENGTH;
            /**
             * The number of bits which contain the operation code.
             */
            static final int OPERATION_CODE_BITS_LENGTH = 2;
        }
    }

    /**
     * <p>This method builds a human readable label corresponding to the given state value as "CLOSING",
     * "ESTABLISHED", "LISTEN" and "SYN_SENT". It returns "Unknown state" for any other value.</p>
     *
     * @param state
     *          The state for which is required a human readable value.
     *
     * @return A human readable label for the given value.
     */
    static String getStateLabel(@State int state) {
        switch (state) {
            case State.CLOSING:
                return "CLOSING";
            case State.ESTABLISHED:
                return "ESTABLISHED";
            case State.LISTEN:
                return "LISTEN";
            case State.SYN_SENT:
                return "SYN_SENT";
            default:
                return "Unknown state (" + state + ")";
        }
    }

}


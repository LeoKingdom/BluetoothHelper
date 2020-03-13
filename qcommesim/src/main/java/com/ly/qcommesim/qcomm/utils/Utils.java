/**************************************************************************************************
 * Copyright 2017 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.ly.qcommesim.qcomm.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.ly.qcommesim.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * <p>This class contains all useful methods for this module.</p>
 */
public class Utils {

    /**
     * <p>The number of bits contains in a byte.</p>
     */
    private static final int BITS_IN_BYTE = 8;
    /**
     * <p>The number of bytes in a long.</p>
     */
    private static final int BYTES_IN_LONG = 8;
    /**
     * To display a number in a specific decimal format.
     */
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat();

    /**
     * Convert a byte array to a human readable String.
     *
     * @param value
     *            The byte array.
     * @return String object containing values in byte array formatted as hex.
     */
    public static String getStringFromBytes(byte[] value) {
        if (value == null)
            return "null";
        final StringBuilder stringBuilder = new StringBuilder(value.length*2);
        //noinspection ForLoopReplaceableByForEach // the for loop is more efficient than the foreach one
        for (int i = 0; i < value.length; i++) {
            stringBuilder.append(String.format("0x%02x ", value[i]));
        }
        return stringBuilder.toString();
    }

    /**
     * Get 16-bit hexadecimal string representation of byte.
     *
     * @param i
     *            The value.
     *
     * @return Hex value as a string.
     */
    public static String getIntToHexadecimal(int i) {
        return String.format("0x%04X", i & 0xFFFF);
    }

    /**
     * <p>This method returns the image which corresponds to the given rssi signal. it will return the following:</p>
     * <ul>
     *     <li>For <code>rssi</code> between <code>-60</code> and <code>0</code> the method returns
     *     {@link R.drawable#ic_signal_level_4_24dp ic_signal_level_4_24dp}</li>
     *     <li>For <code>rssi</code> between <code>-70</code> and <code>-60</code> the method returns
     *     {@link R.drawable#ic_signal_level_3_24dp ic_signal_level_3_24dp}</li>
     *     <li>For <code>rssi</code> between <code>-80</code> and <code>-70</code> the method returns
     *     {@link R.drawable#ic_signal_level_2_24dp ic_signal_level_2_24dp}</li>
     *     <li>For <code>rssi</code> between <code>-90</code> and <code>-80</code> the method returns
     *     {@link R.drawable#ic_signal_level_1_24dp ic_signal_level_1_24dp}</li>
     *     <li>For <code>rssi</code> less than <code>-90</code> the method returns
     *     {@link R.drawable#ic_signal_level_0_24dp ic_signal_level_0_24dp}</li>
     *     <li>For all other values the method returns
     *     {@link R.drawable#ic_signal_unknown_24dp ic_signal_unknown_24dp}</li>
     * </ul>
     *
     * @param context
     *          The context of the application for the method to be able to retrieve the image.
     * @param rssi
     *          The value for which we want to retrieve the corresponding image
     *
     * @return A drawable picture of a signal strength depending on the given rssi value.
     */
    @SuppressWarnings("deprecation")
    public static Drawable getSignalIconFromRssi(Context context, int rssi) {
        if (-60 <= rssi && rssi <= 0) {
            //noinspection deprecation
            return context.getResources().getDrawable(R.drawable.ic_signal_level_4_24dp);
        }
        else if (-70 <= rssi && rssi < -60) {
            return context.getResources().getDrawable(R.drawable.ic_signal_level_3_24dp);
        }
        else if (-80 <= rssi && rssi < -70) {
            return context.getResources().getDrawable(R.drawable.ic_signal_level_2_24dp);
        }
        else if (-90 <= rssi && rssi < -80) {
            return context.getResources().getDrawable(R.drawable.ic_signal_level_1_24dp);
        }
        else if (rssi < -90) {
            return context.getResources().getDrawable(R.drawable.ic_signal_level_0_24dp);
        }
        else {
            return context.getResources().getDrawable(R.drawable.ic_signal_unknown_24dp);
        }
    }

    /**
     * <p>To get the percentage as a String, formatted with the % type as follow.</p>
     *
     * @return the percentage as a formatted String value as follow: <code>value %</code>
     */
    public static String getStringForPercentage(double percentage) {
        if (percentage <= 1) {
            DECIMAL_FORMAT.setMaximumFractionDigits(2);
        }
        else {
            DECIMAL_FORMAT.setMaximumFractionDigits(1);
        }

        return DECIMAL_FORMAT.format(percentage) + " " + Consts.PERCENTAGE_CHARACTER;
    }

    /**
     * <p>To get a readable value of a time in seconds, minutes and hours as follows: "h:mm:ss".</p>
     *
     * @param milliseconds
     *          The time to display in milliseconds.
     *
     * @return A human readable value of the time.
     */
    public static String getStringForTime(long milliseconds) {
        // calculate the seconds, minutes and hours to display
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds - minutes*60;
        long hours = minutes / 60;
        minutes = minutes - hours*60;

        // build the corresponding label
        return String.valueOf((hours > 0) ? hours : 0) + ":" +
                ((minutes < 10) ? "0" : "") + minutes + ":" +
                ((seconds < 10) ? "0" : "") + seconds;
    }

    /**
     * Parse the UUIDs from an advert for filtering purposes. This code is based on code from
     * https://code.google.com/p/android/issues/detail?id=59490 and is required to work around issues in the Android API
     * with UUID filtering on scan results.
     *
     * @param advData
     *            Advertising data from a scan result.
     * @return List of UUIDs found in the advertising data.
     */
    public static List<UUID> getUuidsFromRawBytes(final byte[] advData) {
        final List<UUID> uuids = new ArrayList<>();

        // Pointer within advData to the start of the current ad element being processed.
        int ptrToAdElement = 0;

        // Offsets from start of ad element (i.e. from ptrToAdElement).
        final int OFFSET_LENGTH = 0;
        final int OFFSET_TYPE = 1;
        final int OFFSET_DATA = 2;

        final byte AD_TYPE_UUID_16BIT = 0x02;
        final byte AD_TYPE_UUID_16BIT_LIST = 0x03;
        final byte AD_TYPE_UUID_16BIT_SOLICITATION = 0x14;
        final byte AD_TYPE_UUID_128BIT = 0x06;
        final byte AD_TYPE_UUID_128BIT_LIST = 0x07;

        final int UUID_16_LENGTH = 2;
        final int UUID_128_LENGTH = 16;

        final String BASE_UUID_FORMAT = "%08x-0000-1000-8000-00805f9b34fb";

        while (ptrToAdElement < advData.length - 1) {
            final byte length = advData[ptrToAdElement + OFFSET_LENGTH];

            // The advert data returned by the Android API is padded out with trailing zeroes, so if we reach a
            // zero length then we are done.
            if (length == 0)
                break;

            // Check that there is enough remaining data in the advert for the indicated length.
            if (length > (advData.length - ptrToAdElement - 1)) {
                // This was a malformed advert so return an empty list, even if we got some UUIDs already.
                uuids.clear();
                return uuids;
            }

            final byte adType = advData[ptrToAdElement + OFFSET_TYPE];

            switch (adType) {
                case AD_TYPE_UUID_16BIT:
                case AD_TYPE_UUID_16BIT_LIST:
                case AD_TYPE_UUID_16BIT_SOLICITATION:
                    for (int i = length; i > UUID_16_LENGTH - 1; i -= UUID_16_LENGTH) {
                        int uuid16 = (advData[ptrToAdElement + OFFSET_DATA] & 0xFF);
                        uuid16 |= ((advData[ptrToAdElement + OFFSET_DATA + 1] & 0xFF) << 8);
                        uuids.add(UUID.fromString(String.format(BASE_UUID_FORMAT, uuid16)));
                    }
                    break;
                case AD_TYPE_UUID_128BIT:
                case AD_TYPE_UUID_128BIT_LIST:
                    for (int i = length; i > UUID_128_LENGTH - 1; i -= UUID_128_LENGTH) {
                        long leastSigBits = extractLongFromByteArray(advData, ptrToAdElement+OFFSET_DATA,
                                BYTES_IN_LONG, true);
                        long mostSigBits = extractLongFromByteArray(advData,
                                ptrToAdElement+OFFSET_DATA+BYTES_IN_LONG, BYTES_IN_LONG, true);

                        uuids.add(new UUID(mostSigBits, leastSigBits));
                    }
                    break;
                default:
                    // An advert type we don't care about.
                    break;
            }

            // Length byte isn't included in length, hence the +1.
            ptrToAdElement += length + 1;
        }

        return uuids;
    }


    /**
     * Extract a <code>long</code> field from an array.
     *
     * @param source
     *         The array to extract from.
     * @param offset
     *         Offset within source array.
     * @param length
     *         Number of bytes to use (maximum 8).
     * @param reverse
     *         True if bytes should be interpreted in reverse (little endian) order.
     *
     * @return The extracted long.
     */
    private static long extractLongFromByteArray(byte [] source, int offset, int length, boolean reverse) {
        if (length < 0 | length > BYTES_IN_LONG) {
            throw new IndexOutOfBoundsException("Length must be between 0 and "+ BYTES_IN_LONG);
        }
        long result = 0;
        int shift = (length-1) * BITS_IN_BYTE;

        if (reverse) {
            for (int i = offset+length-1; i >= offset; i--) {
                result |= ((source[i] & 0xFFL) << shift);
                shift -= BITS_IN_BYTE;
            }
        }
        else {
            for (int i = offset; i < offset+length; i++) {
                result |= ((source[i] & 0xFFL) << shift);
                shift -= BITS_IN_BYTE;
            }
        }
        return result;
    }


}
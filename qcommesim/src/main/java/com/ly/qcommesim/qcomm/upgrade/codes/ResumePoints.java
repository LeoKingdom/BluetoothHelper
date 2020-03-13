/**************************************************************************************************
 * Copyright 2016 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.ly.qcommesim.qcomm.upgrade.codes;


import com.ly.qcommesim.qcomm.annotation.Enums;

/**
 * <p>This class contains all the resume points as defined in the VM Upgrade protocol the Device can send to the
 * Host through the {@link OpCodes.Enum#UPGRADE_SYNC_CFM UPGRADE_SYNC_CFM} packet.</p>
 * <p>Each resume point represents the step where the upgrade should restart once the INIT PHASE of the process has
 * been finished.</p>
 * <p>The resume points are defined from the VM library documentation. Except the last one of the
 * {@link Enum enumeration} which is only used for display.</p>
 */
@SuppressWarnings("unused")
public final class ResumePoints {

    @SuppressWarnings("FieldCanBeLocal")
    private static final int RESUME_POINTS_COUNT = 5;



    /**
     * To get the number of resume points in this enumeration.
     *
     * @return
     *          the number of resume points.
     */
    public static int getLength () {
        return RESUME_POINTS_COUNT;
    }

    /**
     * To get the label for the corresponding resume point.
     *
     * @return The label which corresponds to the resume point.
     */
    public static String getLabel(@Enums int step) {
        switch (step) {
            case Enums.DATA_TRANSFER:
                return "Data transfer";
            case Enums.VALIDATION:
                return "Data validation";
            case Enums.TRANSFER_COMPLETE:
                return "Data transfer complete";
            case Enums.IN_PROGRESS:
                return "Upgrade in progress";
            case Enums.COMMIT:
                return "Upgrade commit";
            default:
                return "Initialisation";
        }
    }

    /**
     * To get the ResumePoint corresponding to the given value.
     *
     * @param value
     *          The value for which we would like the corresponding Resume Point.
     *
     * @return The corresponding ResumePoint, and {@link Enums#DATA_TRANSFER} as the default one if the
     * value does not have a corresponding ResumePoint.
     */
    public static @Enums int getResumePoint (byte value) {
        switch (value) {
            case Enums.DATA_TRANSFER:
                return Enums.DATA_TRANSFER;
            case Enums.VALIDATION:
                return Enums.VALIDATION;
            case Enums.TRANSFER_COMPLETE:
                return Enums.TRANSFER_COMPLETE;
            case Enums.IN_PROGRESS:
                return Enums.IN_PROGRESS;
            case Enums.COMMIT:
                return Enums.COMMIT;
            default:
                return Enums.DATA_TRANSFER;
        }
    }

}

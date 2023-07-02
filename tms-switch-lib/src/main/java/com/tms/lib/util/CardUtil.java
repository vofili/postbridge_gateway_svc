package com.tms.lib.util;

import org.apache.commons.lang3.StringUtils;

public class CardUtil {

    private CardUtil(){

    }
    public static String maskPan(String pan) {
        if (StringUtils.isEmpty(pan)) {
            return pan;
        } else {
            return pan.length() <= 4 ? pan : pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4, pan.length());
        }
    }

    public static String maskTrack2(String track2) {
        if (StringUtils.isEmpty(track2)){
            return track2;
        }
        String delimeter = "D";
        String[] track2Data;
        if (track2.contains("D")) {
            delimeter = "D";
            track2Data = track2.split("D");
        } else {
            if (!track2.contains("=")) {
                if (track2.length() <= 4) {
                    return track2;
                }

                return track2.substring(0, 6) + "***************";
            }

            delimeter = "=";
            track2Data = track2.split("=");
        }

        return maskPan(track2Data[0]) + delimeter + track2Data[1].substring(0, 4) + "*************";
    }
}

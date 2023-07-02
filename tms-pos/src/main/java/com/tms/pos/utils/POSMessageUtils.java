package com.tms.pos.utils;

import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.model.DefaultIsoResponseCodes;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class POSMessageUtils {

    public static final int TERMINAL_ID_FIELD = 41;
    public static final int CARD_ACCEPTOR_ID_FIELD = 42;

    private static final Logger log = LoggerFactory.getLogger(POSMessageUtils.class);

    private POSMessageUtils() {
    }

    public static boolean isMasterKeyDownload(String processingCode) {
        return processingCode != null && processingCode.startsWith("9A");
    }

    public static boolean isSessionKeyDownload(String processingCode) {
        return processingCode != null && processingCode.startsWith("9B");
    }

    public static boolean isPinKeyDownload(String processingCode) {
        return processingCode != null && processingCode.startsWith("9G");
    }

    public static boolean isParametersDownload(String processingCode) {
        return processingCode != null && processingCode.startsWith("9C");
    }

    public static boolean isPurchaseMessage(String processingCode) {
        return processingCode != null && processingCode.startsWith("00");
    }

    public static boolean isMasterKeyDownload(ISOMsg isoMsg) {
        String processingCode = isoMsg.getString(3);
        return is0800Message(isoMsg) && processingCode != null && processingCode.startsWith("9A");
    }

    public static boolean isSessionKeyDownload(ISOMsg isoMsg) {
        String processingCode = isoMsg.getString(3);
        return is0800Message(isoMsg) && processingCode != null && processingCode.startsWith("9B");
    }

    public static boolean isPinKeyDownload(ISOMsg isoMsg) {
        String processingCode = isoMsg.getString(3);
        return is0800Message(isoMsg) && processingCode != null && processingCode.startsWith("9G");
    }

    public static boolean isParametersDownload(ISOMsg isoMsg){
        String processingCode = isoMsg.getString(3);
        return is0800Message(isoMsg) && processingCode != null && processingCode.startsWith("9C");
    }

    public static boolean isCallHome(ISOMsg isoMsg){
        String processingCode = isoMsg.getString(3);
        return is0800Message(isoMsg) && processingCode != null && processingCode.startsWith("9D");
    }

    private static boolean is0800Message(ISOMsg isoMsg) {
        try {
            String mti = isoMsg.getMTI();
            return "0800".equals(mti);
        } catch (ISOException e) {
            log.error("Could not get mti", e);
            return false;
        }
    }

    public static boolean isApprovedResponse(ISOMsg isoResponse) {
        validateIsoResponse(isoResponse);
        return isoResponse.getString(39).equals(DefaultIsoResponseCodes.Approved.toString());
    }

    public static String getEncryptedKeyFromResponse(ISOMsg isoResponse) throws UtilOperationException {
        validateIsoResponse(isoResponse);

        String field53 = isoResponse.getString(53);
        if (field53 == null || field53.length() < 32) {
            throw new UtilOperationException("Invalid key length in field 53");
        }

        return field53.substring(0, 32);
    }

    private static void validateIsoResponse(ISOMsg isoResponse) {
        if (isoResponse == null) {
            throw new IllegalArgumentException("Null iso response");
        }
    }
}

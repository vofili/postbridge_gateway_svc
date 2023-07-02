package com.tms.lib.util;

import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.model.AdditionalAmount;
import com.tms.lib.model.EmvData;
import com.tms.lib.model.OriginalDataElements;
import com.tms.lib.model.ReplacementAmounts;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.jpos.iso.ISODate;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

public class IsoUtil {

    private final static Logger logger = LoggerFactory.getLogger(IsoUtil.class);
    private final static int BALANCE_LENGTH = 20;

    private IsoUtil() {

    }

    public static void setIsoField(ISOMsg isoMsg, int field, String value) {
        isoMsg.set(field, value);

    }

    public static void setIsoField(ISOMsg isoMsg, int field, byte[] value) {
        isoMsg.set(field, value);

    }

    public static void setMti(ISOMsg isoMsg, String mti) {
        try {
            isoMsg.setMTI(mti);
        } catch (ISOException e) {
            throw new IllegalArgumentException("Could not set MTI", e);
        }
    }

    public static void copyFields(ISOMsg from, ISOMsg to) {
        for (int i = 1; i <= 128; i++) {
            if (from.hasField(i)) {
                to.set(i, from.getString(i));
            }
        }
    }

    public static long extractAmount(String field) throws IllegalArgumentException {
        if (StringUtils.isEmpty(field)) {
            return 0;
        }
        return Long.valueOf(field);
    }

    public static Date extractDateTime(String field) throws ParseException {
        if (StringUtils.isEmpty(field)) {
            return null;
        }
        SimpleDateFormat sd = new SimpleDateFormat();
        field = String.valueOf(Calendar.getInstance().get(Calendar.YEAR)) + field;
        sd.applyPattern("yyyyMMddHHmmss");
        return sd.parse(field);
    }

    public static Date extractDate(String field) throws ParseException {
        if (StringUtils.isEmpty(field)) {
            return null;
        }
        SimpleDateFormat sd = new SimpleDateFormat();
        sd.setLenient(false);
        field = String.valueOf(Calendar.getInstance().get(Calendar.YEAR)) + field;
        sd.applyPattern("yyyyMMdd");
        return sd.parse(field);
    }

    public static Date extractDate(String field, String pattern) throws ParseException {
        if (StringUtils.isEmpty(field) || StringUtils.isEmpty(pattern)) {
            return null;
        }
        SimpleDateFormat sd = new SimpleDateFormat();
        sd.applyPattern(pattern);
        return sd.parse(field);
    }

    public static Date extractTime(String field) throws ParseException {
        if (StringUtils.isEmpty(field)) {
            return null;
        }
        SimpleDateFormat sd = new SimpleDateFormat();
        field = String.format("%04d%02d%02d%s", Calendar.getInstance().get(Calendar.YEAR), (Calendar.getInstance().get(Calendar.MONTH) + 1),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH), field);
        sd.applyPattern("yyyyMMddHHmmss");
        return sd.parse(field);
    }

    public static Date extractExpiration(String field) throws ParseException {
        if (StringUtils.isEmpty(field)) {
            return null;
        }
        SimpleDateFormat sd = new SimpleDateFormat();
        sd.applyPattern("yyMM");
        return sd.parse(field);
    }

    public static Long extractFee(String fee) {
        if (StringUtils.isEmpty(fee)) {
            return null;
        }
        String feeSign = fee.substring(0, 1);
        Long amount = Long.parseLong(fee.substring(1));
        if (feeSign.equals("D") && amount >= 0) {
            amount = 0 - amount;
        }
        return amount;
    }

    public static List<AdditionalAmount> extractAdditionalAmounts(ISOMsg isoMsg) throws UtilOperationException {
        if (isoMsg.hasField(54)) {
            String field54 = isoMsg.getString(54);
            try {
                int k = 0;
                List<AdditionalAmount> additionalAmounts = new ArrayList<>();
                while (k < field54.length()) {
                    AdditionalAmount additionalAmount = new AdditionalAmount();
                    String balance = field54.substring(k, k + BALANCE_LENGTH);
                    String accountType = balance.substring(0, 2);
                    String amountType = balance.substring(2, 4);
                    String currencyCode = balance.substring(4, 7);
                    String amountSign = balance.substring(7, 8);
                    Long amount = Long.parseLong(balance.substring(8, balance.length()));
                    if (amountSign.equals("D") && amount >= 0) {
                        amount = 0 - amount;
                    }
                    additionalAmount.setAccountType(accountType);
                    additionalAmount.setAmountType(amountType);
                    additionalAmount.setCurrencyCode(currencyCode);
                    additionalAmount.setAmount(amount);
                    additionalAmounts.add(additionalAmount);
                    k = k + BALANCE_LENGTH;
                }
                return additionalAmounts;
            } catch (Exception e) {
                throw new UtilOperationException("An error occurred while processing additional amount", e);
            }
        }
        return new ArrayList<>();
    }


    public static EmvData extractEmvData(String emvString) throws UtilOperationException {
        if (StringUtils.isEmpty(emvString)) {
            return null;
        }
        EmvData emvData = new EmvData();

        Map<String, String> emvPairs = extractKeyValuePairs(emvString);

        if (emvPairs.size() == 0) {
            return null;
        }
        emvData.setApplicationInterchangeProfile(emvPairs.get("82"));
        emvData.setHostResponseCode(emvPairs.get("8A"));
        emvData.setApplicationTransactionCounter(emvPairs.get("9F36"));
        emvData.setTransactionSequenceCounter(emvPairs.get("9F41"));
        emvData.setCryptogramInformationData(emvPairs.get("9F27"));
        emvData.setCvmResult(emvPairs.get("9F34"));
        emvData.setMerchantCategoryCode(emvPairs.get("9F15"));
        emvData.setIssuerApplicationData(emvPairs.get("9F10"));
        emvData.setTerminalCapabilities(emvPairs.get("9F33"));
        emvData.setTerminalType(emvPairs.get("9F35"));
        emvData.setTerminalVerificationResult(emvPairs.get("95"));
        emvData.setUnpredictableNumber(emvPairs.get("9F37"));
        emvData.setAcquirerIdentifier(emvPairs.get("9F01"));
        emvData.setApplicationIdentifierCard(emvPairs.get("4F"));
        emvData.setApplicationIdentifierTerminal(emvPairs.get("9F06"));
        emvData.setTransactionType(emvPairs.get("9C"));
        if (!StringUtils.isEmpty(emvPairs.get("9F02"))) {
            emvData.setAuthorizedAmount(extractAmount(emvPairs.get("9F02")));
        }
        if (!StringUtils.isEmpty(emvPairs.get("9F03"))) {
            emvData.setAmountOther(extractAmount(emvPairs.get("9F03")));
        }
        emvData.setApplicationExpiryDate(emvPairs.get("5F24"));
        emvData.setApplicationPAN(emvPairs.get("5A"));
        emvData.setApplicationPANSequenceNumber(emvPairs.get("5F34"));
        emvData.setTerminalCountryCode(emvPairs.get("9F1A"));
        emvData.setTrack2EquivalentData(emvPairs.get("57"));
        emvData.setTransactionCurrencyCode(emvPairs.get("5F2A"));
        emvData.setTransactionDate(emvPairs.get("9A"));
        emvData.setCryptogram(emvPairs.get("9F26"));
        emvData.setScriptTemplate1(emvPairs.get("71"));
        emvData.setScriptTemplate2(emvPairs.get("72"));
        emvData.setIssuerAuthenticationData(emvPairs.get("91"));

        return emvData;
    }

    private static Map<String, String> extractKeyValuePairs(String emvString) throws UtilOperationException {
        int fieldIndex = 0;
        Map<String, String> emvPairs = new HashMap<>();
        while (fieldIndex < emvString.length()) {
            byte[] firstByte;
            String key;
            String value;
            int valueLength;
            try {
                firstByte = Hex.decodeHex(emvString.substring(fieldIndex, fieldIndex + 2).toCharArray());
            } catch (DecoderException e) {
                throw new UtilOperationException("An error occurred while reading emv data ", e);
            }

            if ((firstByte[0] & 0x1F) == 0x1F) {
                key = emvString.substring(fieldIndex, fieldIndex + 4).toUpperCase();
                fieldIndex = fieldIndex + 4;
            } else {
                key = emvString.substring(fieldIndex, fieldIndex + 2).toUpperCase();
                fieldIndex = fieldIndex + 2;
            }

            try {
                valueLength = Hex.decodeHex(emvString.substring(fieldIndex, fieldIndex + 2).toCharArray())[0];
                valueLength = valueLength * 2;
                fieldIndex += 2;
            } catch (DecoderException e) {
                throw new UtilOperationException("An error occurred while reading emv data ", e);
            }
            try {
                value = emvString.substring(fieldIndex, fieldIndex + valueLength);
            } catch (IndexOutOfBoundsException e) {
                throw new UtilOperationException("An error occurred while reading emv data", e);
            }
            fieldIndex += valueLength;

            emvPairs.put(key, value.toUpperCase());
        }
        return emvPairs;
    }

    public static OriginalDataElements extractOriginalDataElements(ISOMsg isoMsg) throws UtilOperationException {
        try {
            if (isoMsg.hasField(90)) {
                OriginalDataElements originalDataElements = new OriginalDataElements();
                String field90 = isoMsg.getString(90);
                originalDataElements.setMti(field90.substring(0, 4));
                originalDataElements.setStan(field90.substring(4, 10));
                originalDataElements.setTransmissionDateTime(field90.substring(10, 20));
                originalDataElements.setAcquiringInstitutionIdCode(field90.substring(20, 31));
                originalDataElements.setForwardingInstitutionIdCode(field90.substring(31, 42));
                return originalDataElements;
            }
        } catch (Exception e) {
            throw new UtilOperationException("Could not extract original data elements", e);
        }
        return null;
    }

    public static ReplacementAmounts extractReplacementAmounts(ISOMsg isoMsg) throws UtilOperationException {
        try {
            if (isoMsg.hasField(95)) {
                ReplacementAmounts replacementAmounts = new ReplacementAmounts();
                String field95 = isoMsg.getString(95);
                replacementAmounts.setActualAmountTransaction(Long.parseLong(field95.substring(0, 12)));
                replacementAmounts.setActualAmountSettlement(Long.parseLong(field95.substring(12, 24)));
                replacementAmounts.setActualAmountTransactionFee(extractFee(field95.substring(24, 33)));
                replacementAmounts.setActualAmountSettlementFee(extractFee(field95.substring(33, 42)));
                return replacementAmounts;
            }
        } catch (Exception e) {
            throw new UtilOperationException("Could not extract replacement amounts", e);
        }
        return null;
    }

    public static byte[] emvDataToIsoBytes(EmvData emvData) throws UtilOperationException {
        if (emvData == null) {
            return new byte[]{};
        }

        String emvString = emvDataToIsoString(emvData);

        byte[] isoBytes;
        try {
            isoBytes = Hex.decodeHex(emvString.toUpperCase().toCharArray());
        } catch (DecoderException e) {
            throw new UtilOperationException("An error occurred while decoding emv string", e);
        }
        return isoBytes;
    }

    public static String emvDataToIsoString(EmvData emvData) {
        if (emvData == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(createStringFromTLV("82", emvData.getApplicationInterchangeProfile()));
        stringBuilder.append(createStringFromTLV("8A", emvData.getHostResponseCode()));
        stringBuilder.append(createStringFromTLV("9F36", emvData.getApplicationTransactionCounter()));
        stringBuilder.append(createStringFromTLV("9F41", emvData.getTransactionSequenceCounter()));
        stringBuilder.append(createStringFromTLV("9F27", emvData.getCryptogramInformationData()));
        stringBuilder.append(createStringFromTLV("9F34", emvData.getCvmResult()));
        stringBuilder.append(createStringFromTLV("9F15", emvData.getMerchantCategoryCode()));
        stringBuilder.append(createStringFromTLV("9F10", emvData.getIssuerApplicationData()));
        stringBuilder.append(createStringFromTLV("9F33", emvData.getTerminalCapabilities()));
        stringBuilder.append(createStringFromTLV("9F35", emvData.getTerminalType()));
        stringBuilder.append(createStringFromTLV("95", emvData.getTerminalVerificationResult()));
        stringBuilder.append(createStringFromTLV("9F37", emvData.getUnpredictableNumber()));
        stringBuilder.append(createStringFromTLV("9F01", emvData.getAcquirerIdentifier()));
        stringBuilder.append(createStringFromTLV("9C", emvData.getTransactionType()));
        stringBuilder.append(createStringFromAmountTLV("9F02", emvData.getAuthorizedAmount()));
        stringBuilder.append(createStringFromAmountTLV("9F03", emvData.getAmountOther()));
        stringBuilder.append(createStringFromTLV("5F24", emvData.getApplicationExpiryDate()));
        stringBuilder.append(createStringFromTLV("5A", emvData.getApplicationPAN()));
        stringBuilder.append(createStringFromTLV("5F34", emvData.getApplicationPANSequenceNumber()));
        stringBuilder.append(createStringFromTLV("9F1A", emvData.getTerminalCountryCode()));
        stringBuilder.append(createStringFromTLV("57", emvData.getTrack2EquivalentData()));
        stringBuilder.append(createStringFromTLV("5F2A", emvData.getTransactionCurrencyCode()));
        stringBuilder.append(createStringFromTLV("9A", emvData.getTransactionDate()));
        stringBuilder.append(createStringFromTLV("9F26", emvData.getCryptogram()));
        stringBuilder.append(createStringFromTLV("9F06", emvData.getApplicationIdentifierTerminal()));
        stringBuilder.append(createStringFromTLV("4F", emvData.getApplicationIdentifierCard()));
        stringBuilder.append(createStringFromTLV("71", emvData.getScriptTemplate1()));
        stringBuilder.append(createStringFromTLV("72", emvData.getScriptTemplate2()));

        return stringBuilder.toString();
    }

    private static String createStringFromAmountTLV(String tag, Long value) {
        //divide length by two because one byte is 2 string literals
        String val = String.format("%012d", value);
        int stringLength = val.length() / 2;
        byte bytelength = (byte) stringLength;
        String lengthInHex = String.valueOf(Hex.encodeHex(new byte[]{bytelength}));
        if (stringLength == 0) {
            val = "";
        }
        return tag + lengthInHex + val;
    }

    private static String createStringFromTLV(String tag, String value) {
        if (value == null) {
            return "";
        }
        if ((value.length() % 2) != 0) {
            value = StringUtils.leftPad(value, value.length() + 1, '0');
        }
        int stringLength = value.length() / 2;
        byte bytelength = (byte) stringLength;
        String lengthInHex = String.valueOf(Hex.encodeHex(new byte[]{bytelength}));
        return tag + lengthInHex + value;
    }

    public static byte[] buildKeyExchangeField53(byte[] key, byte[] keyCheckValue) {
        byte[] field53Bytes = new byte[48];
        System.arraycopy(key, 0, field53Bytes, 0, key.length);
        System.arraycopy(keyCheckValue, 0, field53Bytes, key.length, 3);

        return field53Bytes;
    }

    public static String convertAdditionalAmountsToString(List<AdditionalAmount> additionalAmounts) {
        if (additionalAmounts == null || additionalAmounts.isEmpty()) {
            return null;
        }
        StringBuilder additionalAmountString = new StringBuilder();
        for (AdditionalAmount additionalAmount : additionalAmounts) {
            additionalAmountString.append(toAdditionalAmountString(additionalAmount.getAccountType(),
                    additionalAmount.getAmountType(), additionalAmount.getCurrencyCode(), additionalAmount.getAmount()));
        }
        return additionalAmountString.toString();
    }

    private static String toAdditionalAmountString(String accountType, String amountType, String currencyCode, long amount) {
        String feeSign = (amount >= 0) ? "C" : "D";
        amount = amount >= 0 ? amount : 0 - amount;
        return String.format("%s%s%s%s%012d", accountType, amountType, currencyCode, feeSign, amount);
    }

    public static String originalDataElementsToString(OriginalDataElements originalDataElements) {
        if (originalDataElements == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(StringUtils.isNotEmpty(originalDataElements.getMti()) ? originalDataElements.getMti() : "0000");
        builder.append(originalDataElements.getStan());
        builder.append(originalDataElements.getTransmissionDateTime());
        builder.append(StringUtils.leftPad(
                (StringUtils.isEmpty(originalDataElements.getAcquiringInstitutionIdCode()) ?
                        "" : originalDataElements.getAcquiringInstitutionIdCode()), 11, '0'));
        builder.append(StringUtils.leftPad(
                (StringUtils.isEmpty(originalDataElements.getForwardingInstitutionIdCode()) ?
                        "" : originalDataElements.getForwardingInstitutionIdCode()), 11, '0'));
        return builder.toString();
    }

    public static String replacementAmountsToString(ReplacementAmounts replacementAmounts) {
        if (replacementAmounts == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        long actualAmountTransaction = (replacementAmounts.getActualAmountTransaction() == null) ? 0 : replacementAmounts.getActualAmountTransaction();
        long actualAmountSettlement = (replacementAmounts.getActualAmountSettlement() == null) ? 0 : replacementAmounts.getActualAmountSettlement();
        long actualAmountTransactionFee = (replacementAmounts.getActualAmountTransactionFee() == null) ? 0 : replacementAmounts.getActualAmountTransactionFee();
        long actualAmountSettlementFee = (replacementAmounts.getActualAmountSettlementFee() == null) ? 0 : replacementAmounts.getActualAmountSettlementFee();
        builder.append(String.format("%012d", actualAmountTransaction));
        builder.append(String.format("%012d", actualAmountSettlement));
        builder.append(convertFeeToString(actualAmountTransactionFee));
        builder.append(convertFeeToString(actualAmountSettlementFee));

        return builder.toString();
    }

    public static String convertFeeToString(Long amount) {
        if (amount == null) {
            return null;
        }
        String feeSign = (amount >= 0) ? "C" : "D";
        amount = amount >= 0 ? amount : 0 - amount;
        return String.format("%s%08d", feeSign, amount);
    }

    public static String timeLocalTransaction(Date transDate) {
        return ISODate.getTime(transDate, TimeZone.getTimeZone(ZoneId.systemDefault()));
    }

    public static String transmissionDateAndTime(Date transDate) {
        return ISODate.getDateTime(transDate, TimeZone.getTimeZone(ZoneId.systemDefault()));
    }

    public static String dateLocalTransaction(Date transDate) {
        return ISODate.getDate(transDate, TimeZone.getTimeZone(ZoneId.systemDefault()));
    }
}

package com.tms.postbridge.util;

import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jpos.iso.ISOMsg;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PostBridgeIsoUtil {

    private static final int BALANCE_LENGTH = 20;
    private static Marshaller marshaller;
    private static Unmarshaller unmarshaller;

    private PostBridgeIsoUtil() {

    }

    public static void setMarshaller(Marshaller marshaller) {
        PostBridgeIsoUtil.marshaller = marshaller;
    }

    public static void setUnmarshaller(Unmarshaller unmarshaller) {
        PostBridgeIsoUtil.unmarshaller = unmarshaller;
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

    public static long extractAmount(String field) throws IllegalArgumentException {
        if (StringUtils.isEmpty(field)) {
            return 0;
        }
        return Long.parseLong(field);
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


    public static synchronized IccData extractXMLICCData(ISOMsg isoMsg) throws UtilOperationException {
        IccData iccData = new IccData();
        if (isoMsg.hasField("127.25")) {
            try {
                iccData = (IccData) unmarshaller.unmarshal(new StringReader(isoMsg.getString("127.25")));
            } catch (JAXBException e) {
                throw new UtilOperationException("Could not unmarshal icc data in field 127.25", e);
            }
        }
        return iccData;
    }

    public static synchronized String iccDataToXmlString(IccData iccData) throws UtilOperationException {
        if (iccData == null) {
            return null;
        }
        if (iccData.getIccRequest() == null && iccData.getIccResponse() == null) {
            return null;
        }
        StringWriter xmlString = new StringWriter();
        xmlString.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?> ");
        try {
            marshaller.marshal(iccData, xmlString);
        } catch (JAXBException e) {
            throw new UtilOperationException("Could not marshall icc data to xml string", e);
        }
        return xmlString.toString();
    }

    public static EmvData iccRequestToEmvData(IccRequest iccRequest) {
        if (iccRequest == null) {
            return null;
        }
        EmvData emvData = new EmvData();
        if (StringUtils.isNotEmpty(iccRequest.getAmountAuthorized())) {
            emvData.setAuthorizedAmount(Long.parseLong(iccRequest.getAmountAuthorized()));
        }
        if (StringUtils.isNotEmpty(iccRequest.getAmountOther())) {
            emvData.setAmountOther(Long.parseLong(iccRequest.getAmountOther()));
        }
        emvData.setApplicationIdentifierCard(iccRequest.getApplicationIdentifier());
        emvData.setApplicationInterchangeProfile(iccRequest.getApplicationInterchangeProfile());
        emvData.setApplicationTransactionCounter(iccRequest.getApplicationTransactionCounter());
        emvData.setApplicationUsageControl(iccRequest.getApplicationUsageControl());
        emvData.setHostResponseCode(iccRequest.getAuthorizationResponseCode());
        emvData.setCardAuthReliabilityIndicator(iccRequest.getCardAuthenticationReliabilityIndicator());
        emvData.setCardAuthenticationResultCode(iccRequest.getCardAuthenticationResultsCode());
        emvData.setChipConditionCode(iccRequest.getChipConditionCode());
        emvData.setCryptogram(iccRequest.getCryptogram());
        emvData.setCryptogramInformationData(iccRequest.getCryptogramInformationData());
        emvData.setCvmList(iccRequest.getCvmList());
        emvData.setCvmResult(iccRequest.getCvmResults());
        emvData.setInterfaceDeviceSerialNumber(iccRequest.getInterfaceDeviceSerialNumber());
        emvData.setIssuerActionCode(iccRequest.getIssuerActionCode());
        emvData.setIssuerApplicationData(iccRequest.getIssuerApplicationData());
        emvData.setIssuerScriptResults(iccRequest.getIssuerScriptResults());
        emvData.setTerminalApplicationVersionNumber(iccRequest.getTerminalApplicationVersionNumber());
        emvData.setTerminalCapabilities(iccRequest.getTerminalCapabilities());
        emvData.setTerminalCountryCode(iccRequest.getTerminalCountryCode());
        emvData.setTerminalType(iccRequest.getTerminalType());
        emvData.setTerminalVerificationResult(iccRequest.getTerminalVerificationResult());
        emvData.setTransactionCategoryCode(iccRequest.getTransactionCategoryCode());
        emvData.setTransactionCurrencyCode(iccRequest.getTransactionCurrencyCode());
        emvData.setTransactionDate(iccRequest.getTransactionDate());
        emvData.setTransactionSequenceCounter(iccRequest.getTransactionSequenceCounter());
        emvData.setTransactionType(iccRequest.getTransactionType());
        emvData.setUnpredictableNumber(iccRequest.getUnpredictableNumber());

        return emvData;
    }

    public static EmvData iccResponseToEmvData(IccResponse iccResponse) {
        if (iccResponse == null) {
            return null;
        }
        EmvData emvData = new EmvData();
        emvData.setApplicationTransactionCounter(iccResponse.getApplicationTransactionCounter());
        emvData.setCardAuthenticationResultCode(iccResponse.getCardAuthResultsCode());
        emvData.setIssuerAuthenticationData(iccResponse.getIssuerAuthData());
        emvData.setScriptTemplate1(iccResponse.getIssuerScriptTemplate1());
        emvData.setScriptTemplate2(iccResponse.getIssuerScriptTemplate2());

        return emvData;
    }

    public static IccRequest emvDataToIccRequest(EmvData emvData) {
        if (emvData == null) {
            return null;
        }
        IccRequest iccRequest = new IccRequest();
        iccRequest.setAmountAuthorized(String.format("%012d", emvData.getAuthorizedAmount()));
        iccRequest.setAmountOther(String.format("%012d", emvData.getAmountOther()));
        iccRequest.setApplicationIdentifier(emvData.getAcquirerIdentifier());
        iccRequest.setApplicationInterchangeProfile(emvData.getApplicationInterchangeProfile());
        iccRequest.setApplicationTransactionCounter(emvData.getApplicationTransactionCounter());
        iccRequest.setApplicationUsageControl(emvData.getApplicationUsageControl());
        iccRequest.setAuthorizationResponseCode(emvData.getHostResponseCode());//likely host response code
        iccRequest.setCardAuthenticationReliabilityIndicator(emvData.getCardAuthReliabilityIndicator());
        iccRequest.setCardAuthenticationResultsCode(emvData.getCardAuthenticationResultCode());
        iccRequest.setChipConditionCode(emvData.getChipConditionCode());
        iccRequest.setCryptogram(emvData.getCryptogram());
        iccRequest.setCryptogramInformationData(emvData.getCryptogramInformationData());
        iccRequest.setCvmList(emvData.getCvmList());
        iccRequest.setCvmResults(emvData.getCvmResult());
        iccRequest.setInterfaceDeviceSerialNumber(emvData.getInterfaceDeviceSerialNumber());
        iccRequest.setIssuerActionCode(emvData.getIssuerActionCode());
        iccRequest.setIssuerApplicationData(emvData.getIssuerApplicationData());
        iccRequest.setIssuerScriptResults(emvData.getIssuerScriptResults());
        iccRequest.setTerminalApplicationVersionNumber(emvData.getTerminalApplicationVersionNumber());
        iccRequest.setTerminalCapabilities(emvData.getTerminalCapabilities());
        iccRequest.setTerminalCountryCode(StringUtils.isEmpty(emvData.getTerminalCountryCode()) ? null
                : String.format("%03d", Integer.parseInt(emvData.getTerminalCountryCode())));
        iccRequest.setTerminalType(StringUtils.isEmpty(emvData.getTerminalType()) ? null
                : String.format("%02d", Integer.parseInt(emvData.getTerminalType())));
        iccRequest.setTerminalVerificationResult(emvData.getTerminalVerificationResult());
        iccRequest.setTransactionCategoryCode(emvData.getTransactionCategoryCode());
        iccRequest.setTransactionCurrencyCode(StringUtils.isEmpty(emvData.getTransactionCurrencyCode()) ? null
                : String.format("%03d", Integer.parseInt(emvData.getTransactionCurrencyCode())));
        iccRequest.setTransactionDate(emvData.getTransactionDate());
        iccRequest.setTransactionSequenceCounter(emvData.getTransactionSequenceCounter());
        iccRequest.setTransactionType(emvData.getTransactionType());
        iccRequest.setUnpredictableNumber(emvData.getUnpredictableNumber());
        return iccRequest;
    }

    public static IccResponse emvDataToIccResponse(EmvData emvData) {
        if (emvData == null) {
            return null;
        }
        IccResponse iccResponse = new IccResponse();
        boolean iccResponseNotEmpty = false;
        if (StringUtils.isNotEmpty(emvData.getApplicationTransactionCounter())) {
            iccResponseNotEmpty = true;
            iccResponse.setApplicationTransactionCounter(emvData.getApplicationTransactionCounter());
        }
        if (StringUtils.isNotEmpty(emvData.getCardAuthenticationResultCode())) {
            iccResponseNotEmpty = true;
            iccResponse.setCardAuthResultsCode(emvData.getCardAuthenticationResultCode());
        }
        if (StringUtils.isNotEmpty(emvData.getIssuerAuthenticationData())) {
            iccResponseNotEmpty = true;
            iccResponse.setIssuerAuthData(emvData.getIssuerAuthenticationData());
        }
        if (StringUtils.isNotEmpty(emvData.getScriptTemplate1())) {
            iccResponseNotEmpty = true;
            iccResponse.setIssuerScriptTemplate1(emvData.getScriptTemplate1());
        }
        if (StringUtils.isNotEmpty(emvData.getScriptTemplate2())) {
            iccResponseNotEmpty = true;
            iccResponse.setIssuerScriptTemplate2(emvData.getScriptTemplate2());
        }
        return (iccResponseNotEmpty) ? iccResponse : null;
    }

    public static String emvDataToRequestIsoString(EmvData emvData) throws UtilOperationException {
        if (emvData == null) {
            return null;
        }
        IccRequest iccRequest = emvDataToIccRequest(emvData);
        IccData iccData = new IccData();
        iccData.setIccRequest(iccRequest);
        return iccDataToXmlString(iccData);
    }

    public static String emvDataToResponseIsoString(EmvData emvData) throws UtilOperationException {
        if (emvData == null) {
            return null;
        }
        IccResponse iccResponse = emvDataToIccResponse(emvData);

        IccData iccData = new IccData();
        iccData.setIccResponse(iccResponse);
        return iccDataToXmlString(iccData);

    }
}



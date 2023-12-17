package com.tms.postbridge.processors;

import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.model.RequestType;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.util.IsoUtil;
import com.tms.postbridge.PostBridgeInterchange;
import com.tms.postbridge.model.PostBridgeUserParameters;
import com.tms.postbridge.util.PostBridgeSinkIsoChannelAdapter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jpos.iso.ISODate;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class FundsTransferSinkProcessor implements PostBridgeSinkTransactionProcessor {

    @Override
    public boolean canConvert(RequestType requestType) {
        return RequestType.PURCHASE == requestType;
    }

    @Override
    public ISOMsg toISOMsg(TransactionRequest transactionRequest) throws TransactionProcessingException {
        log.trace("Sending a funds transfer request");
        ISOMsg isoMsg = new ISOMsg();

        try {
            isoMsg.setMTI("0200");
            String processingCode = transactionRequest.getProcessingCode();
            log.trace("Processing Code received from Bankly-TMS: >>> "+processingCode);
            String fromAndToAccountType = StringUtils.isEmpty(processingCode) ? "0000" : processingCode.substring(2);
            String iswProcessingCode;
            if(

                            transactionRequest.getPan().startsWith("519911") ||
                            transactionRequest.getPan().startsWith("492069")
//                            transactionRequest.getPan().startsWith("521090") ||
//                            transactionRequest.getPan().startsWith("522899") ||
//                            transactionRequest.getPan().startsWith("525634") ||
//                            transactionRequest.getPan().startsWith("588655") ||
//                            transactionRequest.getPan().startsWith("422522") ||
//                            transactionRequest.getPan().startsWith("517868") ||
//                            transactionRequest.getPan().startsWith("519863") ||
//                            transactionRequest.getPan().startsWith("519885") ||
//                            transactionRequest.getPan().startsWith("404905") ||
//                            transactionRequest.getPan().startsWith("407591") ||
//                            transactionRequest.getPan().startsWith("420358") ||
//                            transactionRequest.getPan().startsWith("420359") ||
//                            transactionRequest.getPan().startsWith("422500") ||
//                            transactionRequest.getPan().startsWith("422584") ||
//                            transactionRequest.getPan().startsWith("422594") ||
//                            transactionRequest.getPan().startsWith("428223") ||
//                            transactionRequest.getPan().startsWith("539941")



            ) {
                iswProcessingCode="50" + fromAndToAccountType;
           } else {
                //Recommended to set isWProcessing Ccode to 500000
                iswProcessingCode = "500000";
           }
            log.trace("Processing Code Forwarded to ISW >>> "+iswProcessingCode);
            isoMsg.set(3, iswProcessingCode);
            PostBridgeSinkIsoChannelAdapter.transactionRequestToCommonIsoMsg(transactionRequest, isoMsg);

            PostBridgeUserParameters userParameters = ((PostBridgeInterchange) transactionRequest.getSinkInterchange()).getPostBridgeUserParameters();
            //transactionRequest.getca
            //transactionRequest
            //set field43
            String field43 =StringUtils.leftPad(String.valueOf(transactionRequest.getTransactionId()), 17, ' ');

            String tid = String.valueOf(transactionRequest.getTransactionId());
            String terminalid =transactionRequest.getTerminalId() != null ? transactionRequest.getTerminalId() :"" ;
            String f43=tid+ " "+terminalid;
            int f43size = f43.length();
            int rem = 36-f43size;
            String loc= transactionRequest.getCardAcceptorLocation() != null ? transactionRequest.getCardAcceptorLocation() :"";
            if(loc.length() > rem){
                loc = loc.substring(0,rem);
            }else{
                loc = StringUtils.rightPad(loc, rem, ' ');
            }
            f43=f43+loc+"LANG";
            //2HIG0010 TELLERPOINT LAGOS          LANG


            isoMsg.set(43,f43);
            isoMsg.set(40, extractF40(transactionRequest.getTrack2Data()));
            isoMsg.set(15, ISODate.getDate(new Date()));
            isoMsg.set(18, userParameters.getMcc());
            isoMsg.set(33, userParameters.getForwardingInstitutionId());
            isoMsg.set(32, "636092");

            isoMsg.set(28, IsoUtil.convertFeeToString((-1) * userParameters.getSurcharge()));
            isoMsg.set(103, userParameters.getTransferDestinationAccount());
            isoMsg.set(98, userParameters.getPayee());
            isoMsg.set(100, userParameters.getReceivingInstitutionId());
            isoMsg.set(123, "510101513344101");
            String f59 = null;
            isoMsg.set(59, f59);

            long nanoTime = System.nanoTime();
            isoMsg.set("127.002", String.valueOf(nanoTime));
            String f127033 = StringUtils.rightPad("", 24, " ")
                    + StringUtils.leftPad(transactionRequest.getStan(), 6, '0')
                    + StringUtils.leftPad(transactionRequest.getStan(), 6, '0')
                    + StringUtils.rightPad("", 12, " ");
            isoMsg.set("127.003", f127033);
            isoMsg.set("127.013", StringUtils.leftPad("000000 566", 17, ' '));
            isoMsg.set("127.033", userParameters.getExtendedTransactionType());

        } catch (ISOException | UtilOperationException e) {
            String msg = String.format("There was a channel error converting postbridge funds transfer message ex: %s", e.toString());
            throw new TransactionProcessingException(msg, e);
        }

        return isoMsg;
    }

    private String extractF40(String track2) throws IllegalArgumentException {
        if (StringUtils.isEmpty(track2)) {
            return "000";
        }
        try {
            char separator = ((track2.indexOf('=') != -1) ? '=' : 'D');
            int separatorIndex = track2.indexOf(separator);
            if (separatorIndex + 8 <= track2.length()) {/*track2 data with no servic restriction code*/
                return track2.substring(separatorIndex + 5, separatorIndex + 8);
            }
        } catch (NumberFormatException | IndexOutOfBoundsException ex) {
            throw new IllegalArgumentException(ex);
        }

        return "000";
    }

    @Override
    public TransactionResponse toTransactionResponse(ISOMsg isoMsg, TransactionRequest transactionRequest) throws TransactionProcessingException {
        if (isoMsg == null) {
            String msg = "Raw Response is null";
            throw new TransactionProcessingException(msg);
        }

        TransactionResponse transactionResponse = new TransactionResponse(transactionRequest);
        try {
            PostBridgeSinkIsoChannelAdapter.commonIsoMsgToTransactionResponse(isoMsg, transactionResponse);
        } catch (UtilOperationException e) {
            throw new TransactionProcessingException("There was an ISO error while converting the ISO message ex: %s", e);
        }

        return transactionResponse;
    }
}

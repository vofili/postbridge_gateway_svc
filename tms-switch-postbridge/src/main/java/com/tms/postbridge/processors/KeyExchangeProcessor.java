package com.tms.postbridge.processors;

import com.tms.lib.exceptions.CryptoException;
import com.tms.lib.exceptions.HsmException;
import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.hsm.HsmService;
import com.tms.lib.hsm.model.GeneratedKeyMessage;
import com.tms.lib.interchange.Interchange;
import com.tms.lib.interchange.InterchangeConfigService;
import com.tms.lib.model.RequestType;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.provider.StanProvider;
import com.tms.lib.security.Encrypter;
import com.tms.postbridge.util.PostBridgeSinkIsoChannelAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jpos.iso.ISODate;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeyExchangeProcessor implements PostBridgeSinkTransactionProcessor {

    private static final int DOUBLE_KEY_STRING_LENGTH = 32;
    private static final int KCV_LENGTH = 6;

    private final HsmService hsmService;
    private final Encrypter encrypter;
    private final InterchangeConfigService interchangeConfigService;

    public static TransactionRequest createKeyExchangeMessage(StanProvider stanProvider) {
        Date now = new Date();

        return TransactionRequest.builder()
                .requestType(RequestType.KEY_EXCHANGE)
                .stan(stanProvider.getNextStan())
                .transactionTime(ISODate.getTime(now))
                .transactionDate(ISODate.getDate(now))
                .transmissionDateTime(ISODate.getDateTime(now))
                .mti("0800")
                .build();
    }

    @Override
    public boolean canConvert(RequestType requestType) {
        return RequestType.KEY_EXCHANGE.equals(requestType);
    }

    @Override
    public ISOMsg toISOMsg(TransactionRequest transactionRequest) throws TransactionProcessingException {
        ISOMsg isoMsg = new ISOMsg();

        try {
            isoMsg.setMTI("0800");
            isoMsg.set(70, "101");
            PostBridgeSinkIsoChannelAdapter.transactionRequestToCommonIsoMsg(transactionRequest, isoMsg);
        } catch (UtilOperationException | ISOException e) {
            String msg = String.format("There was a channel error converting postbridge key exchange message ex: %s", e.toString());
            throw new TransactionProcessingException(msg, e);
        }

        return isoMsg;
    }

    @Override
    public TransactionResponse toTransactionResponse(ISOMsg isoMsg, TransactionRequest transactionRequest) throws TransactionProcessingException {
        if (isoMsg == null) {
            String msg = "Raw Response is null";
            throw new TransactionProcessingException(msg);
        }

        TransactionResponse transactionResponse = transactionRequest.constructResponse();
        try {
            PostBridgeSinkIsoChannelAdapter.commonIsoMsgToTransactionResponse(isoMsg, transactionResponse);
            transactionResponse.setKeyToKcvPair(extractMasterSessionKeyData(isoMsg));
        } catch (UtilOperationException e) {
            throw new TransactionProcessingException("There was an ISO error while converting the ISO message ex: %s", e);
        }

        return transactionResponse;
    }

    private Pair<String, String> extractMasterSessionKeyData(ISOMsg isoMsg) throws TransactionProcessingException {

        String sessionKey;
        String kcv;
        String fullKeyData;
        if (isoMsg.hasField(125)) {
            fullKeyData = isoMsg.getString(125);
        } else if (isoMsg.hasField(53)) {
            fullKeyData = isoMsg.getString(53);
        } else {
            throw new TransactionProcessingException("Error extracting key data, no key data found in both field 53 and field 125");
        }
        log.info("<<<<<<<<<<<<<<<<fullKeyData>>>>>>>>>>>>>>>>"+fullKeyData);
        sessionKey = fullKeyData.substring(0, DOUBLE_KEY_STRING_LENGTH);
        log.info("<<<<<<<<<<<<<<<<sessionKey>>>>>>>>>>>>>>>>"+sessionKey);
        kcv = fullKeyData.substring(DOUBLE_KEY_STRING_LENGTH, DOUBLE_KEY_STRING_LENGTH + KCV_LENGTH);
        log.info("<<<<<<<<<<<<<<<<sessionKey>>>>>>>>>>>>>>>>"+kcv);

        return new ImmutablePair<>(sessionKey, kcv);
    }

    @Override
    public boolean canProcess(RequestType requestType) {
        return canConvert(requestType);
    }

    @Override
    public void process(TransactionResponse transactionResponse, Interchange interchange) throws TransactionProcessingException {

        log.info("Processing key exchange channel response");
        Pair<String, String> keyToKcvPair = transactionResponse.getKeyToKcvPair();

        if (keyToKcvPair == null) {
            throw new TransactionProcessingException("No key data was found for processing");
        }

        String keyCheckGotten = keyToKcvPair.getRight();

        String zpkUnderZmk = keyToKcvPair.getLeft();

        log.info(String.format("zpkUnderZmk is %s, keyCheckGotten is %s", zpkUnderZmk, keyCheckGotten));

        String encryptedInterchangeKey = interchange.getConfig().getEncryptedInterchangeKey();
        if (encryptedInterchangeKey == null) {
            throw new TransactionProcessingException("Cannot process key exchange response. No key was configured for this interchange");
        }

        String interchangeKey;
        try {
            interchangeKey = encrypter.decrypt(encryptedInterchangeKey);
        } catch (CryptoException e) {
            throw new TransactionProcessingException("Could not decrypt interchange key", e);
        }

        GeneratedKeyMessage generatedKeyMessage;
        try {
            generatedKeyMessage = hsmService.convertZpkUnderZmkToZpkUnderLmk(zpkUnderZmk, interchangeKey);
        } catch (HsmException e) {
            throw new TransactionProcessingException("Error converting zpk from zmk to lmk encryption", e);
        }
        String generatedKeyCheck = generatedKeyMessage.getKeyCheckValue();

        if (StringUtils.isEmpty(generatedKeyCheck)) {
            throw new TransactionProcessingException("Invalid key check generated");
        }
        log.info(String.format("Key check Gotten is %s, Generated key check is %s", keyCheckGotten, generatedKeyCheck));

        if (!(generatedKeyCheck.equalsIgnoreCase(keyCheckGotten))) {
            throw new TransactionProcessingException("Key check does not match, Are the public keys the same?");
        }

        String zpkUnderLmk = generatedKeyMessage.getKeyUnderLmk();
        if (StringUtils.isEmpty(zpkUnderLmk)) {
            throw new TransactionProcessingException("Invalid zpk under lmk was returned");
        }

        String encryptedZpk;
        try {
            encryptedZpk = encrypter.encrypt(zpkUnderLmk.toUpperCase());
        } catch (CryptoException e) {
            throw new TransactionProcessingException("Could not encrypt zpk", e);
        }

        try {
            interchangeConfigService.updateInterchangeSinkZpk(encryptedZpk, interchange.getConfig().getId());
        } catch (Exception e) {
            throw new TransactionProcessingException("Could not update interchange sink zpk", e);
        }


        interchange.getConfig().setEncryptedSinkZpk(encryptedZpk);
        log.info("Successfully processed key exchange response");
    }
}

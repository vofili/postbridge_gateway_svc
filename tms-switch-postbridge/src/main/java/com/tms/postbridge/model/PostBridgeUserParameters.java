package com.tms.postbridge.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.lib.exceptions.InvalidOperationException;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

@Data
public class PostBridgeUserParameters {

    private String transferDestinationAccount;
    private String payee;
    private String receivingInstitutionId;
    private String extendedTransactionType;
    private long surcharge;
    private String forwardingInstitutionId;
    private String mcc;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static PostBridgeUserParameters getUserParameters(String interchangeSpecificData) throws InvalidOperationException {
        try {
            if (interchangeSpecificData == null || interchangeSpecificData.isEmpty()) {
                throw new InvalidOperationException("User parameters data cannot be empty");
            }
            OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            return OBJECT_MAPPER.readValue(interchangeSpecificData, PostBridgeUserParameters.class);
        } catch (IOException e) {
            throw new InvalidOperationException("Could not read user parameters data", e);
        }
    }

    public boolean isRestartRequired(PostBridgeUserParameters postBridgeUserParameters) {
        if (postBridgeUserParameters == null) {
            return true;
        }

        return !StringUtils.equals(this.transferDestinationAccount, postBridgeUserParameters.getTransferDestinationAccount()) ||
                !StringUtils.equals(this.payee, postBridgeUserParameters.getPayee()) ||
                this.surcharge != postBridgeUserParameters.getSurcharge() ||
                !StringUtils.equals(this.extendedTransactionType, postBridgeUserParameters.getExtendedTransactionType()) ||
                !StringUtils.equals(this.receivingInstitutionId, postBridgeUserParameters.getReceivingInstitutionId()) ||
                !StringUtils.equals(this.forwardingInstitutionId, postBridgeUserParameters.getForwardingInstitutionId()) ||
                !StringUtils.equals(this.mcc, postBridgeUserParameters.getMcc());
    }
}

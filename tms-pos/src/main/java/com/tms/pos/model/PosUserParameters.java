package com.tms.pos.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.lib.exceptions.InvalidOperationException;
import lombok.Data;

import java.io.IOException;

@Data
public class PosUserParameters {

    private boolean forwarding;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static PosUserParameters getUserParameters(String interchangeSpecificData) throws InvalidOperationException {
        try {
            if (interchangeSpecificData == null || interchangeSpecificData.isEmpty()) {
                throw new InvalidOperationException("User parameters data cannot be empty");
            }
            OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            return OBJECT_MAPPER.readValue(interchangeSpecificData, PosUserParameters.class);
        } catch (IOException e) {
            throw new InvalidOperationException("Could not read user parameters data", e);
        }
    }

    public boolean isRestartRequired(PosUserParameters postBridgeUserParameters) {
        if (postBridgeUserParameters == null) {
            return true;
        }

        return this.forwarding != postBridgeUserParameters.isForwarding();
    }
}

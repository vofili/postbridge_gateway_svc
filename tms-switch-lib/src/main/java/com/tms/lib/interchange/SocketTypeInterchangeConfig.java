package com.tms.lib.interchange;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.lib.exceptions.UtilOperationException;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

@Data
public class SocketTypeInterchangeConfig {


    private int socketTimeOut;
    private int sourcePort;
    private String sinkPorts;
    private String sourceHost;
    private String sinkHost;
    private int pollingInterval;
    private boolean useSSL;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static SocketTypeInterchangeConfig getConfig(String interchangeSpecificData) throws UtilOperationException {
        try {
            if (interchangeSpecificData == null || interchangeSpecificData.isEmpty()) {
                throw new UtilOperationException("Socket type config data cannot be empty");
            }
            OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            return OBJECT_MAPPER.readValue(interchangeSpecificData, SocketTypeInterchangeConfig.class);
        } catch (IOException e) {
            throw new UtilOperationException("Could not read socket type data", e);
        }
    }

    public boolean isRestartRequired(SocketTypeInterchangeConfig socketTypeInterchangeConfig, InterchangeMode interchangeMode) {
        if (socketTypeInterchangeConfig == null) {
            return true;
        }
        boolean valuesChanged = this.socketTimeOut != socketTypeInterchangeConfig.getSocketTimeOut()
                || this.pollingInterval != socketTypeInterchangeConfig.getPollingInterval();
        switch (interchangeMode) {
            case SourceMode:
                return isSourceChanged(socketTypeInterchangeConfig.getSourceHost(), socketTypeInterchangeConfig.getSourcePort())
                        || valuesChanged;
            case SinkMode:
                return isSinkChange(socketTypeInterchangeConfig)
                        || valuesChanged;
            default:
                return false;
        }
    }

    private boolean isSourceChanged(String sourceHost, int sourcePort) {
        return !StringUtils.equals(this.getSourceHost(),sourceHost)
                || this.getSourcePort() != sourcePort;
    }

    private boolean isSinkChange(SocketTypeInterchangeConfig config) {
        return !StringUtils.equals(this.getSinkHost(), config.getSinkHost())
                || !StringUtils.equals(this.getSinkPorts(), config.getSinkPorts())
                || this.useSSL != config.isUseSSL();
    }

}

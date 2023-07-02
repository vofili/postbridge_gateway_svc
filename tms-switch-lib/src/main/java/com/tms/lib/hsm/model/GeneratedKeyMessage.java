package com.tms.lib.hsm.model;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class GeneratedKeyMessage {
    private String keyUnderEncryptionKey;
    private String keyUnderLmk;
    private String keyCheckValue;

    public String getKeyUnderEncryptionKey() {
        return StringUtils.capitalize(keyUnderEncryptionKey);
    }

    public String getKeyUnderLmk() {
        return StringUtils.capitalize(keyUnderLmk);
    }

    public String getKeyCheckValue() {
        return StringUtils.capitalize(keyCheckValue);
    }

}

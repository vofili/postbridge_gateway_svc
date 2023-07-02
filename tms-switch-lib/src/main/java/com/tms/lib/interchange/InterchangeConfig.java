package com.tms.lib.interchange;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterchangeConfig {

    @Id
    @GeneratedValue
    private int id;
    @NotNull
    @Column(unique = true)
    private String name;
    private String description;
    @NotNull
    private String typeName;
    private boolean active;
    private String encryptedSinkZpk;
    private String interchangeSpecificData;
    private boolean pinTranslationRequired;
    private String encryptedInterchangeKey;
    private String code;
    @Enumerated(EnumType.STRING)
    private InterchangeMode interchangeMode;

    @Transient
    private SocketTypeInterchangeConfig socketTypeInterchangeConfig;

    private Date dateCreated = new Date();

    public boolean isRestartRequired(InterchangeConfig config) {
        return config == null
                || (keyConfigChanged(config)
                || this.isActive() != config.isActive());
    }

    private boolean keyConfigChanged(InterchangeConfig config) {
        return (this.isPinTranslationRequired() != config.isPinTranslationRequired()) || (this.isPinTranslationRequired())
                && (this.getEncryptedInterchangeKey() != null) && (config.getEncryptedInterchangeKey() != null)
                && !this.getEncryptedInterchangeKey().equals(config.getEncryptedInterchangeKey());
    }
}

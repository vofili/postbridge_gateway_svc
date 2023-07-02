package com.tms.service.apimodel;

import com.tms.lib.interchange.InterchangeConfig;
import com.tms.lib.interchange.InterchangeMode;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
public class InterchangeConfigDTO {

    @NotBlank(message = "cannot be blank")
    private String name;
    private String description;
    @NotBlank(message = "cannot be blank")
    private String typeName;
    private boolean pinTranslationRequired;
    private String interchangeKey;
    private String interchangeSpecificData;
    private String code;
    @NotNull(message = "cannot be null")
    private InterchangeMode interchangeMode;



    public InterchangeConfig toInterchangeConfig() {
        this.code = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return InterchangeConfig.builder()
                .name(name)
                .description(description)
                .typeName(typeName)
                .interchangeSpecificData(interchangeSpecificData)
                .interchangeMode(interchangeMode)
                .code(code)
                .build();

    }
}

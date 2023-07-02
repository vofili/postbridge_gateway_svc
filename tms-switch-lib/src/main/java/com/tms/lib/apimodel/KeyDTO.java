package com.tms.lib.apimodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyDTO {

    @NotBlank(message = "cannot be blank")
    private String component1;
    @NotBlank(message = "cannot be blank")
    private String component2;
    @NotBlank(message = "cannot be blank")
    private String kcv;

    private String combinedComponent;
    private String generatedKcv;
}

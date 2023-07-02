package com.tms.lib.hsm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinTranslationRequest {
    private String pinBlock;
    private String sourceZpk;
    private String sourcePinBlockFormat;
    private String destinationZpk;
    private String destinationPinBlockFormat;
    private String pan;
}

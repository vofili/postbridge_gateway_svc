package com.tms.service.apimodel;

import com.tms.lib.terminals.entities.PosTerminal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosTerminalDTO {

    @NotNull
    private String terminalId;
    @NotBlank(message = "cannot be blank")
    private String cardAcceptorId;
    @Min(value = 20, message = "must be above 20")
    private int timeOutInSeconds;
    @NotBlank(message = "cannot be blank")
    private String currencyCode;
    @NotBlank(message = "cannot be blank")
    private String countryCode;
    private int callHomeTimeInHours;
    @NotBlank(message = "cannot be blank")
    private String merchantNameLocation;
    @NotBlank(message = "must be supplied")
    private String mcc;

    public PosTerminal toPosTerminal() {
        return PosTerminal.builder()
                .terminalId(terminalId)
                .cardAcceptorId(cardAcceptorId)
                .timeOutInSeconds(timeOutInSeconds)
                .currencyCode(currencyCode)
                .countryCode(countryCode)
                .callHomeTimeInHours(callHomeTimeInHours)
                .merchantNameLocation(merchantNameLocation)
                .mcc(mcc).build();

    }
}

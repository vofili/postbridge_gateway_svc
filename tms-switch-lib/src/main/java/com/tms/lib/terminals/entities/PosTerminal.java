package com.tms.lib.terminals.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosTerminal {

    @Id
    @GeneratedValue
    private Long id;
    @NotNull
    @Column(unique = true)
    private String terminalId;
    private String cardAcceptorId;
    private int timeOutInSeconds;
    private String currencyCode;
    private String countryCode;
    private int callHomeTimeInHours;
    private String merchantNameLocation;
    private String mcc;
}

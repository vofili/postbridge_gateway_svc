package com.tms.pos.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Entity
public class MappedNibssTerminal {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String nibssTerminalId;
    private String nibssCardAcceptorId;
    private String mappedTerminalId;
    private int interchangeConfigId;

}

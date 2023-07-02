package com.tms.lib.terminals.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Data
public class TmsCtmk {

    @Id
    @GeneratedValue
    private Long id;
    private String encryptedComponent1;
    private String encryptedComponent2;
    private String encryptedCtmk;
    private String keyCheckValue;
    private boolean active;
}

package com.tms.lib.terminals.entities;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Calendar;

@Entity
@Data
public class TerminalKeys {

    @Id
    @GeneratedValue
    private Long id;
    @NotNull
    @Column(unique = true, nullable = false)
    private String terminalId;
    private String tmkUnderLmk;
    private String encryptedTmk;
    private String tskUnderLmk;
    private String encryptedTsk;
    private String tpkUnderLmk;
    private String encryptedTpk;
    private Timestamp createdOn = Timestamp.valueOf(LocalDateTime.now());
    private Timestamp lastModifiedOn;

    @PreUpdate
    public void preUpdate() {
        lastModifiedOn = Timestamp.from(Calendar.getInstance().toInstant());
    }
}

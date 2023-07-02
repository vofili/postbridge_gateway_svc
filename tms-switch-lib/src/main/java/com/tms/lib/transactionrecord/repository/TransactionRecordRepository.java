package com.tms.lib.transactionrecord.repository;

import com.tms.lib.transactionrecord.entities.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface TransactionRecordRepository  extends JpaRepository<TransactionRecord, Long> {

    TransactionRecord findByRequestInterchangeIdAndMtiAndStanAndTransmissionDateTimeAndAcquiringInstitutionIdentifierAndForwardingInstitutionCode(Long interchangeId, String mti,
                                                                                                                                                  String stan, Date transmissionDateTime,
                                                                                                                                                  String acquiringInstitutionIdCode, String forwardingInstitutionCode);

    TransactionRecord findByRequestInterchangeIdAndMtiAndStanAndTransmissionDateTimeAndAcquiringInstitutionIdentifier(Long interchangeId, String mti,
                                                                                                                      String stan, Date transmissionDateTime,
                                                                                                                      String acquiringInstitutionIdCode);

    TransactionRecord findByRequestInterchangeIdAndMtiAndStanAndTransmissionDateTimeAndForwardingInstitutionCode(Long interchangeId, String mti,
                                                                                                                 String stan, Date transmissionDateTime, String forwardingInstitutionCode);

    TransactionRecord findByRequestInterchangeIdAndMtiAndStanAndTransmissionDateTime(Long interchangeId, String mti, String stan, Date transmissionDateTime);

}

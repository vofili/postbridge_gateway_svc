package com.tms.lib.terminals.repository;

import com.tms.lib.terminals.entities.TmsCtmk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TmsCtmkRepository extends JpaRepository<TmsCtmk, Long> {

    TmsCtmk findByActive(boolean active);
}

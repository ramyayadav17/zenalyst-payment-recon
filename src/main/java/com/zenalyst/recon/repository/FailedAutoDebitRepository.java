package com.zenalyst.recon.repository;

import com.zenalyst.recon.entity.FailedAutoDebit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FailedAutoDebitRepository extends JpaRepository<FailedAutoDebit, Long> {

    @Query("""
            select f from FailedAutoDebit f
            where f.recovered = false
              and f.attemptDate >= :from
              and f.attemptDate <= :to
            """)
    List<FailedAutoDebit> findOpenInWindow(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<FailedAutoDebit> findByLoanAccountIdAndRecoveredFalse(Long loanAccountId);
}

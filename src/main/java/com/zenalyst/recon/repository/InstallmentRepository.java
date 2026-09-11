package com.zenalyst.recon.repository;

import com.zenalyst.recon.entity.Installment;
import com.zenalyst.recon.enums.InstallmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {

    @Query("""
            select i from Installment i
            where i.loanAccount.id = :loanId
              and i.status in :statuses
            order by i.dueDate asc
            """)
    List<Installment> findOpenByLoan(
            @Param("loanId") Long loanId,
            @Param("statuses") List<InstallmentStatus> statuses);

    @Query("""
            select i from Installment i
            join i.loanAccount l
            where i.status in ('DUE', 'PARTIALLY_PAID', 'OVERDUE')
              and i.dueDate < :asOf
            order by i.dueDate asc
            """)
    List<Installment> findUnpaidPastDue(@Param("asOf") LocalDate asOf);
}

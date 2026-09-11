package com.zenalyst.recon.repository;

import com.zenalyst.recon.entity.LoanAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long> {

    @Query("select l from LoanAccount l join fetch l.borrower where l.status = 'ACTIVE'")
    List<LoanAccount> findActiveWithBorrower();
}

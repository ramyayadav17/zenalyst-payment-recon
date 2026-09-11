package com.zenalyst.recon.repository;

import com.zenalyst.recon.entity.BankTransaction;
import com.zenalyst.recon.enums.BankTxnStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
    Optional<BankTransaction> findByBankReference(String bankReference);

    List<BankTransaction> findByStatus(BankTxnStatus status);

    List<BankTransaction> findByTxnDateBetweenAndStatusIn(
            LocalDate from, LocalDate to, List<BankTxnStatus> statuses);

    List<BankTransaction> findByMatchedLoanId(Long loanId);
}

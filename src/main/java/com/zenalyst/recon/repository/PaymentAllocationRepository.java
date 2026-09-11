package com.zenalyst.recon.repository;

import com.zenalyst.recon.entity.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {
    List<PaymentAllocation> findByLoanAccountId(Long loanAccountId);
}

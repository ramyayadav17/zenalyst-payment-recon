package com.zenalyst.recon.controller;

import com.zenalyst.recon.dto.BankTxnRequest;
import com.zenalyst.recon.dto.DefaultNoticeDto;
import com.zenalyst.recon.dto.WhoPaidDto;
import com.zenalyst.recon.entity.BankTransaction;
import com.zenalyst.recon.enums.BankTxnStatus;
import com.zenalyst.recon.entity.PaymentAllocation;
import com.zenalyst.recon.entity.ReconciliationRun;
import com.zenalyst.recon.service.LoanPaymentService;
import com.zenalyst.recon.service.ReconService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class PaymentApi {

    private final ReconService reconService;
    private final LoanPaymentService loanPaymentService;

    public PaymentApi(ReconService reconService, LoanPaymentService loanPaymentService) {
        this.reconService = reconService;
        this.loanPaymentService = loanPaymentService;
    }

    @PostMapping("/bank-transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public BankTransaction addTxn(@Valid @RequestBody BankTxnRequest request) {
        return reconService.addBankTxn(request);
    }

    @PostMapping("/bank-transactions/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<BankTransaction> addTxns(@Valid @RequestBody List<BankTxnRequest> requests) {
        return reconService.addBankTxns(requests);
    }

    @PostMapping("/reconciliation/runs")
    public ReconciliationRun runMatching(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {
        return reconService.runMatching(businessDate != null ? businessDate : LocalDate.now());
    }

    @GetMapping("/bank-transactions")
    public List<BankTransaction> listTxns(
            @RequestParam(defaultValue = "NEEDS_REVIEW") BankTxnStatus status) {
        return reconService.listByStatus(status);
    }

    @PostMapping("/bank-transactions/{txnId}/confirm")
    public BankTransaction confirm(@PathVariable Long txnId, @RequestParam Long loanId) {
        return reconService.confirmMatch(txnId, loanId);
    }

    @GetMapping("/loans/{loanId}/who-paid")
    public WhoPaidDto getWhoPaid(@PathVariable Long loanId) {
        return loanPaymentService.getWhoPaid(loanId);
    }

    @GetMapping("/loans/{loanId}/allocations")
    public List<PaymentAllocation> getAllocations(@PathVariable Long loanId) {
        return loanPaymentService.getAllocations(loanId);
    }

    @GetMapping("/defaults/candidates")
    public List<DefaultNoticeDto> getDefaultList(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return loanPaymentService.getDefaultList(asOf != null ? asOf : LocalDate.now());
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}

package com.zenalyst.recon.config;

import com.zenalyst.recon.entity.BankTransaction;
import com.zenalyst.recon.entity.Borrower;
import com.zenalyst.recon.entity.FailedAutoDebit;
import com.zenalyst.recon.entity.Installment;
import com.zenalyst.recon.entity.LoanAccount;
import com.zenalyst.recon.enums.BankTxnStatus;
import com.zenalyst.recon.enums.InstallmentStatus;
import com.zenalyst.recon.enums.LoanStatus;
import com.zenalyst.recon.repository.BankTransactionRepository;
import com.zenalyst.recon.repository.BorrowerRepository;
import com.zenalyst.recon.repository.FailedAutoDebitRepository;
import com.zenalyst.recon.repository.InstallmentRepository;
import com.zenalyst.recon.repository.LoanAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class SampleDataLoader {

    @Bean
    @ConditionalOnProperty(name = "recon.demo-data.enabled", havingValue = "true")
    CommandLineRunner loadSample(
            BorrowerRepository borrowers,
            LoanAccountRepository loans,
            InstallmentRepository installments,
            FailedAutoDebitRepository bounces,
            BankTransactionRepository bankTxns) {
        return args -> {
            if (borrowers.count() > 0) {
                return;
            }

            LocalDate today = LocalDate.now();
            LocalDate due = today.withDayOfMonth(Math.min(5, today.lengthOfMonth()));
            if (due.isAfter(today)) {
                due = due.minusMonths(1);
            }

            Borrower b1 = saveBorrower(borrowers, "BRW-1001", "Priya Sharma", "9876500001",
                    "priya.sharma@oksbi,XXXX1111");
            LoanAccount l1 = saveLoan(loans, b1, "LN10010001", "5000.00", "45000.00", "UMRNPRIYA001");
            Installment i1 = saveEmi(installments, l1, 1, due, "5000.00");
            FailedAutoDebit bounce = new FailedAutoDebit();
            bounce.setLoanAccountId(l1.getId());
            bounce.setInstallmentId(i1.getId());
            bounce.setAttemptDate(due);
            bounce.setRecovered(false);
            bounces.save(bounce);
            saveTxn(bankTxns, "UTR-BOUNCE-RECOVERY-1", due.plusDays(3), "5000.00",
                    "NEFT PRIYA SHARMA LOAN EMI", "XXXX1111");

            Borrower b2 = saveBorrower(borrowers, "BRW-1002", "Ravi Kumar", "9876500002", "ravikumar@ybl");
            LoanAccount l2 = saveLoan(loans, b2, "LN10010002", "4000.00", "32000.00", null);
            saveEmi(installments, l2, 1, due, "4000.00");
            saveTxn(bankTxns, "UTR-HALF-1", due.plusDays(1), "2000.00",
                    "UPI-RAVIKUMAR@YBL-EMI PART", "ravikumar@ybl");

            Borrower b3 = saveBorrower(borrowers, "BRW-1003", "Asha Devi", "9876500003", "ASHADEVXXXX");
            LoanAccount l3 = saveLoan(loans, b3, "LN10010003", "3000.00", "27000.00", null);
            saveEmi(installments, l3, 1, due.minusMonths(1), "3000.00");
            saveEmi(installments, l3, 2, due, "3000.00");
            saveTxn(bankTxns, "UTR-TWOEMI-1", due.plusDays(2), "6000.00",
                    "IMPS ASHA DEVI LN10010003", "ASHADEVXXXX");

            Borrower b4 = saveBorrower(borrowers, "BRW-1004", "Mohammed Irfan", "9876500004", "irfan@okhdfc");
            LoanAccount l4 = saveLoan(loans, b4, "LN10010004", "7500.00", "22000.00", null);
            saveEmi(installments, l4, 1, due, "7500.00");
            saveTxn(bankTxns, "UTR-CLOSE-1", due.plusDays(1), "22000.00",
                    "RTGS FORECLOSE LN10010004 IRFAN", "irfan@okhdfc");

            Borrower b5 = saveBorrower(borrowers, "BRW-1005", "Sita Ramulu", "9876500005", null);
            LoanAccount l5 = saveLoan(loans, b5, "LN10010005", "2500.00", "15000.00", null);
            saveEmi(installments, l5, 1, due, "2500.00");
            saveTxn(bankTxns, "UTR-MESSY-1", due.plusDays(2), "2500.00",
                    "UPI/CR/XY99ZZ/SITARAMULU/NA", null);

            Borrower b6 = saveBorrower(borrowers, "BRW-1006", "No Pay Yet", "9876500006", null);
            LoanAccount l6 = saveLoan(loans, b6, "LN10010006", "3500.00", "28000.00", null);
            saveEmi(installments, l6, 1, due.minusDays(20), "3500.00");
        };
    }

    private static Borrower saveBorrower(
            BorrowerRepository repo, String code, String name, String mobile, String handles) {
        Borrower b = new Borrower();
        b.setBorrowerCode(code);
        b.setFullName(name);
        b.setMobile(mobile);
        b.setKnownPaymentHandles(handles);
        return repo.save(b);
    }

    private static LoanAccount saveLoan(
            LoanAccountRepository repo, Borrower b, String lan, String emi, String outstanding, String mandate) {
        LoanAccount l = new LoanAccount();
        l.setBorrower(b);
        l.setLoanAccountNumber(lan);
        l.setEmiAmount(new BigDecimal(emi));
        l.setOutstandingPrincipal(new BigDecimal(outstanding));
        l.setStatus(LoanStatus.ACTIVE);
        l.setMandateReference(mandate);
        return repo.save(l);
    }

    private static Installment saveEmi(
            InstallmentRepository repo, LoanAccount loan, int seq, LocalDate due, String amount) {
        Installment i = new Installment();
        i.setLoanAccount(loan);
        i.setSequenceNo(seq);
        i.setDueDate(due);
        i.setAmountDue(new BigDecimal(amount));
        i.setAmountPaid(BigDecimal.ZERO);
        i.setStatus(due.isBefore(LocalDate.now()) ? InstallmentStatus.OVERDUE : InstallmentStatus.DUE);
        return repo.save(i);
    }

    private static void saveTxn(
            BankTransactionRepository repo, String ref, LocalDate date, String amount, String narration, String cpty) {
        BankTransaction t = new BankTransaction();
        t.setBankReference(ref);
        t.setTxnDate(date);
        t.setAmount(new BigDecimal(amount));
        t.setNarration(narration);
        t.setCounterpartyAccount(cpty);
        t.setStatus(BankTxnStatus.UNMATCHED);
        repo.save(t);
    }
}

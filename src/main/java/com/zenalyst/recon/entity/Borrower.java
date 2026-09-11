package com.zenalyst.recon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "borrowers")
public class Borrower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String borrowerCode;

    @Column(nullable = false)
    private String fullName;

    private String mobile;

    @Column(length = 1000)
    private String knownPaymentHandles;

    @OneToMany(mappedBy = "borrower")
    private List<LoanAccount> loans = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBorrowerCode() { return borrowerCode; }
    public void setBorrowerCode(String borrowerCode) { this.borrowerCode = borrowerCode; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getKnownPaymentHandles() { return knownPaymentHandles; }
    public void setKnownPaymentHandles(String knownPaymentHandles) { this.knownPaymentHandles = knownPaymentHandles; }
    public List<LoanAccount> getLoans() { return loans; }
    public void setLoans(List<LoanAccount> loans) { this.loans = loans; }
}

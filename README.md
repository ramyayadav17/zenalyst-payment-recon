# zenalyst-payment-recon

Backend for matching bank statement credits to loan accounts.

Problem we were solving: money does land in the bank, but narration is messy
(bounce + late NEFT, half EMI, two EMIs together, foreclosure amount, garbage
text). If we don't match those lines properly, someone who already paid can
still get a default notice.

## What it does

- takes bank credits (UTR, amount, narration, counterparty)
- scores them against active loans (loan no, name, UPI/account, EMI patterns, bounce window)
- auto-matches when score is high, sends unclear ones to review
- applies money to overdue / current EMIs, leftover as advance or prepayment
- `who-paid` tells you if that loan has actually received payment
- default candidates list skips people who are still inside grace / bounce recovery / review

## Packages

```
com.zenalyst.recon
  controller
  exception
  service
  entity
  enums
  repository
  dto
  config
```

## Setup

Java 17, MySQL on localhost.

```sql
CREATE DATABASE IF NOT EXISTS zenalyst_recon
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'zenalyst'@'127.0.0.1' IDENTIFIED BY 'Zenalyst@123';
GRANT ALL PRIVILEGES ON zenalyst_recon.* TO 'zenalyst'@'127.0.0.1';
FLUSH PRIVILEGES;
```

App defaults (override with `DB_URL` / `DB_USER` / `DB_PASSWORD` if needed):

- db `zenalyst_recon`
- user `zenalyst` / `Zenalyst@123`
- port `8080`

```bash
./gradlew bootRun
./gradlew test
```

On empty DB it loads 6 sample borrowers (bounce recovery, half EMI, two EMIs,
foreclosure, messy narration, unpaid). Turn off with `recon.demo-data.enabled=false`.

## APIs

```bash
# add a bank credit
curl -X POST http://localhost:8080/api/v1/bank-transactions \
  -H 'Content-Type: application/json' \
  -d '{
    "bankReference": "UTR-1",
    "txnDate": "2026-09-10",
    "amount": 5000,
    "narration": "NEFT PRIYA SHARMA LN10010001",
    "counterpartyAccount": "XXXX1111"
  }'

# run matching
curl -X POST http://localhost:8080/api/v1/reconciliation/runs

# review queue
curl 'http://localhost:8080/api/v1/bank-transactions?status=NEEDS_REVIEW'

# ops confirms a review item
curl -X POST 'http://localhost:8080/api/v1/bank-transactions/5/confirm?loanId=5'

# main answer
curl http://localhost:8080/api/v1/loans/1/who-paid

# how money was split
curl http://localhost:8080/api/v1/loans/1/allocations

# who is safe to send default notice
curl http://localhost:8080/api/v1/defaults/candidates
```

Same UTR posted twice is ignored.

Thresholds live in `application.yml` under `recon.*`.

## Assumptions

- Loan book (borrower, loan, EMI schedule, bounce history) already exists in DB. This service focuses on matching bank credits to that book, not on originating loans.
- Bank statement lines come in via API (as if from a file ingest / bank feed). Matching is a separate batch-style run so ingest stays idempotent on UTR.
- If confidence is medium, better to park in review than auto-apply to the wrong loan.
- Default notices should wait if: still inside grace, bounce recovery window is open, or a possible payment is sitting in review.
- Demo data covers the messy cases from the brief (bounce recovery, half EMI, two EMIs, foreclosure, unreadable narration, unpaid).

## What I left out (and why)

- UI / mobile app — brief asked for backend; APIs + README are enough to exercise the flow.
- Auth / roles — would matter in production, but it distracts from the matching problem in 48 hours.
- Real bank file parsers (CSV/MT940/etc.) — ingest is JSON API; same matcher would sit behind a file reader later.
- Exact interest / tax / GL posting — allocation is EMI-first then advance/prepay; full LMS accounting is a different service.
- Scale-out for ~40k borrowers (sharding, async workers, indexing beyond basics) — logic is there; production hardening is follow-up work.
- ML / fuzzy entity resolution beyond name similarity + rules — keep it explainable for interview; rules + score thresholds are easier to defend live.

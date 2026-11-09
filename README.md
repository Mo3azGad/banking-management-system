# Banking Management System

Advanced OOP, Project 2. **Milestone 4: Bank account opening, ownership validation and retrieval.**
Incremental university project; transfers, freezing and monthly interest posting remain
later milestones. This is not yet the final submission.

## Run the tests

Install a **JDK 17 or later** (not just a Java runtime) and Maven 3.9.x, then run:

```sh
mvn clean verify
```

Tests are the only application entry point. There is no main method, UI, menu, Scanner,
console output or system-clock access. JUnit 5 and JaCoCo results appear under
`target/surefire-reports/` and `target/site/jacoco/index.html`. First build requires Maven Central.

## Implemented

- Immutable Customer; Bank customer registration, lookup and immutable customer snapshots.
- Bank `openCheckingAccount(accountId, customerId, openingBalance, openedOn)` and
  `openSavingsAccount(accountId, customerId, openingBalance, openedOn)` validate and register
  new accounts owned by the bank's canonical registered customer.
- `findAccount(accountId)` returns the registered instance; `getAccounts()` returns an immutable
  membership snapshot in opening order. The accounts themselves retain their mutable posting state.
- Account IDs are unique across checking/savings within each Bank. Failed openings change neither
  registry and do not reserve IDs; duplicate IDs and missing accounts have dedicated exceptions.
- Abstract Account; CheckingAccount and SavingsAccount with different balance floors/rates.
- Checking: minimum 0.00, no overdraft, monthly rate metadata 0.001 (0.1%).
- Savings: minimum 100.00 on opening and withdrawals, monthly rate metadata 0.003 (0.3%).
- Final `deposit(BigDecimal, LocalDate)` and `withdraw(BigDecimal, LocalDate)` return the posted
  Transaction. Balance/history change only after all domain validation succeeds.
- Opening ledger entry plus dated postings; `getTransactionHistory()` returns an immutable snapshot.
- Posting dates cannot precede the latest entry; same-day postings and opening-day postings work.
- Amounts must be positive and exactly representable in cents. Trailing zeros are accepted;
  nonzero fractions of a cent are rejected instead of silently rounded.
- Custom exception hierarchy: invalid input, duplicate/missing customer or account, insufficient
  funds, and invalid posting chronology. Tests exercise every concrete custom exception.
- Transaction is an immutable record; its construction alone never executes an operation.

Transaction IDs start at 1 for opening and increase within each account. The identifier is the
pair `(accountId, transactionId)`, not the sequence number alone. Failed operations consume no ID
and do not advance the latest posting date. This replaces the earlier proposed Bank-wide
sequence; future Bank transfers will coordinate separate cross-account transfer references.

Direct Account constructors still create standalone domain objects with opening history. They
are not implicitly registered. Use the Bank opening methods to enforce registered ownership and
unique account IDs. No external-account import API is introduced. Lookup uses normalized,
case-sensitive IDs, and customer/account IDs have separate namespaces. Different Banks may
use the same IDs independently.
Names may repeat and identifiers are case-sensitive. Rates are metadata; calculation and posting
will be introduced with InterestPolicy later. The fixed floors/rates are project choices,
not numerical requirements specified by the professor. Single-threaded, in-memory use only.

## Documentation

- `docs/REQUIREMENTS.md`: PDF obligations versus explicit implementation choices.
- `docs/ARCHITECTURE.md`: current design, Template Method, planned Strategy and SOLID mapping.
- `docs/DEVELOPMENT_PLAN.md`: remaining work roadmap and actual progress; schedule versus execution distinguished.
- `docs/current-uml.svg` / `.dot`: implemented model; `docs/proposed-uml.mmd`: final target design.
- `docs/TEST_MATRIX.md` and `docs/VERIFICATION.md`: current test/coverage evidence and limitations.
- `docs/MILESTONE_1_VERIFICATION.md` through `docs/MILESTONE_3_VERIFICATION.md`: earlier evidence.
- `docs/milestone-report.docx`: updated five-section interim Word report, not the final report.
- `docs/DEFENSE.md`: current walkthrough, demonstration commands and eventual defense outline.

## Git and GitHub

Private repository: https://github.com/Mo3azGad/banking-management-system

Original milestone-1 commit `dba37153be8867b854a1aade7f5b891c45ad27aa` and milestone-2 commit
`244a7b6ed03ddd42f6c262a583f04b9d146aa3d9` are preserved. Milestone 1 was independently checked
against GitHub's exact SHA and private visibility before further development. Credentials and
machine-specific tooling stay outside tracked source. On another computer use `gh auth login`
and `gh auth setup-git` before pushing.

Milestones 1–3 were genuinely developed on 8 November 2026; milestone 4 was developed on
9 November. The professor's at-least-two-week history requirement is still outstanding.
Never change author/committer dates or invent activity. A work automation was saved and enabled
on 8 November for 9–23 November evenings in Europe/Istanbul. Saved scheduling is not proof of
future execution or pushes; verify each actual session and its result separately.

## Next milestone

Add account freezing with typed failures and tests for blocked postings and readable history.
Then prepare/implement atomic transfers, InterestPolicy/monthly interest, the final report and
defense rehearsal. Keep UML and documentation synchronized with each tested increment.

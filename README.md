# Banking Management System

Advanced OOP, Project 2. **Milestone 3: dated deposits, withdrawals and account history.**
Incremental university project; Bank account registration, transfers, freezing and monthly
interest posting remain later milestones. This is not yet the final submission.

## Run the tests

Install a **JDK 17 or later** (not just a Java runtime) and Maven 3.9.x, then run:

```sh
mvn clean verify
```

Tests are the only application entry point. There is no main method, UI, menu, Scanner,
console output or system-clock access. JUnit 5 and JaCoCo results appear under
`target/surefire-reports/` and `target/site/jacoco/index.html`. First build requires Maven Central.

## Implemented

- Immutable Customer; Bank registration, lookup and immutable customer snapshots.
- Abstract Account; CheckingAccount and SavingsAccount with different balance floors/rates.
- Checking: minimum 0.00, no overdraft, monthly rate metadata 0.001 (0.1%).
- Savings: minimum 100.00 on opening and withdrawals, monthly rate metadata 0.003 (0.3%).
- Final `deposit(BigDecimal, LocalDate)` and `withdraw(BigDecimal, LocalDate)` return the posted
  Transaction. Balance/history change only after all domain validation succeeds.
- Opening ledger entry plus dated postings; `getTransactionHistory()` returns an immutable snapshot.
- Posting dates cannot precede the latest entry; same-day postings and opening-day postings work.
- Amounts must be positive and exactly representable in cents. Trailing zeros are accepted;
  nonzero fractions of a cent are rejected instead of silently rounded.
- Custom exception hierarchy: invalid input, duplicate/missing customer, insufficient funds,
  and invalid posting chronology. Tests exercise every concrete custom exception.
- Transaction is an immutable record; its construction alone never executes an operation.

Transaction IDs start at 1 for opening and increase within each account. The identifier is the
pair `(accountId, transactionId)`, not the sequence number alone. Failed operations consume no ID
and do not advance the latest posting date. This replaces the earlier proposed Bank-wide
sequence; future Bank transfers will coordinate separate cross-account transfer references.

Account constructors create standalone domain objects with opening history. They do not register
the account at Bank; uniqueness/registered ownership will be enforced by Bank integration.
Names may repeat and identifiers are case-sensitive. Rates are metadata; calculation and posting
will be introduced with InterestPolicy later. The fixed floors/rates are project choices,
not numerical requirements specified by the professor. Single-threaded, in-memory use only.

## Documentation

- `docs/REQUIREMENTS.md`: PDF obligations versus explicit implementation choices.
- `docs/ARCHITECTURE.md`: current design, Template Method, planned Strategy and SOLID mapping.
- `docs/DEVELOPMENT_PLAN.md`: proposed sessions and actual progress; nothing scheduled.
- `docs/current-uml.svg` / `.dot`: implemented model; `docs/proposed-uml.mmd`: final target design.
- `docs/TEST_MATRIX.md` and `docs/VERIFICATION.md`: current test/coverage evidence and limitations.
- `docs/MILESTONE_1_VERIFICATION.md` and `docs/MILESTONE_2_VERIFICATION.md`: earlier evidence.
- `docs/milestone-report.docx`: updated five-section interim Word report, not the final report.
- `docs/DEFENSE.md`: current walkthrough, demonstration commands and eventual defense outline.

## Git and GitHub

Private repository: https://github.com/Mo3azGad/banking-management-system

Original milestone-1 commit `dba37153be8867b854a1aade7f5b891c45ad27aa` and milestone-2 commit
`244a7b6ed03ddd42f6c262a583f04b9d146aa3d9` are preserved. Milestone 1 was independently checked
against GitHub's exact SHA and private visibility before further development. Credentials and
machine-specific tooling stay outside tracked source. On another computer use `gh auth login`
and `gh auth setup-git` before pushing.

Milestones 1–3 were genuinely developed on 8 November 2026. The professor's at-least-two-week
history requirement is still outstanding. Never change author/committer dates or invent activity.
No future work or pushes have been automatically scheduled.

## Next milestone

Add Bank account registration/opening and validated ownership/uniqueness, then coordinated atomic
transfers. Freezing, InterestPolicy/monthly interest, the final report and defense rehearsal
remain to complete. Keep UML and documentation synchronized with each tested increment.

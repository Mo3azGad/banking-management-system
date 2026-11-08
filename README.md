# Banking Management System

Advanced OOP, Project 2. **Milestone 2: account models and validated transaction records.**
This is an incremental university project, not the completed submission. Account posting
operations, Bank account registration, account histories and interest calculation are not yet implemented.

## Run the tests

Install a **JDK 17 or later** (not just a Java runtime) and Maven 3.9.x, then run:

```sh
mvn clean verify
```

Tests are the only executable application entry point. There is no main method, UI, menu,
Scanner, console output, or system-clock access in application code.
JUnit 5 and JaCoCo are configured; generated results appear under `target/surefire-reports/`
and `target/site/jacoco/index.html`. Network access to Maven Central is needed on the first build.
The package uses Java 17 language/API compatibility.

## Implemented now

- Immutable `Customer` with validated, stripped ID and name.
- `Bank.registerCustomer`, `Bank.findCustomer`, and immutable customer snapshots.
- Unchecked `BankingException` hierarchy: invalid input, duplicate customer, missing customer.
- Success, failure, normalization, duplicate preservation, and collection-isolation tests.
- Abstract Account opening state, concrete CheckingAccount and SavingsAccount.
- Polymorphic minimum balances (0.00 / 100.00) and monthly decimal rates (0.001 / 0.003).
- Available balance derived from each account type's floor; savings floor enforced on construction.
- Immutable Transaction records with explicit date, positive ID, validated money and transfer links.
- Shared internal BankingInput validation; no new custom exceptions were needed for these inputs.
- In-memory, single-threaded storage; no external infrastructure.

Account constructors create domain objects only; they do not register them at Bank or post an
opening ledger entry. A Transaction describes a fact; constructing it never executes a transfer.
Bank will later coordinate ledger entries, unique IDs, chronology, and account registration.
Amounts reject nonzero sub-cent precision and normalize accepted values to two decimals.
Rates are exposed as metadata; monthly interest posting will be added with InterestPolicy later.

Names may repeat. IDs are case-sensitive. Deleting/updating customers is not in this milestone;
customer registration and retrieval satisfy the planned customer-management scope.

## Documentation

- `docs/REQUIREMENTS.md`: authoritative obligations versus implementation decisions.
- `docs/ARCHITECTURE.md`: proposed final model, contracts, tradeoffs, and design patterns.
- `docs/DEVELOPMENT_PLAN.md`: daily real-work steps, November 8–22, 2026.
- `docs/current-uml.svg` and `docs/current-uml.dot`: implemented classes only.
- `docs/proposed-uml.mmd`: proposed final design; posting operations and policies remain unimplemented.
- `docs/TEST_MATRIX.md`: present evidence and later test obligations.
- `docs/DEFENSE.md`: milestone walkthrough and future five-minute demo outline.
- `docs/milestone-report.docx`: updated milestone-2 interim report, not the final report.
- `docs/VERIFICATION.md`: actual build and coverage evidence.

## Git and GitHub

Private repository: https://github.com/Mo3azGad/banking-management-system

The original milestone-1 commit `dba37153be8867b854a1aade7f5b891c45ad27aa` was pushed to `main`;
GitHub's API independently confirmed the exact SHA and `private: true` before milestone 2 began.
The existing local repository was retained; its original commit was neither amended nor replaced.
Authentication and remote settings are local Git configuration, never committed credentials.
On another computer, authenticate GitHub CLI (`gh auth login`, then `gh auth setup-git`) before pushing.
Do not change author/committer dates. The two-week history is not yet complete: both milestones
were actually developed on 8 November 2026. Future sessions must contain real meaningful work.
Nothing is scheduled automatically.

## Next development step

Implement dated deposits and withdrawals with atomic balance/history updates and focused JUnit
success/failure tests. Account registration/opening at Bank, transfer coordination, monthly
interest and freezing remain later milestones. Keep UML/report synchronized with actual code.
The project is not ready for final submission.

# Banking Management System

Advanced OOP, Project 2. **Milestone 1: requirements, design, and tested customer management.**
This is a working foundation, not the completed submission. No account operations exist yet.

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
- In-memory, single-threaded storage; no external infrastructure.

Names may repeat. IDs are case-sensitive. Deleting/updating customers is not in this milestone;
customer registration and retrieval satisfy the planned customer-management scope.

## Documentation

- `docs/REQUIREMENTS.md`: authoritative obligations versus implementation decisions.
- `docs/ARCHITECTURE.md`: proposed final model, contracts, tradeoffs, and design patterns.
- `docs/DEVELOPMENT_PLAN.md`: daily real-work steps, November 8–22, 2026.
- `docs/current-uml.svg` and `docs/current-uml.dot`: implemented classes only.
- `docs/proposed-uml.mmd`: planned account model, explicitly not implemented yet.
- `docs/TEST_MATRIX.md`: present evidence and later test obligations.
- `docs/DEFENSE.md`: milestone walkthrough and future five-minute demo outline.
- `docs/milestone-report.docx`: interim report, not the final report.
- `docs/VERIFICATION.md`: actual build and coverage evidence.

## Git and GitHub

The local repository contains only real completed work with real commit timestamps.
No GitHub repository or remote has been created, and nothing has been pushed or scheduled.
After choosing your GitHub repository, add its actual URL with `git remote add origin URL`,
then push the current branch with `git push -u origin HEAD`. Do not use the literal `URL`.
Do not change author/committer dates. Commit later only after meaningful verified work.

## Next development step

Review the proposed account rules, then implement the abstract account, checking/savings
subclasses, and their invariant tests. Keep the report and UML synchronized with real code.
Final account functionality, final report, full defense materials, and two-week GitHub history
remain future milestones; the project is not ready for submission.

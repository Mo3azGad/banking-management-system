# Milestone 14 verification — input/error and mutation-order review

Verified 19 November 2026 (Europe/Istanbul), complete Maven `clean verify`: BUILD SUCCESS.
Eclipse Temurin 21.0.13+9, Java 17 compiler target; Maven 3.9.11; JUnit 5.11.4;
Surefire 3.5.2; JaCoCo 0.8.12. Full build finished at 2026-11-19T16:54:54Z.

JUnit: 659 invocations, 0 failures, 0 errors, 0 skipped.
All 639 prior cases remain; InputContractTest adds 20 cases through public operations.

- Instructions: 1054/1054 (100.00%).
- Branches: 70/71 (98.59%).
- Lines: 239/239 (100.00%).
- Methods: 70/70 (100.00%).
- Classes: 23/23 (100.00%).

The new insufficient-transfer test first failed for both checking/savings on the parent behavior:
the actual diagnostic said `cannot withdraw` rather than `cannot transfer`. The two-case targeted
reproduction recorded two failures, zero errors/skips. After the local diagnostic correction,
the complete suite passes. Withdrawals retain their exact wording and normalized amount.
Both outcomes of the new operation-label selection are covered; Transaction's documented enum
fallback remains the sole missed branch. All eleven custom exception types remain exercised.

New cases check overlapping-invalid-input order: lookup/distinctness before account guards in
Bank transfers, money before missing direct dates, chronology before funds, and occupied account
ID before owner/opening validation. Rejected operations preserve observable ledgers/registries;
valid retries use earlier dates, consecutive local IDs and first transfer reference as applicable.
No financial publication defect was found. Ledger/paired-transfer/month-marker order is retained.

INPUT_MUTATION_REVIEW.md records guard order, mutation boundaries, the actual defect and limits.
All 74 unique selectors across it and API_EXCEPTION_AUDIT.md exist in source and fresh passing
JUnit reports (67 prior audit witnesses plus seven new method families). No clocks/prohibited
execution APIs or private-state manipulation were added. Only Account's insufficient-funds
message choice changes in production; no new public API/class/relationship warrants a UML change.
Current docs and the five-section interim DOCX are updated; structure/retained image are checked
without claiming full pagination rendering. Final UML/report/defense and independent review remain.

Persisted JDK/Maven/settings/cache and system Java CA trust were reused. Credentials/tooling
stay outside tracked source. Scope remains single-threaded in-memory execution; concurrency,
persistence, VM/resource failure and rollback of arbitrary impure policy side effects are not
guaranteed by this review or its tests.

## Repository preflight and genuine history

Existing main was clean; safe fetch matched origin/main exactly at
282f2b48785ff8faaf075c43c118bb40f93c7950. GitHub confirmed authorized authentication, private
visibility and push permission; original ancestry was checked. A meaningful real-date commit/push
follows review, with exact remote SHA/private visibility/ancestry/clean worktree independently
verified and reported in the session result. No history/timestamps are rewritten. The two-week
span remains unfinished; saved scheduling proves neither future runs nor professor acceptance.

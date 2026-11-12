# Milestone 6 verification — prepared posting and guarded publication

Verified 11 November 2026 (Europe/Istanbul), full Maven `clean verify`: BUILD SUCCESS.
Eclipse Temurin 21.0.13+9, Java 17 compiler target; Maven 3.9.11; JUnit 5.11.4;
Surefire 3.5.2; JaCoCo 0.8.12.

JUnit: 402 invocations, 0 failures, 0 errors, 0 skipped.
All 323 existing cases remain; PreparedPostingTest adds 79 cases.

- Instruction: 775/775 (100.00%).
- Branch: 52/53 (98.11%).
- Line: 177/177 (100.00%).
- Method: 57/57 (100.00%).
- Class: 20/20 (100.00%).

All Bank, Account and PreparedPosting branches are covered. The one missed branch is the
compiler-emitted default of Transaction's exhaustive enum switch; all legal types/null rejection
are tested. All nine concrete custom exceptions are thrown by operation tests. No reflection,
forged enum values, clock reads or test-only mutators are used. Current coverage includes internal
preparation but does not imply implemented Bank transfers or monthly interest.

Private LedgerState/PreparedPosting keep allocation and validation before publication. Public
API regression tests cover both account types and active/frozen behavior; new tests cover
non-mutating preparation, immutable snapshots, guarded one-time commitment, invalid/stale/frozen
plans and independent-account prevalidation. These are boundary tests, not completed transfer tests.

Existing workspace-local JDK/Maven/proxy settings and system Java CA trust store were reused.
No credentials or tooling are committed. Normal installations run `mvn clean verify`.
UML is regenerated/visually inspected and the interim Word report structurally checked; no full
Word/LibreOffice pagination render is claimed. Earlier evidence is retained in numbered files.

## Repository preflight

The existing repository was clean on main. Fetch found HEAD and origin/main identical at
10541dbcb6f7dacba14da7e579f8b7a6d499b6b4. GitHub confirmed private visibility and push permission.
Original commits were verified as ancestors. A new real-date commit/push follows successful
verification/review, with its exact SHA and remote checks reported in the session result.
No history rewrite, replacement project or fabricated future activity was performed. The two-week
span remains unfinished; no future run or push is claimed successful from a saved schedule.

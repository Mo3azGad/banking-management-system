# Milestone 11 verification — cross-operation integration

Verified 16 November 2026 (Europe/Istanbul), full Maven `clean verify`: BUILD SUCCESS.
Eclipse Temurin 21.0.13+9, Java 17 compiler target; Maven 3.9.11; JUnit 5.11.4;
Surefire 3.5.2; JaCoCo 0.8.12. Build finished at 2026-11-16T16:29:11Z.

JUnit: 639 invocations, 0 failures, 0 errors, 0 skipped.
All 618 prior cases remain; BankScenarioTest adds 21 invocations through public APIs.

- Instructions: 1046/1046 (100.00%).
- Branches: 68/69 (98.55%).
- Lines: 238/238 (100.00%).
- Methods: 70/70 (100.00%).
- Classes: 23/23 (100.00%).

Production code remains unchanged after review; no defect or new feature was needed. New cases
establish two-month ledger/external-flow reconciliation, transfer references across different
local IDs, transfer/interest ordering for every type pair, interest-driven chronology/availability
and freezing after prior mixed activity. See INTEGRATION_REVIEW.md for gaps, financial examples
and limits. Test count increases behavioral evidence without claiming a line-coverage increase.

Every concrete custom exception (eleven) remains thrown through operation tests. The sole missed
branch is Transaction's compiler-emitted enum fallback; all legal values/null are tested. The
abstract-method-only InterestPolicy has no executable code. No clock/prohibited execution APIs,
private-state manipulation or internal posting preparers are used in the new scenarios. The
running-balance helper reconstructs each ledger from signed facts; independently specified totals
anchor the financial result. No concurrency/database/persistence/impure-policy rollback claim.

Persisted JDK/Maven/settings/cache and the system Java CA trust store were reused; normal local
installations run `mvn clean verify`. No credentials/tools enter tracked source. Production/UML
trees are unchanged; the milestone 9 diagram remains accurate and was checked/retained. The
five-section interim DOCX was updated; its structure and retained embedded diagram were checked,
without claiming full Word pagination rendering. Earlier verification is archived in numbered
files. Final subtype/requirements/input audits, final report and defense remain unfinished.

## Repository preflight and genuine history

Existing main was clean. Safe fetch matched origin/main exactly at
797b1450cb104f4ca37e9aa2eaf5bdd1931040d9. Authorized GitHub credentials, private visibility,
push permission and original ancestry were checked before work. A meaningful real-date commit/push
follows review; exact remote SHA, private visibility, clean worktree and preserved ancestry are
independently verified and reported in the session result. No history/timestamps are rewritten.
The two-week span is still in progress; a saved automation is not proof of future execution.

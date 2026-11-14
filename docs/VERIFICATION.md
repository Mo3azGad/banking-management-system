# Milestone 9 verification — dated monthly interest posting

Verified 14 November 2026 (Europe/Istanbul), full Maven `clean verify`: BUILD SUCCESS.
Eclipse Temurin 21.0.13+9, Java 17 compiler target; Maven 3.9.11; JUnit 5.11.4;
Surefire 3.5.2; JaCoCo 0.8.12.

JUnit: 581 invocations, 0 failures, 0 errors, 0 skipped.
All 529 prior cases remain; MonthlyInterestTest adds 52 cases.

- Instruction: 1046/1046 (100.00%).
- Branch: 68/69 (98.55%).
- Line: 238/238 (100.00%).
- Method: 70/70 (100.00%).
- Class: 23/23 (100.00%).

All Account, Bank, prepared-posting and policy branches are covered. The one missed branch remains
Transaction's compiler-emitted enum fallback; all legal values/null rejection are tested. Eleven
concrete custom exceptions are now exercised through operations, including duplicate interest and
unexpected policy failure with its cause. The interface has no executable code and is excluded
from JaCoCo class counts. No private state, clock or forged enum values inflate coverage.

New tests cover default/overridden policy construction, actual INTEREST ledger facts, month-end/
chronology/frozen rejection, YearMonth protection after ordinary postings and zero credit, invalid
results, propagated/wrapped failures, retries without ID/month reservation, stale plans and actual
Bank-owned accounts. Policy doubles deliberately return invalid outputs/throw once to test defensive
handling. Pure policies must not mutate Account state: arbitrary callback side effects, concurrency,
VM resources and persistence are not rollback-tested or claimed supported.

Existing local JDK/Maven/proxy settings/Java CA trust were reused; no credentials/tools are committed.
Normal installations run `mvn clean verify`. UML is rendered/visually inspected and Word sections/
diagram structurally checked; no full Word pagination rendering is claimed. Prior evidence remains
in numbered files. A final artifact/requirements review is still needed before submission.

## Repository preflight

Existing main was clean; fetch matched origin exactly at 044f15c2b9b0209ba912752283946e275372d97a.
GitHub confirmed private visibility and push permission, and original commits remain ancestors.
A meaningful real-date commit/push follows review; exact remote SHA/private visibility/ancestry are
verified and reported in the session result. No original history or timestamps are rewritten.
The two-week span remains unfinished; saved scheduling is not proof of later completion.

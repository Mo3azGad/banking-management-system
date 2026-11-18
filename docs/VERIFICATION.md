# Milestone 13 verification — public API/custom-exception audit

Verified 18 November 2026 (Europe/Istanbul), complete Maven `clean verify`: BUILD SUCCESS.
Eclipse Temurin 21.0.13+9, Java 17 compiler target; Maven 3.9.11; JUnit 5.11.4;
Surefire 3.5.2; JaCoCo 0.8.12. Final build finished at 2026-11-18T16:08:36Z after
the two source-comment corrections. The earlier baseline build also passed.

JUnit: 639 invocations, 0 failures, 0 errors, 0 skipped. No test added or removed.

- Instructions: 1046/1046 (100.00%).
- Branches: 68/69 (98.55%).
- Lines: 238/238 (100.00%).
- Methods: 70/70 (100.00%).
- Classes: 23/23 (100.00%).

JDK javap confirms 60 direct public callable declarations across 21 public top-level types.
The audit classifies 24 domain-operation/constructor declarations and 36 basic/generated/carrier
declarations, including no-input queries without a valid-state failure condition. All 67 named
selectors in API_EXCEPTION_AUDIT.md exist in source and have fresh passing JUnit report entries.
The complete set of eleven concrete BankingException subtypes matches the audit table, and each
selected witness body uses assertThrows for that exact type through an operation.

The audit found no missing meaningful public success/rejection contract or unthrown custom type.
The new index closes a traceability gap; source/assertions were reviewed beyond static matching.
InvalidPostingDateException now describes non-month-end interest rejection as well as backdating;
InsufficientFundsException describes outgoing transfer rejection as well as withdrawal. These
are comment-only corrections: removing block comments gives identical Java code to the parent.
Tests, public signatures, runtime behavior and UML files remain unchanged.

The sole missed branch remains Transaction's constructor switch, source line 24; legal enum
values/null rejection are tested and the compiler fallback is retained. InterestPolicy has no
executable body. No clock/prohibited execution APIs or private-state manipulation were added.
Coverage is supporting evidence, not proof for every input or arbitrary external policy.
Concurrency, persistence, VM/resource failure and impure callback rollback remain outside scope.

Persisted JDK/Maven/settings/cache/system CA trust were reused; credentials/tools stay outside
tracked source. The accurate UML is retained. Current documentation and the five-section interim
DOCX now reference the audit; Word structure and retained image are checked without claiming full
pagination rendering. Input/error/mutation-order review, final artifacts and defense remain.

## Repository preflight and genuine history

Existing main was clean; safe fetch matched origin/main exactly at
f20b7e00d88d4f66f2bcfea6de791a4b72869407. GitHub confirmed authorized authentication, private
visibility and push permission; original ancestry was checked. A meaningful real-date commit/push
follows review, with exact remote SHA/private visibility/ancestry/clean worktree independently
checked and reported in the session result. No timestamps/history are rewritten. The two-week
span is unfinished; saved scheduling does not prove future runs or professor acceptance.

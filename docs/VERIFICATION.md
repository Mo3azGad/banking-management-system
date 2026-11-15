# Milestone 10 verification — interest-boundary review

Verified 15 November 2026 (Europe/Istanbul), full Maven `clean verify`: BUILD SUCCESS.
Eclipse Temurin 21.0.13+9, Java 17 compiler target; Maven 3.9.11; JUnit 5.11.4;
Surefire 3.5.2; JaCoCo 0.8.12. Build finished at 2026-11-15T16:15:14Z.

JUnit: 618 invocations, 0 failures, 0 errors, 0 skipped.
All 581 prior cases remain; MonthlyInterestBoundaryTest adds 37 cases.

- Instructions: 1046/1046 (100.00%).
- Branches: 68/69 (98.55%).
- Lines: 238/238 (100.00%).
- Methods: 70/70 (100.00%).
- Classes: 23/23 (100.00%).

Production code is unchanged; coverage therefore remains the same. New cases establish posted
rounding, zero-credit sequences, exact no-catch-up behavior, calendar boundaries and rejection
after prior monthly success. See INTEREST_BOUNDARY_REVIEW.md for gaps and financial examples.
No production defect was found and no feature/framework was added to manufacture activity.

All eleven concrete custom exceptions remain exercised through operations. The only missed
branch is Transaction's documented compiler-emitted enum fallback; legal values/null are tested.
InterestPolicy has no executable code. No clocks, prohibited execution APIs or private-state
manipulation were added. Impure policy side effects, concurrency, VM failure and persistence
remain outside guarantees; the later-failure doubles do not mutate accounts.

Persisted local JDK/Maven/settings/cache and the system Java CA trust store were reused.
Credentials and tooling remain outside tracked source. UML source/diagram were checked against
the unchanged production tree and retained. The five-section interim DOCX was updated and its
structure/embedded diagram checked; no full Word pagination rendering is claimed. Prior evidence
is preserved in numbered verification files. Final audits/report/defense remain unfinished.

## Repository preflight and genuine history

The existing worktree was clean; safe fetch matched origin/main at
c81ba880a05c1bcfca155ba7aa7a9df9e1b14532. GitHub confirmed authentication, private visibility and
push permission. Original commit ancestry was checked. Review precedes a meaningful real-date
commit/push; exact remote SHA/private visibility/ancestry are independently checked and reported
in the session result. No history or dates are rewritten. The two-week span remains unfinished;
a saved automation configuration does not prove future runs or pushes.

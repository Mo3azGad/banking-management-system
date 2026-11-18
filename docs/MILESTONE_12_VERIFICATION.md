# Milestone 12 verification — shared subtype and SOLID/pattern review

Verified 17 November 2026 (Europe/Istanbul), full Maven `clean verify`: BUILD SUCCESS.
Eclipse Temurin 21.0.13+9, Java 17 compiler target; Maven 3.9.11; JUnit 5.11.4;
Surefire 3.5.2; JaCoCo 0.8.12. Build finished at 2026-11-17T16:52:43Z.

JUnit: 639 invocations, 0 failures, 0 errors, 0 skipped. No tests were added or removed.
This session codifies the already-tested Account contract and reviews actual design evidence;
it does not claim a behavior change or inflate activity with redundant cases.

- Instructions: 1046/1046 (100.00%).
- Branches: 68/69 (98.55%).
- Lines: 238/238 (100.00%).
- Methods: 70/70 (100.00%).
- Classes: 23/23 (100.00%).

Only Account Javadoc changes in Java sources; removing block comments yields identical Account
code to the previous commit. Test sources and UML files are unchanged. DESIGN_REVIEW.md maps
availability/date/history/freeze/policy contracts to existing named success/failure tests and
qualifies all five SOLID claims plus Template Method/Strategy. The review found no behavioral
defect or justified simplification. New arbitrary subtypes/impure policies are not proved safe.

Every concrete custom exception (eleven) remains exercised through operations. The sole missed
branch is Transaction's compiler-emitted enum fallback; legal values/null are tested. The
abstract-method-only InterestPolicy has no executable code. No clock/prohibited execution APIs
or private-state manipulation were added. JDK Javadoc HTML/reference doclint also passed with
no diagnostics (missing-documentation warnings disabled). Concurrency, persistence, VM/resource failure and
rollback of arbitrary policy side effects remain outside guarantees.

Persisted local JDK/Maven/settings/cache and system Java CA trust were reused; normal installed
tools run `mvn clean verify`. Credentials/tooling remain outside tracked source. UML classes,
signatures and relationships remain accurate after the contract clarification; no diagram change
is warranted. The five-section interim Word report now has precise contract/pattern/SOLID evidence;
structure and retained embedded diagram are checked without claiming full pagination rendering.
Final public-operation/exception/input audits, final report and defense remain unfinished.

## Repository preflight and authentic history

Existing main was clean; safe fetch matched origin/main at
e7a723cea1277d30fb9e80b6827a51c0815339c3. GitHub confirmed authorized authentication, private
visibility and push permission. The three original commits remain ancestors. A meaningful
real-date commit/push follows review; exact remote SHA, private visibility, clean worktree and
preserved ancestry are independently checked and reported in the session result. No timestamps
or history are replaced. The two-week span remains unfinished; a saved automation does not
prove future execution or acceptance by the professor.

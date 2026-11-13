# Milestone 8 verification — pure interest calculation policy

Verified 13 November 2026 (Europe/Istanbul), full Maven `clean verify`: BUILD SUCCESS.
Eclipse Temurin 21.0.13+9, Java 17 compiler target; Maven 3.9.11; JUnit 5.11.4;
Surefire 3.5.2; JaCoCo 0.8.12.

JUnit: 529 invocations, 0 failures, 0 errors, 0 skipped.
All 488 prior cases remain; FixedRateInterestPolicyTest adds 41 cases.

- Instruction: 892/892 (100.00%).
- Branch: 60/61 (98.36%).
- Line: 203/203 (100.00%).
- Method: 62/62 (100.00%).
- Class: 21/21 (100.00%).

All executable policy code, including custom wrapping of decimal scale arithmetic failure,
is covered. The only missed branch remains Transaction's compiler-emitted enum-switch fallback;
all legal types/null rejection are tested. JaCoCo counts 21 executable classes, excluding the
InterestPolicy interface because it contains only an abstract method. All nine concrete custom
exceptions remain exercised; invalid policy inputs reuse InvalidBankingInputException.

Calculation tests cover monthly decimal semantics, zero/high-precision rates, exact-cent balance
validation, final-only HALF_EVEN ties/neighbors, large money and repeatability. Tests use actual
checking/savings metadata and show that even a frozen Account's quote does not mutate its ledger.
No Account policy field/injection, month-end eligibility, duplicate month or interest posting is
implemented/claimed tested. No production Account/Bank posting implementation changed today.

The existing local JDK/Maven, external proxy settings and system Java CA trust store were reused.
No credentials/tools are committed. Normal installations use `mvn clean verify`. Source restrictions
are checked separately; current UML is rendered/visually inspected and DOCX sections/diagram are
structurally checked. No full Word pagination rendering or unbounded resource guarantees are claimed.
Earlier evidence is preserved in the numbered milestone files.

## Repository preflight

The EXISTING worktree was clean on main. Fetch confirmed HEAD/origin agreement at
080fe14a40fc94a2d6a2265fdf2c83afc3abe066. GitHub confirmed private visibility and push permission;
original commits were verified as ancestors. A meaningful real-date commit/push follows review,
then its exact remote SHA/private visibility/ancestry are checked and reported in the session result.
No history rewrite, replacement project or delayed publication of prewritten future code is used.
The two-week span is unfinished; a saved automation is not proof of future completion.

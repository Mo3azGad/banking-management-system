# Milestone 7 verification — atomic Bank transfers

Verified 12 November 2026 (Europe/Istanbul), full Maven `clean verify`: BUILD SUCCESS.
Eclipse Temurin 21.0.13+9, Java 17 compiler target; Maven 3.9.11; JUnit 5.11.4;
Surefire 3.5.2; JaCoCo 0.8.12.

JUnit: 488 invocations, 0 failures, 0 errors, 0 skipped.
All 402 prior cases remain; BankTransferTest adds 86 cases.

- Instruction: 856/856 (100.00%).
- Branch: 58/59 (98.31%).
- Line: 193/193 (100.00%).
- Method: 60/60 (100.00%).
- Class: 20/20 (100.00%).

Every Bank, Account and PreparedPosting branch is covered. The one missed branch is Transaction's
compiler-emitted exhaustive-enum fallback; all legal values and null rejection are tested.
All nine custom concrete exceptions remain exercised. Transfer integration tests cover all type
pairs, conservation, paired immutable history, both-side failure-state preservation and separate
reference/local ID sequences. No private counter manipulation inflates boundary coverage.

Coverage does not imply implemented monthly interest or concurrency/persistence guarantees.
Long-range reference exhaustion and VM resource errors are not simulated. The existing local
JDK/Maven, external proxy settings and Java CA trust store were reused; no credentials or tools
are committed. On a normal setup run `mvn clean verify`. Source restrictions are checked separately.
UML is regenerated/visually inspected and DOCX sections/embedded diagram structurally checked;
no full Word/LibreOffice pagination rendering is claimed. Prior evidence is kept in numbered files.

## Repository preflight

The EXISTING repository was clean on main; fetch confirmed exact HEAD/origin agreement at
40c296431fdfa9f3ae7dd26538c90d230c32d95c. GitHub confirmed private visibility and push permission;
original commits remain ancestors. A real-date commit/push follows verified review, then exact
remote SHA/private visibility/ancestry are checked and reported in the session result. No original
history is rewritten and no future feature is prewritten for delayed publication. The two-week
span remains outstanding; saved scheduling is not proof of future completion.

# Milestone 4 verification — Bank account opening and retrieval

Verified 9 November 2026 (Europe/Istanbul), from full Maven `clean verify`: BUILD SUCCESS.
JDK Eclipse Temurin 21.0.13+9; compiler release 17; Maven 3.9.11; JUnit 5.11.4;
Surefire 3.5.2; JaCoCo 0.8.12.

JUnit: 273 invocations, 0 failures, 0 errors, 0 skipped.
Existing 205 cases remain; BankAccountTest adds 68 cases.

- Instruction: 662/662 (100.00%).
- Branch: 48/49 (97.96%).
- Line: 146/146 (100.00%).
- Method: 45/45 (100.00%).
- Class: 16/16 (100.00%).

All Bank and Account branches are covered. The single uncovered branch is the compiler-emitted
default in Transaction's exhaustive enum switch; all six legal values/null rejection are tested.
Coverage applies to implemented code only; freezing, transfers and interest remain unimplemented.
Every custom concrete exception, including the two account-registry additions, is thrown by an
operation test. Getter/snapshot methods have no artificial domain-failure input.

The workspace-local JDK/Maven, external Maven proxy settings and existing Java CA trust store
were reused. No dependencies or credentials were copied into source. On a normal JDK/Maven
installation run `mvn clean verify`. No main/menu/UI, Scanner, console output or clock reads
are allowed; source restrictions are reviewed separately from functional coverage.

## Pre-session repository checks

The EXISTING /workspace/work/banking-management was clean on main. A successful fetch showed
HEAD and origin/main identical at 0809133609badb8b20f42b7491d03d297aa6ea00. GitHub API confirmed
repository Mo3azGad/banking-management-system is private and the current authentication has push
permission. The three original milestone commits were verified as ancestors. No history rewrite,
new project or replacement credentials were needed. A new real-date commit/push follows review;
the run summary records its SHA and remote verification after completion.

Previous build evidence is retained in MILESTONE_1_VERIFICATION.md through MILESTONE_3_VERIFICATION.md.
UML is regenerated/visually inspected and the interim DOCX structurally checked; no full Word
pagination-render check is claimed. Planned later sessions are not represented as completed.

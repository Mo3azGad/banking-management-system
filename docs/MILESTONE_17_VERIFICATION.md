# Milestone 17 verification — individual defense and verified JUnit demo

Verified 22 November 2026 (Europe/Istanbul). The existing repository was clean at
cca1b414af879ad9b6ad2ea4ac49eec003ada6bf; safe fetch matched origin/main exactly.
GitHub authentication, private visibility, push permission and all three original ancestors
were checked before work. Existing history and user files are preserved.

## Meaningful completed preparation

DEFENSE.md replaces the chronological draft with a roughly 520-word speaking plan allocated
across five minutes, source navigation for the important operations, actual pattern/SOLID
explanations and likely questions/answers. Its timing is an estimate for personal rehearsal,
not a claim that the student has delivered it aloud or attended the mandatory defense.
DEFENSE_DEMO.md supplies exact selected-method commands, actual fixture/expected ledger facts,
single-threaded transfer limits and laptop/offline setup guidance. DEFENSE_EXERCISE.md gives
an independent constructor-injected policy modification task and a separately hidden outcome
checklist, without prewriting a solution or adding redundant tests/features to the submission.

No Java source, JUnit source, pom.xml, UML sources/exports or final-report artifacts change.
The verified technical report retains its explicit 21 November edition; its descriptions and
659-case figures remain accurate. This day's current evidence is here. The prior current
verification is preserved byte-for-byte as MILESTONE_16_VERIFICATION.md. Current documentation
links preparation to the remaining student rehearsal/attendance and independent review.

## Focused commands actually executed

The four methods named in DEFENSE_DEMO.md were selected through Surefire, using the existing
Maven configuration. Both commands returned BUILD SUCCESS with no failures/errors/skips:

| Selection | Invocations | Finished (UTC) | Maven reported duration |
|---|---|---|---|
| BankTransferTest transfer success and recipient-opening-date rejection | 8 (two methods × four product pairs) | 2026-11-22T16:42:23Z | 2.367 s |
| MonthlyInterestTest default posting and repeated-month rejection | 4 (two methods × two products) | 2026-11-22T16:42:41Z | 2.002 s |

These are reruns of twelve cases already in the complete suite, not twelve new cases. Timings
are cached cloud build observations, not promised laptop duration or measured spoken delivery.
The source assertions and passing XML establish the demonstrated results. Offline operation
on the student's laptop must still be configured/verified there; it is not claimed tested here.

## Complete clean build after the focused runs

Maven `clean verify`: BUILD SUCCESS, finished 2026-11-22T16:42:51Z, 5.122 s.
Eclipse Temurin 21.0.13+9, Java 17 compiler target; Maven 3.9.11; JUnit 5.11.4;
Surefire 3.5.2; JaCoCo 0.8.12. Persisted external tools/settings/cache and the system Java
CA trust store are used; no credentials, caches or machine-specific tools enter the commit.

JUnit: 659 invocations in fourteen suites; 0 failures, 0 errors, 0 skipped.

- Instructions: 1054/1054 (100.00%).
- Branches: 70/71 (98.59%).
- Lines: 239/239 (100.00%).
- Methods: 70/70 (100.00%).
- Classes: 23/23 (100.00%).

The sole missed branch is still the compiler-generated exhaustive enum-switch fallback at
Transaction.java line 24. Every legal TransactionType and null rejection is tested. Each of the
eleven concrete custom exceptions remains thrown by an operation test. The pure InterestPolicy
interface has no executable body. The final clean run restores authoritative full Surefire and
JaCoCo reports after focused `test` runs; mixed/stale subset reports are not counted as full evidence.

## Documentation and prediction checks

- Both exact command selectors resolve to real source methods and passing parameterized XML
  invocations. Fixtures, dates, amounts, paired references, rejection snapshots and valid retry
  behavior are checked against their test assertions and the production coordination path.
- Method/class/interface links in the defense material resolve to the actual implementation.
  Explanations of validation/publication order, constructors, floors/rates, immutable history,
  scoped identity, pure-policy/month-marker rules, two patterns and all five SOLID principles
  are reviewed against source and the existing design/exception audit.
- The quoted speaking paragraphs contain 519 word tokens in the preparation check, allocated in
  contiguous 0:00–5:00 intervals; at 120 words/minute this is about 4:20 of speech plus navigation.
  That arithmetic does not certify a student's pace, understanding or completed rehearsal.
- Independent decimal predictions: override 1000.00 × 0.002 credits 2.00, then 1002.00 × 0.002
  credits 2.00, ending 1004.00. Default savings credits 3.00; metadata remains 0.003. Floor,
  duplicate, frozen and ID expectations follow the unchanged tested contracts. No exercise
  implementation or extra passing exercise tests are claimed.
- Diff, relative file links, archive identity and unchanged production/tests/report/UML are checked.
  No prohibited execution/clock API or private-state manipulation is added. These are preparation
  checks, not additional JUnit invocations or the independent final review proposed for 23 November.

## Genuine dates and remaining work

The original first commit's author/committer timestamp is 2026-11-08T16:23:30Z. At this clean
verification, 14 days 00:19:21 had elapsed. Meaningful development has taken place on multiple
real dates; elapsed wall time alone is not proof of the professor's Git criterion. This session's
real-date commit/push is independently checked against exact remote HEAD, private visibility,
clean worktree, original ancestry and actual timestamp span, and reported in the session result.
No dates/history are fabricated or replaced, and professor acceptance is not asserted.

Next proposed session: the separate 23 November independent clean-build/submission-artifact review.
The student must still rehearse, complete the independent exercise and attend the assigned defense.
No future run/push is certified by an existing saved schedule, and no automatic continuation past
that configured final session is implied.

# JUnit defense demonstration — verified 22 November 2026

Use this alongside [the timed explanation](DEFENSE.md). These commands execute existing tests;
they add no application entry point or new test cases. The demo is a subset of the full suite.
All amounts/dates below come from the actual fixture/assertions, not from live clock values.

## Prepare on your laptop before the appointment

From the existing repository root with JDK 17+ and Maven 3.9.x:

```sh
mvn clean verify
```

Expect 659 invocations, no failures/errors/skips. Open `target/site/jacoco/index.html` for the
coverage report. Run the two focused commands below once with network available; verify your
offline setup by rerunning each with Maven's `-o` flag. First use needs dependency downloads;
do not assume this cloud workspace's cache or machine paths exist on the laptop.

The commands were verified here using the persisted JDK/Maven, external Maven settings/cache
and system Java CA store; those machine paths are not required by the portable commands.
Bring your laptop, keep the relevant IDE tabs open, and use the test runner rather than typing
a main program. Course weeks 11–12 determine the defense slot; confirm the assigned date.

## Demo A — a transfer succeeds, and recipient rejection cannot partly debit

```sh
mvn '-Dtest=BankTransferTest#transferConservesMoneyAndPostsOneLinkedEntryPerAccount+dateValidForSourceButBeforeTargetOpeningCannotPartiallyDebit' test
```

The complete quoted `-Dtest=...` is one argument. Surefire's `+` selects two named methods;
each method runs for all four checking/savings source/target pairs. Expect **8 passing
invocations**, failures/errors/skips 0/0/0. An expected exception caught by `assertThrows`
is a passing rejection test, not a broken demonstration.

Open [BankTransferTest.java](../src/test/java/edu/banking/BankTransferTest.java) at these methods.
Before running, predict the following; after running, point to the matching assertions.

| Test / step | Fixture and expected observable result |
|---|---|
| Fixture | Customer C1/Ada owns S, opening 250.00 on 2024-02-28; C2/Grace owns T, opening 150.00 on 2024-02-29. Each starts with OPENING ID 1. A fresh fixture is created for each invocation. |
| transferConservesMoneyAndPostsOneLinkedEntryPerAccount | Transfer 10.0100 on 2024-02-29 normalizes to 10.01. S becomes 239.99; T becomes 160.01; combined balance stays 400.00. Receipt order is TRANSFER_OUT, TRANSFER_IN. Both facts have local ID 2, shared reference 1, the same date/amount and the correct result balances. History contains the identical returned immutable facts. |
| dateValidForSourceButBeforeTargetOpeningCannotPartiallyDebit | Attempt 1.00 on 2024-02-28. The source date is valid; the target has not opened. InvalidPostingDateException is thrown, and snapshots of both balances/histories remain equal to their pre-attempt values. |
| Valid retry within that rejection test | Transfer 1.00 on 2024-02-29 succeeds using reference 1 and local ID 2 on each side: the failed attempt reserved neither reference nor IDs. The success invocation above is a separate fixture, not earlier state for this retry. |

Walk [Bank.transfer](../src/main/java/edu/banking/Bank.java): canonical lookups → distinctness →
candidate reference → outgoing preparation → incoming preparation → receipt allocation → both
validations → both guarded commits → reference advancement. Show Account's private replacement
ledger and PreparedPosting.commit. A valid source preview is not an early withdrawal. Explain
the single-threaded/distinct-account/no-interleaving assumptions; do not promise concurrent or
database transactions. Local IDs happen to match in this fixture; they need not match generally.

## Demo B — default interest posts once per supplied month

```sh
mvn '-Dtest=MonthlyInterestTest#defaultPolicyPostsTheCorrectRateAndOneCompleteInterestEntry+repeatedMonthRejectsWithoutChangingBalanceOrHistory' test
```

Expect **4 passing invocations**, failures/errors/skips 0/0/0: two methods × two products.
Open [MonthlyInterestTest.java](../src/test/java/edu/banking/MonthlyInterestTest.java).

| Test / step | Fixture and expected observable result |
|---|---|
| Fixture | A1 opens at 1000.00 on 2024-01-15 with its default policy. January date is 2024-01-31; February date is leap-day 2024-02-29. Each invocation starts fresh. |
| defaultPolicyPostsTheCorrectRateAndOneCompleteInterestEntry | Checking credits 1.00 and ends at 1001.00; savings credits 3.00 and ends at 1003.00. INTEREST is local ID 2, uses January's supplied date and has no transfer reference. The opening-only snapshot stays length 1. |
| repeatedMonthRejectsWithoutChangingBalanceOrHistory | After January success, repeating January throws InterestAlreadyAppliedException with its message identifying A1 and 2024-01. Balance/history are preserved. February then succeeds with ID 3, showing no ID/month reservation by rejection. |

Show Account.applyMonthlyInterest's eligibility guards and history-derived month marker, then
the interface call and FixedRateInterestPolicy's final HALF_EVEN rounding. The sample numbers
are exact and not rounding ties; use the existing `halfEvenRoundingHandlesBothTieDirectionsAndAdjacentValues`
method in FixedRateInterestPolicyTest if the professor asks about ties. Zero interest still
creates a month marker; that is covered elsewhere in MonthlyInterestTest.

## After the focused runs

```sh
mvn clean verify
```

Focused `test` runs replace selected Surefire XML files and do not regenerate the JaCoCo HTML
report. Do not add mixed report counts or present stale HTML as focused-run/full-suite evidence.
The final clean verify restores authoritative complete test/coverage artifacts. The 12 focused
invocations are reruns of existing cases, not 12 new cases added to the 659.

Here, Demo A finished at 2026-11-22T16:42:23Z (Maven 2.367 s); Demo B finished at
2026-11-22T16:42:41Z (2.002 s). These are actual cached cloud build times, not guaranteed laptop
demo durations or a measurement of spoken delivery. The subsequent clean verify finished at
2026-11-22T16:42:51Z with all 659 passing. See VERIFICATION.md for full metrics/toolchain.

If the network is unavailable during the defense, use the offline setup you have actually
verified, or the IDE's installed JUnit runner. A failed build must be shown and diagnosed
honestly; a prior report is not a claim that a new live run succeeded. Keep a completed build
summary ready for reference and demonstrate your ability to explain the actual tests/code.

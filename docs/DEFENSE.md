# Individual oral defense — final preparation, 22 November 2026

The official PDF requires a five-minute individual defense in weeks 11–12: bring your laptop,
run tests and walk through your code. Attendance is compulsory; absence fails the project.
The defense is worth 30 points. Confirm your assigned course slot separately from the
7 December UZEM submission deadline. These notes prepare you; they do not record that you
have personally rehearsed, completed the exercise or attended the defense.

Use the timed explanation below, [verified demo](DEFENSE_DEMO.md), and
[independent exercise](DEFENSE_EXERCISE.md). The [technical report](final-report.docx) and
[13-page preview](final-report-preview.pdf) are refreshed/verified on 23 November;
no Java behavior/UML change is needed for defense preparation. Current test evidence is in
[VERIFICATION.md](VERIFICATION.md).

## Five-minute explanation

The quoted paragraphs are a speaking draft, not claims about future demonstrations. The
speaking draft is about 520 words: approximately 4 minutes 20 seconds at 120 words/minute,
leaving about 40 seconds for navigation and the two brief JUnit runs. Rehearse aloud with a stopwatch, explain in your own
words and pause for the professor's questions. The times are targets, not a measured personal
rehearsal. Run the complete suite before the appointment; use only the focused runs during it.

### 0:00–0:30 — Scope and constraints

Action: open the domain UML and keep the IDE ready.

> My project models a small bank with customers, checking and savings accounts, and immutable
> transactions. It supports opening, deposits, withdrawals, transfers, history, monthly interest
> and freezing. It contains classes and JUnit tests only. Dates are supplied explicitly, and
> BigDecimal represents money in exact cents. There is no main program or system-clock access.

### 0:30–1:10 — Class structure and invariants

Action: point to Bank, Account, the two products, Transaction and InterestPolicy in the UML.

> Bank registers customers and opens accounts for its own registered owners. Account controls
> the ledger and posting rules. Checking retains zero and savings retains one hundred; their
> default monthly rates are point one and point three percent. These numbers are my documented
> choices. Available balance subtracts the floor. An immutable LedgerState holds balance and
> history together; callers cannot edit that history. Freezing blocks all financial postings
> while preserving readable state.

### 1:10–2:10 — Transfer success and rejection

Action: run Demo A, then show its recipient-date rejection and Bank.transfer.

> These tests cover all four account-type pairs. A successful transfer moves ten point zero one
> from two hundred fifty to one hundred fifty, leaving two hundred thirty-nine point ninety-nine
> and one hundred sixty point zero one. The total remains four hundred. The paired facts share a
> Bank-local reference; transaction IDs belong to each account. The rejection test uses a date
> valid for the sender but before the recipient opened. Both ledgers stay unchanged. Bank
> prepares both complete ledgers and the receipt, validates both plans, then publishes them.
> That guarantee assumes single-threaded execution and distinct accounts; it is not database
> or concurrent atomicity.

### 2:10–2:55 — Monthly interest and the strategy

Action: run Demo B; show applyMonthlyInterest and InterestPolicy.calculate.

> Interest requires an explicit month-end, valid chronology and a month not already processed.
> On a balance of one thousand, the defaults credit one for checking and three for savings.
> Repeating that month throws InterestAlreadyAppliedException without changing the ledger or
> consuming an ID. Account delegates arithmetic to an injected InterestPolicy. The fixed policy
> multiplies exactly and rounds once with HALF_EVEN. Even zero interest creates an INTEREST
> entry, which is also the processed-month marker.

### 2:55–3:50 — Patterns and all five SOLID principles

Action: point to preparePosting, getMinimumBalance and the injected policy field/constructor.

> Template Method shares posting validation and preparation while the minimum-balance hook
> varies the product rule. Strategy separates interest arithmetic from eligibility and posting.
> For SOLID: single responsibility separates Bank coordination, Account state and policy math.
> Open/closed lets an injected policy vary calculation without rewriting posting. Liskov means
> both account types honor the same contract expressed through available balance, not raw
> balance. Interface segregation keeps InterestPolicy to one calculation operation. Dependency
> inversion makes Account depend on that interface; concrete constructors compose defaults.
> These are specific extension points, not a claim that any future change needs no edits.

### 3:50–4:30 — Exceptions and evidence

Action: show the exception view and the completed full-build summary.

> BankingException is an abstract unchecked base with eleven specific domain exceptions.
> Failures carry clear messages rather than returning null or printing errors. Unexpected
> policy failures retain their cause in InterestCalculationException. The complete suite has
> six hundred fifty-nine invocations, including success, rejection and boundary cases. Every
> concrete custom exception is thrown by an operation test. Line coverage is one hundred
> percent; the sole missed branch is the compiler-generated enum-switch fallback.

### 4:30–5:00 — Limits and a change you can explain

Action: finish on the policy constructor; invite questions when appropriate.

> This is in-memory, single-threaded code. History copying costs linear time, and policies must
> not mutate accounts. For a different rate on one standalone savings account, I can inject a
> fixed policy through its existing constructor while preserving the default product metadata.
> I would predict the cents, write focused success and rejection tests, then run the full suite.

## Source navigation and important methods

Open these files in advance. Use method-name search rather than memorizing line numbers.
The diagrams select members; [UML_GUIDE.md](UML_GUIDE.md) expands the exact signatures.

| File / methods | Explain the responsibility and the point where state changes |
|---|---|
| [Bank.java](../src/main/java/edu/banking/Bank.java): registerCustomer, openCheckingAccount, openSavingsAccount | Validate a complete model before inserting it. Account IDs are shared across products; opening resolves the Bank's canonical Customer. Failure does not reserve an ID. |
| Bank.findCustomer / findAccount / getCustomers / getAccounts | Lookup throws typed failures. List copies protect membership and retrieval order. Accounts in a membership snapshot are still the actual mutable objects. |
| [Account.java](../src/main/java/edu/banking/Account.java): deposit / withdraw / preparePosting | Final entry points use one preparation template: active guard, positive exact-cent input, supplied date/chronology, next balance, minimum hook, complete immutable entry/ledger. Public methods immediately commit. |
| Account.prepareEntry / PreparedPosting.validate / commit | Allocate the entry/history/replacement before mutation. Plan validation checks active state then expected ledger identity. commit revalidates and assigns ledger once. Reuse or any intervening posting makes a plan stale. |
| Bank.transfer | Lookup both, require distinct accounts, candidate reference, outgoing/incoming preparation, receipt allocation, both validations, both commits, reference advancement. A valid sender preview is not an early debit. |
| Account.applyMonthlyInterest | Active, date, month-end, chronology, duplicate YearMonth, one policy call, validated result, immutable preparation/commit. INTEREST history is authoritative; no separate month set can get out of sync. |
| [FixedRateInterestPolicy.java](../src/main/java/edu/banking/FixedRateInterestPolicy.java): calculate | Validate balance, multiply by the immutable nonnegative monthly rate exactly, round only the product to cents using HALF_EVEN. Calculation itself posts nothing. |
| Account.freeze / getTransactionHistory / getAvailableBalance | Freeze changes only a private flag and is irreversible; repeat calls reject. History remains immutable/readable. Availability is balance minus floor, not permission to post while frozen. |
| [Transaction.java](../src/main/java/edu/banking/Transaction.java): canonical constructor | Validate an immutable fact: positive local ID, money/date/type and transfer reference. It does not execute a posting, establish account chronology or prove the counterpart exists. |
| [CheckingAccount.java](../src/main/java/edu/banking/CheckingAccount.java) / [SavingsAccount.java](../src/main/java/edu/banking/SavingsAccount.java) | Product floors/default rates, four-argument default constructors and five-argument policy overloads. Savings establishes its floor at opening; base construction calls no overridable product hook. |

## Likely questions and concise answers

| Question | Answer grounded in this implementation |
|---|---|
| Why no main, menu or console output? | The professor requires classes/methods executed through JUnit only. Maven's test runner output is not application console I/O. |
| Why BigDecimal, strings and compareTo? | Strings preserve the intended decimal value. compareTo checks numeric order independent of scale; normalization establishes exact cents. Binary floating point is inappropriate for these exact monetary contracts. |
| Why reject 1.001 rather than round it? | External money must already be exact cents; trailing zeros are harmless. Rounding is an explicit interest-calculation rule, not a way to silently change an invalid request. |
| Are the floor and rate values mandated by the PDF? | No. The PDF requires different rules/rates; 0/100.00 and 0.001/0.003 are explicit chosen rules. Rates are monthly decimal multipliers, not annual rates or percent-number inputs. |
| Does savings violate LSP by keeping 100.00? | The base contract permits spending up to available balance. At raw balance 250, checking can spend 250 and savings 150. Both obey that same contract and shared tests. |
| Can a subclass bypass posting guards? | Supplied products are final; inherited posting methods and internal entry points are final. The abstract minimum hook must be stable and pure. Tests do not prove arbitrary future subtypes correct. |
| Why unchecked exceptions? | These are input/state rejections in a small in-memory API, without checked I/O. Callers can catch a specific subtype or BankingException. No failure is swallowed or converted to null. |
| How do you prove each exception is tested? | API_EXCEPTION_AUDIT.md names an actual assertThrows witness for all eleven concrete subtypes. The abstract base is exercised through them; constructing an exception alone is not the evidence. |
| What error wins when inputs are invalid in several ways? | The first reached guard. Bank transfer lookups/distinctness precede preparation; each account checks frozen state before its posting inputs. Chronology precedes funds and, for interest, duplicate-month checks. There is no global frozen-first rule across both accounts. |
| Why not withdraw and then deposit for a transfer? | Recipient date or frozen validation could fail after the sender was debited. Preparation/prevalidation of both sides prevents that ordinary domain partial failure. |
| Could the second commit fail after the first? | Under the stated contract both distinct plans are prevalidated, with no interleaving or callbacks/allocation/arithmetic on the successful publication path. Concurrency and VM failure are outside the guarantee; this is not durable rollback. |
| Why ledger identity rather than equal balances? | The same balance can follow a deposit/withdrawal round trip with different history and sequence. Identity detects that changed ledger and rejects the old plan. |
| Are transaction IDs globally unique? | No. (accountId, transactionId) identifies a fact within a Bank account namespace. A separate positive Bank-local transfer reference pairs legs, whose local IDs may differ. Neither scope persists across restarts. |
| Is a list snapshot a deep account snapshot? | Customer/transaction values are immutable. An account-list snapshot fixes membership, while contained Accounts retain mutable identity/state. History snapshots remain stable after new postings. |
| Why does freeze have no date? | It records no timestamp or financial entry and performs no date comparison. It changes permission only. All date-dependent opening/posting methods take LocalDate. |
| Why permit supplied future dates? | There is no explicit reference date input and no clock read. Only opening/latest-entry chronology is enforced; an invented today-based check would violate the contract. |
| What about zero interest or missed months? | Zero INTEREST still marks the month. Skipped months are not credited automatically; earlier months cannot be backfilled after a later posting. First-month interest is not prorated. |
| Can a same-day deposit reopen interest? | No. Ordinary same-day posts are allowed, but the existing INTEREST entry still blocks a second application for that YearMonth. |
| Does getMonthlyInterestRate reveal a custom policy's rate? | It describes the default product multiplier. Actual posting calls the injected algorithm, which need not even be a fixed-rate algorithm. |
| What if an injected policy fails or mutates an account? | Invalid output and failures without side effects preserve the ledger/month/ID. Domain failures propagate; unexpected runtime failures are wrapped with cause. Mutation violates the pure-policy contract; arbitrary external effects are not rolled back. |
| How do you know there are patterns? | Name their problem and actual participants: Account template plus minimum hook; Account context, InterestPolicy strategy and FixedRateInterestPolicy algorithm. Bank opening is not GoF Factory Method; freezing is not a State-pattern framework. |
| Does several assertions in a test violate one behavior per test? | No. A rejected transfer's single postcondition includes both balances, both histories and unchanged sequence/reference behavior. The assertions establish that one complete rejection behavior. |
| Why not claim 100% branch coverage? | JaCoCo is 70/71 branches (98.59%). The exhaustive enum switch has one unreachable compiler fallback. All legal values/null rejection are tested; no reflection or invented enum is used to chase a number. |
| What do the tests not establish? | Persistence, concurrent atomicity, VM/resource failure, counter exhaustion and arbitrary impure callback rollback. Coverage also does not prove all future inputs/subtypes correct. |
| What would you improve if the scope grew? | Define durability/concurrency requirements first; then design storage/transaction boundaries and measure the O(n) history-copy cost. Those systems are not required by this assignment. |

## Personal rehearsal and completion status

1. On your own laptop, confirm JDK 17+ and Maven 3.9.x; run `mvn clean verify` before the appointment
   and prepare the dependencies for an offline demo. Follow DEFENSE_DEMO.md to verify that setup.
2. Keep the domain, internal and exception UML views, four demo methods, Bank.transfer,
   Account.preparePosting/applyMonthlyInterest and the policy constructor open in the IDE.
3. Predict the demo balances/exceptions aloud before running. Explain the source and assertions,
   then deliver the speaking draft with a stopwatch and adapt its length to your pace.
4. Complete DEFENSE_EXERCISE.md without opening its answer checklist first. Explain which change
   uses constructor injection and why changing a global default would be a different requirement.
5. Attend your assigned individual slot with the laptop. Test output and prepared notes do not
   substitute for your understanding, personal rehearsal or compulsory attendance.

The two focused commands and complete build were actually run on 22 November; their evidence
is preserved in MILESTONE_17_VERIFICATION.md and current coverage is in VERIFICATION.md. This
material prepares the student; it does not establish spoken delivery. The separate independent
technical review is completed on 23 November in FINAL_REVIEW.md. No execution after the
configured final session is implied.

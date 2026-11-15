# Banking Management System

Advanced OOP, Project 2. **Milestone 10: interest-boundary review and behavioral test refinement.**
All listed banking operations now exist. Boundary/integration review, final documentation/report,
defense preparation and the genuine two-week history remain; this is not yet the final submission.

## Run the tests

Install a **JDK 17 or later** (not just a Java runtime) and Maven 3.9.x, then run:

```sh
mvn clean verify
```

Tests are the only application entry point. There is no main method, UI, menu, Scanner,
console output or system-clock access. JUnit 5 and JaCoCo results appear under
`target/surefire-reports/` and `target/site/jacoco/index.html`. First build requires Maven Central.

## Implemented

- Immutable Customer; Bank customer registration, lookup and immutable customer snapshots.
- Bank `openCheckingAccount(accountId, customerId, openingBalance, openedOn)` and
  `openSavingsAccount(accountId, customerId, openingBalance, openedOn)` validate and register
  new accounts owned by the bank's canonical registered customer.
- `findAccount(accountId)` returns the registered instance; `getAccounts()` returns an immutable
  membership snapshot in opening order. The accounts themselves retain their mutable posting state.
- Account IDs are unique across checking/savings within each Bank. Failed openings change neither
  registry and do not reserve IDs; duplicate IDs and missing accounts have dedicated exceptions.
- Abstract Account; CheckingAccount and SavingsAccount with different balance floors/rates.
- Checking: minimum 0.00, no overdraft, monthly rate metadata 0.001 (0.1%).
- Savings: minimum 100.00 on opening and withdrawals, monthly rate metadata 0.003 (0.3%).
- Final `deposit(BigDecimal, LocalDate)` and `withdraw(BigDecimal, LocalDate)` return the posted
  Transaction. They now prepare an immutable replacement ledger and publish its reference only
  after validation. Balance and history change together; old snapshots remain immutable.
- Package-private preparation returns an account-bound `PreparedPosting` with immutable facts,
  `validate()` and guarded `commit()`. Preparing/validating does not reserve an ID or advance dates.
  Intervening postings and repeated commits raise `StalePostingException`; freezing after
  preparation raises `AccountFrozenException`. No new public preparation/commit API is exposed.
- `Bank.transfer(sourceId, targetId, amount, date)` moves a positive exact-cent amount between
  distinct accounts registered in that Bank. It returns an immutable two-entry receipt ordered
  `TRANSFER_OUT`, `TRANSFER_IN`, with one shared positive Bank-local transfer reference.
- Transfer preparation validates both sides and builds both complete ledgers/receipt before
  publication. Rejection preserves both balances/histories, posting IDs/dates and the Bank reference.
  Savings floors and frozen-state/chronology checks apply to the relevant transfer legs.
- Final `freeze()` permanently blocks postings; `isFrozen()` exposes status. Repeated freezing
  and all deposit/withdraw or transfer-leg attempts on frozen accounts throw `AccountFrozenException` before
  input validation. Access through Bank lookup or an existing account reference behaves alike.
- Freezing does not add a financial transaction, consume a sequence ID or alter balance/chronology.
  No date is needed because it records no timestamp. History and all read-only metadata remain
  available; balance above the minimum floor stays readable but grants no posting permission.
- Opening ledger entry plus dated postings; `getTransactionHistory()` returns an immutable snapshot.
- Posting dates cannot precede the latest entry; same-day postings and opening-day postings work.
- Amounts must be positive and exactly representable in cents. Trailing zeros are accepted;
  nonzero fractions of a cent are rejected instead of silently rounded.
- Custom exception hierarchy: invalid input, duplicate/missing customer or account, insufficient
  funds, frozen-account state, stale posting plans, and invalid posting chronology. Tests exercise every concrete custom exception.
- `InterestPolicy.calculate(balance)` is a one-operation pure calculation contract.
  `FixedRateInterestPolicy(monthlyRate)` validates a nonnegative monthly decimal rate, multiplies
  exactly and rounds the final result once to two decimals with `HALF_EVEN`. Zero rates/results
  are valid. Missing, negative, sub-cent balance input and unsupported decimal arithmetic fail
  with `InvalidBankingInputException`; rates retain their precision and have no invented cap.
- `Account.applyMonthlyInterest(date)` validates active state, supplied month-end and chronology,
  rejects a repeated YearMonth, calculates/validates the injected policy result, and publishes one
  `INTEREST` entry through the guarded immutable-ledger path. Zero credit is recorded too.
- Original four-argument concrete constructors preserve defaults: 0.001 checking, 0.003 savings.
  New five-argument constructors accept an explicit `InterestPolicy` override. Account depends on
  the interface; concrete account constructors compose default fixed-rate policies.
- `getMonthlyInterestRate()` is default product-rate metadata, not introspection of an injected
  custom algorithm. Null policies are rejected. Negative/null/sub-cent policy results are rejected;
  domain policy failures propagate and unexpected runtime failures become `InterestCalculationException`
  with their original cause. `InterestAlreadyAppliedException` identifies a duplicate month.
- Transaction is an immutable record; its construction alone never executes an operation.

Transaction IDs start at 1 for opening and increase within each account. The identifier is the
pair `(accountId, transactionId)`, not the sequence number alone. Failed operations consume no ID
and do not advance the latest posting date. This replaces the earlier proposed Bank-wide
sequence. Bank now coordinates separate cross-account transfer references: local posting IDs
can differ between outgoing and incoming entries while their transfer reference agrees.

Direct Account constructors still create standalone domain objects with opening history. They
are not implicitly registered. Use the Bank opening methods to enforce registered ownership and
unique account IDs. No external-account import API is introduced. Lookup uses normalized,
case-sensitive IDs, and customer/account IDs have separate namespaces. Different Banks may
use the same IDs independently.
Names may repeat and identifiers are case-sensitive. Accounts still expose rate metadata.
The pure policy can calculate a quote from metadata/balance; Account now also holds/invokes an
injected policy for dated posting. The fixed floors/rates are project choices,
not numerical requirements specified by the professor. Single-threaded, in-memory use only.

## Documentation

- `docs/REQUIREMENTS.md`: PDF obligations versus explicit implementation choices.
- `docs/ARCHITECTURE.md`: current design, Template Method, injected calculation Strategy and actual SOLID mapping.
- `docs/DEVELOPMENT_PLAN.md`: remaining work roadmap and actual progress; schedule versus execution distinguished.
- `docs/current-uml.svg` / `.dot`: implemented model; `docs/proposed-uml.mmd`: final target design.
- `docs/TEST_MATRIX.md` and `docs/VERIFICATION.md`: current test/coverage evidence and limitations.
- `docs/MILESTONE_1_VERIFICATION.md` through `docs/MILESTONE_9_VERIFICATION.md`: earlier evidence.
- `docs/INTEREST_BOUNDARY_REVIEW.md`: 15 November findings, exact financial examples and 37 added cases; production code unchanged.
- `docs/milestone-report.docx`: updated five-section interim Word report, not the final report.
- `docs/DEFENSE.md`: current walkthrough, demonstration commands and eventual defense outline.

## Git and GitHub

Private repository: https://github.com/Mo3azGad/banking-management-system

Original milestone-1 commit `dba37153be8867b854a1aade7f5b891c45ad27aa` and milestone-2 commit
`244a7b6ed03ddd42f6c262a583f04b9d146aa3d9` are preserved. Milestone 1 was independently checked
against GitHub's exact SHA and private visibility before further development. Credentials and
machine-specific tooling stay outside tracked source. On another computer use `gh auth login`
and `gh auth setup-git` before pushing.

Milestones 1–3 were genuinely developed on 8 November 2026; milestone 4 was developed on
9 November; milestone 5 freezing on 10 November; milestone 6 posting preparation on 11 November; milestone 7 transfers on 12 November; milestone 8 policy calculation on 13 November; milestone 9 monthly posting on 14 November; milestone 10 interest-boundary tests/review on 15 November. The professor's at-least-two-week
history requirement is still outstanding.
Never change author/committer dates or invent activity. A work automation was saved and enabled
on 8 November for 9–23 November evenings in Europe/Istanbul. Saved scheduling is not proof of
future execution or pushes; verify each actual session and its result separately.

## Next milestone

Review cross-operation scenarios and invariants, then continue SOLID/coverage
audits, final UML/report and defense preparation. Do not manufacture edits solely to create activity. Keep UML and documentation synchronized with each tested increment.

## Internal preparation contract

Each plan is bound to its originating Account object and exact ledger version. Its constructor
and state are private; callers cannot forge a plan or commit it onto another Account. Plans are
not reservations or queued transactions. Same-balance round trips still invalidate old plans
because the ledger version/history changed. Frozen state is checked before stale state.

LedgerState is a private record containing balance and immutable history. Preparing a posting
copies history and allocates its result before publication: O(n) preparation cost and storage
for a retained plan, versus O(1) immutable history retrieval. This tradeoff is acceptable for the
small in-memory project. No concurrency, database transaction or distributed two-phase commit
is claimed. Bank transfer coordination enforces distinct accounts, prevalidates both plans before either
commit and allows no intervening mutations. Internal boundary tests remain separate from the
new BankTransferTest integration cases.

## Transfer validation and reference scope

Bank resolves both account IDs, rejects the same resolved account, then prepares the outgoing
leg before the incoming leg. Each preparer checks its own frozen status before amount/date/funds.
If multiple problems exist across accounts, the first reached failure wins; there is no global
error-priority rule across two accounts. All failures occur before any financial publication.
The immutable receipt is also allocated before commitment, with no application callbacks or
intervening mutations between final prevalidation and commits. Atomicity is limited to the
single-threaded in-memory model, not database durability or concurrent access.

References start at 1 per Bank and advance only after both commits succeed. They are neither
account posting IDs nor globally unique across Banks or process restarts. Different account
owners and different account types are allowed; foreign-bank accounts are not imported by ID.

## Interest calculation versus posting

A monthly rate is a decimal multiplier, e.g. 0.001 = 0.1%, not 1% and not an annual rate divided
by twelve. Balance input must be nonnegative and exactly representable in cents; extra trailing
zeros are allowed. Rate precision is not rounded before multiplication. HALF_EVEN is applied
only to the final result: 5.00 × 0.001 = 0.005 becomes 0.00, while 15.00 × 0.001 = 0.015 becomes
0.02. Unsupported BigDecimal scale arithmetic raises a custom input exception, not a raw error.

Calculation stores no dates, reads no clock, changes no ledger and decides no month eligibility.
It can quote a frozen account's balance because a quote is a read, not a posting permission.
The interface and fixed algorithm are implemented and exercised through InterestPolicy references;
Account-to-policy constructor injection and monthly posting are now implemented, with the
operation depending only on the InterestPolicy abstraction.

## Monthly application rules

Call `bank.findAccount(id).applyMonthlyInterest(date)` with an explicit month-end. Validation order
is frozen status, nonnull date, month-end, posting chronology, duplicate YearMonth, then policy
calculation/result checks and guarded publication. Dates before the latest posting fail chronology,
including a past already-processed month; same-current-month repetition has its dedicated exception.

Recorded INTEREST entries are the processed-month source of truth, independently of the latest
entry type. No separate mutable month flag is needed. A successful zero entry marks the month.
The first month earns a full calculation without proration, including opening on month-end.
Skipped months are not auto-credited; backdating remains forbidden. The amount uses balance
immediately before application on the supplied month-end. Ordinary later same-day postings remain
allowed and never trigger recalculation. Default rates describe product defaults; a custom policy
is an explicit algorithm override. There is no bulk Bank-wide monthly posting API.

Policies must obey the documented pure calculation contract: no clock or Account mutations.
Rejection-state guarantees apply to such policies; Account does not undo external side effects
of a contract-violating callback. Frozen/stale guards still prevent an invalid prepared interest
entry from overwriting changed state. No concurrency/database rollback or VM-failure guarantee.

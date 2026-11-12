# Banking Management System

Advanced OOP, Project 2. **Milestone 7: atomic Bank transfers with paired ledger history.**
Incremental university project; interest policies and monthly interest posting remain
later milestones. This is not yet the final submission.

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
Names may repeat and identifiers are case-sensitive. Rates are metadata; calculation and posting
will be introduced with InterestPolicy later. The fixed floors/rates are project choices,
not numerical requirements specified by the professor. Single-threaded, in-memory use only.

## Documentation

- `docs/REQUIREMENTS.md`: PDF obligations versus explicit implementation choices.
- `docs/ARCHITECTURE.md`: current design, Template Method, planned Strategy and SOLID mapping.
- `docs/DEVELOPMENT_PLAN.md`: remaining work roadmap and actual progress; schedule versus execution distinguished.
- `docs/current-uml.svg` / `.dot`: implemented model; `docs/proposed-uml.mmd`: final target design.
- `docs/TEST_MATRIX.md` and `docs/VERIFICATION.md`: current test/coverage evidence and limitations.
- `docs/MILESTONE_1_VERIFICATION.md` through `docs/MILESTONE_6_VERIFICATION.md`: earlier evidence.
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
9 November; milestone 5 freezing on 10 November; milestone 6 posting preparation on 11 November; milestone 7 transfers on 12 November. The professor's at-least-two-week
history requirement is still outstanding.
Never change author/committer dates or invent activity. A work automation was saved and enabled
on 8 November for 9–23 November evenings in Europe/Istanbul. Saved scheduling is not proof of
future execution or pushes; verify each actual session and its result separately.

## Next milestone

Implement the narrow InterestPolicy/FixedRateInterestPolicy strategy with BigDecimal calculation
and rounding tests. Monthly interest posting follows later, then final report/defense preparation. Keep UML and documentation synchronized with each tested increment.

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

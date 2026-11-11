# Architecture — current milestone 6 and final target

## Implemented responsibilities

Bank manages customer and account registration/lookup in private insertion-ordered maps.
Its named opening methods take a customer ID, not an arbitrary Customer or prebuilt Account.
They validate/normalize account ID and uniqueness, resolve the canonical registered Customer,
fully construct the concrete account (including balance/date validation and opening history),
and only then put it in the map. Any domain rejection leaves both maps unchanged and the
attempted ID reusable. DuplicateAccountException applies across both account types/owners.
There is no generic external-account import, repository framework or separate factory class.

findAccount returns the exact registered instance, so its balance/history updates remain visible.
getAccounts returns an immutable membership snapshot in opening order, not a deep snapshot of
mutable account balances. Bank ID namespaces are case-sensitive and normalize surrounding
whitespace; customer/account namespaces are distinct and each Bank is independent. Customer is immutable. BankingInput
is package-private and shares text, required-value and exact-cent validation across entities.
Account validates identity, owner, opening balance/date and creates an OPENING Transaction.
CheckingAccount permits a zero floor; SavingsAccount enforces 100.00. Both expose monthly
rate metadata. No overridable method is called from the base constructor.

Account owns a private frozen flag and one reference to a private immutable LedgerState record
(balance plus immutable Transaction list). Direct constructors create an opening entry; Bank
opening methods also register the account for a known customer. Existing public signatures and
success/failure contracts remain unchanged.

## Posting preparation, validation and publication

Final public deposit/withdraw now call the corresponding package-private prepareDeposit or
prepareWithdrawal and immediately commit the returned PreparedPosting. One private template:

1. Requires active state, then validates positive exact-cent amount, explicit date and chronology.
2. Computes the prospective balance and enforces the polymorphic minimum floor.
3. Builds a complete Transaction with the next account-local sequence ID.
4. Copies immutable history with that entry and allocates the full replacement LedgerState.
5. Returns a plan bound to this Account, expected ledger reference, replacement state and entry.

Preparing and validating are side-effect free: no balance/history/date change or ID reservation.
The package-private, final, non-static inner PreparedPosting has a private constructor and private
final fields, so it can only operate on its originating Account. Its transaction() exposes an
immutable preview, not proof of publication. validate() checks active state then expected-ledger
identity; mismatch throws StalePostingException. commit() revalidates, assigns the prepared state
reference once and returns the prepared entry. No allocation, arithmetic, subclass hook or other
fallible domain operation occurs after successful validation and before publication.

This rejects consumed plans and plans superseded by another posting, even if the balance returns
to the same numeric value. Freeze does not change ledger state, but the active guard blocks any
prepared plan afterwards and takes precedence over stale state. Failed preparation/public
posting does not invalidate a previously valid plan. Two plans prepared against one version may
both validate initially, but committing either invalidates the other; plans are not reservations.

Template Method remains the shared preparation workflow with the account-specific minimum hook.
Subclasses cannot override the final public or package-private preparation entry points. Existing
323 test cases preserve active/frozen posting contracts; new boundary tests exercise the split.
Immutable state publication replaces the earlier separate history append and balance assignment.
History retrieval returns its immutable list directly; old snapshots remain stable.

Preparation now costs O(n) copying per ledger, and retained plans retain their histories. The
small in-memory scope favors predictable publication over throughput optimization. This is not a
concurrency mechanism: ledger validation/assignment is not synchronized and frozen state is separate.
No database rollback, persistence or distributed two-phase commit pattern is implemented.

For future two-account coordination, first prepare and validate every participating plan. Accounts
must be distinct, with no intervening mutations. Each commit then rechecks only that Account's
state; committing the other distinct Account cannot invalidate it, and no callbacks/allocations
run on the successful publication path. The future Bank coordinator must enforce these assumptions.
There is no paired commit API or transfer operation yet, and transfer types/references are not
preimplemented here. Never treat a sequential public withdraw/deposit as atomic transfer.

## Freezing contract implemented in milestone 5

New accounts are active. Final `freeze()` checks active state and sets the private frozen flag;
repeat calls throw AccountFrozenException. Both current posting operations enter the same
private `requireActive()` guard during preparation and again on plan validation/commit. There is no unfreeze method,
setter or need for a State-pattern framework for this irreversible two-state lifecycle.

Freezing is a state change, not a financial posting: it consumes no transaction ID, adds no
ledger entry and changes no date or balance. It reads no clock and stores no freeze timestamp,
so `freeze()` has no date argument. History, identity/owner/opening date and rate/minimum getters
remain readable. `getAvailableBalance()` continues to report balance above the floor; posting
permission is separately controlled by frozen status. Bank lookup/list references reflect the
same frozen Account, while other accounts of the same customer remain independent.

Frozen attempts reject before any mutation, even when amount/date/funds are also invalid. This
is an explicit error-precedence decision, not a rule specified by the PDF. Existing active
account input/date/funds behavior is preserved. Future transfer/interest posting paths must
also enforce this rule when implemented; they are not preimplemented in this milestone.

## Transaction facts and revised ID ownership

Transaction validates a positive ID, account ID, type, unsigned money, result balance, explicit
date and OptionalLong transfer reference. OPENING amount equals its balance; OPENING/INTEREST
allow zero; money-movement types require positive values; credits cannot exceed result balance.
A positive transfer reference is required exactly for transfer types. A record alone cannot
validate account chronology or prove a transfer counterpart exists; posting coordinators must.

**Design change from milestone 2:** each Account allocates its own sequence, starting at 1 for
opening, then `history.size() + 1L`. No external allocator, global static state or caller-supplied
posting ID is needed. A ledger entry is identified by `(accountId, transactionId)`. The long
addition avoids int arithmetic overflow; history storage still has normal collection limits.
Later Bank transfer references will identify pairs across accounts independently of local IDs.
The PDF does not prescribe an ID strategy. This replaces the earlier proposed Bank-global
transaction sequence to support standalone accounts and deterministic tests with less coupling.

## Exception hierarchy

Abstract BankingException extends RuntimeException. InvalidBankingInputException covers missing,
nonpositive and imprecise inputs; DuplicateCustomerException and CustomerNotFoundException
cover customer registry failures. DuplicateAccountException and AccountNotFoundException now
cover duplicate opening IDs and absent account lookups. New InsufficientFundsException reports a withdrawal that breaches
the type-specific floor, and InvalidPostingDateException reports chronology violations. A null
date is invalid input, not a chronological value for active accounts. StalePostingException
rejects reused/superseded plans before they can overwrite a newer ledger. New AccountFrozenException
rejects repeated freezing and postings on frozen accounts before other input/funds/date failures. Every concrete subtype is thrown by tests.
Unchecked domain failures fit the in-memory API; no checked I/O is hidden or printed.

## SOLID mapping

- SRP, implemented: Bank coordinates customer/account registration, Customer owns identity, Account enforces posting
  invariants, Transaction validates ledger facts, BankingInput handles common input rules.
- OCP, partly implemented: the inherited posting template uses the account-specific balance-floor
  hook. Final checking/savings types require no duplicated algorithms. InterestPolicy extension
  is planned and must not be described as already implemented.
- LSP, implemented for supplied subtypes: both obey a base contract expressed in terms of available
  balance. Shared success/failure tests verify it through Account references.
- ISP: current APIs avoid UI/persistence duties. The narrow InterestPolicy interface is planned.
- DIP: planned Account-to-InterestPolicy dependency injection remains unimplemented. Current
  BankingInput is a helper, not an invented abstraction used to claim DIP.

## Remaining design

Bank now registers accounts against known owners and enforces unique account IDs. It still
must coordinate transfers. A future transfer must validate and prepare both legs before mutation; do not compose
public withdraw and deposit calls, since the second can reject after the first changes state.
The prepared boundary now exists for deposit/withdraw facts. Transfer coordination must add
validated transfer facts/references and both-side orchestration while preserving its guards.

Frozen accounts now reject deposits/withdrawals. Future transfer/interest paths must also
respect frozen status. InterestPolicy/FixedRateInterestPolicy will supply the
Strategy pattern. Monthly interest must receive dates, validate month-end/chronology, round
HALF_EVEN once, and reject a duplicate YearMonth independently of last posting date. Rate
metadata already differs, but interest is not calculated or posted yet. No database, UI,
concurrency or framework is justified. The final UML/report must follow completed code.

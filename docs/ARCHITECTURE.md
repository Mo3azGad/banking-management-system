# Architecture — current milestone 7 and final target

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
Subclasses cannot override the final public or package-private preparation entry points. Regression tests preserve active/frozen posting contracts; boundary tests exercise the split.
Immutable state publication replaces the earlier separate history append and balance assignment.
History retrieval returns its immutable list directly; old snapshots remain stable.

Preparation now costs O(n) copying per ledger, and retained plans retain their histories. The
small in-memory scope favors predictable publication over throughput optimization. This is not a
concurrency mechanism: ledger validation/assignment is not synchronized and frozen state is separate.
No database rollback, persistence or distributed two-phase commit pattern is implemented.

For future two-account coordination, first prepare and validate every participating plan. Accounts
must be distinct, with no intervening mutations. Each commit then rechecks only that Account's
state; committing the other distinct Account cannot invalidate it, and no callbacks/allocations
run on the successful publication path. Bank.transfer now enforces these assumptions using both internal transfer preparers.
It prevalidates both plans before either guarded commit and allocates the receipt first. Never treat a sequential public withdraw/deposit as atomic transfer.

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
account input/date/funds behavior is preserved. Transfer preparers now enforce the same per-account rule; future interest posting must also
respect it when implemented.

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
Bank-local transfer references now identify pairs across accounts independently of local IDs.
The PDF does not prescribe an ID strategy. This replaces the earlier proposed Bank-global
transaction sequence to support standalone accounts and deterministic tests with less coupling.

## Atomic transfer coordination implemented in milestone 7

Bank.transfer(sourceId, targetId, amount, date) resolves canonical accounts from this Bank and
rejects the same instance (including whitespace-normalized duplicate IDs). Accounts with different
owners or concrete types may transfer. Separate Banks have independent ID/reference namespaces.
Bank then generates a candidate reference, prepares TRANSFER_OUT followed by TRANSFER_IN using
Account's shared template, allocates an immutable [outgoing, incoming] receipt, validates both
plans, commits both, and only then records the reference as used.

Account's package-private transfer preparers carry OptionalLong.of(reference). The shared template
adds for DEPOSIT/TRANSFER_IN and subtracts for WITHDRAWAL/TRANSFER_OUT, preserving type-specific
floors, explicit dates, exact-cent validation and frozen/stale guards. There are no public
single-leg transfer mutators. Transaction independently validates the positive transfer link.

All allocation, arithmetic, subclass hooks and receipt creation occur before final prevalidation.
Successful guarded commits check only the owning account's active/expected-ledger state and
publish a reference. For distinct registered accounts, with no callbacks/interleaving, committing
one cannot invalidate the other. No rollback path is needed for ordinary rejected domain operations
because neither is published until all preconditions pass. This is not concurrent/database atomicity.

Both balances/histories, local sequence IDs/latest dates and Bank reference count are preserved
on rejection. References start at 1 per Bank and advance only after success, independently of
local ledger sequence numbers. The signed long candidate is validated as a positive reference
by Transaction before any publication; no invalid reference is accepted into a ledger. References
are not globally unique or persistent. Receipt/history values are immutable snapshots.

Lookup/distinctness precedes preparation. Outgoing validation precedes incoming validation. Each
leg's frozen check wins over its other invalid inputs, but there is no global ordering of errors
spread across two accounts. A valid source plan followed by a recipient date/frozen failure is
still uncommitted, so a rejected transfer cannot partially debit the sender.

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

Bank registration and transfer coordination are now implemented. The transfer code deliberately
does not compose public withdraw/deposit, which could leave a partial result on recipient failure.
Interest policy calculation and monthly posting remain the next functionality.

Frozen accounts now reject deposits, withdrawals and incoming/outgoing transfers. Future
interest posting must also respect frozen status. InterestPolicy/FixedRateInterestPolicy will supply the
Strategy pattern. Monthly interest must receive dates, validate month-end/chronology, round
HALF_EVEN once, and reject a duplicate YearMonth independently of last posting date. Rate
metadata already differs, but interest is not calculated or posted yet. No database, UI,
concurrency or framework is justified. The final UML/report must follow completed code.

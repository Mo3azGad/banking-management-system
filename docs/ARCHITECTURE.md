# Architecture — implemented model and milestone 11 review

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
CheckingAccount permits a zero floor; SavingsAccount enforces 100.00. Both expose default product monthly
rate metadata and compose default FixedRateInterestPolicy instances. No overridable method is called from the base constructor.

Account receives a nonnull InterestPolicy through its protected constructor and owns a private frozen flag and one reference to a private immutable LedgerState record
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

For two-account coordination, first prepare and validate every participating plan. Accounts
must be distinct, with no intervening mutations. Each commit then rechecks only that Account's
state; committing the other distinct Account cannot invalidate it, and no callbacks/allocations
run on the successful publication path. Bank.transfer now enforces these assumptions using both internal transfer preparers.
It prevalidates both plans before either guarded commit and allocates the receipt first. Never treat a sequential public withdraw/deposit as atomic transfer.

## Freezing contract implemented in milestone 5

New accounts are active. Final `freeze()` checks active state and sets the private frozen flag;
repeat calls throw AccountFrozenException. Deposits, withdrawals and transfer preparations enter the same
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
account input/date/funds behavior is preserved. Transfer preparers and monthly interest posting
enforce the same per-account rule.

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

## Interest calculation components implemented in milestone 8

InterestPolicy is a public @FunctionalInterface with only calculate(BigDecimal balance).
FixedRateInterestPolicy is final, stores an immutable monthlyRate, and implements that contract.
Its constructor rejects null/negative rates through the existing InvalidBankingInputException.
Zero, high-precision and rates above 1 are allowed; no professor-specified cap exists. The rate
is a monthly decimal multiplier, not an annual percentage. There is no rate setter/getter API.

calculate validates a nonnegative exact-cent balance using BankingInput, multiplies exactly,
and calls setScale(2, HALF_EVEN) once on the final product. Zero results are valid. It never
rounds the rate first, annualizes, prorates, compounds or reads a clock. BigDecimal arithmetic
scale overflow is translated into a clear custom input exception. Arbitrary numeric magnitude
is supported within Java's decimal representation/resource limits; no unlimited-resource claim.

The pure contract/algorithm remain usable independently for read-only quotes, even from frozen
balances. Account now receives/stores InterestPolicy and invokes it for monthly posting. Standard
four-argument checking/savings constructors wire their default rates; explicit five-argument
overloads inject an alternative algorithm. The base Account never constructs FixedRateInterestPolicy.
getMonthlyInterestRate is retained as default product metadata, not a custom-policy rate query.

## Monthly posting implemented in milestone 9

Final applyMonthlyInterest(LocalDate) performs: active guard; nonnull date; month-end check using
YearMonth; chronology check; search for an existing INTEREST record in that YearMonth; one policy
calculation from the pre-posting balance; nonnegative exact-cent result validation; prepared-entry
creation and guarded commitment. Both normal posting and interest now share date validation and
entry/immutable-ledger preparation. Interest may be zero without relaxing positive ordinary posting.

The ledger is the month marker. Its INTEREST entry and balance publish together; no separate
mutable set/cache can get ahead of a failed operation. Duplicate checking is independent of the
latest transaction type. Chronology is checked before duplicates, so an older date after a newer
posting fails as a backdate rather than using the duplicate-month exception. Current-month repeats
use InterestAlreadyAppliedException. Domain policy exceptions propagate unchanged; unexpected
RuntimeExceptions are wrapped in InterestCalculationException with their cause retained via a
new protected BankingException(message, cause) constructor. Invalid results use the existing
InvalidBankingInputException. No policy is called before eligibility validation succeeds.

First-month application is allowed without proration, including opening at month-end. Missing
months do not generate catch-up entries and past dates cannot be inserted later. The calculation
uses the balance at invocation; same-day ordinary postings after application remain legal, without
recalculation. Zero result still creates a new ledger version and prevents repeat application.
Injected policies explicitly override the default algorithm; product rate metadata stays unchanged.

Pure-policy contract: no Account mutation or clock reads. State-preservation claims concern
contract-conforming calculations, invalid outputs and exceptions without external side effects.
Impure callbacks are not rolled back; frozen/stale validation prevents publication over changed
state. This remains single-threaded in-memory code, not concurrency or database rollback.

## Exception hierarchy

Abstract BankingException extends RuntimeException. InvalidBankingInputException covers missing,
nonpositive and imprecise inputs; DuplicateCustomerException and CustomerNotFoundException
cover customer registry failures. DuplicateAccountException and AccountNotFoundException now
cover duplicate opening IDs and absent account lookups. New InsufficientFundsException reports a withdrawal that breaches
the type-specific floor, and InvalidPostingDateException reports chronology violations. A null
date is invalid input, not a chronological value for active accounts. StalePostingException
rejects reused/superseded plans before they can overwrite a newer ledger. New AccountFrozenException
rejects repeated freezing and postings on frozen accounts before other input/funds/date failures. Every concrete subtype is thrown by tests.
FixedRateInterestPolicy reuses InvalidBankingInputException for invalid rate/balance and
unsupported decimal precision; no new exception subtype is needed. InterestAlreadyAppliedException reports repeated current-month application. InterestCalculationException
wraps unexpected policy failures with their cause. Unchecked domain failures fit the in-memory API; no checked I/O is hidden or printed.

## SOLID mapping

- SRP, implemented: Bank coordinates registration/transfers, Customer owns identity, Account enforces posting
  invariants, Transaction validates facts, BankingInput handles shared input, and FixedRateInterestPolicy
  isolates pure interest arithmetic.
- OCP: the inherited posting template uses the account-specific balance-floor
  hook. Final checking/savings types require no duplicated algorithms. The injected InterestPolicy can change calculation behavior without changing the Account
  eligibility/publication algorithm; custom-policy tests exercise that extension point.
- LSP, implemented for supplied subtypes: both obey a base contract expressed in terms of available
  balance. Shared success/failure tests verify it through Account references.
- ISP: current APIs avoid UI/persistence duties. InterestPolicy now exposes only calculation, with no account lifecycle, dates, UI or persistence methods.
- DIP: Account stores/uses the injected InterestPolicy interface. Concrete checking/savings
  constructors compose the default fixed policy; Account has no concrete algorithm dependency.
  BankingInput remains a helper rather than a fabricated DIP abstraction.

## Remaining review

The 15 November boundary review found behavioral test gaps, with no production correction needed.
Posted rounding, skipped-month amounts and policy rejection after prior success now have explicit
tests; see INTEREST_BOUNDARY_REVIEW.md. The production model and milestone 9 UML remain unchanged.

The 16 November integration review verifies ledger reconciliation across two months, both orders
of transfer/interest, interest-driven transfer date bounds/floors and freeze isolation after prior
activity. Its 21 cases use public operations only. No production correction was needed; see
INTEGRATION_REVIEW.md for exact financial examples and design conclusions.

The PDF's listed banking operations are now implemented. Further sessions must review actual
rounding/eligibility boundaries, cross-operation invariants, subtype contracts and documentation,
fix genuine defects and complete the final report/defense. Existing constructor/getter compatibility
and the distinction between default product rates/custom policies must remain clear. No database,
UI or concurrency framework is required. The final UML/report must continue to reflect real code.

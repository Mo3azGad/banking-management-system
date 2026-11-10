# Architecture — current milestone 5 and final target

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

Account owns a private frozen flag, mutable private balance and an append-only private ArrayList of immutable
Transaction records. External callers receive List.copyOf snapshots. There is no public balance
setter, mutable collection view or transaction-posting helper. Direct constructors do not register
accounts at Bank; the Bank opening methods perform that registration for known owners.

## Posting contracts and Template Method

`deposit(amount, date)` and `withdraw(amount, date)` are final public operations delegating to
one private posting template:

0. Require an active account; AccountFrozenException takes precedence over invalid posting inputs.
1. Validate a nonnull, strictly positive exact-cent amount and nonnull LocalDate.
2. Reject a date before the latest ledger entry (the opening entry establishes the initial bound).
3. Calculate the resulting balance using BigDecimal, consulting the polymorphic minimum floor.
4. Reject a withdrawal below that floor with InsufficientFundsException.
5. Construct the complete immutable Transaction before touching account state.
6. Append the prepared entry, then assign the new balance; no domain validation occurs afterwards.

The template fixes the algorithm while `getMinimumBalance()` varies by concrete account.
This is the implemented Template Method pattern; it avoids duplicate validation and recording
in checking/savings subclasses. Final public methods prevent those subclasses bypassing it.
Savings is substitutable under the documented Account contract: withdrawals are limited by
each type's available balance, not by a promise that the entire raw balance may be withdrawn.

Under the documented single-threaded in-memory model, rejected domain operations preserve
balance, history, next sequence ID and latest posting date. This is not a database transaction
or concurrency guarantee. Dates can equal the opening/latest date; no implicit current-date
comparison or future-date rejection is introduced. Immutable history is in posting order,
which is chronological with sequence order breaking same-day ties.

## Freezing contract implemented in milestone 5

New accounts are active. Final `freeze()` checks active state and sets the private frozen flag;
repeat calls throw AccountFrozenException. Both current posting operations enter the same
private `requireActive()` guard before amount/date/funds validation. There is no unfreeze method,
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
date is invalid input, not a chronological value for active accounts. New AccountFrozenException
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
Transfer coordination will need a prepared posting/commit boundary preserving both accounts.

Frozen accounts now reject deposits/withdrawals. Future transfer/interest paths must also
respect frozen status. InterestPolicy/FixedRateInterestPolicy will supply the
Strategy pattern. Monthly interest must receive dates, validate month-end/chronology, round
HALF_EVEN once, and reject a duplicate YearMonth independently of last posting date. Rate
metadata already differs, but interest is not calculated or posted yet. No database, UI,
concurrency or framework is justified. The final UML/report must follow completed code.

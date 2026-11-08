# Architecture — current milestone 3 and final target

## Implemented responsibilities

Bank manages customer registration/lookup in a private map. Customer is immutable. BankingInput
is package-private and shares text, required-value and exact-cent validation across entities.
Account validates identity, owner, opening balance/date and creates an OPENING Transaction.
CheckingAccount permits a zero floor; SavingsAccount enforces 100.00. Both expose monthly
rate metadata. No overridable method is called from the base constructor.

Account now owns mutable private balance and an append-only private ArrayList of immutable
Transaction records. External callers receive List.copyOf snapshots. There is no public balance
setter, mutable collection view or transaction-posting helper. Constructors do not register
accounts at Bank. Customer/account registration remains separate from a standalone model.

## Posting contracts and Template Method

`deposit(amount, date)` and `withdraw(amount, date)` are final public operations delegating to
one private posting template:

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
cover current registry failures. New InsufficientFundsException reports a withdrawal that breaches
the type-specific floor, and InvalidPostingDateException reports chronology violations. A null
date is invalid input, not a chronological value. Every concrete subtype is thrown by tests.
Unchecked domain failures fit the in-memory API; no checked I/O is hidden or printed.

## SOLID mapping

- SRP, implemented: Bank coordinates customers, Customer owns identity, Account enforces posting
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

Bank must register accounts against known owners, enforce unique account IDs and coordinate
transfers. A future transfer must validate and prepare both legs before mutation; do not compose
public withdraw and deposit calls, since the second can reject after the first changes state.
Transfer coordination will need a prepared posting/commit boundary preserving both accounts.

Frozen accounts will reject postings; InterestPolicy/FixedRateInterestPolicy will supply the
Strategy pattern. Monthly interest must receive dates, validate month-end/chronology, round
HALF_EVEN once, and reject a duplicate YearMonth independently of last posting date. Rate
metadata already differs, but interest is not calculated or posted yet. No database, UI,
concurrency or framework is justified. The final UML/report must follow completed code.

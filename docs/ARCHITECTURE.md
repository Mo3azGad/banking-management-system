# Architecture: current implementation and proposed final design

## Implemented now

`Bank` owns a private insertion-ordered customer map. `Customer` is immutable and validates
its own identity. Three final exceptions extend abstract unchecked `BankingException`.
The customer snapshot is a defensive immutable copy, not a mutable view. No repository
interface or dependency injection framework is justified for this small in-memory milestone.

## Planned final classes

| Type | Responsibility |
|---|---|
| Bank | Customer/account registries, account opening, transaction IDs, transfer coordination |
| Customer | Immutable ID and name |
| abstract Account | Owner, balance, freeze status, dates, history; shared operation validation |
| CheckingAccount | Zero balance floor and default checking interest policy |
| SavingsAccount | 100.00 balance floor and default savings interest policy |
| Transaction | Immutable sequence ID, type, amount, balance-after, date, optional transfer reference |
| TransactionType | OPENING, DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT, INTEREST |
| InterestPolicy | Narrow interface: calculate monthly interest from a balance |
| FixedRateInterestPolicy | Immutable rate, BigDecimal calculation, explicit rounding |
| BankingException subtypes | Invalid input, missing/duplicate identities, insufficient funds, frozen account, invalid date/state |

Use one domain package initially; split packages only as the code size requires it. No
artificial presentation layer or database layer. Public account mutations will follow one
shared validation path. Bank exposes transfer coordination; package-private account posting
helpers cannot be used by outside callers to bypass invariants. Bank creates/accounts registers
only valid concrete accounts; account opening methods validate registered ownership.

## Patterns planned (not yet claimed as implemented)

**Template Method:** final deposit/withdraw/interest workflows in Account enforce common
validation and transaction recording; protected balance-floor hooks vary by account type.
This avoids duplicated business checks and prevents subclasses skipping them.

**Strategy:** Account depends on InterestPolicy, supplied on construction. FixedRateInterestPolicy
implements the default rates. A test policy can verify delegation without a system clock or
external service. Policy results must be validated before changing state. Use only this small
interface; do not add a registry of strategies or a framework.

Bank is a facade over banking operations, but we do not count every ordinary class as a pattern.
A factory is unnecessary with only two clearly named Bank opening methods.

## SOLID defense mapping (planned unless stated otherwise)

- SRP: Customer handles identity now. Later, Bank coordinates, Account enforces invariants,
  Transaction holds history facts, and InterestPolicy handles interest calculation.
- OCP: a new InterestPolicy can be added without rewriting Account's posting workflow.
- LSP: each account respects the documented base contract: an account-specific available amount
  limits withdrawals. The base contract does not promise unlimited withdrawal or zero minimum.
- ISP: InterestPolicy contains only interest calculation; Customer is not forced to expose
  account, printing, persistence, or UI methods.
- DIP: Account accepts the InterestPolicy abstraction rather than constructing a fixed-rate
  implementation internally. Bank wires the chosen default policies at the composition boundary.

## Failure contracts and testability

Unchecked domain exceptions represent operations the caller cannot complete; no checked I/O
failures exist in the in-memory model. Catch BankingException only where a caller can handle
it; never swallow it. Getter and defensive-list behavior have no domain failure condition;
do not invent invalid arguments for methods without parameters.

Date-dependent methods receive dates. Interest tracks processed YearMonth values independently
of the last posting date. Transfer preparation validates balances, dates, frozen flags and
transaction contents for both accounts before a commit step that cannot fail under the
single-threaded assumptions. Do not implement transfer as withdraw followed by public deposit:
the second operation might fail after the first changed state.

No broad claim of full SOLID compliance, banking completeness, or final coverage is made for
this first milestone. The final diagram and report must be regenerated after implementation.

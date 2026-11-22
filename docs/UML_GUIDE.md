# Final implemented design — 20 November 2026

These diagrams describe the existing Java implementation in `src/main/java/edu/banking`.
They introduce no proposed classes or public operations. The final five-section technical report
was assembled and its rendered pagination/figures reviewed on 21 November; see final-report.docx
and REPORT_BUILD.md. The UML model itself remains the verified 20 November design.

## Three complementary UML views

1. [Domain view](current-uml.svg), editable [DOT source](current-uml.dot): Bank, Customer,
   Account, the two products, Transaction/TransactionType and the interest strategy.
2. [Posting internals](uml/posting-internals.svg), [DOT source](uml/posting-internals.dot):
   the same Account/Bank/Transaction plus private LedgerState, package-private PreparedPosting
   and BankingInput. Public members already in the domain view are not repeated here.
3. [Exception hierarchy](uml/exceptions.svg), [DOT source](uml/exceptions.dot): the abstract
   unchecked base and all eleven concrete custom types, with external JDK RuntimeException.

Together the views contain all **24 source-declared banking types**: 21 public top-level types,
one package-private top-level helper and two nested types. Repeated context boxes denote the
same class, not additional entities. Compiler-synthetic helper classes are not domain classifiers.

Notation: `+` public, `-` private, `#` protected, `~` package-private. A hollow triangle on a solid
line points to a superclass; a dashed hollow triangle is interface realization. A solid open
arrow denotes a navigable association; a dashed open arrow is usage/dependency, not ownership.
Multiplicity is printed at the endpoint it counts. No composition/lifetime guarantee is implied
by registry/history associations. Italic operations are abstract; Account's two product hooks are
abstract even though concrete types supply constant results.

These are selective class views, not exhaustive Java listings. `...` elides parameters; some
return types and basic identity accessors are omitted for legibility. The exact signatures below
and public inventory in API_EXCEPTION_AUDIT.md complete that information. Java `final` modifiers
are omitted from most boxes: Bank, Customer, both products, FixedRateInterestPolicy and all
concrete exception types are final; record components/private plan fields are read-only. Product
MINIMUM_BALANCE and MONTHLY_RATE are private static final BigDecimals, not per-account mutable
attributes. The six enumeration literals and seven Transaction components are all shown.

### Relationships and cardinalities

| Association / usage | Actual implementation and interpretation |
|---|---|
| Bank → Customer / Account | Private maps hold 0..* registered instances. A particular instance is registered at 0..1 Bank: direct construction may be standalone, and Bank creates fresh canonical instances rather than importing existing objects. This is membership, not exclusive object lifetime. |
| Account → Customer | Exactly one owner per Account; a Customer may own 0..* accounts. Bank-opened accounts use that Bank's canonical registered owner. |
| Account → InterestPolicy | Exactly one nonnull injected policy; a pure policy instance may be shared by 0..* accounts. Default constructors compose fixed policies; explicit constructors accept overrides. |
| Account ⇢ Transaction | Creates/returns immutable facts. This dashed usage is not a fabricated direct history field: the actual list is in LedgerState. |
| Transaction → TransactionType | Exactly one validated enum value per fact. |
| Account → LedgerState | Exactly one current ledger reference. The private nested record is implicitly static and does not capture its Account. Account has no direct balance field. |
| PreparedPosting → Account | Exactly one captured enclosing object; an Account may have 0..* plans. PreparedPosting is a non-static inner class. |
| PreparedPosting → LedgerState | Two separate required roles, expected and next, each exactly one. The fields/role arrows are shown separately; omitted edge multiplicities are supplied here. |
| PreparedPosting → Transaction | Exactly one immutable entry preview, also contained in the prepared next history. A preview alone does not prove publication. |
| LedgerState → Transaction | Immutable list of 1..* facts, starting with OPENING. Facts can be shared by successive immutable ledger versions and external history snapshots. |
| Bank ⇢ PreparedPosting | Coordinates the two plans of a transfer. There is no public single-leg transfer mutator. |
| Account ⇢ BankingInput | Uses shared validation. Bank, Customer, Transaction and FixedRateInterestPolicy also use that helper; every validation usage arrow is not repeated in the overview. |

### Signature details elided in the figures

All amounts/balances/rates are BigDecimal; all operation/opening dates are LocalDate. Identity/name
parameters are String. Both concrete products expose these public overloads (substitute the
actual class name; there is no generic product factory class):

```java
CheckingAccount(String id, Customer owner, BigDecimal openingBalance, LocalDate openedOn)
CheckingAccount(String id, Customer owner, BigDecimal openingBalance,
                LocalDate openedOn, InterestPolicy interestPolicy)
SavingsAccount(String id, Customer owner, BigDecimal openingBalance, LocalDate openedOn)
SavingsAccount(String id, Customer owner, BigDecimal openingBalance,
               LocalDate openedOn, InterestPolicy interestPolicy)
```

Account's protected constructor takes `(String id, Customer owner, BigDecimal openingBalance,
LocalDate openedOn, InterestPolicy interestPolicy)`. Customer's public constructor takes
`(String id, String name)`. FixedRateInterestPolicy takes `(BigDecimal monthlyRate)`.
Transaction's canonical constructor takes, in order, `(long id, String accountId,
TransactionType type, BigDecimal amount, BigDecimal balanceAfter, LocalDate date,
OptionalLong transferReference)`; component accessors use those component names.

| Public operation | Exact parameter types (in displayed order) | Return type |
|---|---|---|
| Bank.registerCustomer | String id, String name | Customer |
| Bank.findCustomer | String id | Customer |
| Bank.getCustomers | none | List<Customer> |
| Bank.openCheckingAccount | String accountId, String customerId, BigDecimal openingBalance, LocalDate openedOn | CheckingAccount |
| Bank.openSavingsAccount | Same types/order as checking opening | SavingsAccount |
| Bank.findAccount | String accountId | Account |
| Bank.getAccounts | none | List<Account> |
| Bank.transfer | String sourceId, String targetId, BigDecimal amount, LocalDate date | List<Transaction> (outgoing, incoming) |
| Account.deposit / withdraw | BigDecimal amount, LocalDate date | Transaction |
| Account.applyMonthlyInterest | LocalDate date | Transaction |
| Account.freeze / isFrozen | none | void / boolean respectively |
| Account.getAvailableBalance / getMinimumBalance / getMonthlyInterestRate | none | BigDecimal |
| Account.getTransactionHistory | none | List<Transaction> |
| InterestPolicy.calculate / FixedRateInterestPolicy.calculate | BigDecimal balance | BigDecimal |

Account's package-private preparers take `(BigDecimal amount, LocalDate date)`, with an additional
`long reference` for prepareTransferOut/In; they return PreparedPosting. Private preparePosting
takes `(TransactionType type, BigDecimal amount, LocalDate date, OptionalLong transferReference)`.
Private validatePostingDate takes `(LedgerState expected, LocalDate date)` and returns void.
Private prepareEntry takes `(LedgerState expected, TransactionType type, BigDecimal amount,
BigDecimal nextBalance, LocalDate date, OptionalLong transferReference)` and returns PreparedPosting.
Private requireActive returns void. The private plan constructor takes `(LedgerState expected,
LedgerState next, Transaction entry)`; Java's implicit enclosing-instance parameter is not a
source-level argument. BankingInput.text/required/nonNegativeMoney take `(value, String field)`;
their value/return types are String/String, generic T/T, and BigDecimal/BigDecimal respectively.

Bank also has its implicit public no-argument constructor and private
availableAccountId(String accountId): String helper. Account's omitted identity accessors are
getId(): String, getOwner(): Customer and getOpenedOn(): LocalDate. Default-construction usage
from concrete products to FixedRateInterestPolicy is explained here rather than drawn across
the inheritance lines; the diagrams do not attempt to show every implementation dependency.

## Class responsibilities and implemented rules

| Type | Responsibility |
|---|---|
| Bank | Customer/account registries, normalized unique IDs, canonical ownership, lookup/snapshots and two-account transfer coordination. Membership snapshots contain actual mutable Accounts. |
| Customer | Immutable validated ID/name; names may repeat, IDs are case-sensitive. |
| Account | Private ledger/lifecycle, shared dated posting validation, floor hook, immutable history, injected interest eligibility/publication. Balance is obtained from ledger. |
| CheckingAccount | Opening/debit floor 0.00, no overdraft; default monthly multiplier 0.001. |
| SavingsAccount | Establishes/preserves floor 100.00; default monthly multiplier 0.003. |
| Transaction | Immutable financial fact and validation of local ID, amounts/result/date/type/reference. Construction alone does not execute a posting. |
| TransactionType | OPENING, DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT and INTEREST. All operations now exist. |
| InterestPolicy | Pure calculate(balance) contract; no lifecycle, month eligibility or clock. |
| FixedRateInterestPolicy | Immutable nonnegative monthly decimal multiplier, exact multiplication and final HALF_EVEN cents rounding. |
| Account.LedgerState | Private immutable balance/history pair published through one reference assignment; no separate mutable month set. |
| Account.PreparedPosting | Account-bound expected/next state and entry, non-mutating validation, guarded publication and stale/frozen rejection. |
| BankingInput | Package-private normalization/required/exact-cent validation; static helper methods and private constructor. |
| BankingException | Abstract RuntimeException base with protected message and message/cause constructors. |

Every concrete exception extends BankingException directly. Each has a public `(String message)`
constructor except InterestCalculationException, which takes `(String message, Throwable cause)`.

| Concrete exception | Operation failure described |
|---|---|
| InvalidBankingInputException | Missing/invalid identity, value, exact money or policy result |
| DuplicateCustomerException / CustomerNotFoundException | Customer registration/lookup |
| DuplicateAccountException / AccountNotFoundException | Bank account opening/lookup |
| InsufficientFundsException | Withdrawal/outgoing transfer would breach the floor |
| InvalidPostingDateException | Backdating or interest on a non-month-end date |
| AccountFrozenException | Repeated freeze or forbidden posting |
| StalePostingException | Reused/superseded internal plan |
| InterestAlreadyAppliedException | Existing current eligible YearMonth marker |
| InterestCalculationException | Unexpected policy runtime failure, with original cause retained |

Numeric floors/rates, exact cents, no proration/catch-up and zero-interest markers are documented
project decisions, not numeric rules prescribed by the PDF. Transaction IDs identify facts by
(accountId, transactionId) within a Bank's account namespace; paired transfer references are
separate Bank-local values. getMonthlyInterestRate reports the default product multiplier, not
introspection of a custom injected algorithm. Explicit dates are never compared to a system clock.

## Patterns and five SOLID principles

**Template Method** solves duplicated posting validation/publication: final public/internal entry
points use Account.preparePosting with the polymorphic getMinimumBalance hook. Subtypes vary
the rule while sharing money/date/frozen checks and immutable preparation. Interest has distinct
month/zero rules and reuses only appropriate helpers. PreparedPosting is not distributed two-phase
commit, and named Bank creation methods are not the GoF Factory Method pattern.

**Strategy** separates arithmetic from posting eligibility: Account receives/calls InterestPolicy,
and product constructors compose FixedRateInterestPolicy defaults or accept overrides. The pure
algorithm multiplies/rounds; Account checks dates/months/frozen status and publishes the ledger.
No State-pattern framework, persistence layer or second artificial algorithm is claimed.

| SOLID | Concrete evidence and limit |
|---|---|
| SRP | Bank coordinates registries/transfers; Account owns ledger/lifecycle/eligibility; policy owns arithmetic; Transaction validates facts. Each is a coherent reason to change. |
| OCP | The minimum hook and injected policy vary rules without replacing the posting workflow. A new Bank-supported product still requires intentional opening API work. |
| LSP | Both supplied subtypes satisfy Account's availability/date/rejection contract. At balance 250, withdrawing availability leaves checking 0 and savings 100; raw balance is not promised wholly spendable. Future subtype hooks must be stable/pure. |
| ISP | InterestPolicy has only calculate(balance), so an arithmetic implementation need not implement registration, freeze, dates or storage. |
| DIP | Account's field/constructor/call depend on InterestPolicy; concrete product constructors are the default composition boundary. Helpers/maps are not fabricated inversion abstractions. |

Named executable evidence is in DESIGN_REVIEW.md, API_EXCEPTION_AUDIT.md and TEST_MATRIX.md.
Current counts/limits are in VERIFICATION.md. Single-threaded execution, pure policies and full
preparation before publication bound the financial guarantees; no concurrency/durability or
rollback of arbitrary callback side effects is promised by the diagrams.

## Regeneration and provenance

With Graphviz installed, run from the repository root:

```sh
dot -Tsvg docs/current-uml.dot -o docs/current-uml.svg
dot -Tsvg docs/uml/posting-internals.dot -o docs/uml/posting-internals.svg
dot -Tsvg docs/uml/exceptions.dot -o docs/uml/exceptions.svg
```

SVGs are the zoomable exports; DOT files are the editable semantic sources. PNGs embedded in
the final report are rendered from the same sources. Rendering/disassembly do not execute
domain operations; JUnit remains their only execution entry point.

The obsolete [proposal](history/proposed-uml-before-final-review.mmd) is retained byte-for-byte
as history, not final design. It had a direct Account balance, a nonexistent minimumBalance hook,
and overly strong composition/registration multiplicities. The final views show actual LedgerState,
getMinimumBalance, optional Bank membership, immutable nonempty history and injected strategy.
Earlier complete diagrams remain available in Git history. No Java API/behavior change or new
test was needed for that documentation milestone. Final report assembly is complete as of
21 November. Defense preparation is completed on 22 November in DEFENSE.md, with a verified demo
and independent exercise; personal rehearsal/attendance and the independent review remain. Current
verification records the genuine session timing without implying professor acceptance.

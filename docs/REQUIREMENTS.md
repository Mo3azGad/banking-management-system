# Requirements analysis

Source: the entire supplied Advanced OOP Final Project_EN.pdf (cover plus pages numbered 1–4).
The PDF is authoritative; its assignment table contains multiple Banking (2) students. This
project uses the user's explicitly stated assignment; it does not infer identity from the table.

## Mandatory PDF requirements

| Source | Requirement | Milestone 8 status |
|---|---|---|
| Cover | Individual Java classes and JUnit tests; no running application, main, menu, UI, Scanner or System.out | Foundation follows this restriction |
| Cover | Meaningful Git/GitHub commits spread over at least two weeks | First commit pushed to verified private GitHub repository; two-week history still outstanding |
| Cover | Own base and specialized exceptions; clear messages; no null/error printing on failure | Customer/account registry, input, frozen-account, stale-plan, insufficient-funds and posting-date exceptions implemented and tested |
| Cover | Every nontrivial public method tested for success and failure; every exception thrown in a test | Covered for implemented operations; see test matrix |
| Cover | Learned patterns and SOLID | Template Method, polymorphic rules and pure InterestPolicy components implemented; account policy integration remains |
| Cover | Dates passed as parameters; never read system clock | Model and posting dates supplied explicitly; chronology validated without reading clock |
| Banking (2), p.1 | Bank, customer, checking/savings accounts, transaction | Bank customer/account registries, concrete models and transaction records implemented |
| Banking (2), p.1 | Open, deposit, withdraw, transfer, history, monthly interest, freeze | Bank opening/retrieval, deposits, withdrawals, history, transfers and freezing implemented; interest remains |
| Banking (2), p.1 | Different rules and monthly rates for checking and savings | Different floors/rate metadata and pure fixed-rate calculation implemented; monthly posting planned |
| Oral defense, p.1 | Individual five-minute defense with laptop, tests and code walkthrough; absence means failure | Interim preparation included; attend in weeks 11/12 |
| Final report, p.2 | DOC/DOCX: UML/class overview, patterns, exceptions/checked choice, all five SOLID, testing/limitations/redesign | Interim DOCX only; final follows completed code |
| Cover | Submit on UZEM by 07.12.2026 | Interpreted as 7 December 2026; PDF gives no time |

Rubric: exceptions 10; tests 15; patterns/SOLID 25; Git 5; report 15; oral defense 30.
Make-up exam note in the PDF: the same project is required, with grade multiplied by 0.8.
Defense dates in weeks 11/12 are separate from the submission deadline; confirm course dates.

## Additional user requirements

Maven, JUnit 5, BigDecimal, clean and restrained architecture, comprehensive boundaries and
failure tests, an accurate UML, DOCX report, defense notes, and genuine incremental history.
Do not delay testing to days 11/12; test each implementation step.

## Chosen rules, not specified by the professor

- One abstract currency with two decimal places; no exchange rates. Reject amounts with
  nonzero sub-cent precision instead of silently rounding user input.
- Checking: zero opening balance allowed; no overdraft; monthly rate 0.001 (0.1%).
- Savings: opening balance at least 100.00; withdrawals/transfers out must leave at least
  100.00; monthly rate 0.003 (0.3%). Equality at the floor is allowed.
- Deposits, withdrawals and transfers require strictly positive amounts.
- Opening date and every posting date are explicit LocalDate values. Dates must be at least
  opening date and not earlier than that account's latest posting; same-day postings allowed.
  No future-date rule is possible without an explicit reference date, so none is invented.
- Frozen accounts reject deposits, withdrawals, transfers in/out, and interest. History remains
  readable. Freezing is irreversible within this project's scope; repeat freezing is an error.
- Apply monthly interest to closing balance on the supplied month's final day, once per
  YearMonth. HALF_EVEN to two decimals. No pro-rating, catch-up interest or fees. First-month
  interest is permitted; skipped months are not automatically credited. Zero rounded interest
  is recorded and marks the month processed so it cannot be reapplied.
- Transfer must involve distinct accounts registered at the same bank, validates both sides
  before mutation, and records linked out/in entries. Failure must leave both accounts unchanged.
- Customer/account IDs are caller-supplied and unique in their respective registries. Transaction
  sequence IDs are deterministic and local to each Account; opening is 1. A ledger identity
  is (accountId, transactionId). This revises the earlier Bank-global proposal, not a PDF rule.
  Failed operations consume no sequence; Bank transfer references now correlate two legs.
- In-memory, single-threaded operation. No database, authentication, persistent audit, concurrency
  or production banking claims. Frozen status applies to account operations, not customer lookup.

These are explicit project decisions. Floors/rate metadata, deposits/withdrawals, opening/posting history and chronology are implemented. Bank ownership/ID validation is now implemented; freezing of current postings is implemented; transfers are now implemented; monthly interest remains future work.

## Bank opening and lookup decisions implemented on 9 November

Only registered customer IDs may own Bank-opened accounts. Customer objects are resolved from
the same Bank; registration at another Bank alone grants no ownership here. Both concrete types
share an account ID namespace. IDs are stripped and case-sensitive; customer IDs have a separate
namespace. Validation/construction completes before registry publication. Direct account model
construction remains usable but is not registration; no external-account import is required by
the PDF. Account list snapshots protect membership, not immutable copies of balance/state.

## Freezing decisions implemented on 10 November

`freeze()` is irreversible and repeat calls fail with AccountFrozenException. Current deposits
and withdrawals check frozen state before validating amount/date/funds; history and all read
access remain available. Freezing itself makes no financial entry, consumes no ID and stores no
timestamp, so no date parameter or clock read is required. Available-balance metadata remains
balance minus floor, not permission to post. Transfers now respect that frozen rule; future interest operations must also enforce it. The PDF requires freezing but leaves these details open.

## Posting preparation decisions implemented on 11 November

Existing public deposits/withdrawals use an internal prepared-plan boundary: validation and full
ledger allocation precede publication. Plans do not reserve IDs, advance dates or alter balances.
They are bound to the originating object/state, reject repeated/stale commits and respect frozen
status at preparation and commitment. No public prepare/commit API is exposed; Bank.transfer now coordinates the internal plans.
Private immutable ledger snapshots replace a mutable history/balance pair. These are architecture
choices supporting the PDF's banking rules, not additional professor-mandated features.

## Transfer decisions implemented on 12 November

Bank resolves distinct registered accounts and permits different customers and types. A successful
transfer posts exactly one outgoing/incoming entry with equal amount/date and a shared positive
Bank-local reference, returning an immutable receipt in that order. Both legs/receipt are prepared
and validated before publication. No rejected transfer changes either ledger or consumes local
posting IDs/dates or the Bank reference. References are independent of account sequence IDs;
separate Banks use independent counters. Atomicity assumes single-threaded in-memory operation.
No fees, external bank transfer, persistence or concurrency requirement is invented.

## Interest-calculation decisions implemented on 13 November

InterestPolicy exposes only calculate(balance). FixedRateInterestPolicy accepts a nonnegative
monthly decimal rate, validates nonnegative exact-cent balance input and rounds only the exact
product to cents with HALF_EVEN. Zero rates/results are allowed; rate precision is preserved,
and no upper cap, annualization, fees or proration is invented. Invalid/unsupported decimal
input uses InvalidBankingInputException. Calculation needs no date because it is pure math;
it neither posts history nor decides eligible months. Account policy injection and the actual
dated application of monthly interest remain future work.

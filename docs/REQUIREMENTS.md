# Requirements analysis

Source: the entire supplied Advanced OOP Final Project_EN.pdf (cover plus pages numbered 1–4).
The PDF is authoritative; its assignment table contains multiple Banking (2) students. This
project uses the user's explicitly stated assignment; it does not infer identity from the table.

## Mandatory PDF requirements

| Source | Requirement | Milestone 1 status |
|---|---|---|
| Cover | Individual Java classes and JUnit tests; no running application, main, menu, UI, Scanner or System.out | Foundation follows this restriction |
| Cover | Meaningful Git/GitHub commits spread over at least two weeks | Initial local commit only; future history cannot be manufactured |
| Cover | Own base and specialized exceptions; clear messages; no null/error printing on failure | Customer exceptions implemented and tested |
| Cover | Every nontrivial public method tested for success and failure; every exception thrown in a test | Covered for implemented operations; see test matrix |
| Cover | Learned patterns and SOLID | Final design proposed; not all principles implemented yet |
| Cover | Dates passed as parameters; never read system clock | No date-dependent operations yet |
| Banking (2), p.1 | Bank, customer, checking/savings accounts, transaction | Bank and customer implemented; rest planned |
| Banking (2), p.1 | Open, deposit, withdraw, transfer, history, monthly interest, freeze | Planned |
| Banking (2), p.1 | Different rules and monthly rates for checking and savings | Explicit proposed rules below |
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
  sequence IDs are deterministic, allocated by Bank; failed operations consume no sequence.
- In-memory, single-threaded operation. No database, authentication, persistent audit, concurrency
  or production banking claims. Frozen status applies to account operations, not customer lookup.

These choices can be revised before account implementation without contradicting the PDF.

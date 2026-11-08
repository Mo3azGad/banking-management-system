# Test evidence and obligations — milestone 3

| Public operation | Successful behavior | Failure / boundary behavior |
|---|---|---|
| Customer constructor | Valid identity; whitespace stripping | Null, empty and whitespace ID/name |
| Bank constructor | Empty registry | No input/domain failure condition |
| registerCustomer | Retrieval; same names allowed | Invalid identity/name; normalized duplicate preserves original |
| findCustomer | Registered instance; normalized lookup | Missing, invalid and case-mismatched ID |
| getCustomers | Immutable, ordered, isolated snapshots | External mutation rejected |
| Account constructors | Ownership, dates, floors, rates, exact money; opening history | Missing values, negative/sub-cent amounts, below savings floor |
| getAvailableBalance | Polymorphic floor before/after postings | Valid models have no failure inputs |
| Transaction constructor | All six types, normalized money, zero opening/interest, paired references | Invalid IDs/fields/money; opening mismatch; impossible credit balance; invalid transfer links |
| deposit | Balance and returned/stored ledger entry; one cent; trailing zeros; large amounts | Null/zero/negative/sub-cent amount; null, pre-opening and backdated dates |
| withdraw | Balance and returned/stored ledger entry; exact floor; replenishment | Invalid inputs/dates; one cent over available; at floor; insufficient funds |
| getTransactionHistory | Opening, posting order, running balances, IDs and immutable snapshots | Mutation rejected; previous snapshots unchanged after later postings |

Shared account-operation tests run for both concrete types through Account references.
Posting tests verify same-day/opening-day acceptance, leap dates, chronology after both deposits
and withdrawals, and a supplied far-future date without a system-clock rule. Failure checks
assert unchanged balance/history and no consumed sequence or advanced ledger date. Two
accounts have independent sequences/state. Large deposit/withdrawal round trips preserve cents.

Every concrete custom exception is thrown by an operation test. The abstract BankingException
base is exercised through its subclasses. Getter/derived values on valid state have no meaningful
failure input; do not invent failures. UnsupportedOperationException is the JDK immutable-list
contract, not a custom banking exception. Generated record accessors are checked through facts.

## Remaining test obligations

- Bank account registration: registered owner and unique ID; retrieve/open accounts.
- Transfers: paired entries/references, conservation, both-side prevalidation, no partial state
  on missing/same account, insufficient funds, invalid date/amount or frozen status.
- Freezing: first/repeated attempt and every blocked operation; history remains readable.
- Interest: two policies/rates, HALF_EVEN ties, zero result, month-end/chronology, duplicate month,
  frozen status, injected-policy validation and failure-state preservation.

Current totals/coverage are in VERIFICATION.md. History for transfer and interest types is only
record-level tested until those posting operations exist. No persistence, concurrency, UI,
GitHub network integration or unattended scheduler is part of the Java test suite. Git pushes
are verified independently. One compiler-generated switch default remains uncovered; every legal
TransactionType and null rejection is tested. No fake enum values or removed checks chase coverage.

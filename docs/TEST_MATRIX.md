# Test evidence and obligations

## Implemented

| Public operation | Successful behavior | Failure / boundary behavior |
|---|---|---|
| Customer constructor | Preserves valid identity; strips surrounding whitespace | Null, empty, spaces, control whitespace, Unicode whitespace for ID/name |
| Bank constructor | Empty registry | No inputs or meaningful domain failure |
| registerCustomer | Registers retrievable customer; allows repeated names | Invalid ID/name; normalized duplicate ID preserves original registry |
| findCustomer | Returns same registered instance; strips lookup ID | Missing ID, invalid ID, case-sensitive mismatch |
| getCustomers | Empty/nonempty snapshot; registration order | Mutation rejected; later registration cannot mutate old snapshot |
| Exception constructors | Messages checked through actual operation failures | No operation-specific invalid message contract; no artificial constructor failures |

Simple getters are exempt in the PDF, but customer values are checked in constructor tests.
Every concrete custom exception is thrown by an operation test. Because BankingException is
abstract, the same tests exercise its constructor through subclasses; an abstract base has no
independent instance to throw. UnsupportedOperationException comes from the JDK collection
contract and is not a custom banking exception.

## Required as accounts arrive

- Opening: each type, missing owner, duplicate ID, invalid date/balance/precision, savings floor.
- Deposits: positive, null/zero/negative, sub-cent precision, frozen, pre-opening/backdated date.
- Withdrawals: exact floor allowed, one cent below rejected, checking insufficient funds,
  invalid amount/date/frozen status; rejection preserves balance and history.
- Transfer: valid paired entries, same-account/missing-account, either side frozen, date/amount
  invalid, insufficient funds, conservation of total money, neither side changes on rejection.
- History: opening and posting order, immutable contents/snapshot, transfer correlation.
- Interest: two rates, rounding ties, zero credit, duplicate month, month-end validation,
  frozen/backdated date, injected policy delegation/failure preserves state.
- Freeze: first succeeds, repeated attempt fails, read access remains available.
- Polymorphism: shared account contracts for both concrete subtypes.

Coverage numbers are evidence for the current code only, not proof of final behavior coverage.
No database, GitHub network calls, concurrency or scheduler is claimed as tested. Git/GitHub
activity is verified separately; application code must never contact GitHub to satisfy tests.

# Input/error contracts and mutation-order review — 19 November 2026

Scope: inspect validation order and observable state publication in the existing implementation.
The PDF requires clear custom failures and supplied dates; it does not prescribe error priority
when several inputs are invalid. The ordering below records the project's actual decisions.

## Concrete defect corrected

An insufficient outgoing transfer used the shared posting template's withdrawal wording:
`Account S has 150.00 available; cannot withdraw 150.01.` The requested operation was a transfer.
The regression `InputContractTest#insufficientTransferNamesTheRequestedOperationAndKeepsRetryState`
failed for both checking and savings before the fix, reproducing the exact incorrect word.

The floor-rejection branch now selects `transfer` for TRANSFER_OUT and `withdraw` otherwise.
The corrected example is `Account S has 150.00 available; cannot transfer 150.01.` Existing
withdrawal diagnostics, exception type, amounts/floors, validation order and publication remain
unchanged. Extra trailing zeros are normalized before either message, so 150.0100 prints 150.01.
This is a diagnostic fix, not a new fee, posting rule, exception or public API.

## Input and error precedence

| Entry point / stage | Actual order | State consequence |
|---|---|---|
| BankingInput.text | Reject null/blank, then strip surrounding whitespace; IDs remain case-sensitive | Normalization returns a value without registry mutation; no invented character/length rules |
| BankingInput.nonNegativeMoney | Required value, negative check, exact-cent normalization with UNNECESSARY | Sub-cent positive input is rejected, not silently rounded; negative input is rejected before scale validation |
| Bank.registerCustomer | Fully validate Customer ID/name, then duplicate check, then put | An invalid name is reached before duplicate detection; no partially validated customer is registered |
| Bank account opening | Validate free normalized ID, resolve this Bank's owner, construct complete subtype, then put | Occupied ID wins over missing owner/invalid opening state; constructor failure cannot publish an account or reserve a new ID |
| Direct deposit/withdraw | Frozen guard, money/strict positivity, required date, chronology, arithmetic/floor, complete entry/ledger preparation, guarded commit | Invalid amount precedes missing date; a backdate precedes insufficient funds; rejection leaves ledger and next ID unchanged |
| Bank.transfer | Source lookup, target lookup, distinctness, outgoing preparation, incoming preparation, receipt allocation, both validations/commits, reference update | Lookup/distinctness precede account guards; source errors can precede recipient frozen state. There is no global frozen-first priority across two accounts. |
| Monthly interest | Frozen guard, required date, month-end, chronology, existing YearMonth marker, policy/result validation, full ledger preparation, guarded commit | No policy call before eligibility; zero success records a month marker; ordinary rejection consumes no month/ID |
| PreparedPosting.validate/commit | Active guard before expected-ledger identity; publish one prebuilt ledger reference only after validation | Frozen beats stale; a changed or consumed plan cannot overwrite the newer ledger |

The direct Account frozen guard always precedes that Account's other posting checks. This does
not mean Bank.transfer can skip account lookup or distinctness. A frozen source plus a missing
recipient therefore produces AccountNotFoundException; a normalized same-account request fails
distinctness even with frozen state/null posting inputs. These are first-reached guard rules,
not an authorization service or a professor-mandated cross-operation priority.

## Mutation boundaries reviewed

- Customer/account constructors can allocate rejected objects, including opening ledger facts,
  but those objects are never returned/registered on failure. Maps publish only fully constructed
  objects. Caller-supplied identity/BigDecimal values and immutable Customer objects are not mutated.
- Account computes and allocates its complete next entry/history/LedgerState before commit.
  Local IDs derive from published history, not a separate reserved counter. Earlier snapshots
  remain immutable. freeze changes only its private lifecycle flag, not financial facts or dates.
- Bank prepares both distinct accounts and allocates the receipt before either publication.
  Under the single-threaded/no-interleaving contract, committing one cannot invalidate the other;
  guarded publication has no arithmetic, allocation or callback. The transfer reference is stored
  only after both commits. This is not concurrent/database atomicity.
- Monthly processing uses the INTEREST entry as its marker, so no independent month set can be
  left ahead of a failed posting. Policies must be pure; arbitrary external side effects are not
  rolled back. VM/resource failure and long-range counter exhaustion remain outside guarantees.

Existing preparation/transfer/monthly suites already cover each publication boundary. No balance,
history, month-marker or reference mutation defect was found; their ordering is retained.

## New evidence

InputContractTest adds **20 invocations**, all through public Bank/Account operations:

| Behavior | Cases | Evidence |
|---|---:|---|
| Correct insufficient-transfer diagnostic | 2 | `InputContractTest#insufficientTransferNamesTheRequestedOperationAndKeepsRetryState`: both ledgers stay intact, an earlier valid date succeeds, next IDs are 2 and first transfer reference is 1 |
| Preserve withdrawal diagnostic | 2 | `InputContractTest#insufficientWithdrawalRetainsItsSpecificDiagnosticAndLedger`: exact old wording, normalized amount and retry ID |
| Lookup before frozen source/bad inputs | 2 | `InputContractTest#targetLookupPrecedesFrozenSourceAndInvalidPostingInputs`: typed lookup error, unchanged ledgers/frozen status, independent transfer still starts at reference 1 |
| Distinctness before frozen/bad inputs | 2 | `InputContractTest#distinctAccountCheckPrecedesFrozenStateAndMissingPostingInputs`: normalized identity check and unchanged state |
| Money before missing date | 8 | `InputContractTest#invalidAmountPrecedesMissingDateWithoutReservingAnIdentifier`: zero/sub-cent, both account types and both direct operations; next valid ID is 2 |
| Chronology before funds at the floor | 2 | `InputContractTest#backdatedWithdrawalPrecedesInsufficientFundsAtTheFloor`: typed date error then valid replenishment/withdrawal with consecutive IDs |
| Free account ID before other opening checks | 2 | `InputContractTest#occupiedAccountIdPrecedesMissingOwnerAndInvalidOpeningState`: original owner/account, both registries and ledgers survive |

Complete Maven clean verify passes **659 tests**, including all 639 prior cases. JaCoCo is
239/239 lines, 1054/1054 instructions, 70/71 branches, 70/70 methods and 23/23 executable classes.
The new diagnostic selection has both outcomes covered. Only Transaction's documented enum
switch fallback remains missed; no fake enums/private state manipulate the coverage result.

The current UML remains accurate: only a local message choice changed, with no new classes,
members, signatures or relationships. Documentation and interim Word evidence are updated.
Next proposed work is the final UML/class/pattern/SOLID presentation, followed by final report,
defense and independent review. No later changes are prewritten; the genuine two-week span is
unfinished and saved scheduling proves neither future execution nor professor acceptance.

# Interest boundary review — 15 November 2026

Scope: review milestone 9's implemented monthly interest, not add another banking feature.
The PDF requires monthly interest and supplied dates; the precise floors, decimal rates,
month-end eligibility, HALF_EVEN rounding, no catch-up and zero-entry rules are our documented
project decisions. Requirements and those decisions were checked against the code and tests.

## Gaps found and closed

The existing suite covered pure quotation ties and a rounded-zero posting, but did not establish
both default products' actual ledger credits at nonzero ties and adjacent cent balances. Its
policy-failure retries started before any successful monthly posting. The skipped-year test
checked IDs/dates without asserting the exact credited amount. These were behavioral evidence
gaps despite full line coverage; no production defect was found in this session.

`MonthlyInterestBoundaryTest` adds 37 invocations:

| Boundary | Evidence added |
|---|---|
| Posted rounding | Twelve explicit checking/savings examples assert returned/stored INTEREST facts and actual new balance at ties and one-cent neighbors |
| Calendar eligibility | Both products accept common-year February, the leap century 2000, non-leap century 2100 and December month-end (eight cases) |
| Skipped month | January then March posts only one March credit; exact amount/balance/history and rejected February backfill checked (two cases) |
| Old processed month | After January and March, January fails chronology before duplicate detection or policy execution, preserving the ledger (two cases) |
| Later-month policy failure | Null, negative, sub-cent, domain and unexpected failures after successful January preserve that month marker; February remains eligible and March retry uses consecutive IDs (ten cases) |
| Real decimal failure | Actual FixedRateInterestPolicy scale overflow through monthly application leaves state intact and an earlier deposit date usable (two cases) |
| Repeated rounded zero | Four monthly credits on checking 5.00 remain zero, each consumes one ID, each blocks a duplicate and no hidden half-cent accumulates (one case) |

Selected independently specified amounts help explain the assertions:

| Product / starting balance | Exact product | Credited interest | New balance |
|---|---|---|---|
| Checking 1005.00 at 0.001 | 1.005 | 1.00 | 1006.00 |
| Checking 1015.00 at 0.001 | 1.015 | 1.02 | 1016.02 |
| Savings 1005.00 at 0.003 | 3.015 | 3.02 | 1008.02 |
| Savings 1015.00 at 0.003 | 3.045 | 3.04 | 1018.04 |

With January then March and no other postings, checking 1000.00 finishes at 1002.00;
savings finishes at 1006.01. No February credit is synthesized. With checking 5.00,
each 0.005 calculation rounds independently to 0.00; fractions are not carried forward.

## Design conclusions and limits

Keep production code unchanged: the existing pure Strategy, ledger-derived month marker and
prepared publication boundary satisfy the reviewed contracts. Do not add another mutable month
set, fraction accumulator, callback rollback framework or date clock. Failure doubles deliberately
return invalid outputs or throw on their second call; their counters observe invocation order.
They do not mutate Account state. Side effects from impure policies remain outside the guarantee.

There are no class/API/relationship changes. The milestone 9 UML remains accurate and was checked
against the unchanged production tree; it was not regenerated just to create diagram activity.
The interim Word report is updated with this review and current test evidence. It remains interim.

Full Maven clean verify: 618 tests, zero failures/errors/skips. JaCoCo: 238/238 lines,
1046/1046 instructions, 68/69 branches, 70/70 methods, 23/23 executable classes. The missed
branch remains the documented compiler-emitted Transaction enum fallback. Higher test count
does not increase line coverage or establish correctness for all possible inputs.

Next proposed scope: genuine cross-operation scenarios and invariants, using Bank transfers,
interest and lifecycle together. No later changes were prewritten and no future push is certified
by this review. Two weeks of authentic history and final submission/defense work remain.

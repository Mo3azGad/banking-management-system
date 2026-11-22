# Test evidence and obligations — milestone 17

API_EXCEPTION_AUDIT.md provides the complete public declaration accounting, named success/rejection
witnesses and all eleven custom-exception throw witnesses. Its 67 unique test selectors were checked
against source and this session's passing JUnit reports. This behavior matrix remains the broader
boundary overview; line coverage alone is not the audit evidence.

| Public operation | Successful behavior | Failure / boundary behavior |
|---|---|---|
| Customer constructor | Valid identity; whitespace stripping | Null, empty and whitespace ID/name |
| Bank constructor | Empty registry | No input/domain failure condition |
| registerCustomer | Retrieval; same names allowed | Invalid identity/name; normalized duplicate preserves original |
| findCustomer | Registered instance; normalized lookup | Missing, invalid and case-mismatched ID |
| getCustomers | Immutable, ordered, isolated snapshots | External mutation rejected |
| openCheckingAccount / openSavingsAccount | Canonical owner, explicit date, floor boundaries, one opening entry and registered instance | Blank account/customer IDs; missing/foreign-bank owner; duplicate IDs across types/owners; invalid balance/date; retry after rejection |
| findAccount | Normalized ID lookup, registered identity and posting state | Missing/invalid/case-mismatched ID; no implicit registration from direct construction |
| getAccounts | Empty/ordered immutable membership snapshots; actual account instances | Membership mutation rejected; later openings do not change earlier snapshots |
| Account constructors | Ownership, dates, floors, rates, exact money; opening history | Missing values, negative/sub-cent amounts, below savings floor |
| freeze / isFrozen | Initially active; first transition; unchanged financial metadata/history; independent accounts | Repeat freeze rejects with AccountFrozenException and preserves state |
| getAvailableBalance | Polymorphic floor before/after postings; remains readable when frozen | Valid models have no failure inputs |
| Transaction constructor | All six types, normalized money, zero opening/interest, paired references | Invalid IDs/fields/money; opening mismatch; impossible credit balance; invalid transfer links |
| deposit | Balance and returned/stored ledger entry; one cent; trailing zeros; large amounts | Null/zero/negative/sub-cent amount; null, pre-opening and backdated dates; frozen status takes precedence |
| withdraw | Balance and returned/stored ledger entry; exact floor; replenishment | Invalid inputs/dates; one cent over available; at floor; insufficient funds; frozen status takes precedence |
| Internal prepareDeposit / prepareWithdrawal | Validated preview; no publication, reservation or date advance; exact floor | Invalid amount/date/funds/frozen state preserves ledger and prior valid plans |
| PreparedPosting.validate / commit | Origin-bound publication; old snapshots preserved; distinct prevalidated plans remain independent | Reuse/intervening posting/same-balance round trip rejects as stale; freeze-after-prepare blocks; frozen takes precedence over stale |
| Bank.transfer | All four type pairs; conserved total; exact floor; one cent; linked entries/date/reference; immutable receipt; independent local IDs | Missing/same/foreign accounts; invalid money/date; either frozen; source funds/floor; recipient opening/latest-date failure; neither ledger nor counters partly change |
| Internal prepareTransferOut / prepareTransferIn | Shared prepared-state contracts exercised through Bank transfers | Nonpositive reference rejects without publication |
| FixedRateInterestPolicy constructor | Nonnegative rates including zero, high precision and rates above 1 | Null/negative rate rejects with clear custom input exception |
| InterestPolicy.calculate via FixedRateInterestPolicy | Actual checking/savings quotes; HALF_EVEN ties/neighbors; zero; large money; trailing/scientific exact-cent input; repeatability | Null/negative/sub-cent balance; zero rate cannot bypass validation; unsupported arithmetic scale becomes custom failure |
| CheckingAccount / SavingsAccount injected constructors | Default constructors unchanged; custom policy delegates and overrides algorithm | Null policy rejects; existing constructor validation still runs through injected overloads |
| applyMonthlyInterest | Default rates; complete INTEREST facts; compounding on next month; zero marker; Bank lookup; injected algorithm | Null/non-month-end/pre-opening/backdated date; duplicate month after ordinary posts; frozen; invalid result; propagated/wrapped policy errors; no ledger/ID/month reservation on rejection |
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

- Final artifact/independent review must confirm that documentation matches actual behavior and
  close genuine gaps found. Input/error and publication ordering are reviewed in
  INPUT_MUTATION_REVIEW.md; API/exception, design, integration and interest-boundary evidence remain
  separate. The remaining sessions are not permission to defer testing current changes.

Current totals/coverage are in VERIFICATION.md. Transfer history is now verified through Bank execution. Interest history is now verified through actual Account monthly application. No persistence, concurrency, UI,
GitHub network integration or unattended scheduler is part of the Java test suite. Git pushes
are verified independently. One compiler-generated switch default remains uncovered; every legal
TransactionType and null rejection is tested. No fake enum values or removed checks chase coverage.

Milestone 4 adds 68 Bank account test invocations. Failure checks preserve both registries,
existing account balance/history and reusability of failed IDs. Cross-type duplicate IDs,
canonical customer identity, multiple accounts per customer, separate namespaces/Banks and
posting through retrieved instances are verified. New DuplicateAccountException and
AccountNotFoundException are each thrown and their messages checked in operation tests.

Milestone 5 adds 50 AccountFreezeTest invocations shared across checking/savings and both current
posting operations. They exercise first/repeated freeze, valid/invalid blocked postings, state
preservation, readable immutable history, Bank lookup/snapshot visibility and isolation between
accounts owned by the same customer. An active account remains active after an insufficient-funds
failure. No unfreeze operation exists; tests do not bypass private state merely to observe a
hypothetical subsequent posting. Bank transfers now have frozen-state tests; monthly application now also has frozen-state rejection tests.

Milestone 6 adds 79 PreparedPostingTest invocations. They test the internal domain boundary
rather than reflection or private field manipulation. Preparation/validation leaves state intact;
rejection does not consume IDs. Commit publishes the exact prepared facts. Changed ledgers,
reused plans, competing same-account plans and same-balance round trips cannot overwrite state.
Plans stay bound to their original object even when another standalone object has the same ID.
Frozen state beats both invalid inputs and staleness. Separate-account preflight tests demonstrate
that validation can precede publication; they do not test a Bank transfer or paired transfer history.

Milestone 7 adds 86 BankTransferTest invocations, including all checking/savings directions and
snapshot equality after rejection. Tests check paired immutable facts, exact floor/over-limit
boundaries, both-side chronology, each frozen state, same/missing/case-sensitive/foreign IDs,
transfer-reference independence/no gaps after failures, and huge round trips without lost cents.
Source preparation is not published when the recipient fails. Existing prepared plans on both
accounts become stale after success; a source plan survives a failed recipient preparation.
No private counter manipulation or fabricated execution failures are used. Long-range reference
exhaustion, VM resource failure, concurrency and persistence are not simulated/claimed tested.

Milestone 8 adds 41 FixedRateInterestPolicyTest invocations. They check monthly-rate semantics,
zero rates/results, invalid constructor/calculation input, tie parity and adjacent cent boundaries,
no premature rate rounding, very large exact values, stable repeated calls, supported decimal
scale failure translation and actual Account rate metadata. A quote from a frozen bank-owned
account does not post interest, change balance/history or unfreeze it. These are calculation
and read-state tests, not month eligibility, policy injection or monthly application tests.

Milestone 9 adds 52 MonthlyInterestTest invocations. They exercise both default/custom constructors,
month-end/leap-day cases, chronology after opening/deposit/withdrawal, current-month duplicates
independent of latest entry, rounded/explicit zero credit, policy call eligibility, invalid outputs,
domain and unexpected failures with retries, month/ID preservation, old prepared-plan validity and
same-month-number/different-year behavior. Bank-owned default accounts use injected fixed policies.
Test doubles deliberately return invalid values/throw once to verify defensive result handling;
no private state or clock is manipulated. Impure policy side effects/concurrency/VM failure are
outside the documented pure single-threaded contract and are not claimed rollback-tested.

Milestone 10 adds 37 MonthlyInterestBoundaryTest invocations. They establish actual posted
HALF_EVEN ties/neighbors for both products, Gregorian century/common-year month-ends, precise
no-catch-up amounts and rejected backfill, chronology precedence for old processed months, five
later-month failure modes after prior success, real fixed-policy scale failure through Account,
and consecutive rounded-zero months without fraction accumulation. Tests preserve old markers,
chronology and IDs on failure. No production change was necessary; coverage remains unchanged.

Milestone 11 adds 21 BankScenarioTest invocations through public APIs. They reconstruct every
running ledger balance/ID/date/floor through a two-month external-flow scenario, pair transfer
references across unequal local IDs, exercise interest before/after transfers for all four type
pairs, reject transfers before either side's latest interest without state/counter changes, spend
interest up to the floor and freeze after mixed financial activity without blocking other accounts.
Existing snapshots/markers remain intact. No production defect was found; coverage is unchanged.

Milestone 12 reviews the already shared AccountTest, AccountOperationsTest, AccountFreezeTest
and monthly suites through Account references. DESIGN_REVIEW.md names success/failure evidence
for the base availability, chronology, snapshot, lifecycle and injected-policy contracts. No new
test or executable refactor was necessary; all 639 existing cases were rerun. Account Javadoc
now states the obligations those tests assess. This review does not prove future subtype or
impure-policy correctness and does not replace the remaining full public-operation audit.

Milestone 13 completes that public-operation/custom-exception audit without adding redundant
tests or changing executable code. All 639 cases pass. Public APIs with meaningful failure paths
have named successful/rejected witnesses; valid-state no-input queries, basic getters, generated
record/enum utilities and exception carrier constructors have explicit classifications. The sole
missed branch is the Transaction constructor's enum fallback at source line 24. Two exception
comments were corrected; all signatures, behavior and UML relationships remain unchanged.

Milestone 14 adds 20 InputContractTest invocations. The insufficient-transfer regression failed
twice before the message fix, then passes for both products; withdrawal wording stays unchanged.
Further cases establish lookup/distinctness before frozen/bad transfer inputs, money before a
missing direct-posting date, chronology before insufficient funds and occupied account ID before
owner/opening validation. Rejections preserve both ledgers/registries as applicable; retries show
no reserved local IDs/dates/transfer reference. Only diagnostic wording changes in production;
both outcomes of that choice are covered. Full suite: 659 cases, 100% lines and 98.59% branches.

Milestone 15 adds no behavioral test or executable change. The full existing 659-case suite is
rerun. Separate documentation checks compare all 24 depicted banking types, 30 displayed field
references and 48 displayed operation references/visibility with compiled metadata, verify the
generalization/realization edges and rendered SVG node sets, and inspect all three views. These
are diagram checks, not extra JUnit cases or proof of every behavior. UML_GUIDE.md supplies the
selected-member legend, omitted exact signatures, multiplicities and actual pattern/SOLID limits.

Milestone 16 rebuilds the same 659-case suite for final technical report assembly. Its five required
sections, fourteen per-suite counts, coverage figures, named test selectors and three embedded
figures are checked against current source/generated evidence. The rendered 13-page PDF is reviewed
for pagination, text/table bounds and diagram legibility. These artifact checks add no JUnit cases;
Java source/test files and the UML model are unchanged. See REPORT_BUILD.md and VERIFICATION.md.

Milestone 17 verifies the four existing methods selected in DEFENSE_DEMO.md: eight parameterized
transfer invocations and four interest invocations. Their commands, fixtures, balances, references,
rejection state and successful retries are checked against source and real Surefire results. These
are subsets/reruns, not twelve added cases. A subsequent complete clean verify restores all 659
passing cases and authoritative coverage. The independent exercise is a student task with checked
predictions, not implemented tests or a claim of student completion. Production/tests/UML remain
unchanged; the dated 21 November report retains its accurate edition. See VERIFICATION.md.

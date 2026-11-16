# Cross-operation review — 16 November 2026

Scope: verify existing operations together through public Bank/Account APIs. The PDF's banking
operations are implemented; this review tests their interactions and introduces no new feature
or architecture. Earlier suites checked transfers and monthly posting separately, leaving the
following mixed-workflow evidence gaps. No production defect was found in this session.

## Evidence added

BankScenarioTest adds 21 invocations:

| Scenario | Cases | Contract verified |
|---|---:|---|
| Two-month customer/account workflow | 1 | Deposits, withdrawal, two paired transfers and monthly interest reconcile to specified balances and bank-wide external flows |
| Transfer before month-end interest | 4 | All source/target type pairs calculate from the post-transfer balances |
| Transfer after same-day interest | 4 | Transfers conserve funds and cannot reopen either processed YearMonth |
| Interest establishes either transfer date bound | 8 | All type pairs and either side reject a backdated transfer without partial state or consumed reference/IDs; same-day retry works |
| Interest-earned funds above the floor | 2 | Each source type rejects one cent over availability, then allows the exact amount while preserving its original floor |
| Freeze after transfer and interest | 2 | Each frozen subtype blocks all five financial directions/operations, preserves both ledgers and leaves unrelated accounts usable; next successful transfer has no reference gap |

The scenarios use registered customers with canonical ownership and actual default policies.
An independent test helper reconstructs running balances from signed transaction facts, checking
every balance-after, account ID, consecutive local ID, date bound and product floor. Explicit
expected amounts and external-flow totals anchor those consistency checks; a matching balance
and history alone would not prove the financial result correct. No internal preparer, reflection,
clock or callback stub is used by these integration tests.

## Two-month worked example

Checking belongs to Ada, savings to Grace. Checking has two separate deposits so its local
posting sequence differs from savings; transfer references must still pair the correct legs.

| Step | Checking | Savings | Combined balance |
|---|---:|---:|---:|
| Opening | 500.00 | 1000.00 | 1500.00 |
| Checking deposits 150 + 50; savings withdraws 100 | 700.00 | 900.00 | 1600.00 |
| January transfer: checking to savings 250 | 450.00 | 1150.00 | 1600.00 |
| January interest: checking 0.45, savings 3.45 | 450.45 | 1153.45 | 1603.90 |
| February transfer: savings to checking 53.45 | 503.90 | 1100.00 | 1603.90 |
| February interest: checking 0.50, savings 3.30 | 504.40 | 1103.30 | 1607.70 |

The final total is 1500.00 + 200.00 - 100.00 + 7.70 = 1607.70. Transfers neither create nor
destroy funds. Each receipt's two entries have equal amount/date/reference, opposite directions,
different account-local sequence numbers and matching records in the respective histories.
The earlier checking history snapshot remains unchanged after February operations.

## Design conclusions and limits

Keep production code unchanged: ledger-derived month eligibility survives ordinary transfer
entries; interest advances the same chronology bound as other postings; type-specific floors
apply to the newly earned funds; lifecycle permission is separate from readable ledger state.
The existing immutable replacement-ledger boundary and prevalidated two-account coordination
satisfy these reviewed contracts under single-threaded in-memory use.

The model has no new classes, APIs or relationships. The milestone 9 UML remains accurate and
is retained after comparison with the unchanged production tree. The five-section interim DOCX,
test matrix, walkthrough and current verification are updated with this evidence. Full Maven
clean verify passes 639 cases; line/branch coverage remains 100%/98.55%. One documented compiler
enum fallback is unvisited. These scenarios do not establish concurrent/database atomicity,
persistence or rollback of arbitrary impure policy side effects.

Next proposed work is the shared subtype/SOLID/pattern review, followed by the remaining audits,
final report and defense. No later changes were prewritten. Authentic development over at least
two weeks is still in progress; saved scheduling does not certify future runs or pushes.

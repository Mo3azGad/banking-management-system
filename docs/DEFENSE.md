# Oral defense preparation — interim

Milestone 9 integrates InterestPolicy into dated Account monthly posting; milestone 10 reviews
interest boundaries with 37 additional cases. Milestone 11 adds 21 cross-operation scenarios;
neither review required production changes. Milestone 12 clarifies the shared base contract and
precise SOLID/pattern claims using existing tests. Milestone 13 maps public operations and all
custom exceptions to named passing cases. Milestone 14 fixes an insufficient-transfer diagnostic
and verifies input/publication ordering with 20 new cases. All listed banking operations now exist, with final reviews/report/defense remaining. Do not present planned
classes as implemented. The final defense carries 30 points and attendance is compulsory.

## Explain the current code

- Customer constructor establishes valid immutable identity; BankingInput.text rejects missing data.
- registerCustomer validates first, checks uniqueness, and changes the map only on success.
- findCustomer normalizes input and throws a clear typed exception when the identity is absent.
- getCustomers copies the map values, preventing callers changing bank state or old snapshots.

Run `mvn -Dtest=CustomerTest,BankTest test`. Walk through duplicateRegistrationKeepsOriginalCustomer
and customerSnapshotDoesNotChangeAfterLaterRegistration. Assertions on unchanged state are part
of a single rejection/isolation behavior, rather than unrelated checks.

## Explain milestone 2

- Account validates shared opening state once. Checking and savings override the floor and rate;
  getAvailableBalance uses polymorphism through an Account reference.
- Savings rejects 99.99 and accepts 100.00; checking accepts zero without an overdraft.
- BankingInput rejects fractions of a cent with UNNECESSARY rather than silently rounding.
- Transaction is immutable: type determines direction, amounts remain unsigned, and optional
  transfer references distinguish ordinary entries from transfer legs.
- Transaction creation is validation only; Account executes dated postings and Bank coordinates transfers (added in milestone 7).
- All dates are supplied. Record validation cannot determine account chronology without account
  state; that validation belongs in the posting workflow.

Run `mvn -Dtest=AccountTest,TransactionTest test`. Show
`permitsOpeningExactlyAtTheAccountFloor`, `rejectsOpeningOneCentBelowTheAccountFloor`, and
`rejectsTransferWithoutLinkReference`. Explain the rule before the expected assertion.

## Explain and demonstrate milestone 3

Run `mvn -Dtest=AccountOperationsTest test`, then `mvn clean verify` for the full suite.

- `deposit` and `withdraw` are final entry points into Account's shared posting preparation template.
  Show validation, date bound, calculation, minimum hook, immutable replacement ledger and guarded publication.
- `permitsWithdrawalOfExactlyTheAvailableBalance` demonstrates different floors through Account.
- `withdrawalOneCentOverAvailableBalancePreservesState` proves rejection leaves the ledger intact.
- `chronologyTracksTheLatestWithdrawalAsWellAsDeposits` shows that either posting advances the bound.
- `failedLaterDatedWithdrawalDoesNotAdvanceLedgerDateOrSequence` shows rejected operations do not
  affect the next valid operation.
- `previouslyRetrievedHistoryRemainsUnchangedAfterPosting` demonstrates snapshot encapsulation.

Why include an opening entry? It establishes a complete starting balance and chronology bound.
Why account-local IDs? The pair (accountId, transactionId) is unique within a registered bank;
independent accounts need no static counter or Bank service to post. Transfer pairing uses
another Bank-coordinated reference. This replaces an earlier plan, not a professor requirement.
Why is this Template Method? One non-overridable algorithm uses a polymorphic minimum hook;
subclasses vary the rule without duplicating or bypassing posting validation.
What does atomic mean here? All domain validation/record preparation happens before state
changes, under single-threaded in-memory use. This is not a database/concurrency guarantee.
Why not reject future dates? The professor says dates must be supplied, not read from the clock;
there is no supplied reference date, so only opening/latest-posting chronology is enforced.

## Explain milestone 4 (9 November)

Run `mvn -Dtest=BankAccountTest test` for this scope, then `mvn clean verify` for everything.

- Bank.openCheckingAccount/openSavingsAccount validate a free ID, find the canonical Customer,
  construct the complete account and only then publish it in the account map.
- Show `constructorRejectionPreservesRegistriesAndLeavesIdReusable` and explain why putting
  an incomplete account in the map before validation would break this contract.
- `accountIdentifiersAreUniqueAcrossCheckingAndSavings` demonstrates one Bank-wide account
  namespace. The PDF leaves ID rules open; these are documented implementation decisions.
- `openingPublishesTheReturnedAccountWithTheCanonicalRegisteredOwner` and
  `registrationAtAnotherBankDoesNotAuthorizeOpeningHere` demonstrate registered ownership.
- `lookupRetainsTheRegisteredPostingStateAndSequence` connects earlier deposit/withdraw work
  to Bank retrieval. No copying of accounts or ledger IDs is done by lookup.
- `priorAccountSnapshotDoesNotGainLaterAccounts` protects registry membership. A snapshot's
  account objects still mutate via their validated APIs; this is intentionally not a balance snapshot.

Why named opening methods? Only two concrete types exist, so named creation/registration
operations are clear without a separate factory framework. They return concrete types and
use shared uniqueness validation plus the existing Customer lookup/Account constructors.
Why not register an arbitrary Account? This scope does not need importing external histories;
Bank creates accounts using its own registered owner and validated opening state.
Why new exception types? DuplicateAccountException identifies a conflicting account ID,
AccountNotFoundException identifies failed lookup; existing input/customer exceptions are reused.

## Explain milestone 5 (10 November)

Run `mvn -Dtest=AccountFreezeTest test`, then `mvn clean verify` for the whole project.

- `freeze()` and `isFrozen()` live in Account; final methods and a private flag prevent subtype
  overrides/setters bypassing the lifecycle. `requireActive()` is shared by freeze and posting.
- Show `frozenAccountRejectsValidPostingsWithoutChangingLedger` and
  `frozenFailureTakesPrecedenceOverInvalidPostingInput`: frozen state is rejected before input checks.
- Show `freezingPreservesFinancialStateAndExistingHistory` and
  `frozenHistoryRemainsReadableAndRejectsExternalMutation`: freezing is not a financial posting.
- `repeatedFreezeThrowsTypedFailureAndPreservesState` demonstrates the new unchecked subtype.
- `freezingOneAccountDoesNotBlockAnotherAccountOfTheSameOwner` demonstrates account-level scope.

Why no date parameter on freeze? It neither compares dates nor records a timestamp; explicit
dates remain required for financial postings and no system clock is read. Why no freeze ledger
entry? The existing Transaction model holds financial facts; freezing only controls permission.
Why not a State pattern? A private boolean and shared guard fit this two-state irreversible scope.
Why does available balance remain visible? It is balance minus floor metadata; frozen permission
is independent. Bank transfers check frozen status since milestone 7, and monthly interest does so
since milestone 9; both have success/failure tests.

## Explain milestone 6 (11 November)

Run `mvn -Dtest=PreparedPostingTest test`, then `mvn clean verify` for all tests.

- Public deposit/withdraw still execute fully; they prepare a plan and immediately commit it.
  Package-private preparers expose a reusable validation/allocation boundary for later coordination.
- `preparingAndValidatingDoesNotPublishFinancialState` shows a preview is not a ledger entry yet.
- `committedPlanCannotBeValidatedOrCommittedAgain` shows the new StalePostingException.
- `returningToTheSameBalanceDoesNotReviveAnOldPlan` explains why ledger identity matters rather
  than comparing balances alone: sequence/history may differ while balance is numerically equal.
- `freezingAfterPreparationPreventsValidationAndCommit` preserves frozen-state protection.
- `failedSecondPreparationLeavesTheFirstAccountUncommitted` illustrates prevalidation before
  publication; it is not an implemented Bank transfer.

Why a private LedgerState record? Balance and immutable history publish together through one
reference assignment. All arithmetic/list allocation happens before the commit guard succeeds.
Why an inner opaque plan? Its private fields/constructor and originating-account binding prevent
forged/cross-account application. Why revalidate at commit? The account may have changed or frozen
after preparation. Why no public preparation API? Preparation/commit is internal; clients use deposit/withdraw
and, since milestone 7, Bank.transfer. What is the cost? O(n) ledger copy per preparation and retained
history per plan, acceptable here. This is neither concurrency control nor distributed two-phase
commit. Bank.transfer now enforces distinct accounts and validates both plans before publication.

## Explain milestone 7 (12 November)

Run `mvn -Dtest=BankTransferTest test`, then `mvn clean verify` for the full suite.

- Walk Bank.transfer through canonical lookup, distinctness, candidate reference, both transfer
  plans, receipt allocation, both validations, both guarded commits and reference advancement.
- `transferConservesMoneyAndPostsOneLinkedEntryPerAccount` checks all four concrete type pairs.
- `dateValidForSourceButBeforeTargetOpeningCannotPartiallyDebit` and
  `eitherFrozenAccountBlocksTransferWithoutChangingEitherLedger` explain recipient-side failure.
- `transferReferencesAreIndependentOfLocalPostingSequenceNumbers` distinguishes the common
  pairing key from each account's ledger ID. References are scoped to Bank, not globally unique.
- `rejectedTransferAfterSuccessDoesNotLeaveAGapInTransferReferences` demonstrates no reservation.

Why not withdraw then deposit? Recipient date/frozen validation could fail after debiting source.
Why is the second guarded commit safe here? Both plans were prevalidated; registered accounts are
distinct, execution is single-threaded, and publication runs no callbacks/arithmetic/allocation.
There is no supported mutation between validations/commits. This is not concurrency/database
atomicity. Why return List<Transaction>? Two immutable facts in documented out/in order are enough;
a separate receipt framework adds no needed behavior. Are same-owner accounts allowed? Yes, as
long as accounts differ. Does target frozen always beat bad source money? No global cross-account
error priority is promised: lookups/distinctness run first, then outgoing and incoming preparation.

## Explain milestone 8 (13 November)

Run `mvn -Dtest=FixedRateInterestPolicyTest test`, then `mvn clean verify` for the entire project.

- InterestPolicy has one operation; FixedRateInterestPolicy validates an immutable monthly decimal
  rate, validates a balance, multiplies exactly and rounds the final product HALF_EVEN.
- Show `halfEvenRoundingHandlesBothTieDirectionsAndAdjacentValues`: 0.005 rounds to 0.00,
  0.015 to 0.02, 0.025 to 0.02. Explain the parity before running the assertions.
- `retainsRatePrecisionUntilTheFinalInterestRounding` guards against rounding the rate too early.
- `actualCheckingAndSavingsRateMetadataProduceDifferentQuotes` demonstrates 0.001 versus 0.003
  on the actual model metadata; the interface is used for calls rather than a concrete API dependency.
- `calculationDoesNotPostInterestOrMutateEvenAFrozenAccountsLedger` distinguishes a quote from
  dated posting permission. No eligible month, balance update or interest history is created.

Why no date parameter? Pure math uses only supplied balance/rate; month eligibility belongs in
the monthly posting operation added in milestone 9. Why no rate cap? None is specified by the PDF; nonnegative
input is the documented contract. Why allow zero? Rounded-zero interest is a valid financial
result, unlike zero deposit/withdraw requests. Why not another exception subclass? Existing input
failure covers invalid rate/balance and unsupported decimal precision. Is Account DIP finished?
Milestone 8 had only strategy components. Milestone 9 now injects the abstraction into Account
and invokes it from actual monthly posting, so the new context is concrete DIP/Strategy evidence.

## Explain milestone 9 (14 November)

Run `mvn -Dtest=MonthlyInterestTest test`, then `mvn clean verify`.

- Account receives a nonnull InterestPolicy; ordinary constructors compose default policies,
  optional five-argument constructors accept an override. Account depends only on the interface.
- Walk applyMonthlyInterest: frozen/date/month-end/chronology/duplicate checks, one calculation,
  result validation, prepared immutable entry/ledger, then guarded commit. No system clock is read.
- Show `defaultPolicyPostsTheCorrectRateAndOneCompleteInterestEntry` and
  `ordinarySameDayPostingsDoNotPermitReapplicationOrRecallThePolicy`.
- `zeroCreditIsRecordedAndStillProtectsTheProcessedMonth` explains the ledger-as-month-marker.
- `invalidPolicyResultPreservesTheMonthAndSequenceForAValidRetry` and
  `unexpectedPolicyFailureIsWrappedWithItsCauseAndAllowsRetry` demonstrate failure contracts.

Why no separate processed-month set? The INTEREST entry is already an immutable authoritative
record; publishing it with balance makes the marker atomic. What about same-day later deposits?
They are allowed, but the earlier interest is not recomputed. What does getMonthlyInterestRate
mean with a custom algorithm? The default product rate, not introspection of an arbitrary policy.
What if a callback mutates an account itself? It violates InterestPolicy's pure contract; its
external side effects are not rolled back. Frozen/stale guards still stop invalid prepared interest
publication. State-preservation tests use controlled output/failure doubles, not arbitrary callbacks.

## Explain milestone 10 (15 November)

Run `mvn -Dtest=MonthlyInterestBoundaryTest test`. Explain why 100% line coverage did not establish
posted rounding or recovery after a previously successful month. Show
`postedCreditsAndBalancesRespectHalfEvenTiesAndTheirNeighbors`: checking 1005.00 credits 1.00,
while 1015.00 credits 1.02; the ledger and actual balance must agree with the rounded quote.
Show `failedLaterMonthKeepsPriorMarkersAndAllowsAnEarlierMonthThenRetry`: a failed March
calculation must retain January's marker without preventing February or reserving March/IDs.
No production fix was needed. The review strengthened evidence rather than inventing a feature.

## Explain milestone 11 (16 November)

Run `mvn -Dtest=BankScenarioTest test`. Show
`twoMonthWorkflowReconcilesExternalFlowsInterestAndPairedTransfers` with the financial table in
INTEGRATION_REVIEW.md. The total 1607.70 equals initial 1500 + deposits 200 - withdrawal 100 +
interest 7.70. Explain why paired transfers change individual balances but conserve that total,
and why their shared reference differs from each leg's local posting ID. The test helper rebuilds
running balances from facts; explicit expected totals check the actual financial result too.
Show `sameDayTransferAfterInterestCannotReopenEitherProcessedMonth`: an ordinary transfer entry
cannot erase the INTEREST marker. No new production abstraction was needed to pass these cases.

## Explain milestone 12 (17 November)

Use DESIGN_REVIEW.md and Account's class Javadoc. For an active 250.00 balance, withdrawing
availability leaves checking at 0.00 and savings at 100.00. The client uses Account and an eligible
supplied date; it needs no instanceof branch. Point to
`permitsWithdrawalOfExactlyTheAvailableBalance` and
`withdrawalOneCentOverAvailableBalancePreservesState` for both success and rejection evidence.
LSP does not promise that the raw balance is entirely spendable or prove future subclasses safe.

Trace Template Method through final deposit/withdraw, preparePosting and the minimum hook;
trace Strategy through Account's injected InterestPolicy and concrete default composition.
Explain each SOLID row using a real member. A new policy changes calculation without changing
eligibility, but a new Bank product still needs a creation API. Named opening methods are not
GoF Factory Method and the frozen flag is not State. No additional pattern framework was needed.

## Explain milestone 13 (18 November)

Use API_EXCEPTION_AUDIT.md to locate a successful and rejected operation, then show its exact
test selector. Explain why constructor line coverage alone does not prove an exception was thrown:
each of the eleven concrete types has an operation/assertThrows witness. For example, run
`mvn -Dtest=MonthlyInterestTest#unexpectedPolicyFailureIsWrappedWithItsCauseAndAllowsRetry test`
to show the real wrapper path, retained cause, unchanged state and retry.

The audit accounts for 60 public callables, including generated/basic/carrier declarations;
24 are domain-operation/constructor declarations. A no-input snapshot getter cannot fail in valid
state: its returned collection rejects mutation with a JDK exception. Do not invent a banking
failure for that getter or miscount that JDK exception as a custom type. All 639 cases pass;
the documented exhaustive-enum fallback is the only missed branch. Coverage is supporting
evidence, not proof for all possible inputs. No new behavioral test was warranted by this audit.

## Explain milestone 14 (19 November)

Run `mvn -Dtest=InputContractTest test`. Show
`insufficientTransferNamesTheRequestedOperationAndKeepsRetryState`: the old `cannot withdraw`
message failed the new test; the corrected `cannot transfer` identifies the actual request.
The withdrawal test preserves its old wording. Both use normalized money and verify rejection
before a valid earlier-date retry with consecutive IDs/first transfer reference.

Explain the phase boundary with `targetLookupPrecedesFrozenSourceAndInvalidPostingInputs`:
Bank resolves both accounts before preparing any leg, so an absent recipient is reported before
examining a frozen source or bad amount/date. Direct Account methods still check frozen state
first. This is not a global frozen-first rule across two accounts. Use INPUT_MUTATION_REVIEW.md
to distinguish guard order, full allocation/prevalidation and actual immutable-ledger publication.
No financial mutation-order defect was found; the existing boundary remains justified.

## Proposed final five-minute outline (to refine in the defense session)

0:00–0:40: scope and constraints: classes/tests only, in-memory, two account types.
0:40–1:35: show final UML: Bank coordinates, Account holds invariants, concrete types vary rules.
1:35–2:25: show atomic transfer success/failure tests and explain why both sides validate first.
2:25–3:15: show interest strategy, rounding, explicit date and duplicate-month test.
3:15–4:10: explain Template Method and all five SOLID mappings using actual method names.
4:10–5:00: show exception hierarchy, run tests, state limits and explain one modification.

## Likely questions

Why BigDecimal? Decimal financial values avoid binary floating-point rounding artifacts; create
from strings and make rounding explicit for interest, not silent for invalid user input.
Why no main? The professor requires JUnit to be the only way domain code executes.
Why unchecked exceptions? This in-memory API has rejected operations, not checked I/O;
callers may handle BankingException while retaining precise subtype information.
Why immutable snapshots? Encapsulation includes collections; an unmodifiable live view still
changes when the bank changes, while a snapshot preserves the retrieval result.
Why no database/repository framework? It adds scope without a stated requirement.
How can date tests be repeatable? Supply fixed LocalDate values; never use now() or system time.
What does atomic transfer mean here? Validation completes for both accounts before either
changes, within single-threaded in-memory execution; no concurrency/persistence guarantees.
Does inheritance alone prove LSP? No: each subtype must satisfy the base contract and shared tests.
What would you change to add a rate? Supply another validated InterestPolicy through a concrete account's policy constructor.

Practice changing a real rule (for example, savings floor) and adjusting the boundary tests.
Explain why each expected result changes before running the tests. Memorization is insufficient.

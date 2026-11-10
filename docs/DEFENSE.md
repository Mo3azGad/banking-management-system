# Oral defense preparation — interim

Milestone 5 adds account freezing, repeated-freeze errors and frozen-posting protection; transfers and monthly interest remain unimplemented. Do not present planned
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
- Transaction creation is validation only; Account executes dated deposits/withdrawals, while Bank transfer coordination remains future work.
- All dates are supplied. Record validation cannot determine account chronology without account
  state; that validation belongs in the posting workflow.

Run `mvn -Dtest=AccountTest,TransactionTest test`. Show
`permitsOpeningExactlyAtTheAccountFloor`, `rejectsOpeningOneCentBelowTheAccountFloor`, and
`rejectsTransferWithoutLinkReference`. Explain the rule before the expected assertion.

## Explain and demonstrate milestone 3

Run `mvn -Dtest=AccountOperationsTest test`, then `mvn clean verify` for the full suite.

- `deposit` and `withdraw` are final entry points into Account's private posting template.
  Show validation, date bound, calculation, minimum hook, Transaction construction and commit.
- `permitsWithdrawalOfExactlyTheAvailableBalance` demonstrates different floors through Account.
- `withdrawalOneCentOverAvailableBalancePreservesState` proves rejection leaves the ledger intact.
- `chronologyTracksTheLatestWithdrawalAsWellAsDeposits` shows that either posting advances the bound.
- `failedLaterDatedWithdrawalDoesNotAdvanceLedgerDateOrSequence` shows rejected operations do not
  affect the next valid operation.
- `previouslyRetrievedHistoryRemainsUnchangedAfterPosting` demonstrates snapshot encapsulation.

Why include an opening entry? It establishes a complete starting balance and chronology bound.
Why account-local IDs? The pair (accountId, transactionId) is unique within a registered bank;
independent accounts need no static counter or Bank service to post. Transfer pairing will use
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
is independent. Future transfers and interest must check frozen status, but those operations
are not implemented yet and cannot honestly be claimed as tested.

## Final five-minute outline (planned demo, not available yet)

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
What would you change to add a rate? Supply another validated InterestPolicy after that interface exists.

Practice changing a real rule (for example, savings floor) and adjusting the boundary tests.
Explain why each expected result changes before running the tests. Memorization is insufficient.

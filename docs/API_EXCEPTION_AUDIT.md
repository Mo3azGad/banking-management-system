# Public API and custom-exception audit — 18 November 2026

Scope: compare the professor's PDF, current source/compiled declarations, named JUnit cases and
JaCoCo evidence. All required banking operations exist. No missing successful/rejected operation
contract or unthrown custom exception was found. The evidence gap was traceability: the existing
test matrix described behaviors without a complete named-operation/exception index. This index
closes that gap. Two exception comments were corrected to describe already-implemented behavior:
InvalidPostingDateException also covers a non-month-end interest date; InsufficientFundsException
also covers outgoing transfers. No executable code, test, public signature or UML relationship changes.

## Inventory and counting rules

JDK `javap -public` against all public top-level types reports **60 directly declared public
callables**. Inherited members are not counted again. Counts include overloads, abstract/interface
declarations, implicit Bank construction and compiler-generated record/enum methods.

| Type(s) | Direct declarations | Classification |
|---|---:|---|
| Bank | 9 | Implicit constructor plus eight registry/transfer operations |
| Customer | 3 | Constructor plus two basic getters |
| Account | 13 | Six substantive posting/query operations plus seven metadata/state getters or abstract hooks |
| CheckingAccount, SavingsAccount | 8 | Four constructors total; four floor/rate overrides |
| InterestPolicy, FixedRateInterestPolicy | 3 | Interface and implementation calculate declarations; fixed-policy constructor |
| Transaction | 11 | Canonical constructor, seven component accessors, generated equals/hashCode/toString |
| TransactionType | 2 | Generated values/valueOf; enum constants are fields, not callables |
| Eleven concrete exception types | 11 | Message/cause carrier constructors |
| Abstract BankingException | 0 | Its two constructors are protected, exercised through thrown subtypes |
| **Total** | **60** | **24 domain-operation/constructor declarations; 36 basic/generated/carrier declarations** |

The 24 declarations are indexed below. Interface calculate and its implementation are two
declarations of one contract. Constructor overloads are counted separately even when delegated.
Basic getters and generated value utilities have no custom business algorithm/failure contract;
facts are checked through model/operation assertions. Exception carrier constructors are exercised
when operations throw; they are not specified to reject missing message/cause arguments. The
package-private BankingInput and PreparedPosting APIs are outside the public inventory but their
validation/commit/stale/frozen behavior remains covered by the suite.

## Success and failure evidence

Selectors use `TestClass#methodName`; parameterized methods include every invocation in their
fresh passing report. These are representative named witnesses, not a claim to enumerate every
possible input or replace the broader TEST_MATRIX.md cases.

| Declaration | Successful witness | Rejection / boundary witness |
|---|---|---|
| Bank() | `BankTest#newBankHasEmptyCustomerSnapshot`; `BankAccountTest#newBankHasAnEmptyAccountSnapshot` | No input or valid-state constructor failure condition |
| Customer(id, name) | `CustomerTest#createsCustomerWithProvidedIdentity` | `CustomerTest#rejectsMissingCustomerIdentifier`; `CustomerTest#rejectsMissingCustomerName` |
| CheckingAccount four-argument constructor | `AccountTest#permitsOpeningExactlyAtTheAccountFloor` | `AccountTest#rejectsOpeningOneCentBelowTheAccountFloor`; `AccountTest#rejectsFractionsOfACentInsteadOfRounding` |
| SavingsAccount four-argument constructor | `AccountTest#permitsOpeningExactlyAtTheAccountFloor` | `AccountTest#rejectsOpeningOneCentBelowTheAccountFloor`; `AccountTest#rejectsMissingOwner` |
| CheckingAccount five-argument constructor | `MonthlyInterestTest#injectedPolicyReceivesThePrePostingBalanceAndOverridesTheDefaultAlgorithm` | `MonthlyInterestTest#nullPolicyIsRejectedByTheInjectedConstructor` |
| SavingsAccount five-argument constructor | `MonthlyInterestTest#injectedPolicyReceivesThePrePostingBalanceAndOverridesTheDefaultAlgorithm` | `MonthlyInterestTest#nullPolicyIsRejectedByTheInjectedConstructor` |
| FixedRateInterestPolicy(rate) | `FixedRateInterestPolicyTest#appliesTheMonthlyDecimalRateWithoutAnnualizationOrInventedRateCap` | `FixedRateInterestPolicyTest#rejectsMissingRateWithAClearDomainException`; `FixedRateInterestPolicyTest#rejectsNegativeMonthlyRates` |
| Transaction canonical constructor | `TransactionTest#retainsValidTransactionFacts` | `TransactionTest#rejectsNonpositiveTransactionIdentifier`; `TransactionTest#rejectsCreditLargerThanResultingBalance`; `TransactionTest#rejectsTransferWithoutLinkReference` |
| Bank.registerCustomer | `BankTest#registrationMakesCustomerAvailableForLookup` | `BankTest#duplicateRegistrationKeepsOriginalCustomer`; `BankTest#rejectsInvalidRegistrationNameWithoutChangingRegistry` |
| Bank.findCustomer | `BankTest#lookupStripsSurroundingWhitespace` | `BankTest#lookupRejectsUnregisteredIdentifier`; `BankTest#lookupRejectsMissingIdentifier` |
| Bank.getCustomers | `BankTest#customerSnapshotPreservesRegistrationOrder`; `BankTest#customerSnapshotDoesNotChangeAfterLaterRegistration` | No valid-state method failure; returned-list mutation rejected in `BankTest#customerSnapshotRejectsExternalModification` |
| Bank.openCheckingAccount | `BankAccountTest#openingPublishesTheReturnedAccountWithTheCanonicalRegisteredOwner` | `BankAccountTest#constructorRejectionPreservesRegistriesAndLeavesIdReusable`; `BankAccountTest#duplicateOpeningPreservesTheOriginalOwnerBalanceAndLedger` |
| Bank.openSavingsAccount | `BankAccountTest#savingsOpeningReturnsSavingsAccountAndAcceptsExactMinimum` | `BankAccountTest#savingsBelowMinimumIsRejectedWithoutReservingId`; `BankAccountTest#rejectsUnknownOwnerWithoutRegisteringAnAccount` |
| Bank.findAccount | `BankAccountTest#lookupRetainsTheRegisteredPostingStateAndSequence` | `BankAccountTest#lookupRejectsUnregisteredAccount`; `BankAccountTest#lookupRejectsMissingAccountIdentifier` |
| Bank.getAccounts | `BankAccountTest#accountSnapshotPreservesOpeningOrderAcrossTypesAndOwners`; `BankAccountTest#priorAccountSnapshotDoesNotGainLaterAccounts` | No valid-state method failure; returned-list mutation rejected in `BankAccountTest#accountSnapshotRejectsExternalMembershipChanges` |
| Bank.transfer | `BankTransferTest#transferConservesMoneyAndPostsOneLinkedEntryPerAccount` | `BankTransferTest#dateValidForSourceButBeforeTargetOpeningCannotPartiallyDebit`; `BankTransferTest#eitherFrozenAccountBlocksTransferWithoutChangingEitherLedger` |
| Account.freeze | `AccountFreezeTest#firstFreezeTransitionsAnActiveAccountToFrozen` | `AccountFreezeTest#repeatedFreezeThrowsTypedFailureAndPreservesState` |
| Account.getAvailableBalance | `AccountTest#availableBalanceRespectsThePolymorphicAccountFloor`; `AccountFreezeTest#freezingPreservesFinancialStateAndExistingHistory` | No input/failure for a valid supplied subtype; frozen availability remains readable and grants no posting permission |
| Account.deposit | `AccountOperationsTest#depositUpdatesBalanceAndReturnsTheRecordedEntry` | `AccountOperationsTest#invalidAmountPreservesBalanceHistoryAndNextIdentifier`; `AccountOperationsTest#dateBeforeLatestPostingIsRejectedEvenIfAfterOpening` |
| Account.withdraw | `AccountOperationsTest#permitsWithdrawalOfExactlyTheAvailableBalance` | `AccountOperationsTest#withdrawalOneCentOverAvailableBalancePreservesState`; `AccountFreezeTest#frozenAccountRejectsValidPostingsWithoutChangingLedger` |
| Account.applyMonthlyInterest | `MonthlyInterestTest#defaultPolicyPostsTheCorrectRateAndOneCompleteInterestEntry` | `MonthlyInterestTest#repeatedMonthRejectsWithoutChangingBalanceOrHistory`; `MonthlyInterestTest#invalidPolicyResultPreservesTheMonthAndSequenceForAValidRetry` |
| Account.getTransactionHistory | `AccountOperationsTest#historyKeepsChronologicalPostingOrderAndRunningBalances`; `AccountOperationsTest#previouslyRetrievedHistoryRemainsUnchangedAfterPosting` | No valid-state method failure; returned-list mutation rejected in `AccountOperationsTest#historySnapshotRejectsExternalMutation` |
| InterestPolicy.calculate | `FixedRateInterestPolicyTest#halfEvenRoundingHandlesBothTieDirectionsAndAdjacentValues` | `FixedRateInterestPolicyTest#rejectsMissingNegativeOrSubCentBalances` through the actual implementation |
| FixedRateInterestPolicy.calculate | `FixedRateInterestPolicyTest#retainsRatePrecisionUntilTheFinalInterestRounding` | `FixedRateInterestPolicyTest#unsupportedDecimalScaleIsReportedAsACustomException`; `FixedRateInterestPolicyTest#zeroRateDoesNotBypassBalanceValidation` |

Ordinary account constructors delegate to the injected overloads, so their invalid-field/floor
tests exercise the same constructor bodies. Explicit override and null-policy cases verify the
additional parameter directly. Tests do not claim every cross-product of invalid fields/policies.
Interface evidence concerns the supplied implementation/context; arbitrary external policies
must obey the documented pure contract. Returned-list mutation raises the JDK's
UnsupportedOperationException; the snapshot getter itself does not fail. No invented failure is
required for a valid-state no-input query. Generated enum parsing/value utilities are not new
application operations or custom banking failures.

## Every concrete custom exception is actually thrown

Each witness calls an operation and uses assertThrows for the listed type; simply constructing
an exception object or covering its constructor line is not the evidence used here.

| Exception | Actual throwing operation / condition | Passing witness |
|---|---|---|
| InvalidBankingInputException | Customer constructor rejects missing identity | `CustomerTest#rejectsMissingCustomerIdentifier` |
| DuplicateCustomerException | Bank registration rejects normalized duplicate | `BankTest#duplicateRegistrationKeepsOriginalCustomer` |
| CustomerNotFoundException | Bank lookup rejects absent customer | `BankTest#lookupRejectsUnregisteredIdentifier` |
| DuplicateAccountException | Bank opening rejects occupied account ID | `BankAccountTest#duplicateOpeningPreservesTheOriginalOwnerBalanceAndLedger` |
| AccountNotFoundException | Bank lookup rejects absent account | `BankAccountTest#lookupRejectsUnregisteredAccount` |
| InsufficientFundsException | Withdrawal would exceed availability | `AccountOperationsTest#withdrawalOneCentOverAvailableBalancePreservesState` |
| InvalidPostingDateException | Posting precedes opening; interest date is not month-end | `AccountOperationsTest#dateBeforeOpeningPreservesBalanceAndHistory`; `MonthlyInterestTest#nonMonthEndDateRejectsWithoutChangingStateOrCallingThePolicy` |
| AccountFrozenException | Repeated freeze is forbidden | `AccountFreezeTest#repeatedFreezeThrowsTypedFailureAndPreservesState` |
| StalePostingException | Internal committed plan cannot be validated/committed again | `PreparedPostingTest#committedPlanCannotBeValidatedOrCommittedAgain` |
| InterestAlreadyAppliedException | Current eligible month has an existing INTEREST marker | `MonthlyInterestTest#repeatedMonthRejectsWithoutChangingBalanceOrHistory` |
| InterestCalculationException | Account wraps an unexpected policy runtime failure, retaining cause | `MonthlyInterestTest#unexpectedPolicyFailureIsWrappedWithItsCauseAndAllowsRetry` |

All eleven extend unchecked BankingException; the abstract base cannot be directly instantiated.
Its message constructor is exercised by typed failures; its message/cause constructor is exercised
by the wrapper witness. Unchecked rejection fits this in-memory API; no checked I/O is hidden.
The injected failure witness deliberately causes a policy exception, then exercises Account's real
catch/wrap/throw path, preserved state and retry. VM/resource failure is not claimed covered.

## Fresh coverage, findings and remaining work

Full Maven clean verify passes **639 tests**, zero failures/errors/skips. JaCoCo reports
238/238 lines, 1046/1046 instructions, 68/69 branches, 70/70 methods and 23/23 executable classes.
The only missed branch is Transaction's constructor enum switch at source line 24. All six legal
types and null rejection are tested; the compiler-generated fallback is retained/documented.
InterestPolicy has no executable body, so its absence from executable class counts is expected.
Coverage and matching selectors alone do not prove assertion quality or all-input correctness;
the source bodies and success/rejection assertions were also reviewed this session.

No redundant tests or behavior changes are added for an already-covered contract. Existing tests
continue to prove validation-state preservation, immutable snapshots, paired transfers, date
parameters, product rules and monthly interest. Public/application/test sources have no prohibited
execution/clock APIs. The implementation UML remains accurate; the interim DOCX and test matrix
now reference this complete index. The next proposed session reviews input/error contracts and
mutation order in more detail. Final UML/report/defense and the genuine two-week span remain
unfinished; this audit is neither proof of future runs nor professor acceptance.

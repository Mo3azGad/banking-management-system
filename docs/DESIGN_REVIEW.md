# Shared subtype and SOLID/pattern review — 17 November 2026

Scope: examine the implemented Account contract, its existing shared tests and the design claims
for the oral defense. The professor requires patterns/SOLID and testable individual classes; the
specific rates, floors and architecture are our choices. No new business behavior or test case
is needed to describe contracts already exercised by the 639-case suite. The substantive finding
is that the base-class documentation omitted obligations needed to assess substitution and
extension accurately. Account Javadoc now states them explicitly; executable code is unchanged.

## Shared Account contract

| Contract through an Account reference | Success / postcondition | Rejection / limitation | Existing evidence |
|---|---|---|---|
| Construct valid subtype | Opening balance is at least the product floor; one OPENING fact | Savings 99.99 and checking -0.01 fail; constructor requirements vary by product | AccountTest.permitsOpeningExactlyAtTheAccountFloor / rejectsOpeningOneCentBelowTheAccountFloor |
| Deposit | Active, positive exact-cent amount, supplied nondecreasing date; balance/history publish together | Invalid amount/date/frozen state leaves financial state unchanged | AccountOperationsTest.depositUpdatesBalanceAndReturnsTheRecordedEntry and shared invalid-input/date cases |
| Withdraw | Same amount/date/active conditions; amount is at most available balance; floor preserved | One cent over availability fails without consuming an ID | AccountOperationsTest.permitsWithdrawalOfExactlyTheAvailableBalance / withdrawalOneCentOverAvailableBalancePreservesState |
| Apply monthly interest | Active, valid month-end/chronology, unprocessed YearMonth; pure policy result is posted, including zero | Invalid/duplicate/frozen eligibility and policy failure preserve ledger/month/sequence without external side effects | MonthlyInterestTest and MonthlyInterestBoundaryTest shared cases |
| Freeze | First call permanently blocks postings; reads remain usable | Repeat call fails; no unfreeze is promised | AccountFreezeTest and BankScenarioTest.freezingAfterTransfersAndInterestProtectsHistoryAndIsolatesOtherAccounts |
| Read history / availability | Immutable history snapshots; availability equals balance minus floor | Availability on a frozen account grants no posting permission; Bank membership snapshots contain mutable account objects | AccountOperationsTest.historySnapshotRejectsExternalMutation and AccountFreezeTest.frozenHistoryRemainsReadableAndRejectsExternalMutation |

For an active account at balance 250.00, the same client algorithm can read available balance,
withdraw it if positive with an eligible supplied date, and expect zero availability afterwards.
Checking withdraws 250.00 and retains 0.00; savings withdraws 150.00 and retains 100.00. No
instanceof branch or cast is needed in that client. A client that assumes all raw balance is withdrawable
assumes a stronger contract than Account offers. Different product constructor requirements do
not change the preconditions of an operation on an already valid Account.

Future subtype authors must establish the floor during construction and return stable,
nonnegative exact-cent floor metadata without side effects. The base constructor intentionally
does not call overridable hooks. The supplied final checking/savings types meet those obligations;
tests do not prove arbitrary future subclasses will do so. Public/internal posting entry points
are final, keeping validation and ledger publication under Account's control.

## Five SOLID principles with actual boundaries

| Principle | Implemented evidence | Qualification for the defense |
|---|---|---|
| SRP | Bank owns registries/transfer coordination; Account owns ledger/lifecycle/eligibility; FixedRateInterestPolicy owns arithmetic; Transaction owns fact validation | One responsibility is a coherent reason to change, not one method per class. Separate services for every operation are not required here. |
| OCP | Account.preparePosting uses the getMinimumBalance hook; applyMonthlyInterest calls injected InterestPolicy.calculate without knowing the algorithm | Another policy can change calculation without changing eligibility/publication. A new Bank-supported product still needs intentional creation/registration API work; the entire application is not modification-free. |
| LSP | The shared operation/freeze/monthly tests call through Account references and preserve availability, date, history and rejection contracts for both supplied subtypes | Savings does not break substitution by retaining its floor: base withdrawal permission is defined using availability, not raw balance. Inheritance alone is not evidence. |
| ISP | InterestPolicy has only calculate(balance); Account's calculation dependency has no freeze, registry, date, storage or UI duties | Avoiding a UI alone is not proof of ISP. This one-operation interface is the concrete evidence; artificial interfaces for every entity are unnecessary. |
| DIP | Account's constructor/field/call use InterestPolicy. CheckingAccount/SavingsAccount constructors compose default FixedRateInterestPolicy or accept an override | The composition boundary may know concrete algorithms. BankingInput and private maps are helpers/storage, not invented inversion abstractions. |

MonthlyInterestTest.injectedPolicyReceivesThePrePostingBalanceAndOverridesTheDefaultAlgorithm
demonstrates real context substitution, not just standalone strategy quotations. The retained
getMonthlyInterestRate getter describes the default product rate; callers cannot infer the
behavior of an arbitrary injected algorithm from it. InterestPolicy requires a pure calculation;
Account validates results but does not roll back a callback's external mutations.

## Patterns actually present

**Template Method:** Account's final deposit/withdraw and internal transfer preparers enter one
private preparePosting workflow: active/input/date validation, credit/debit computation, floor
check, complete entry/ledger preparation and guarded publication. getMinimumBalance supplies the
polymorphic rule. The template is private behind final entry points; a public abstract template
method is not required. Monthly interest has different eligibility/zero rules and shares only
the appropriate chronology/entry helpers; it is not forced through positive-only deposit logic.

**Strategy:** InterestPolicy is the algorithm contract, FixedRateInterestPolicy the concrete
algorithm and Account the context. Product constructors compose defaults; explicit overloads
inject alternatives. Date/month eligibility stays with Account, while arithmetic stays pure.
OCP/DIP evidence comes from that production call and the injected-context tests.

Named Bank opening methods perform creation/registration, but do not demonstrate the GoF Factory
Method pattern (no overridable creator choosing products). A private frozen boolean is not the
State pattern. Maps do not by themselves establish Repository, and the prepared posting boundary
is not distributed two-phase commit. Do not inflate the pattern count in the defense.

## Complexity and review decision

Retain the current small design. PreparedPosting and immutable LedgerState support the real
two-account validation/publication requirement; replacing them with public withdraw then deposit
would reintroduce partial failure. A separate processed-month set would duplicate authoritative
INTEREST history. A State framework, repository layer or generic product factory has no current
requirement that justifies it. Repeated small test factories keep fixtures local; a new hierarchy
solely to share construction would add indirection without removing duplicated business rules.

No behavioral defect or justified simplification was found, so no executable refactor or redundant
test was added. The existing shared assertions are the evidence, and the full suite is rerun for
this session. The UML's classes/signatures/relationships remain accurate and are retained. The
interim Word report gains precise SOLID/pattern qualifications. The public-operation/exception
and input/mutation-order audits, final artifacts, defense and authentic two-week span remain
unfinished. Nothing here certifies future automated sessions or the professor's acceptance.

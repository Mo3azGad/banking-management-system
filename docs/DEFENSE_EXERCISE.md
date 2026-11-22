# Independent modification exercise — policy override

This is a task for you to perform and explain independently, outside the five-minute speaking
draft. Preparation does not mean you have completed it. No exercise implementation or extra
JUnit cases have been added to the submitted project; the 659-case baseline is unchanged.
It is not a feature prepared for a delayed future push or a requirement invented for the PDF.

## Prompt — attempt before reading the answer checklist

Allow **one standalone savings account** to use a 0.2% monthly rate (decimal multiplier 0.002),
while normal savings accounts still use 0.3% and keep their 100.00 floor. Use the existing
constructor/strategy extension point. Do not edit the shared default rate, change Account's
eligibility/publication workflow, add a Bank import API or read the system clock.

Before editing, find the relevant constructor, the actual calculation call and the metadata
getter. Explain which is the composition boundary and why changing a product constant would
affect a different scope. Predict every monetary result below on paper.

When practising, use a local practice branch from the existing repository; preserve the submitted
history and leave user changes intact. Do not force-push, replace commits, change timestamps or
claim practice work was previously completed. Keeping the practice separate from submission
lets you compare the actual change without changing the professor's agreed product defaults.

Write your own JUnit 5 tests in package edu.banking. Use a real Customer and a fresh account per
behavior, initial balance 1000.00, opening 2024-01-15, January posting 2024-01-31 and February
posting 2024-02-29. Describe each behavior in its test name; multiple assertions may establish
that single behavior's complete postcondition.

| Behavior to establish | Predict before running |
|---|---|
| Custom January posting | Which five-argument constructor and policy achieve the change? Predict amount, new balance, type, date, local ID, transfer reference and history length. |
| Unchanged product default | Construct a separate ordinary four-argument savings account. Predict its January credit and both accounts' getMonthlyInterestRate() values. |
| Repetition preserves state and permits February | Snapshot balance/history after January. Repeat January, assert the precise custom exception, then apply February and predict amount/balance/next ID. |
| Frozen permission still applies | On a separate fresh custom-policy account, freeze before interest. Predict exception, unchanged history/balance and whether any interest marker exists. |
| Product floor still applies | On a separate fresh custom-policy account, withdraw exactly availability on opening day; then try another 0.01 on that same date. Predict retained balance, exception and ledger preservation. |

Run your focused exercise tests, then the complete `mvn clean verify` on the practice branch.
If they fail, explain which prediction or implementation is wrong. Do not relax existing tests
just to turn them green. The practice suite count may increase; never report it as an already
verified change to the submitted baseline. Discuss what documentation would need changing if a
real product-wide rule changed, rather than treating an account-specific override as that change.

<details>
<summary>Answer checklist — open after your independent attempt</summary>

- Compose FixedRateInterestPolicy with BigDecimal constructed from the string "0.002" and
  supply it to SavingsAccount's five-argument constructor. Direct construction is deliberate:
  Bank's public opening methods select default policies and there is no arbitrary-account import.
  The exercise does not require registering this standalone account in a Bank.
- For January: 1000.00 × 0.002 = 2.00000, rounded once to **2.00**; balance **1002.00**.
  The returned entry is INTEREST, ID **2**, date **2024-01-31**, empty transfer reference and
  history length **2**. Opening remains ID 1. The original opening snapshot remains unchanged.
- The ordinary savings account credits **3.00** and ends at **1003.00**. Both getters still
  report the default product multiplier **0.003**; the getter is not an introspection API for
  the overridden algorithm. The custom account's actual January rate is supplied by its policy.
- Repeated January throws **InterestAlreadyAppliedException**; balance/history stay as captured.
  February uses 1002.00 × 0.002 = 2.00400, HALF_EVEN to **2.00**, ending at **1004.00**, ID **3**.
  There is no skipped ID, hidden fractional-cent carry or reopened January marker.
- A fresh frozen account rejects monthly application with **AccountFrozenException** and stays
  at **1000.00**, with only OPENING ID 1 and no INTEREST entry. Freezing itself creates no entry.
- A fresh custom savings account has availability **900.00**. Withdrawing that amount leaves
  **100.00** and records WITHDRAWAL ID 2. Another 0.01 throws **InsufficientFundsException**;
  balance/history stay unchanged. The strategy override changes arithmetic, not the product floor.
- Account still depends only on InterestPolicy. This is Strategy/OCP/DIP; the unchanged shared
  posting guards and available-balance contract support Template Method/LSP. No new production
  abstraction or change to default product metadata is needed for this scoped requirement.

This checklist supplies expected outcomes and reasoning, not a completed test implementation.
Use `assertThrows` for typed rejections and compare captured state. If you cannot explain why
each assertion follows from the contract, repeat the exercise without the checklist.

</details>

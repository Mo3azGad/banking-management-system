# Oral defense preparation — interim

This milestone cannot yet demonstrate a complete banking system. Do not present planned
classes as implemented. The final defense carries 30 points and attendance is compulsory.

## Explain the current code

- Customer constructor establishes valid immutable identity; requireText rejects missing data.
- registerCustomer validates first, checks uniqueness, and changes the map only on success.
- findCustomer normalizes input and throws a clear typed exception when the identity is absent.
- getCustomers copies the map values, preventing callers changing bank state or old snapshots.

Run `mvn -Dtest=CustomerTest,BankTest test`. Walk through duplicateRegistrationKeepsOriginalCustomer
and customerSnapshotDoesNotChangeAfterLaterRegistration. Assertions on unchanged state are part
of a single rejection/isolation behavior, rather than unrelated checks.

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
Why unchecked exceptions? This in-memory API has invalid operations, not recoverable checked I/O;
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

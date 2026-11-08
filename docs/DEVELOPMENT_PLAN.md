# Genuine development plan

Start: 8 November 2026, Europe/Istanbul. Submission: 7 December 2026 on UZEM.
These are proposed sessions, not scheduled jobs. No unattended execution or future push has
been configured. Return for each session and authorize the concrete next step below.
Build and test immediately, rather than waiting for the designated testing days.

| Day/date | Exact next step for that session | Verification / possible commit after real work |
|---|---|---|
| 1 — Nov 8 | Read PDF; document obligations/rules; create Maven/customer foundation | `mvn clean verify`; requirements and tested customer registration |
| 2 — Nov 9 | Review account contracts, withdrawal floors, exception cases and proposed UML | Verify current suite; document reviewed account contracts |
| 3 — Nov 10 | Implement immutable Transaction and its input/date validation tests | Verify; add validated transaction records |
| 4 — Nov 11 | Implement Account state, checking/savings constructors and opening constraints | Verify; add account types and opening invariants |
| 5 — Nov 12 | Implement positive BigDecimal deposits and dated postings | Verify; add tested account deposits |
| 6 — Nov 13 | Implement withdrawal floors and freeze behavior, including equality boundaries | Verify; enforce withdrawal limits and frozen states |
| 7 — Nov 14 | Add Bank account registration/opening and immutable histories | Verify; coordinate opening and transaction history |
| 8 — Nov 15 | Implement prepared atomic transfers with paired history entries | Verify; add transfers and failure-state preservation |
| 9 — Nov 16 | Audit exception types/messages and prevalidation ordering; fix real gaps | Verify; commit actual error-contract improvements only |
| 10 — Nov 17 | Implement InterestPolicy strategy, monthly rounding and duplicate-month protection | Verify; add monthly interest policies |
| 11 — Nov 18 | Audit every public operation against success/failure matrix; add missing edge tests | Verify and inspect JaCoCo; cover identified gaps |
| 12 — Nov 19 | Review polymorphism and transfer invariants with contract tests; simplify weak design | Verify; commit demonstrated testability improvements |
| 13 — Nov 20 | Regenerate final UML from actual code and update documentation/defense notes | Verify diagrams and suite; document final design |
| 14 — Nov 21 | Rebuild from clean checkout, complete five-section DOCX and rehearse five-minute demo | Verify; review submission artifacts and final report |
| Follow-up — Nov 22 | Perform a genuine independent review after at least 14 elapsed days; correct any actual issue or record substantive review findings | Verify; commit only meaningful completed review work |

Days 1–14 alone span 13 elapsed days. The Nov 22 session conservatively addresses the PDF's
'at least 2 weeks' wording. Check the actual first-commit time and do that review at least
14 days later. Do not manufacture a change just to produce a commit. If no meaningful work
remains, ask the professor how they interpret the required span rather than falsifying history.

At each session: inspect `git status`, do the scoped work, run `mvn clean verify`, inspect the
diff, and commit using the real date. Push verified work only after a real GitHub remote exists.
If work advances faster, adjust later sessions to useful review/refinement; never intentionally
ship broken or unfinished methods to preserve this schedule. No empty placeholder methods.

Immediate next prompt: "Review the proposed account rules and implement Transaction with
JUnit success and failure tests; run Maven verify and update the UML for the implemented model."

# Independent final review — 23 November 2026

Review baseline: main at 57b9c8c6ed788a36408a3bd9af08cfde9a6c6bf5, clean and equal to the
fetched private origin/main. This is the separate final session after the genuine development
period; it does not claim student rehearsal, oral-defense attendance, UZEM upload or grading.
The entire authoritative PDF was rechecked against its exact saved text extraction.

## Source and test findings

No new financial behavior defect or justified architectural simplification was found. The
existing code/tests are retained, without inventing a feature or redundant test to fill a date.
The review re-read the implementation and assessed these concrete boundaries:

| Boundary | Finding and retained guarantee |
|---|---|
| Customer/account registration | Normalized case-sensitive IDs, shared product namespace and canonical registered ownership. Full validation/construction precedes map insertion; failed opening does not reserve an ID. |
| Money, product floors and dates | Exact cents on input; positive ordinary postings; checking floor 0.00 and savings 100.00. Dates are explicit, same-day allowed, latest ledger date enforced. No hidden today/future-date restriction. |
| Shared posting/publication | Frozen/input/date/funds checks precede preparation of complete immutable facts/history/ledger. The account-bound plan checks active status and exact expected ledger identity before one reference assignment. Same-balance round trips still invalidate old plans. |
| Two-account transfers | Both registered distinct accounts, complete legs/receipt and both validations precede either commit. Bank reference advances only after publication. Source and target rejection leave state/counters reusable under the documented single-threaded assumptions. |
| Monthly application | Month-end, chronology and recorded YearMonth checks precede one pure policy call. Validated zero credits also produce an INTEREST marker. No separate mutable month cache or silent catch-up. |
| Policy and failure contracts | Default constructor composition and explicit injection are distinct from default metadata. HALF_EVEN occurs once on the exact final product. Domain exceptions propagate; unexpected policy runtime failures retain their cause. Arbitrary impure callback effects are outside rollback guarantees. |
| Immutable/public facts | History is a nonempty immutable snapshot. Account-list snapshots protect membership, not balances. Transaction construction validates a fact but cannot execute a posting or prove its counterpart. |
| Lifecycle and scope | Freeze is permanent, adds no financial entry/timestamp, and leaves read access available. No persistence, concurrency, authentication or production-bank guarantee is introduced. |

Compiled `javap -public` inventory again totals 60 directly declared public callables on 21 public
top-level types, matching API_EXCEPTION_AUDIT.md (24 domain-operation/constructor declarations;
36 basic/generated/carrier declarations). The audit's 67 unique selectors plus the input review's
seven give 74 named Class#method references; all resolve in source and passing Surefire reports.
Every concrete custom exception has a real assertThrows witness. Valid-state getters and carrier
constructors are not assigned invented failure inputs. No prohibited entry point, console or
system-clock API was found in application/tests.

The full clean Maven verify finished at 2026-11-23T16:25:00Z: 659 invocations, 0 failures/errors/
skips, 239/239 lines, 1054/1054 instructions, 70/71 branches, 70/70 methods and 23/23 executable
classes. The sole missed branch remains the Transaction enum-switch fallback at line 24.
Pure InterestPolicy has no executable body. Coverage does not prove all possible inputs correct.

## Final artifact findings and corrections

1. The 21 November report was internally accurate for its edition, but its cover still described
   the history span and defense preparation as unfinished. The submission edition is refreshed
   to 23 November with actual clean-build evidence and completed technical-review/preparation
   status. Student rehearsal, attendance and upload remain explicit personal actions. The prior
   edition remains in Git history; dated historical evidence is not rewritten.
2. The final delivery needed an unambiguous map between the required Word report, source/tests,
   UML, defense material and the private repository's authentic history. SUBMISSION.md now gives
   those paths, portable build/archive instructions and the remaining user actions. A source ZIP
   is a convenience, not a replacement for the required DOCX or the actual Git repository.
3. Three UML views remain source-accurate. Fresh disassembly/model checks cover all 24 banking
   types plus JDK context, 30 displayed field references, 48 operation references/visibility and
   superclass/interface edges; SVG node sets match. No class/member change warrants redrawing
   the unchanged model. The report embeds new renders from those same tracked DOT sources.
4. DOCX ZIP/XML, five required headings, fourteen suite counts, current metrics, all exceptions,
   representative named test references and three image byte streams are checked. The actual
   final DOCX is rendered to the committed 13-page PDF preview. Every body/table paragraph
   survives extraction; all pages and full-size figures are visually inspected, with contained
   text/images and no figure overlap or extra blank/overflow page. Reflow in another processor
   remains possible, so reopen the DOCX on the submission laptop.

The report builder is documentation tooling; it introduces no domain entry point. Its explicit
timestamp refers to the actual successful clean build, not a future result. REPORT_BUILD.md
records regeneration. The model, Java source, JUnit source and Maven configuration are unchanged.

## Authentic history and package verification

Before this review, the preserved repository had 17 nonempty commits on fifteen Europe/Istanbul
dates, 8–22 November inclusive. The first author/committer timestamp is 2026-11-08T16:23:30Z;
the reviewed parent is 2026-11-22T16:49:39Z: 14 days 00:26:09 apart. All three original hashes
remain ancestors. Today's substantive findings/report/delivery work is committed with real dates;
the exact final remote SHA, ancestry, visibility and updated span are checked after push and
reported in the session result. No history replacement or fabricated activity is used.

The final source ZIP is generated from the actual completed commit outside the repository, then
checked for exact tracked bytes, required deliverables, ZIP integrity and absence of .git,
target/, credentials or external machine tooling. A clean Maven build of its empty-directory
extraction is verified separately after the commit; the exact ZIP SHA-256, commit and build
result accompany the delivered artifact. These post-commit facts are not guessed here. Full
history stays in the existing private GitHub repository, since a source archive is not a clone.

## Remaining personal actions and closed roadmap

Follow SUBMISSION.md: rehearse on your own laptop, complete the independent exercise, confirm
and attend the compulsory individual defense, provide repository access as the course requires,
and upload the required DOCX/source material on UZEM by 7 December 2026. No upload/invitation
or oral attendance has been performed by this review. Professor acceptance is not asserted.

The planned 9–23 November development sequence ends with this review. No additional development
or push is implied after this configured final session. Further changes require a new request;
saved automation configuration is not proof of execution or acceptance.

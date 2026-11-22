# Milestone 16 verification — final five-section technical report

Verified 21 November 2026 (Europe/Istanbul), complete Maven `clean verify`: BUILD SUCCESS.
Eclipse Temurin 21.0.13+9, Java 17 compiler target; Maven 3.9.11; JUnit 5.11.4;
Surefire 3.5.2; JaCoCo 0.8.12. Build finished at 2026-11-21T16:10:03Z.

JUnit: 659 invocations in fourteen suites, 0 failures, 0 errors, 0 skipped.
No Java source, JUnit source, Maven configuration or UML model/export changes were necessary.

- Instructions: 1054/1054 (100.00%).
- Branches: 70/71 (98.59%).
- Lines: 239/239 (100.00%).
- Methods: 70/70 (100.00%).
- Classes: 23/23 (100.00%).

The compiler-generated Transaction enum-switch fallback at line 24 remains the sole missed
branch; every legal type and null rejection are tested. The pure InterestPolicy interface has
no executable body. Every concrete custom exception remains thrown by an operation test.

## Meaningful completed work

final-report.docx replaces the accumulated daily draft with a professional technical report
organized by the five required PDF sections. It includes class descriptions, three source-rendered
UML views, the two actual patterns/problem/fit, unchecked hierarchy and all eleven concrete
failures, all five SOLID principles with actual classes/evidence/limits, and fresh testing evidence,
limitations and testability redesigns. Operation/identity/month-marker/rounding contracts and
single-threaded/pure-policy assumptions match the current source. Planned defense/review and the
unfinished two-week span are explicitly distinguished from completed technical work.

The original interim DOCX is moved unchanged to history/milestone-report-through-nov20.docx;
the previous current verification is preserved byte-for-byte as MILESTONE_15_VERIFICATION.md.
README, requirement status, roadmap and current design/test references point to the final report.
tools/build_report.py regenerates it portably using explicit dates and generated test evidence,
not machine-specific workspace paths. REPORT_BUILD.md records dependencies and reproduction.

## Artifact verification

- DOCX ZIP integrity, exactly five required level-one headings, three inline images/captions,
  all eleven concrete exception names, fourteen actual suite counts and current JaCoCo values.
- All three image byte streams match fresh Graphviz PNGs rendered from the unchanged tracked
  DOT sources. The model remains the source-checked final UML from milestone 15.
- Six fully qualified named report test references and the audit's 67 unique Class#method
  references match actual test source and passing Surefire testcase names. Other representative
  boundary/retry names are checked against their corresponding suites.
- The actual Word file is converted using LibreOfficeDev 26.8.0.0.alpha0 to the committed
  final-report-preview.pdf. All thirteen pages are visually reviewed; each UML page is also
  inspected at full-page size. One wrapped test-count header is corrected and re-rendered.
- Every body/table paragraph survives PDF text extraction, page numbers are 1–13/13, images
  occur exactly on pages 4/5/9, and text/images stay within page bounds without figure overlap.
  All five required report topics and their class/test/coverage claims are reviewed against source.

These document checks are not extra JUnit cases. Rendering emits a nonfatal font-cache warning;
it succeeds and actual output is inspected. Another processor/font environment may reflow Word
pagination; the preview records this inspected layout. No prohibited API or private-state
manipulation is added. Local render/cache/tool/auth artifacts are outside tracked source.

## Repository preflight and genuine history

Existing main was clean; safe fetch matched origin/main exactly at
abfbf905db4d39d2dc6ca8f67b2f43080dd3d53c. Authorized authentication, private GitHub visibility,
push permission and all three original ancestors were checked. Following diff review, a meaningful
real-date commit/push is verified separately against exact remote HEAD, private visibility,
preserved ancestry and clean worktree, and reported in the session result. No history/timestamps
are replaced. Defense refinement and independent review remain; no future execution or professor
acceptance is certified by the report or existing saved schedule.

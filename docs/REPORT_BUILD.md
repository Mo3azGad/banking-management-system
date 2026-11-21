# Final technical report — 21 November 2026

The deliverable is [final-report.docx](final-report.docx). The [PDF preview](final-report-preview.pdf)
is its reviewed rendering, not a replacement for the PDF's required DOC/DOCX submission format.
The final technical report is organized by the five required topics rather than the daily history.
It describes implemented behavior and the remaining genuine-history/defense/review obligations.
The earlier interim draft is preserved byte-for-byte in history/milestone-report-through-nov20.docx.

## Content and evidence

| Required section | Final report contents | Rendered pages |
|---|---|---|
| Design overview | Product rules versus PDF obligations; class responsibilities; three complementary UML views across sections 1/3; important operation workflows and bounded guarantees | 2–6; hierarchy on 9 |
| Patterns | Template Method and Strategy: actual participants, problem, fit, executable evidence and restrained claims | 7 |
| Exceptions | Abstract unchecked base, eleven concrete subtypes, throwing-test suites, clear-message/guard/cause contracts and hierarchy | 8–9 |
| All five SOLID | Class-level explanations and real substitution/injection evidence, with extension limits | 10 |
| Testing | Fourteen suites/counts, current coverage, representative named successes/failures, coverage gaps/limitations, testability changes and reproduction | 11–13 |

Cover page 1 identifies scope, actual verification and outstanding delivery obligations. The source
PDF, current Java implementation, UML_GUIDE.md, TEST_MATRIX.md, API_EXCEPTION_AUDIT.md,
DESIGN_REVIEW.md and generated Surefire/JaCoCo XML were used to check claims. No new feature,
future test result, push or defense completion is asserted by preparing the report.

## Rebuild

The document builder is documentation tooling, not an application entry point. It requires
Python 3.10+, python-docx, Graphviz (`dot`) and the Liberation Sans font. PDF rendering additionally
needs LibreOffice (`soffice`); inspection used PyMuPDF. These are not Java/Maven dependencies.
Install document dependencies through your normal environment configuration; do not commit local
tool binaries, caches, credentials or machine-specific paths.

First run the full build from the repository root:

```sh
mvn clean verify
```

Then use the **actual** successful build completion timestamp and current report edition date.
The checked-in edition was produced with:

```sh
python tools/build_report.py --as-of 2026-11-21 --verified-at 2026-11-21T16:10:03Z
soffice -env:UserInstallation=file:///tmp/banking-report-lo --headless --convert-to pdf --outdir /tmp docs/final-report.docx
cp /tmp/final-report.pdf docs/final-report-preview.pdf
```

The builder reads current Surefire counts and JaCoCo counters, rejects missing/nonpassing/skipped
suite evidence, and embeds freshly rendered images from the three editable DOT sources. Dates are
explicit. Rebuilding does not certify that narrative, versions, dates, selector references or
pagination remain accurate: review them whenever code, toolchain or report content changes.
Use a fresh clean verify; old target/ output is not fresh evidence. The timestamp above records
this edition only and must not be reused as a claim about a later run.

## Checks performed for this edition

- Clean Maven verification: 659 JUnit invocations; no failures/errors/skips. JaCoCo 239/239 lines,
  1054/1054 instructions, 70/71 branches, 70/70 methods and 23/23 executable classes. The pure
  interface contributes no executable class body. The enum fallback remains the sole missed branch.
- DOCX ZIP integrity and WordprocessingML: exactly five level-one required headings, three inline
  figures with captions, all eleven concrete exception names, all fourteen suite names/counts,
  current metrics and required limitation/testability explanations. Figure bytes match fresh
  Graphviz renders of the tracked sources; no changed UML model is introduced.
- Named test references in the report and the complete API/exception audit are checked against
  actual source and passing Surefire testcase names, including parameterized invocations.
- LibreOfficeDev 26.8.0.0.alpha0 converts the actual DOCX to the committed preview. All thirteen
  rendered pages are visually reviewed via page images/contact sheets; each UML figure is also
  inspected at full-page size. The test-count header width is corrected after rendering. Text/table
  layout and image bounds are checked; captions remain with their figures, with no blank/overflow page.
- The original interim DOCX and milestone-15 verification are preserved byte-for-byte. Java source,
  JUnit source and the three UML sources/exports are unchanged. Tools/credentials remain untracked.

The renderer reports a nonfatal font-cache warning under the restricted environment; it exits
successfully and its actual output was inspected. Layout may reflow in another Word processor/font
environment; reopen the DOCX before submission. The PDF preview records the inspected layout.
These artifact checks are separate from the 659 JUnit invocations and do not inflate that count.

Defense refinement and the independent submission review remain proposed later sessions. At this
edition date, fewer than fourteen full days have elapsed from the first commit; no report or saved
automation proves future execution, the required span or professor acceptance.

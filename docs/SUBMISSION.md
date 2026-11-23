# Submission and handoff

The technical deliverables and independent review are complete as of 23 November 2026.
This guide describes what to submit and what you still need to do personally. It is not a
claim of UZEM upload, student rehearsal/attendance or professor acceptance.

## Deliverable map

| Deliverable | Location in this project |
|---|---|
| Full Java domain source | src/main/java/edu/banking/ — 22 source files; no main/UI/menu |
| Maven configuration | pom.xml — Java 17 target, JUnit 5 and JaCoCo |
| Complete JUnit 5 suite | src/test/java/edu/banking/ — fourteen suites, 659 invocations |
| Final Microsoft Word report | docs/final-report.docx — 23 November edition, all five PDF sections |
| Inspected layout preview | docs/final-report-preview.pdf — 13 pages; preview does not replace DOCX |
| Accurate UML | docs/current-uml.svg and docs/uml/*.svg; corresponding editable .dot files; signatures/legend in docs/UML_GUIDE.md |
| Oral-defense preparation | docs/DEFENSE.md, DEFENSE_DEMO.md and DEFENSE_EXERCISE.md |
| Requirements/testing/review evidence | docs/REQUIREMENTS.md, TEST_MATRIX.md, VERIFICATION.md and FINAL_REVIEW.md |
| Genuine development record | Existing private GitHub repository and docs/DEVELOPMENT_PLAN.md; earlier dated reviews remain historical evidence |

Private repository: https://github.com/Mo3azGad/banking-management-system

## Run and review on your laptop

Install a **JDK 17 or later** and Maven 3.9.x. From the extracted project/repository root:

```sh
mvn clean verify
```

Expect 659 tests with 0 failures/errors/skips. Read Surefire reports in target/surefire-reports/
and JaCoCo in target/site/jacoco/index.html: 100% lines, 98.59% branches. The documented enum
fallback is the sole missed branch. Maven's runner output is allowed; no console application
is present. First use needs dependency downloads. Configure and verify offline demo operation
on your own laptop as described in DEFENSE_DEMO.md; this workspace's tools/cache are external.

Open the DOCX locally and inspect its figures/pagination against the PDF preview before upload.
The report embeds all three final UML views and explains selected members; zoomable SVGs and
the exact-signature guide are included. Historical drafts under docs/history/ are labelled
superseded and must not be mistaken for the final report/model.

## Source archive

The session result provides the actual final-commit source ZIP and SHA-256. It is stored outside
tracked source so that the package does not recursively contain itself. To reproduce a ZIP from
a verified clean committed revision, run from the existing repository:

```sh
git status --short
git rev-parse HEAD
git archive --format=zip --prefix=banking-management/ --output=../banking-management-submission-2026-11-23.zip HEAD
sha256sum ../banking-management-submission-2026-11-23.zip
```

Use the recorded real final commit; `git archive HEAD` includes only committed files, not pending
edits. The archive preserves tracked source/documentation and labelled historical records, and
excludes the Git database, generated target/ output, external settings/cache/tool binaries and
credentials. Extract it into an empty directory and run the same clean build from its
banking-management/ folder. The final session verifies that actual delivered archive separately.

A ZIP has no Git history. The authentic history remains in the original private repository;
do not replace that repository with a source import or rewrite its commits. If your professor
needs GitHub access, arrange that through the normal GitHub access controls using the identity
specified by the course. No invitation has been sent and privacy has not been changed.

## Actions you must complete

1. Rehearse the five-minute explanation with a stopwatch; predict and explain the focused JUnit
   results before running them. Complete the independent modification exercise yourself.
2. Confirm your assigned weeks 11–12 defense slot, bring your laptop and attend. The official
   PDF says absence fails the project regardless of code/tests/report. Prepared notes do not
   substitute for your understanding or attendance.
3. Upload the individual **DOC/DOCX report** on UZEM by **7 December 2026**. The PDF gives no
   hour; confirm the portal/course instructions. Supply the source ZIP/repository link in the
   format the portal accepts; the ZIP/PDF preview do not replace the mandatory Word report.
4. Verify any required repository access and the files you actually uploaded. This session
   does not perform external submission or send messages/invitations on your behalf.

The technical roadmap ends on 23 November. No automatic development/push beyond that final
configured session is implied. Ask for further changes explicitly if needed.

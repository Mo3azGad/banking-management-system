"""Build the Word report from current JUnit/JaCoCo evidence and editable UML sources.

Documentation tooling only; Java domain operations still execute exclusively through JUnit.
Requires python-docx and Graphviz. Dates are explicit; run Maven clean verify first.
"""
import argparse
from datetime import date, datetime, timezone
from pathlib import Path
import subprocess
import tempfile
import xml.etree.ElementTree as ET

from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor


ROOT = Path(__file__).resolve().parents[1]
BLUE = '203B58'


def build(as_of, verified_at):
    suites = [ET.parse(p).getroot() for p in sorted(
        (ROOT / 'target/surefire-reports').glob('TEST-*.xml'))]
    if not suites:
        raise ValueError('Run Maven clean verify before building the report.')
    totals = {key: sum(int(s.get(key, '0')) for s in suites)
              for key in ('tests', 'failures', 'errors', 'skipped')}
    if any(totals[key] for key in ('failures', 'errors', 'skipped')):
        raise ValueError('The report requires a complete passing, unskipped test run.')
    coverage = {c.get('type'): (int(c.get('covered')), int(c.get('missed')))
                for c in ET.parse(ROOT / 'target/site/jacoco/jacoco.xml').getroot().findall('counter')}

    d = Document()
    sec = d.sections[0]
    sec.page_width, sec.page_height = Inches(8.2677), Inches(11.6929)
    sec.top_margin = sec.bottom_margin = Inches(.68)
    sec.left_margin = sec.right_margin = Inches(.7)
    sec.header_distance = sec.footer_distance = Inches(.28)
    normal = d.styles['Normal']
    normal.font.name = 'Liberation Sans'
    normal.font.size = Pt(11)
    normal.paragraph_format.space_after = Pt(7)
    normal.paragraph_format.line_spacing = 1.08
    for name, size in [('Title', 30), ('Heading 1', 20), ('Heading 2', 13), ('Heading 3', 11)]:
        st = d.styles[name]
        st.font.name = 'Liberation Sans'
        st.font.size = Pt(size)
        st.font.color.rgb = RGBColor.from_string(BLUE)
        st.paragraph_format.space_before = Pt(9)
        st.paragraph_format.space_after = Pt(7)
    sec.header.paragraphs[0].text = 'ADVANCED OOP  /  PROJECT 2                                     BANKING MANAGEMENT SYSTEM'
    sec.header.paragraphs[0].style = 'Caption'
    foot = sec.footer.paragraphs[0]
    foot.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    foot.add_run('Technical report • ' + as_of.isoformat() + '    |    ')
    for field, trailing in [('PAGE', ' / '), ('NUMPAGES', '')]:
        element = OxmlElement('w:fldSimple')
        element.set(qn('w:instr'), field)
        foot._p.append(element)
        foot.add_run(trailing)
    for run in foot.runs:
        run.font.size = Pt(8)

    def p(text, style=None):
        return d.add_paragraph(text, style)

    def h(text, level=2):
        d.add_heading(text, level)

    def page(title, level=2):
        d.add_page_break()
        h(title, level)

    def table(headers, rows, widths):
        t = d.add_table(rows=1, cols=len(headers))
        t.alignment = WD_TABLE_ALIGNMENT.CENTER
        t.autofit = False
        t.style = 'Light Shading Accent 1'
        for column, width in zip(t.columns, widths):
            column.width = Inches(width)
        for cells, values in [(t.rows[0].cells, headers)] + [(t.add_row().cells, row) for row in rows]:
            for cell, value, width in zip(cells, values, widths):
                cell.width = Inches(width)
                cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
                cell.text = str(value)
                for para in cell.paragraphs:
                    para.paragraph_format.space_after = Pt(4)
                    para.paragraph_format.space_before = Pt(4)
                    para.paragraph_format.line_spacing = 1.02
                    for run in para.runs:
                        run.font.size = Pt(9.5)
            props = cells[0]._tc.getparent().get_or_add_trPr()
            props.append(OxmlElement('w:cantSplit'))
        repeat = OxmlElement('w:tblHeader')
        t.rows[0]._tr.get_or_add_trPr().append(repeat)
        p('')
        return t

    def figure(source, title, caption):
        page(title)
        with tempfile.TemporaryDirectory(prefix='bank-report-') as tmp:
            output = Path(tmp) / 'diagram.png'
            subprocess.run(['dot', '-Gdpi=220', '-Tpng', str(ROOT / source), '-o', str(output)], check=True)
            para = d.add_paragraph()
            para.alignment = WD_ALIGN_PARAGRAPH.CENTER
            para.add_run().add_picture(str(output), width=Inches(6.78))
        p(caption, 'Caption')

    # Cover; the five numbered headings below correspond exactly to the PDF's topics.
    p('INDIVIDUAL FINAL TECHNICAL REPORT', 'Subtitle')
    h('Banking\nManagement System', 0)
    p('Advanced Object-Oriented Programming • Project 2', 'Subtitle')
    p('Report edition: ' + as_of.strftime('%d %B %Y'))
    h('Implemented and verified')
    p('A small in-memory Java banking domain supporting registered customers, checking and savings accounts, '
      'dated deposits and withdrawals, paired transfers, immutable history, monthly interest and irreversible freezing. '
      'JUnit tests are the only application entry point.')
    table(['Required report topic', 'Contents'], [
        ('1. Design overview', 'Final class responsibilities, three UML views and operation contracts'),
        ('2. Design patterns', 'Template Method and constructor-injected Strategy'),
        ('3. Exception hierarchy', 'Abstract unchecked base, eleven concrete types and failure semantics'),
        ('4. SOLID principles', 'Specific classes, executable evidence and limits for all five principles'),
        ('5. Testing', 'Fresh results, coverage, boundary cases, limitations and testability changes'),
    ], [2.2, 4.6])
    h('Evidence and delivery status')
    p(f'The clean verification run completed at {verified_at.isoformat().replace("+00:00", "Z")}: '
      f'{totals["tests"]} JUnit invocations, no failures, errors or skipped cases. '
      'The report describes existing code; planned defense refinement and independent submission review are not claimed complete.')
    p('The official PDF is the requirements authority. Numeric floors, rates and detailed posting rules are explicit '
      'implementation choices. The genuine Git history began on 8 November; the two-week span is still in progress '
      'on this report date. A proposed 23 November review is beyond fourteen elapsed days, subject to actual execution.')
    p('Private repository: github.com/Mo3azGad/banking-management-system\n'
      'Submission: individual DOC/DOCX on UZEM by 7 December 2026. The separate five-minute individual defense is mandatory; '
      'the PDF specifies weeks 11–12 and requires a laptop, test demonstration and code walkthrough.', 'Caption')

    page('1. Design overview', 1)
    h('1.1 Requirements and scope')
    p('The supplied Advanced OOP Final Project_EN.pdf requires accounts, customers, transactions and a bank, '
      'with different checking/savings rules and monthly rates. Every listed operation is implemented. '
      'There is no main method, UI, menu, Scanner, console output or system-clock read in application or test code. '
      'Maven builds Java 17-compatible source; JUnit 5 executes the domain.')
    p('All production classes are in edu.banking. A compact domain model keeps business rules close to their state: '
      'Bank coordinates registries and two-account operations; Account controls the ledger and lifecycle; '
      'InterestPolicy separates calculation. No persistence, transport, dependency-injection framework or additional '
      'service layer is needed for this assignment.')
    h('1.2 Financial and date contracts')
    table(['Chosen product rule', 'CheckingAccount', 'SavingsAccount'], [
        ('Minimum opening / retained balance', '0.00; no overdraft', '100.00'),
        ('Default monthly decimal multiplier', '0.001 = 0.1%', '0.003 = 0.3%'),
        ('Amount available for a debit', 'Balance − 0.00', 'Balance − 100.00'),
    ], [2.9, 1.95, 1.95])
    p('Money uses BigDecimal in one abstract currency. External monetary inputs must be exactly representable in '
      'two decimal places: 1.230 is accepted and normalized; 1.231 is rejected. Ordinary deposits, withdrawals and '
      'transfers must be positive. Opening and interest entries may be zero. Equality at the debit floor is allowed.')
    p('LocalDate is supplied on opening and every financial posting. A date cannot precede that account’s latest '
      'entry; same-day operations are allowed. No comparison to “today” occurs. Monthly interest additionally '
      'requires month-end and an unprocessed YearMonth. The current balance earns a full first-month calculation; '
      'there is no proration or skipped-month catch-up. HALF_EVEN rounding is applied once to the final interest.')
    p('Freezing is irreversible in this scope and blocks every financial posting, including incoming transfers '
      'and interest. Repeated freezing is an error. It records no financial entry or timestamp, so freeze() needs '
      'no date. Balance, metadata and immutable history remain readable.')
    h('1.3 Ownership and identity')
    p('Bank opening resolves a registered customer and fully validates construction before registration. IDs are '
      'stripped, case-sensitive and unique within the appropriate Bank registry. Direct account construction remains '
      'possible but does not register it. Customer/account snapshots protect membership; contained Accounts remain '
      'the actual mutable instances. Transaction identity is (accountId, transactionId); Bank-local transfer '
      'references correlate the two legs independently of account-local IDs.')

    page('1.4 Class responsibilities')
    table(['Class / interface', 'Implemented responsibility'], [
        ('Bank', 'Private insertion-ordered customer/account registries; canonical ownership, opening, lookup, immutable membership snapshots and transfer coordination.'),
        ('Customer', 'Immutable validated identifier and name. Names may repeat; identifiers may not repeat within a Bank.'),
        ('Account (abstract)', 'Encapsulated ledger, active/frozen lifecycle, dated posting workflow, minimum-balance hook, injected policy and monthly eligibility.'),
        ('CheckingAccount / SavingsAccount', 'Final product implementations establish their opening/debit floors and default rate metadata. Four-argument constructors compose defaults; five-argument overloads inject a policy.'),
        ('Transaction (record)', 'Immutable fact: id, accountId, type, amount, balanceAfter, date and OptionalLong transferReference. Its constructor validates the fact; it does not execute an operation.'),
        ('TransactionType (enum)', 'OPENING, DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT and INTEREST.'),
        ('InterestPolicy / FixedRateInterestPolicy', 'One-operation pure strategy contract and its immutable fixed-monthly-rate implementation. Eligibility and publication stay in Account.'),
        ('Account.LedgerState', 'Private implicitly static record holding a balance and an immutable nonempty history. Account publishes a complete replacement through one reference assignment.'),
        ('Account.PreparedPosting', 'Package-private final non-static inner plan, bound to its enclosing Account and expected ledger identity; validate/commit reject frozen or superseded state.'),
        ('BankingInput', 'Package-private shared text/required-value/exact-cent validation; no public helper API.'),
        ('BankingException and eleven concrete subtypes', 'Abstract unchecked domain-failure base and specific actionable categories; full hierarchy in Section 3.'),
    ], [2.2, 4.6])
    p('The three complementary diagrams cover all 24 source-declared banking types: 21 public top-level types, '
      'one package-private helper and two nested types. Repeated context boxes refer to the same class. '
      'Twenty-three types have executable bodies in JaCoCo; the pure interface contributes none.', 'Caption')
    p('UML notation: + public, − private, # protected, ~ package-private; italic operations are abstract. '
      'Solid hollow-triangle arrows denote inheritance; dashed hollow triangles denote interface realization. '
      'Associations carry multiplicities; dashed open arrows show usage. Members are selective, with “...” '
      'eliding parameters. Exact signatures and omitted constructors/accessors are in docs/UML_GUIDE.md.', 'Caption')

    figure('docs/current-uml.dot', '1.5 Final UML — domain',
           'Figure 1. Domain view. Product constants are private static final. Bank membership is optional for '
           'standalone models; every Account has one owner and one nonnull policy. Editable DOT and zoomable SVG: docs/current-uml.*.')
    figure('docs/uml/posting-internals.dot', '1.6 Final UML — posting internals',
           'Figure 2. LedgerState owns the immutable nonempty history. PreparedPosting captures one Account and '
           'has separate required expected/next ledger roles. These internal types expose no public transaction API. '
           'Editable and zoomable views: docs/uml/posting-internals.*.')

    page('1.7 Important operation workflows')
    h('Opening and direct postings')
    p('openCheckingAccount / openSavingsAccount validate an unused account ID, resolve the canonical owner, '
      'construct the complete account with OPENING entry 1, then insert it. Rejection changes neither registry '
      'and does not reserve the ID. deposit / withdraw validate active status, positive exact-cent amount, '
      'supplied date and chronology, then calculate the prospective balance and enforce the subtype floor.')
    p('prepareEntry builds the complete Transaction, copies history and creates a replacement LedgerState '
      'before returning a plan. PreparedPosting.commit rechecks active state and expected ledger identity, then '
      'assigns that replacement. Failure preserves balance, history, next local ID and latest date. An intervening '
      'posting invalidates a plan even if the balance later returns to the same number.')
    h('Two-account transfers')
    p('Bank.transfer resolves both registered accounts and rejects the same object. It computes a candidate '
      'reference, prepares outgoing then incoming legs, allocates the immutable [outgoing, incoming] receipt, '
      'validates both plans, commits both and only then advances its reference. Each leg has the same amount, '
      'date and positive reference; local transaction IDs can differ. A recipient chronology/frozen failure '
      'therefore cannot partly debit the sender.')
    p('The guarantee assumes distinct registered accounts, single-threaded execution and no callbacks between '
      'final validation and commitment. The publication path performs no allocation or arithmetic. It does not '
      'provide concurrency control, database rollback, durability or recovery from virtual-machine failure.')
    h('Dated monthly interest')
    p('applyMonthlyInterest validates active status, date, month-end, chronology and the absence of an INTEREST '
      'entry for that YearMonth. It invokes the injected policy once, validates its nonnegative exact-cent '
      'result and commits through the same immutable-ledger boundary. Zero credit still creates an entry and '
      'marks the month processed. The ledger is the month marker; there is no separate mutable month set.')
    p('FixedRateInterestPolicy multiplies exactly, then rounds once with HALF_EVEN: 1,005.00 × 0.001 = 1.005 '
      'becomes 1.00; 1,015.00 × 0.001 becomes 1.02. getMonthlyInterestRate() describes the default product '
      'multiplier; an injected override may calculate differently. Later same-day postings do not reopen an '
      'already processed month. Policies must be pure; arbitrary callback side effects are outside rollback guarantees.')

    page('2. Design patterns', 1)
    h('2.1 Template Method — shared posting invariants')
    p('Problem: independently implemented checking/savings postings could diverge on dates, freezing, cents, '
      'floors or history publication. Account supplies the invariant algorithm behind final deposit/withdraw '
      'and package-private transfer entry points. Its private preparePosting validates, calculates, checks '
      'getMinimumBalance(), and constructs the complete next state; the final entry points commit that state.')
    p('The polymorphic hook varies the retained floor: 0.00 for checking and 100.00 for savings. Product classes '
      'do not replace the posting workflow. This is a restrained Template Method: the algorithm need not itself '
      'be public. Interest has distinct month-end and zero-credit rules, so it shares chronology/entry helpers '
      'rather than being forced through ordinary positive-deposit validation.')
    p('Why it fits: the two products really share an ordered workflow with a small rule variation. Shared '
      'AccountOperationsTest and AccountFreezeTest cases run through Account references, including exact-floor '
      'success, one-cent-over rejection and unchanged history. The pattern directly protects those common invariants.')
    h('2.2 Strategy — interchangeable interest arithmetic')
    p('Problem: changing calculation must not rewrite month eligibility, frozen checks or ledger publication. '
      'InterestPolicy is the strategy interface, FixedRateInterestPolicy its concrete default algorithm and '
      'Account the context. Account receives the interface by constructor injection and calls calculate(balance). '
      'Concrete product constructors compose the default policy or accept an explicit override.')
    p('Why it fits: calculation is pure and independently testable, while Account owns dated posting. '
      'MonthlyInterestTest.injectedPolicyReceivesThePrePostingBalanceAndOverridesTheDefaultAlgorithm proves '
      'the actual context uses a substitute strategy; failure/result tests prove rejection preserves state. '
      'A custom algorithm can be supplied without editing eligibility code. There is no dependency-injection framework.')
    h('2.3 Deliberate limits')
    p('Only these two learned patterns are claimed. Named Bank creation methods do not implement GoF Factory '
      'Method because no overridable creator selects products. A frozen boolean is sufficient for irreversible '
      'freezing; there is no State-pattern framework. PreparedPosting is a local validation/publication boundary, '
      'not a distributed two-phase commit protocol. Separate repositories, commands and services would add '
      'indirection without a current assignment requirement.')
    p('Source evidence: Account.java, CheckingAccount.java, SavingsAccount.java, InterestPolicy.java and '
      'FixedRateInterestPolicy.java. Additional contract/test references: docs/DESIGN_REVIEW.md.', 'Caption')

    page('3. Exception hierarchy', 1)
    h('3.1 Unchecked, specific domain failures')
    p('BankingException is an abstract subclass of RuntimeException, with protected message and message/cause '
      'constructors. All eleven concrete final types extend it directly. Unchecked exceptions fit validation '
      'and state rejection in this small in-memory API: callers can catch a specific actionable category '
      'without adding checked throws clauses to every operation. This choice does not hide I/O failures; '
      'there is no persistence or network operation in the domain.')
    p('Rejected operations throw clear contextual messages instead of returning null or printing errors. '
      'Account checks frozen state before its input/date/funds guards; Bank resolves transfer accounts and '
      'distinctness before preparing either leg. Interest validates chronology before duplicate-month status. '
      'Existing BankingExceptions from a policy propagate; unexpected RuntimeExceptions are wrapped with '
      'their original cause. No catch-and-ignore path converts failure into apparent success.')
    table(['Concrete exception', 'Failure condition', 'Throwing-test suite'], [
        ('InvalidBankingInputException', 'Missing/invalid identity, monetary input, rate or policy result', 'CustomerTest; input/model/policy suites'),
        ('DuplicateCustomerException', 'Normalized customer ID already registered', 'BankTest'),
        ('CustomerNotFoundException', 'Registered customer lookup fails', 'BankTest'),
        ('DuplicateAccountException', 'Account ID already registered, across products', 'BankAccountTest'),
        ('AccountNotFoundException', 'Registered account lookup fails', 'BankAccountTest'),
        ('InsufficientFundsException', 'Withdrawal/outgoing transfer would breach the floor', 'AccountOperationsTest; BankTransferTest'),
        ('InvalidPostingDateException', 'Backdated posting or interest on a non-month-end', 'AccountOperationsTest; MonthlyInterestTest'),
        ('AccountFrozenException', 'Repeat freezing or blocked financial posting', 'AccountFreezeTest; posting suites'),
        ('StalePostingException', 'Reused or superseded prepared plan', 'PreparedPostingTest'),
        ('InterestAlreadyAppliedException', 'Current eligible month already has an interest marker', 'MonthlyInterestTest'),
        ('InterestCalculationException', 'Unexpected policy RuntimeException; cause retained', 'MonthlyInterestTest'),
    ], [2.5, 2.65, 1.65])
    p('Each type is actually thrown by an operation test, rather than merely constructed. The exact named '
      'witness for every type is in docs/API_EXCEPTION_AUDIT.md. Basic getters and exception-carrier constructors '
      'are not given invented failure conditions.', 'Caption')

    figure('docs/uml/exceptions.dot', '3.2 Final UML — exceptions',
           'Figure 3. Abstract unchecked base and all eleven concrete domain failures. RuntimeException is an '
           'external JDK type. Concrete constructors take a message, except InterestCalculationException, which '
           'also takes a Throwable cause. Editable DOT and zoomable SVG: docs/uml/exceptions.*.')

    page('4. All five SOLID principles', 1)
    h('4.1 Single Responsibility Principle')
    p('Bank coordinates registration and transfers; Account owns the ledger, lifecycle and posting eligibility; '
      'FixedRateInterestPolicy owns arithmetic; Transaction validates immutable facts. Each has a coherent '
      'reason to change. Calculation tests need no Bank; posting tests can inject a policy, showing that these '
      'responsibilities can be exercised independently.')
    h('4.2 Open/Closed Principle')
    p('Account’s shared posting algorithm consults getMinimumBalance(), and monthly posting delegates to '
      'InterestPolicy. Product floors or a supplied calculation can vary without replacing validation and '
      'publication. The injected-policy tests exercise that seam. This is a local extension point: supporting '
      'a new product in Bank still requires intentional opening/registration API work.')
    h('4.3 Liskov Substitution Principle')
    p('The Account withdrawal contract promises spending up to getAvailableBalance(), rather than all raw '
      'balance. At balance 250.00, withdrawing availability leaves checking at 0.00 and savings at 100.00; '
      'both preserve the same base contract. Shared tests call both through Account references for success, '
      'rejection, dates, snapshots and freezing. Future subtype hooks must be stable and side-effect free; '
      'inheritance alone does not prove substitution.')
    h('4.4 Interface Segregation Principle')
    p('InterestPolicy has only calculate(BigDecimal). An arithmetic implementation is not forced to implement '
      'registration, storage, dates or freezing. The fixed policy and small test policies implement this '
      'focused contract. No artificial entity interfaces are introduced merely to increase the interface count.')
    h('4.5 Dependency Inversion Principle')
    p('Account stores, accepts and calls the InterestPolicy abstraction instead of constructing a fixed '
      'algorithm. Concrete product constructors form the composition boundary and may know '
      'FixedRateInterestPolicy. The custom-context test verifies the abstraction controls real posting. '
      'BankingInput and private maps are ordinary implementation helpers, not claimed inversion abstractions.')
    h('Encapsulation, abstraction and polymorphism')
    p('Private state and final workflows prevent callers or supplied products from bypassing validation. '
      'Abstract Account expresses common behavior; CheckingAccount and SavingsAccount inherit it and '
      'supply polymorphic rules. Immutable Customer/Transaction values and immutable ledger histories prevent '
      'external mutation. These mechanisms support SOLID rather than serving as substitutes for its reasoning.')

    page('5. Testing strategy and evidence', 1)
    h('5.1 Behavior-focused JUnit 5 suite')
    p('Tests are the only application entry point. Each descriptive test targets one behavior; multiple '
      'assertions may establish that behavior’s complete postcondition. Parameterized cases exercise both '
      'products and all four transfer directions through Account references. Explicit dates and local '
      'fixtures make results independent of the system clock, execution order and external services.')
    suite_focus = {
        'AccountFreezeTest': 'Lifecycle, repeated freezing, blocked postings and readable state',
        'AccountOperationsTest': 'Dated postings, money/floor boundaries, snapshots and rejection state',
        'AccountTest': 'Model construction, product rules, owners and opening facts',
        'BankAccountTest': 'Registered ownership, unique IDs, lookup, snapshots and opening retries',
        'BankScenarioTest': 'Two-month reconciliation, paired transfers and cross-operation invariants',
        'BankTest': 'Customer registration, normalized lookup and registry snapshots',
        'BankTransferTest': 'Conservation, paired references, both-side validation and atomic rejection',
        'CustomerTest': 'Immutable identity/name and invalid text',
        'FixedRateInterestPolicyTest': 'Exact multiplication, final rounding, rates and decimal limits',
        'InputContractTest': 'Diagnostic regression, overlapping invalid inputs and rejection/retry',
        'MonthlyInterestBoundaryTest': 'Posted rounding, zero credit, calendar/skipped months and retries',
        'MonthlyInterestTest': 'Dated application, duplicate markers, injection and policy failures',
        'PreparedPostingTest': 'No mutation at preparation, stale/reused plans and guarded publication',
        'TransactionTest': 'All six fact types, money/date/identity/reference validation',
    }
    table(['JUnit suite', 'Cases', 'Primary behavior'], [
        (s.get('name').split('.')[-1], s.get('tests'), suite_focus[s.get('name').split('.')[-1]])
        for s in suites], [2.2, .7, 3.9])
    p('Counts are JUnit invocations, including parameter combinations, not distinct method declarations. '
      f'Total: {totals["tests"]}; failures/errors/skipped: 0/0/0. No new behavioral test was warranted for this '
      'report-only milestone; the complete existing suite was rebuilt.', 'Caption')

    page('5.2 Fresh verification and representative evidence')
    p('Clean Maven verify completed at ' + verified_at.isoformat().replace('+00:00', 'Z') + '. '
      'Toolchain: Eclipse Temurin 21.0.13+9, Java 17 compiler target, Maven 3.9.11, JUnit 5.11.4, '
      'Surefire 3.5.2 and JaCoCo 0.8.12. Metrics below are read from the generated JaCoCo XML, '
      'and suite counts from Surefire XML.')
    rows = []
    for label, key in [('Instructions', 'INSTRUCTION'), ('Branches', 'BRANCH'), ('Lines', 'LINE'),
                       ('Methods', 'METHOD'), ('Executable classes', 'CLASS')]:
        covered, missed = coverage[key]
        rows.append((label, f'{covered}/{covered + missed}', f'{100 * covered / (covered + missed):.2f}%'))
    table(['Metric', 'Covered / total', 'Coverage'], rows, [3.2, 1.8, 1.8])
    h('Success, rejection and boundaries')
    p('AccountOperationsTest.permitsWithdrawalOfExactlyTheAvailableBalance checks the exact retained floor; '
      'withdrawalOneCentOverAvailableBalancePreservesState checks rejection and unchanged ledger. '
      'Invalid-amount/date tests also prove that rejection consumes no next ID and advances no chronology.')
    p('BankTransferTest.transferConservesMoneyAndPostsOneLinkedEntryPerAccount verifies complete paired facts. '
      'dateValidForSourceButBeforeTargetOpeningCannotPartiallyDebit proves that recipient rejection leaves '
      'both sides unchanged. Frozen-account and same/missing-account cases cover the remaining eligibility rules.')
    p('FixedRateInterestPolicyTest.halfEvenRoundingHandlesBothTieDirectionsAndAdjacentValues tests exact '
      'rounding. MonthlyInterestTest.zeroCreditIsRecordedAndStillProtectsTheProcessedMonth verifies the '
      'zero marker; unexpectedPolicyFailureIsWrappedWithItsCauseAndAllowsRetry checks the typed wrapper, '
      'original cause and preserved month/ID state. The boundary suite checks skipped months and retry after '
      'prior successful interest, rather than testing only fresh accounts.')
    p('BankScenarioTest.twoMonthWorkflowReconcilesExternalFlowsInterestAndPairedTransfers independently '
      'reconciles running facts: final total 1,607.70 = opening 1,500.00 + deposits 200.00 − withdrawals '
      '100.00 + interest 7.70. Transfers preserve the combined total. InputContractTest’s transfer diagnostic '
      'regression failed before the genuine “cannot transfer” correction and now passes for both products.')
    h('Complete public API and exception accounting')
    p('docs/API_EXCEPTION_AUDIT.md inventories 60 directly declared public callables, classifies 24 '
      'domain-operation/constructor declarations and the basic/generated/carrier cases, and maps 67 unique '
      'named test selectors. Every concrete custom exception has an operation-throw witness. '
      'docs/TEST_MATRIX.md records the broader success/failure/boundary contracts; coverage alone is not that proof.')

    page('5.3 Limitations, testability changes and reproduction')
    h('What coverage does not establish')
    p('The sole missed branch is the compiler-generated exhaustive-enum-switch fallback at Transaction.java '
      'line 24. All six legal TransactionType values and null rejection are tested. No fake enum values, '
      'reflection-based state corruption or removed checks are used to chase 100% branches. Full line '
      'coverage does not prove correctness for every input or future subtype/policy.')
    p('Concurrency, durability, persistence, virtual-machine/resource failure and long-range transfer-counter '
      'exhaustion are not simulated. Immutable history copying costs O(n) per posting; no throughput claim '
      'is made. Single-threaded transfer guarantees do not extend to concurrent access. Policies must be '
      'pure: Account preserves its ledger on ordinary rejection but cannot undo arbitrary external callback '
      'effects. GitHub pushes, document rendering and future automation are outside the Java suite and '
      'must be verified separately.')
    h('Design changes that improved testability')
    table(['Decision / redesign', 'Concrete testability benefit'], [
        ('Explicit LocalDate inputs and account-local sequences', 'No clock/static allocator; repeatable chronology and exact ID/retry assertions on standalone accounts. Paired references remain separate.'),
        ('Immutable LedgerState and PreparedPosting boundary', 'Preparation can be inspected without publication; tests exercise stale/frozen/repeated commit rejection and both-side transfer validation without private-state manipulation.'),
        ('Constructor-injected InterestPolicy', 'Small pure test policies count calls, return boundary/invalid results or throw; tests distinguish eligibility, arithmetic and publication failures.'),
        ('INTEREST history as the processed-month marker', 'One authoritative state update; failed/zero-credit/repeated/skipped-month assertions need no access to a separate hidden month cache.'),
    ], [2.65, 4.15])
    h('Reproduce and locate evidence')
    p('With JDK 17+ and Maven 3.9.x, run mvn clean verify from the repository root. Open '
      'target/site/jacoco/index.html and target/surefire-reports/. For a focused demonstration use, for '
      'example, mvn -Dtest=BankTransferTest test. No main program is involved.')
    p('Requirements: the supplied PDF and docs/REQUIREMENTS.md. Source: src/main/java/edu/banking; '
      'tests: src/test/java/edu/banking. Design and exact signatures: docs/UML_GUIDE.md and '
      'docs/ARCHITECTURE.md. Current verification: docs/VERIFICATION.md; dated predecessors preserve '
      'earlier results. Report regeneration and rendering checks: docs/REPORT_BUILD.md. Genuine history '
      'and remaining defense/independent-review obligations: docs/DEVELOPMENT_PLAN.md.')

    props = d.core_properties
    props.title = 'Banking Management System — Final Technical Report'
    props.subject = 'Advanced OOP Project 2: five required report sections'
    props.author = 'Banking Management System project'
    props.keywords = 'Java, JUnit 5, SOLID, UML, banking, final report'
    props.created = props.modified = verified_at.astimezone(timezone.utc).replace(tzinfo=None)
    d.save(ROOT / 'docs/final-report.docx')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--as-of', required=True, type=date.fromisoformat)
    parser.add_argument('--verified-at', required=True, type=datetime.fromisoformat,
                        help='Actual successful clean-verify completion timestamp with timezone')
    args = parser.parse_args()
    if args.verified_at.tzinfo is None:
        parser.error('--verified-at must include a timezone')
    build(args.as_of, args.verified_at)

package edu.banking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/** Shared account contract and dated posting workflows. Single-threaded, in-memory use.
 * Direct construction creates an opening ledger entry; Bank opening methods also register the model.
 *
 * <p>Clients can use either supplied subtype through this contract:
 * <ul>
 *   <li>An active account accepts positive exact-cent deposits, and withdrawals no greater than
 *       {@link #getAvailableBalance()}. Raw balance is not a promise that all funds may be spent.</li>
 *   <li>Posting dates are supplied and cannot precede the latest ledger entry; same-day postings
 *       are allowed. Monthly interest additionally requires month-end and an unprocessed month.</li>
 *   <li>Successful postings publish balance and immutable history together. Domain rejection
 *       preserves the ledger and next local ID for policies without external side effects.</li>
 *   <li>Freezing blocks all financial postings permanently but leaves balances/history readable.
 *       Available balance describes funds above the floor, not permission to post.</li>
 * </ul>
 * <p>Subtype constructors must establish an opening balance at least their nonnegative exact-cent
 * floor. The floor hook must be stable and side-effect free. The default monthly rate is product
 * metadata; actual monthly calculation uses the injected pure {@link InterestPolicy} instead.
 * Subtypes supply these rules without overriding final posting validation/publication methods.
 */
public abstract class Account {
    private final String id;
    private final Customer owner;
    private final InterestPolicy interestPolicy;
    private LedgerState ledger;
    private boolean frozen;
    private final LocalDate openedOn;

    // The history is immutable. Publishing this one reference updates balance and history together.
    private record LedgerState(BigDecimal balance, List<Transaction> history) { }

    protected Account(String id, Customer owner, BigDecimal openingBalance, LocalDate openedOn,
                      InterestPolicy interestPolicy) {
        this.id = BankingInput.text(id, "Account identifier");
        this.owner = BankingInput.required(owner, "Account owner");
        BigDecimal balance = BankingInput.nonNegativeMoney(openingBalance, "Opening balance");
        this.openedOn = BankingInput.required(openedOn, "Opening date");
        this.interestPolicy = BankingInput.required(interestPolicy, "Interest policy");
        Transaction opening = new Transaction(1, this.id, TransactionType.OPENING, balance, balance,
                this.openedOn, OptionalLong.empty());
        ledger = new LedgerState(balance, List.of(opening));
    }

    public final String getId() { return id; }
    public final Customer getOwner() { return owner; }
    public final BigDecimal getBalance() { return ledger.balance(); }
    public final LocalDate getOpenedOn() { return openedOn; }

    /** Whether postings are permanently blocked for this account. */
    public final boolean isFrozen() { return frozen; }

    /** Freezes this account without changing financial history or posting chronology.
     * Repeated freezing throws AccountFrozenException. There is no unfreeze operation.
     */
    public final void freeze() {
        requireActive();
        frozen = true;
    }

    /** Stable, nonnegative exact-cent floor established at opening and preserved by debits. */
    public abstract BigDecimal getMinimumBalance();

    /** Default product monthly decimal rate; an explicitly injected policy may use another algorithm. */
    public abstract BigDecimal getMonthlyInterestRate();

    /** Balance above the type-specific floor; frozen status separately controls posting permission. */
    public final BigDecimal getAvailableBalance() {
        return ledger.balance().subtract(getMinimumBalance());
    }

    /** Posts a strictly positive deposit to an active account on or after the latest ledger date.
     * Returns the recorded entry. Rejection preserves balance, history and sequence.
     */
    public final Transaction deposit(BigDecimal amount, LocalDate date) {
        return prepareDeposit(amount, date).commit();
    }

    /** Posts a positive exact-cent withdrawal no greater than the active account's available balance.
     * The supplied date must be on or after the latest ledger entry; the type-specific floor remains.
     * Returns the recorded entry. Rejection preserves balance, history and sequence.
     */
    public final Transaction withdraw(BigDecimal amount, LocalDate date) {
        return prepareWithdrawal(amount, date).commit();
    }

    /** Applies the injected policy to the current balance at a supplied month-end, once per month.
     * Zero interest marks the month processed. Rejection preserves state for contract-conforming policies.
     * Same-day ordinary postings remain allowed; they do not cause an interest recalculation.
     */
    public final Transaction applyMonthlyInterest(LocalDate date) {
        requireActive();
        BankingInput.required(date, "Interest posting date");
        YearMonth month = YearMonth.from(date);
        if (!date.equals(month.atEndOfMonth())) {
            throw new InvalidPostingDateException("Monthly interest date must be the last day of its month.");
        }
        LedgerState expected = ledger;
        validatePostingDate(expected, date);
        for (Transaction entry : expected.history()) {
            if (entry.type() == TransactionType.INTEREST && YearMonth.from(entry.date()).equals(month)) {
                throw new InterestAlreadyAppliedException("Interest already applied for account " + id
                        + " in " + month + ".");
            }
        }

        BigDecimal amount;
        try {
            amount = interestPolicy.calculate(expected.balance());
        } catch (BankingException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new InterestCalculationException("Interest policy failed for account " + id + ".", failure);
        }
        BigDecimal interest = BankingInput.nonNegativeMoney(amount, "Calculated interest");
        return prepareEntry(expected, TransactionType.INTEREST, interest,
                expected.balance().add(interest), date, OptionalLong.empty()).commit();
    }

    /** Immutable snapshot in posting order, including opening. IDs are local to this account. */
    public final List<Transaction> getTransactionHistory() {
        return ledger.history();
    }

    // Internal preparation boundary; accounts expose no public transfer mutator.
    final PreparedPosting prepareDeposit(BigDecimal amount, LocalDate date) {
        return preparePosting(TransactionType.DEPOSIT, amount, date, OptionalLong.empty());
    }

    final PreparedPosting prepareWithdrawal(BigDecimal amount, LocalDate date) {
        return preparePosting(TransactionType.WITHDRAWAL, amount, date, OptionalLong.empty());
    }

    final PreparedPosting prepareTransferOut(BigDecimal amount, LocalDate date, long reference) {
        return preparePosting(TransactionType.TRANSFER_OUT, amount, date, OptionalLong.of(reference));
    }

    final PreparedPosting prepareTransferIn(BigDecimal amount, LocalDate date, long reference) {
        return preparePosting(TransactionType.TRANSFER_IN, amount, date, OptionalLong.of(reference));
    }

    // Common template: validate and allocate everything before any ledger change.
    private PreparedPosting preparePosting(TransactionType type, BigDecimal amount, LocalDate date,
                                           OptionalLong transferReference) {
        requireActive();
        BigDecimal normalized = BankingInput.nonNegativeMoney(amount, "Posting amount");
        if (normalized.signum() == 0) {
            throw new InvalidBankingInputException("Posting amount must be positive.");
        }
        BankingInput.required(date, "Posting date");
        LedgerState expected = ledger;
        validatePostingDate(expected, date);

        boolean credit = type == TransactionType.DEPOSIT || type == TransactionType.TRANSFER_IN;
        BigDecimal nextBalance = credit
                ? expected.balance().add(normalized) : expected.balance().subtract(normalized);
        if (nextBalance.compareTo(getMinimumBalance()) < 0) {
            throw new InsufficientFundsException("Account " + id + " has " + getAvailableBalance()
                    + " available; cannot withdraw " + normalized + ".");
        }
        return prepareEntry(expected, type, normalized, nextBalance, date, transferReference);
    }

    private void validatePostingDate(LedgerState expected, LocalDate date) {
        List<Transaction> history = expected.history();
        LocalDate latestDate = history.get(history.size() - 1).date();
        if (date.isBefore(latestDate)) {
            throw new InvalidPostingDateException("Posting date " + date
                    + " must not precede latest posting date " + latestDate + ".");
        }
    }

    private PreparedPosting prepareEntry(LedgerState expected, TransactionType type, BigDecimal amount,
                                         BigDecimal nextBalance, LocalDate date, OptionalLong transferReference) {
        List<Transaction> history = expected.history();
        Transaction entry = new Transaction(history.size() + 1L, id, type, amount,
                nextBalance, date, transferReference);
        List<Transaction> nextHistory = new ArrayList<>(history);
        nextHistory.add(entry);
        LedgerState next = new LedgerState(nextBalance, List.copyOf(nextHistory));
        return new PreparedPosting(expected, next, entry);
    }

    /** Opaque account-bound plan. Only Account can construct it; callers cannot alter its facts.
     * Validate every participating plan before committing any in coordinated operations. Distinct
     * accounts and no intervening mutations are required; this is not a concurrency mechanism.
     */
    final class PreparedPosting {
        private final LedgerState expected;
        private final LedgerState next;
        private final Transaction entry;

        private PreparedPosting(LedgerState expected, LedgerState next, Transaction entry) {
            this.expected = expected;
            this.next = next;
            this.entry = entry;
        }

        Transaction transaction() { return entry; }

        void validate() {
            requireActive();
            if (ledger != expected) {
                throw new StalePostingException("Prepared posting for account " + id
                        + " no longer matches its ledger.");
            }
        }

        Transaction commit() {
            validate();
            ledger = next;
            return entry;
        }
    }

    private void requireActive() {
        if (frozen) {
            throw new AccountFrozenException("Account " + id + " is frozen.");
        }
    }
}

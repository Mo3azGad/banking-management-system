package edu.banking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/** Shared account invariants and dated posting workflows. Single-threaded, in-memory use.
 * Constructors create a model with an opening ledger entry; Bank registration arrives later.
 */
public abstract class Account {
    private final String id;
    private final Customer owner;
    private BigDecimal balance;
    private final LocalDate openedOn;
    private final List<Transaction> history = new ArrayList<>();

    protected Account(String id, Customer owner, BigDecimal openingBalance, LocalDate openedOn) {
        this.id = BankingInput.text(id, "Account identifier");
        this.owner = BankingInput.required(owner, "Account owner");
        this.balance = BankingInput.nonNegativeMoney(openingBalance, "Opening balance");
        this.openedOn = BankingInput.required(openedOn, "Opening date");
        history.add(new Transaction(1, this.id, TransactionType.OPENING, balance, balance,
                this.openedOn, OptionalLong.empty()));
    }

    public final String getId() { return id; }
    public final Customer getOwner() { return owner; }
    public final BigDecimal getBalance() { return balance; }
    public final LocalDate getOpenedOn() { return openedOn; }

    /** The balance floor that withdrawals and transfers must preserve. */
    public abstract BigDecimal getMinimumBalance();

    /** Monthly decimal rate, not a percentage: 0.001 means 0.1%. */
    public abstract BigDecimal getMonthlyInterestRate();

    /** Nonnegative spendable balance under this account type's rules. */
    public final BigDecimal getAvailableBalance() {
        return balance.subtract(getMinimumBalance());
    }

    /** Posts a strictly positive deposit on or after the latest ledger date.
     * Returns the recorded entry. Rejection preserves balance, history and sequence.
     */
    public final Transaction deposit(BigDecimal amount, LocalDate date) {
        return post(TransactionType.DEPOSIT, amount, date);
    }

    /** Posts a strictly positive withdrawal while preserving this account type's floor.
     * Returns the recorded entry. Rejection preserves balance, history and sequence.
     */
    public final Transaction withdraw(BigDecimal amount, LocalDate date) {
        return post(TransactionType.WITHDRAWAL, amount, date);
    }

    /** Immutable snapshot in posting order, including opening. IDs are local to this account. */
    public final List<Transaction> getTransactionHistory() {
        return List.copyOf(history);
    }

    // Common template: validate, calculate, prepare the immutable entry, then commit.
    // No domain validation or arithmetic can fail after the history append.
    private Transaction post(TransactionType type, BigDecimal amount, LocalDate date) {
        BigDecimal normalized = BankingInput.nonNegativeMoney(amount, "Posting amount");
        if (normalized.signum() == 0) {
            throw new InvalidBankingInputException("Posting amount must be positive.");
        }
        BankingInput.required(date, "Posting date");
        LocalDate latestDate = history.get(history.size() - 1).date();
        if (date.isBefore(latestDate)) {
            throw new InvalidPostingDateException("Posting date " + date
                    + " must not precede latest posting date " + latestDate + ".");
        }

        BigDecimal nextBalance = type == TransactionType.DEPOSIT
                ? balance.add(normalized) : balance.subtract(normalized);
        if (nextBalance.compareTo(getMinimumBalance()) < 0) {
            throw new InsufficientFundsException("Account " + id + " has " + getAvailableBalance()
                    + " available; cannot withdraw " + normalized + ".");
        }
        Transaction entry = new Transaction(history.size() + 1L, id, type, normalized,
                nextBalance, date, OptionalLong.empty());
        history.add(entry);
        balance = nextBalance;
        return entry;
    }
}

package edu.banking;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Validated opening state shared by account types. Posting operations arrive in later milestones.
 * Constructors create domain models; registration and unique IDs are Bank's responsibility.
 */
public abstract class Account {
    private final String id;
    private final Customer owner;
    private final BigDecimal balance;
    private final LocalDate openedOn;

    protected Account(String id, Customer owner, BigDecimal openingBalance, LocalDate openedOn) {
        this.id = BankingInput.text(id, "Account identifier");
        this.owner = BankingInput.required(owner, "Account owner");
        this.balance = BankingInput.nonNegativeMoney(openingBalance, "Opening balance");
        this.openedOn = BankingInput.required(openedOn, "Opening date");
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
}

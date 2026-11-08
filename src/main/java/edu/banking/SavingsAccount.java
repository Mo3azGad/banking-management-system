package edu.banking;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Savings accounts preserve a minimum balance of 100.00. */
public final class SavingsAccount extends Account {
    private static final BigDecimal MINIMUM_BALANCE = new BigDecimal("100.00");
    private static final BigDecimal MONTHLY_RATE = new BigDecimal("0.003");

    public SavingsAccount(String id, Customer owner, BigDecimal openingBalance, LocalDate openedOn) {
        super(id, owner, openingBalance, openedOn);
        if (getBalance().compareTo(MINIMUM_BALANCE) < 0) {
            throw new InvalidBankingInputException("Savings opening balance must be at least 100.00.");
        }
    }

    @Override
    public BigDecimal getMinimumBalance() { return MINIMUM_BALANCE; }

    @Override
    public BigDecimal getMonthlyInterestRate() { return MONTHLY_RATE; }
}

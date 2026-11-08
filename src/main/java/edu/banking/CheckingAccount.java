package edu.banking;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Checking accounts permit a zero balance and do not offer an overdraft. */
public final class CheckingAccount extends Account {
    private static final BigDecimal MINIMUM_BALANCE = new BigDecimal("0.00");
    private static final BigDecimal MONTHLY_RATE = new BigDecimal("0.001");

    public CheckingAccount(String id, Customer owner, BigDecimal openingBalance, LocalDate openedOn) {
        super(id, owner, openingBalance, openedOn);
    }

    @Override
    public BigDecimal getMinimumBalance() { return MINIMUM_BALANCE; }

    @Override
    public BigDecimal getMonthlyInterestRate() { return MONTHLY_RATE; }
}

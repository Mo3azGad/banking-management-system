package edu.banking;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Immutable monthly decimal rate: 0.001 means 0.1%, not an annual rate or percentage input.
 * Multiplies exactly, then rounds the result once to cents using HALF_EVEN.
 */
public final class FixedRateInterestPolicy implements InterestPolicy {
    private final BigDecimal monthlyRate;

    public FixedRateInterestPolicy(BigDecimal monthlyRate) {
        BankingInput.required(monthlyRate, "Monthly interest rate");
        if (monthlyRate.signum() < 0) {
            throw new InvalidBankingInputException("Monthly interest rate must not be negative.");
        }
        this.monthlyRate = monthlyRate;
    }

    @Override
    public BigDecimal calculate(BigDecimal balance) {
        BigDecimal monetaryBalance = BankingInput.nonNegativeMoney(balance, "Interest balance");
        try {
            return monetaryBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_EVEN);
        } catch (ArithmeticException error) {
            throw new InvalidBankingInputException("Interest calculation exceeds supported decimal precision.");
        }
    }
}

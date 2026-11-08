package edu.banking;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shared input contracts; deliberately not a public banking API. */
final class BankingInput {
    private BankingInput() { }

    static String text(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidBankingInputException(field + " must not be blank.");
        }
        return value.strip();
    }

    static <T> T required(T value, String field) {
        if (value == null) {
            throw new InvalidBankingInputException(field + " must not be null.");
        }
        return value;
    }

    static BigDecimal nonNegativeMoney(BigDecimal value, String field) {
        required(value, field);
        if (value.signum() < 0) {
            throw new InvalidBankingInputException(field + " must not be negative.");
        }
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException error) {
            throw new InvalidBankingInputException(field + " must not contain fractions of a cent.");
        }
    }
}

package edu.banking;

/** An injected interest policy failed unexpectedly; the original cause is retained. */
public final class InterestCalculationException extends BankingException {
    public InterestCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}

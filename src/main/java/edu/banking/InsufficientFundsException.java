package edu.banking;

/** A withdrawal or outgoing transfer would breach the account-specific minimum balance. */
public final class InsufficientFundsException extends BankingException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

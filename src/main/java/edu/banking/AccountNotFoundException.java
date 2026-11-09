package edu.banking;

/** The requested account is not registered at this bank. */
public final class AccountNotFoundException extends BankingException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}

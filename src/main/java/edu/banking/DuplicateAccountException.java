package edu.banking;

/** An account identifier is already registered at this bank. */
public final class DuplicateAccountException extends BankingException {
    public DuplicateAccountException(String message) {
        super(message);
    }
}

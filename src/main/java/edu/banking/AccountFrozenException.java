package edu.banking;

/** The account is already frozen, so the requested state change or posting is forbidden. */
public final class AccountFrozenException extends BankingException {
    public AccountFrozenException(String message) {
        super(message);
    }
}

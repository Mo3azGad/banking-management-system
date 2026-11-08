package edu.banking;

/** A customer identifier is already registered. */
public final class DuplicateCustomerException extends BankingException {
    public DuplicateCustomerException(String message) {
        super(message);
    }
}

package edu.banking;

/** The requested customer is not registered. */
public final class CustomerNotFoundException extends BankingException {
    public CustomerNotFoundException(String message) {
        super(message);
    }
}

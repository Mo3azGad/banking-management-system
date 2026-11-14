package edu.banking;

/** Interest for this account and calendar month has already been posted. */
public final class InterestAlreadyAppliedException extends BankingException {
    public InterestAlreadyAppliedException(String message) {
        super(message);
    }
}

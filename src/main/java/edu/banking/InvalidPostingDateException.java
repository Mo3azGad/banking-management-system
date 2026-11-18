package edu.banking;

/** A posting is backdated, or a monthly-interest date is not month-end. */
public final class InvalidPostingDateException extends BankingException {
    public InvalidPostingDateException(String message) {
        super(message);
    }
}

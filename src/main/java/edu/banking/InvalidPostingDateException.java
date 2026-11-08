package edu.banking;

/** A posting would precede the latest ledger entry, including opening. */
public final class InvalidPostingDateException extends BankingException {
    public InvalidPostingDateException(String message) {
        super(message);
    }
}

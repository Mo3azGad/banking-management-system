package edu.banking;

/** A prepared posting cannot replace ledger state that has changed since preparation. */
public final class StalePostingException extends BankingException {
    public StalePostingException(String message) {
        super(message);
    }
}

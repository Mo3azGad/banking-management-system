package edu.banking;

/** Base type for failures in banking operations. */
public abstract class BankingException extends RuntimeException {
    protected BankingException(String message) {
        super(message);
    }
}

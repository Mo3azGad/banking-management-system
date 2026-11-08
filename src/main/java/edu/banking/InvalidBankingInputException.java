package edu.banking;

/** A required value is missing or invalid. */
public final class InvalidBankingInputException extends BankingException {
    public InvalidBankingInputException(String message) {
        super(message);
    }
}

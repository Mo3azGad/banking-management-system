package edu.banking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.OptionalLong;

/** An immutable ledger fact, not a command to change an account.
 * Amounts are unsigned; type determines the direction. A transfer reference links two legs.
 * Account-level chronology, identity uniqueness and paired transfers are Bank/Account concerns.
 */
public record Transaction(long id, String accountId, TransactionType type, BigDecimal amount,
                          BigDecimal balanceAfter, LocalDate date, OptionalLong transferReference) {
    public Transaction {
        if (id <= 0) {
            throw new InvalidBankingInputException("Transaction identifier must be positive.");
        }
        accountId = BankingInput.text(accountId, "Account identifier");
        BankingInput.required(type, "Transaction type");
        amount = BankingInput.nonNegativeMoney(amount, "Transaction amount");
        balanceAfter = BankingInput.nonNegativeMoney(balanceAfter, "Balance after transaction");
        BankingInput.required(date, "Transaction date");
        BankingInput.required(transferReference, "Transfer reference container");

        switch (type) {
            case OPENING -> {
                if (amount.compareTo(balanceAfter) != 0) {
                    throw new InvalidBankingInputException("Opening amount must equal the opening balance.");
                }
            }
            case INTEREST -> requireCreditFitsBalance(amount, balanceAfter);
            case DEPOSIT, TRANSFER_IN -> {
                requirePositiveAmount(amount);
                requireCreditFitsBalance(amount, balanceAfter);
            }
            case WITHDRAWAL, TRANSFER_OUT -> requirePositiveAmount(amount);
        }

        boolean transfer = type == TransactionType.TRANSFER_IN || type == TransactionType.TRANSFER_OUT;
        if (transfer != transferReference.isPresent()) {
            throw new InvalidBankingInputException("Only transfer transactions require a transfer reference.");
        }
        if (transferReference.isPresent() && transferReference.getAsLong() <= 0) {
            throw new InvalidBankingInputException("Transfer reference must be positive.");
        }
    }

    private static void requirePositiveAmount(BigDecimal amount) {
        if (amount.signum() == 0) {
            throw new InvalidBankingInputException("Transaction amount must be positive for this type.");
        }
    }

    private static void requireCreditFitsBalance(BigDecimal amount, BigDecimal balanceAfter) {
        if (amount.compareTo(balanceAfter) > 0) {
            throw new InvalidBankingInputException("Credit amount cannot exceed the balance after transaction.");
        }
    }
}

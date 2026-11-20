package edu.banking;

/** Types of immutable ledger facts recorded by account and bank operations. */
public enum TransactionType {
    OPENING, DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT, INTEREST
}

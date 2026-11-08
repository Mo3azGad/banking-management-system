package edu.banking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {
    private static final LocalDate DATE = LocalDate.of(2024, 2, 29);

    private OptionalLong referenceFor(TransactionType type) {
        return switch (type) {
            case TRANSFER_IN, TRANSFER_OUT -> OptionalLong.of(100);
            default -> OptionalLong.empty();
        };
    }

    private Transaction transaction(TransactionType type, String amount, String after) {
        return new Transaction(1, "A1", type, new BigDecimal(amount), new BigDecimal(after),
                DATE, referenceFor(type));
    }

    @ParameterizedTest
    @EnumSource(TransactionType.class)
    void retainsValidTransactionFacts(TransactionType type) {
        Transaction entry = transaction(type, "10", "10");
        assertAll(() -> assertEquals(1, entry.id()),
                  () -> assertEquals("A1", entry.accountId()),
                  () -> assertEquals(type, entry.type()),
                  () -> assertEquals(new BigDecimal("10.00"), entry.amount()),
                  () -> assertEquals(new BigDecimal("10.00"), entry.balanceAfter()),
                  () -> assertEquals(DATE, entry.date()),
                  () -> assertEquals(referenceFor(type), entry.transferReference()));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    void rejectsNonpositiveTransactionIdentifier(long id) {
        assertThrows(InvalidBankingInputException.class, () -> new Transaction(id, "A1",
                TransactionType.OPENING, BigDecimal.ZERO, BigDecimal.ZERO, DATE, OptionalLong.empty()));
    }

    @Test
    void permitsLargestPositiveTransactionIdentifier() {
        assertEquals(Long.MAX_VALUE, new Transaction(Long.MAX_VALUE, "A1", TransactionType.OPENING,
                BigDecimal.ZERO, BigDecimal.ZERO, DATE, OptionalLong.empty()).id());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "\u2003"})
    void rejectsMissingAccountIdentifier(String id) {
        assertThrows(InvalidBankingInputException.class, () -> new Transaction(1, id,
                TransactionType.OPENING, BigDecimal.ZERO, BigDecimal.ZERO, DATE, OptionalLong.empty()));
    }

    @Test
    void stripsSurroundingAccountIdentifierWhitespace() {
        assertEquals("A1", new Transaction(1, " A1 ", TransactionType.OPENING, BigDecimal.ZERO,
                BigDecimal.ZERO, DATE, OptionalLong.empty()).accountId());
    }

    @Test
    void rejectsMissingTransactionType() {
        assertThrows(InvalidBankingInputException.class, () -> new Transaction(1, "A1", null,
                BigDecimal.ZERO, BigDecimal.ZERO, DATE, OptionalLong.empty()));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"-0.01", "0.001"})
    void rejectsInvalidTransactionAmount(String value) {
        BigDecimal amount = value == null ? null : new BigDecimal(value);
        assertThrows(InvalidBankingInputException.class, () -> new Transaction(1, "A1",
                TransactionType.DEPOSIT, amount, BigDecimal.ONE, DATE, OptionalLong.empty()));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"-0.01", "1.001"})
    void rejectsInvalidBalanceAfter(String value) {
        BigDecimal after = value == null ? null : new BigDecimal(value);
        assertThrows(InvalidBankingInputException.class, () -> new Transaction(1, "A1",
                TransactionType.WITHDRAWAL, BigDecimal.ONE, after, DATE, OptionalLong.empty()));
    }

    @Test
    void rejectsMissingPostingDate() {
        assertThrows(InvalidBankingInputException.class, () -> new Transaction(1, "A1",
                TransactionType.OPENING, BigDecimal.ZERO, BigDecimal.ZERO, null, OptionalLong.empty()));
    }

    @Test
    void rejectsNullTransferReferenceContainer() {
        assertThrows(InvalidBankingInputException.class, () -> new Transaction(1, "A1",
                TransactionType.OPENING, BigDecimal.ZERO, BigDecimal.ZERO, DATE, null));
    }

    @ParameterizedTest
    @EnumSource(value = TransactionType.class, names = {"OPENING", "INTEREST"})
    void permitsZeroForOpeningAndInterest(TransactionType type) {
        assertEquals(new BigDecimal("0.00"), transaction(type, "0", "0").amount());
    }

    @ParameterizedTest
    @EnumSource(value = TransactionType.class, names = {"DEPOSIT", "WITHDRAWAL", "TRANSFER_IN", "TRANSFER_OUT"})
    void rejectsZeroForMoneyMovement(TransactionType type) {
        assertThrows(InvalidBankingInputException.class, () -> transaction(type, "0", "10"));
    }

    @Test
    void rejectsOpeningAmountDifferentFromBalance() {
        var error = assertThrows(InvalidBankingInputException.class,
                () -> transaction(TransactionType.OPENING, "10.00", "11.00"));
        assertEquals("Opening amount must equal the opening balance.", error.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = TransactionType.class, names = {"DEPOSIT", "TRANSFER_IN", "INTEREST"})
    void rejectsCreditLargerThanResultingBalance(TransactionType type) {
        assertThrows(InvalidBankingInputException.class, () -> transaction(type, "10.01", "10.00"));
    }

    @ParameterizedTest
    @EnumSource(value = TransactionType.class, names = {"DEPOSIT", "TRANSFER_IN", "INTEREST"})
    void permitsCreditSmallerThanResultingBalance(TransactionType type) {
        assertEquals(new BigDecimal("10.01"), transaction(type, "10.00", "10.01").balanceAfter());
    }

    @ParameterizedTest
    @EnumSource(value = TransactionType.class, names = {"WITHDRAWAL", "TRANSFER_OUT"})
    void permitsDebitToLeaveZeroBalance(TransactionType type) {
        assertEquals(new BigDecimal("0.00"), transaction(type, "10", "0").balanceAfter());
    }

    @ParameterizedTest
    @EnumSource(value = TransactionType.class, names = {"TRANSFER_IN", "TRANSFER_OUT"})
    void rejectsTransferWithoutLinkReference(TransactionType type) {
        assertThrows(InvalidBankingInputException.class, () -> new Transaction(1, "A1", type,
                BigDecimal.ONE, BigDecimal.ONE, DATE, OptionalLong.empty()));
    }

    @ParameterizedTest
    @EnumSource(value = TransactionType.class, names = {"OPENING", "DEPOSIT", "WITHDRAWAL", "INTEREST"})
    void rejectsTransferReferenceOnNontransferType(TransactionType type) {
        assertThrows(InvalidBankingInputException.class, () -> new Transaction(1, "A1", type,
                BigDecimal.ONE, BigDecimal.ONE, DATE, OptionalLong.of(100)));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    void rejectsNonpositiveTransferReference(long reference) {
        assertThrows(InvalidBankingInputException.class, () -> new Transaction(1, "A1",
                TransactionType.TRANSFER_OUT, BigDecimal.ONE, BigDecimal.ONE, DATE, OptionalLong.of(reference)));
    }

    @Test
    void pairedTransferFactsRetainTheSameReference() {
        Transaction out = new Transaction(1, "A1", TransactionType.TRANSFER_OUT,
                BigDecimal.TEN, BigDecimal.ZERO, DATE, OptionalLong.of(1));
        Transaction in = new Transaction(2, "A2", TransactionType.TRANSFER_IN,
                BigDecimal.TEN, BigDecimal.TEN, DATE, OptionalLong.of(1));
        assertEquals(out.transferReference(), in.transferReference());
    }

    @Test
    void acceptsTrailingZerosWithoutLosingMonetaryValue() {
        Transaction entry = transaction(TransactionType.OPENING, "100.0000", "100");
        assertAll(() -> assertEquals(new BigDecimal("100.00"), entry.amount()),
                  () -> assertEquals(new BigDecimal("100.00"), entry.balanceAfter()));
    }

    @Test
    void constructionDoesNotMutateAnAccount() {
        Account account = new CheckingAccount("A1", new Customer("C1", "Ada"), BigDecimal.TEN, DATE);
        transaction(TransactionType.WITHDRAWAL, "10", "0");
        assertEquals(new BigDecimal("10.00"), account.getBalance());
    }
}

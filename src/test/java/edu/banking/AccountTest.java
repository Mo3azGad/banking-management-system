package edu.banking;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {
    private enum Kind { CHECKING, SAVINGS }
    private static final Customer OWNER = new Customer("C1", "Ada");
    private static final LocalDate DATE = LocalDate.of(2024, 2, 29);

    private Account open(Kind kind, String id, Customer owner, BigDecimal balance, LocalDate date) {
        return switch (kind) {
            case CHECKING -> new CheckingAccount(id, owner, balance, date);
            case SAVINGS -> new SavingsAccount(id, owner, balance, date);
        };
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void retainsValidatedOpeningState(Kind kind) {
        Account account = open(kind, " A1 ", OWNER, new BigDecimal("250"), DATE);
        assertAll(() -> assertEquals("A1", account.getId()),
                  () -> assertSame(OWNER, account.getOwner()),
                  () -> assertEquals(new BigDecimal("250.00"), account.getBalance()),
                  () -> assertEquals(DATE, account.getOpenedOn()));
    }

    @ParameterizedTest
    @CsvSource({"CHECKING, 0.00", "SAVINGS, 100.00"})
    void permitsOpeningExactlyAtTheAccountFloor(Kind kind, BigDecimal floor) {
        Account account = open(kind, "A1", OWNER, floor, DATE);
        assertAll(() -> assertEquals(floor, account.getMinimumBalance()),
                  () -> assertEquals(new BigDecimal("0.00"), account.getAvailableBalance()));
    }

    @ParameterizedTest
    @CsvSource({"CHECKING, -0.01", "SAVINGS, 99.99"})
    void rejectsOpeningOneCentBelowTheAccountFloor(Kind kind, BigDecimal balance) {
        assertThrows(InvalidBankingInputException.class, () -> open(kind, "A1", OWNER, balance, DATE));
    }

    @ParameterizedTest
    @CsvSource({"CHECKING, 250.00", "SAVINGS, 150.00"})
    void availableBalanceRespectsThePolymorphicAccountFloor(Kind kind, BigDecimal available) {
        Account account = open(kind, "A1", OWNER, new BigDecimal("250.00"), DATE);
        assertEquals(available, account.getAvailableBalance());
    }

    @ParameterizedTest
    @CsvSource({"CHECKING, 0.001", "SAVINGS, 0.003"})
    void exposesTheDocumentedMonthlyDecimalRate(Kind kind, BigDecimal rate) {
        assertEquals(rate, open(kind, "A1", OWNER, new BigDecimal("100.00"), DATE).getMonthlyInterestRate());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void rejectsNullAccountIdentifier(Kind kind) {
        assertThrows(InvalidBankingInputException.class,
                () -> open(kind, null, OWNER, new BigDecimal("100"), DATE));
    }

    @ParameterizedTest
    @CsvSource({"CHECKING, ''", "SAVINGS, ''", "CHECKING, ' '", "SAVINGS, ' '",
                "CHECKING, '\u2003'", "SAVINGS, '\u2003'"})
    void rejectsBlankAccountIdentifier(Kind kind, String id) {
        assertThrows(InvalidBankingInputException.class,
                () -> open(kind, id, OWNER, new BigDecimal("100"), DATE));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void rejectsMissingOwner(Kind kind) {
        var error = assertThrows(InvalidBankingInputException.class,
                () -> open(kind, "A1", null, new BigDecimal("100"), DATE));
        assertEquals("Account owner must not be null.", error.getMessage());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void rejectsMissingOpeningBalance(Kind kind) {
        assertThrows(InvalidBankingInputException.class, () -> open(kind, "A1", OWNER, null, DATE));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void rejectsMissingOpeningDate(Kind kind) {
        assertThrows(InvalidBankingInputException.class,
                () -> open(kind, "A1", OWNER, new BigDecimal("100"), null));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void rejectsNegativeOpeningBalance(Kind kind) {
        assertThrows(InvalidBankingInputException.class,
                () -> open(kind, "A1", OWNER, new BigDecimal("-100"), DATE));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void rejectsFractionsOfACentInsteadOfRounding(Kind kind) {
        var error = assertThrows(InvalidBankingInputException.class,
                () -> open(kind, "A1", OWNER, new BigDecimal("100.001"), DATE));
        assertEquals("Opening balance must not contain fractions of a cent.", error.getMessage());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void permitsTrailingZerosBeyondTwoDecimalPlaces(Kind kind) {
        assertEquals(new BigDecimal("100.00"),
                open(kind, "A1", OWNER, new BigDecimal("100.0000"), DATE).getBalance());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void supportsLargeBalancesWithoutFloatingPointLoss(Kind kind) {
        BigDecimal large = new BigDecimal("123456789012345678901234567890.12");
        assertEquals(large, open(kind, "A1", OWNER, large, DATE).getBalance());
    }

    @Test
    void savingsMinimumFailureHasAClearDomainMessage() {
        var error = assertThrows(BankingException.class,
                () -> new SavingsAccount("A1", OWNER, new BigDecimal("99.99"), DATE));
        assertEquals("Savings opening balance must be at least 100.00.", error.getMessage());
    }
}

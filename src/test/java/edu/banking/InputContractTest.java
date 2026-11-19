package edu.banking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class InputContractTest {
    private enum Kind { CHECKING, SAVINGS }
    private enum Operation { DEPOSIT, WITHDRAWAL }
    private static final LocalDate OPENED = LocalDate.of(2024, 1, 1);
    private static final LocalDate LATER = LocalDate.of(2024, 1, 31);
    private record Fixture(Bank bank, Account source, Account target) { }
    private record Snapshot(BigDecimal sourceBalance, BigDecimal targetBalance,
                            List<Transaction> sourceHistory, List<Transaction> targetHistory) { }

    private Account open(Bank bank, Kind kind, String id, String owner, BigDecimal balance, LocalDate date) {
        return kind == Kind.CHECKING ? bank.openCheckingAccount(id, owner, balance, date)
                : bank.openSavingsAccount(id, owner, balance, date);
    }

    private Fixture fixture(Kind kind) {
        Bank bank = new Bank();
        bank.registerCustomer("C1", "Ada");
        bank.registerCustomer("C2", "Grace");
        Account source = open(bank, kind, "S", "C1", new BigDecimal("250"), OPENED);
        Account target = bank.openCheckingAccount("T", "C2", new BigDecimal("250"), OPENED);
        return new Fixture(bank, source, target);
    }

    private Snapshot snapshot(Fixture f) {
        return new Snapshot(f.source().getBalance(), f.target().getBalance(),
                f.source().getTransactionHistory(), f.target().getTransactionHistory());
    }

    private void assertUnchanged(Fixture f, Snapshot before) {
        assertAll(() -> assertEquals(before.sourceBalance(), f.source().getBalance()),
                  () -> assertEquals(before.targetBalance(), f.target().getBalance()),
                  () -> assertEquals(before.sourceHistory(), f.source().getTransactionHistory()),
                  () -> assertEquals(before.targetHistory(), f.target().getTransactionHistory()));
    }

    private static Stream<Arguments> invalidAmountsWithMissingDate() {
        return Stream.of(Kind.values()).flatMap(kind -> Stream.of(Operation.values()).flatMap(operation -> Stream.of(
                Arguments.of(kind, operation, "0.00", "Posting amount must be positive."),
                Arguments.of(kind, operation, "0.001", "Posting amount must not contain fractions of a cent."))));
    }

    @ParameterizedTest
    @CsvSource({"CHECKING, 250.01, 250.00", "SAVINGS, 150.01, 150.00"})
    void insufficientTransferNamesTheRequestedOperationAndKeepsRetryState(Kind kind, String amount, String available) {
        Fixture f = fixture(kind);
        Snapshot before = snapshot(f);
        var error = assertThrows(InsufficientFundsException.class,
                () -> f.bank().transfer("S", "T", new BigDecimal(amount).setScale(4), LATER));
        assertEquals("Account S has " + available + " available; cannot transfer " + amount + ".", error.getMessage());
        assertUnchanged(f, before);
        List<Transaction> receipt = f.bank().transfer("S", "T", new BigDecimal("0.01"), OPENED);
        assertAll(() -> assertEquals(2, receipt.get(0).id()),
                  () -> assertEquals(2, receipt.get(1).id()),
                  () -> assertEquals(1, receipt.get(0).transferReference().orElseThrow()),
                  () -> assertEquals(OPENED, receipt.get(0).date()));
    }

    @ParameterizedTest
    @CsvSource({"CHECKING, 250.01, 250.00", "SAVINGS, 150.01, 150.00"})
    void insufficientWithdrawalRetainsItsSpecificDiagnosticAndLedger(Kind kind, String amount, String available) {
        Fixture f = fixture(kind);
        Snapshot before = snapshot(f);
        var error = assertThrows(InsufficientFundsException.class,
                () -> f.source().withdraw(new BigDecimal(amount).setScale(4), LATER));
        assertEquals("Account S has " + available + " available; cannot withdraw " + amount + ".", error.getMessage());
        assertUnchanged(f, before);
        assertEquals(2, f.source().withdraw(new BigDecimal("0.01"), OPENED).id());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void targetLookupPrecedesFrozenSourceAndInvalidPostingInputs(Kind kind) {
        Fixture f = fixture(kind);
        f.source().freeze();
        Snapshot before = snapshot(f);
        var error = assertThrows(AccountNotFoundException.class,
                () -> f.bank().transfer("S", "missing", null, null));
        assertEquals("Account not found: missing", error.getMessage());
        assertUnchanged(f, before);
        assertTrue(f.source().isFrozen());
        f.bank().openCheckingAccount("U", "C2", BigDecimal.ZERO, OPENED);
        assertEquals(1, f.bank().transfer("T", "U", BigDecimal.ONE, OPENED).get(0).transferReference().orElseThrow());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void distinctAccountCheckPrecedesFrozenStateAndMissingPostingInputs(Kind kind) {
        Fixture f = fixture(kind);
        f.source().freeze();
        Snapshot before = snapshot(f);
        var error = assertThrows(InvalidBankingInputException.class,
                () -> f.bank().transfer("S", " S ", null, null));
        assertEquals("Transfer requires two distinct accounts.", error.getMessage());
        assertUnchanged(f, before);
        assertTrue(f.source().isFrozen());
    }

    @ParameterizedTest
    @MethodSource("invalidAmountsWithMissingDate")
    void invalidAmountPrecedesMissingDateWithoutReservingAnIdentifier(Kind kind, Operation operation,
                                                                   String amount, String message) {
        Fixture f = fixture(kind);
        Snapshot before = snapshot(f);
        var error = assertThrows(InvalidBankingInputException.class, () -> {
            if (operation == Operation.DEPOSIT) { f.source().deposit(new BigDecimal(amount), null); }
            else { f.source().withdraw(new BigDecimal(amount), null); }
        });
        assertEquals(message, error.getMessage());
        assertUnchanged(f, before);
        Transaction next = operation == Operation.DEPOSIT ? f.source().deposit(BigDecimal.ONE, OPENED)
                : f.source().withdraw(BigDecimal.ONE, OPENED);
        assertEquals(2, next.id());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void backdatedWithdrawalPrecedesInsufficientFundsAtTheFloor(Kind kind) {
        Bank bank = new Bank();
        bank.registerCustomer("C1", "Ada");
        BigDecimal floor = new BigDecimal(kind == Kind.CHECKING ? "0.00" : "100.00");
        Account account = open(bank, kind, "S", "C1", floor, OPENED);
        List<Transaction> before = account.getTransactionHistory();
        var error = assertThrows(InvalidPostingDateException.class,
                () -> account.withdraw(BigDecimal.ONE, OPENED.minusDays(1)));
        assertAll(() -> assertEquals("Posting date 2023-12-31 must not precede latest posting date 2024-01-01.",
                                    error.getMessage()),
                  () -> assertEquals(floor, account.getBalance()),
                  () -> assertEquals(before, account.getTransactionHistory()));
        assertEquals(2, account.deposit(new BigDecimal("0.01"), OPENED).id());
        assertEquals(3, account.withdraw(new BigDecimal("0.01"), OPENED).id());
        assertEquals(floor, account.getBalance());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void occupiedAccountIdPrecedesMissingOwnerAndInvalidOpeningState(Kind kind) {
        Fixture f = fixture(kind);
        Snapshot before = snapshot(f);
        List<Account> accounts = f.bank().getAccounts();
        List<Customer> customers = f.bank().getCustomers();
        var error = assertThrows(DuplicateAccountException.class,
                () -> open(f.bank(), kind, " S ", "missing", new BigDecimal("-0.001"), null));
        assertAll(() -> assertEquals("Account already registered: S", error.getMessage()),
                  () -> assertEquals(accounts, f.bank().getAccounts()),
                  () -> assertEquals(customers, f.bank().getCustomers()),
                  () -> assertSame(f.source(), f.bank().findAccount("S")));
        assertUnchanged(f, before);
    }
}

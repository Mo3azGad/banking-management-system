package edu.banking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AccountOperationsTest {
    private enum Kind { CHECKING, SAVINGS }
    private enum Operation { DEPOSIT, WITHDRAWAL }
    private static final LocalDate OPENED = LocalDate.of(2024, 2, 28);
    private static final LocalDate NEXT_DAY = LocalDate.of(2024, 2, 29);
    private static final Customer OWNER = new Customer("C1", "Ada");

    private Account open(Kind kind) {
        return switch (kind) {
            case CHECKING -> new CheckingAccount("A1", OWNER, new BigDecimal("250"), OPENED);
            case SAVINGS -> new SavingsAccount("A1", OWNER, new BigDecimal("250"), OPENED);
        };
    }

    private Transaction post(Account account, Operation operation, BigDecimal amount, LocalDate date) {
        return operation == Operation.DEPOSIT ? account.deposit(amount, date) : account.withdraw(amount, date);
    }

    private static Stream<Arguments> accountOperations() {
        return Stream.of(Kind.values()).flatMap(kind -> Stream.of(Operation.values())
                .map(operation -> Arguments.of(kind, operation)));
    }

    private static Stream<Arguments> invalidAmounts() {
        return accountOperations().flatMap(args -> Stream.<BigDecimal>of(null, BigDecimal.ZERO,
                new BigDecimal("0.000"), new BigDecimal("-0.01"), new BigDecimal("1.001"))
                .map(amount -> Arguments.of(args.get()[0], args.get()[1], amount)));
    }

    private static Stream<Arguments> centAmounts() {
        return accountOperations().flatMap(args -> Stream.of("0.01", "10.0000")
                .map(amount -> Arguments.of(args.get()[0], args.get()[1], new BigDecimal(amount))));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void openingCreatesTheFirstCompleteLedgerEntry(Kind kind) {
        Account account = open(kind);
        assertEquals(List.of(new Transaction(1, "A1", TransactionType.OPENING,
                new BigDecimal("250.00"), new BigDecimal("250.00"), OPENED,
                OptionalLong.empty())), account.getTransactionHistory());
    }

    @Test
    void zeroBalanceCheckingAccountStillRecordsOpening() {
        Account account = new CheckingAccount("A0", OWNER, BigDecimal.ZERO, OPENED);
        Transaction opening = account.getTransactionHistory().get(0);
        assertAll(() -> assertEquals(TransactionType.OPENING, opening.type()),
                  () -> assertEquals(new BigDecimal("0.00"), opening.amount()),
                  () -> assertEquals(new BigDecimal("0.00"), opening.balanceAfter()));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void depositUpdatesBalanceAndReturnsTheRecordedEntry(Kind kind) {
        Account account = open(kind);
        Transaction entry = account.deposit(new BigDecimal("10.01"), NEXT_DAY);
        assertAll(() -> assertEquals(new BigDecimal("260.01"), account.getBalance()),
                  () -> assertEquals(new Transaction(2, "A1", TransactionType.DEPOSIT,
                          new BigDecimal("10.01"), new BigDecimal("260.01"), NEXT_DAY,
                          OptionalLong.empty()), entry),
                  () -> assertSame(entry, account.getTransactionHistory().get(1)));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void withdrawalUpdatesBalanceAndReturnsTheRecordedEntry(Kind kind) {
        Account account = open(kind);
        Transaction entry = account.withdraw(new BigDecimal("10.01"), NEXT_DAY);
        assertAll(() -> assertEquals(new BigDecimal("239.99"), account.getBalance()),
                  () -> assertEquals(new Transaction(2, "A1", TransactionType.WITHDRAWAL,
                          new BigDecimal("10.01"), new BigDecimal("239.99"), NEXT_DAY,
                          OptionalLong.empty()), entry),
                  () -> assertSame(entry, account.getTransactionHistory().get(1)));
    }

    @ParameterizedTest
    @MethodSource("centAmounts")
    void acceptsOneCentAndExactTrailingZeroAmounts(Kind kind, Operation operation, BigDecimal amount) {
        Account account = open(kind);
        Transaction entry = post(account, operation, amount, NEXT_DAY);
        BigDecimal expected = operation == Operation.DEPOSIT
                ? new BigDecimal("250.00").add(amount) : new BigDecimal("250.00").subtract(amount);
        assertAll(() -> assertEquals(2, entry.amount().scale()),
                  () -> assertEquals(0, account.getBalance().compareTo(expected)));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void permitsWithdrawalOfExactlyTheAvailableBalance(Kind kind) {
        Account account = open(kind);
        account.withdraw(account.getAvailableBalance(), NEXT_DAY);
        assertAll(() -> assertEquals(account.getMinimumBalance(), account.getBalance()),
                  () -> assertEquals(new BigDecimal("0.00"), account.getAvailableBalance()));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void withdrawalOneCentOverAvailableBalancePreservesState(Kind kind) {
        Account account = open(kind);
        BigDecimal before = account.getBalance();
        List<Transaction> history = account.getTransactionHistory();
        var error = assertThrows(InsufficientFundsException.class,
                () -> account.withdraw(account.getAvailableBalance().add(new BigDecimal("0.01")), NEXT_DAY));
        assertAll(() -> assertEquals(before, account.getBalance()),
                  () -> assertEquals(history, account.getTransactionHistory()),
                  () -> assertTrue(error.getMessage().contains("Account A1")),
                  () -> assertTrue(error.getMessage().contains("available; cannot withdraw")));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void accountAtItsMinimumCannotWithdrawOneCent(Kind kind) {
        Account account = open(kind);
        account.withdraw(account.getAvailableBalance(), NEXT_DAY);
        List<Transaction> history = account.getTransactionHistory();
        assertThrows(InsufficientFundsException.class,
                () -> account.withdraw(new BigDecimal("0.01"), NEXT_DAY));
        assertEquals(history, account.getTransactionHistory());
    }

    @ParameterizedTest
    @MethodSource("invalidAmounts")
    void invalidAmountPreservesBalanceHistoryAndNextIdentifier(Kind kind, Operation operation, BigDecimal amount) {
        Account account = open(kind);
        List<Transaction> history = account.getTransactionHistory();
        assertThrows(InvalidBankingInputException.class,
                () -> post(account, operation, amount, NEXT_DAY));
        assertAll(() -> assertEquals(new BigDecimal("250.00"), account.getBalance()),
                  () -> assertEquals(history, account.getTransactionHistory()));
        assertEquals(2, account.deposit(BigDecimal.ONE, NEXT_DAY).id());
    }

    @ParameterizedTest
    @MethodSource("accountOperations")
    void missingDatePreservesBalanceAndHistory(Kind kind, Operation operation) {
        Account account = open(kind);
        List<Transaction> history = account.getTransactionHistory();
        var error = assertThrows(InvalidBankingInputException.class,
                () -> post(account, operation, BigDecimal.ONE, null));
        assertAll(() -> assertEquals("Posting date must not be null.", error.getMessage()),
                  () -> assertEquals(new BigDecimal("250.00"), account.getBalance()),
                  () -> assertEquals(history, account.getTransactionHistory()));
    }

    @ParameterizedTest
    @MethodSource("accountOperations")
    void dateBeforeOpeningPreservesBalanceAndHistory(Kind kind, Operation operation) {
        Account account = open(kind);
        List<Transaction> history = account.getTransactionHistory();
        var error = assertThrows(InvalidPostingDateException.class,
                () -> post(account, operation, BigDecimal.ONE, OPENED.minusDays(1)));
        assertAll(() -> assertEquals("Posting date 2024-02-27 must not precede latest posting date 2024-02-28.",
                          error.getMessage()),
                  () -> assertEquals(new BigDecimal("250.00"), account.getBalance()),
                  () -> assertEquals(history, account.getTransactionHistory()));
    }

    @ParameterizedTest
    @MethodSource("accountOperations")
    void dateBeforeLatestPostingIsRejectedEvenIfAfterOpening(Kind kind, Operation operation) {
        Account account = open(kind);
        account.deposit(BigDecimal.ONE, OPENED.plusDays(2));
        List<Transaction> history = account.getTransactionHistory();
        assertThrows(InvalidPostingDateException.class,
                () -> post(account, operation, BigDecimal.ONE, NEXT_DAY));
        assertAll(() -> assertEquals(new BigDecimal("251.00"), account.getBalance()),
                  () -> assertEquals(history, account.getTransactionHistory()));
    }

    @ParameterizedTest
    @MethodSource("accountOperations")
    void chronologyTracksTheLatestWithdrawalAsWellAsDeposits(Kind kind, Operation operation) {
        Account account = open(kind);
        account.withdraw(BigDecimal.ONE, OPENED.plusDays(2));
        List<Transaction> history = account.getTransactionHistory();
        assertThrows(InvalidPostingDateException.class,
                () -> post(account, operation, BigDecimal.ONE, NEXT_DAY));
        assertAll(() -> assertEquals(new BigDecimal("249.00"), account.getBalance()),
                  () -> assertEquals(history, account.getTransactionHistory()));
    }

    @ParameterizedTest
    @MethodSource("accountOperations")
    void permitsPostingOnOpeningDate(Kind kind, Operation operation) {
        assertEquals(OPENED, post(open(kind), operation, BigDecimal.ONE, OPENED).date());
    }

    @ParameterizedTest
    @MethodSource("accountOperations")
    void permitsPostingOnTheSameDateAsLatestEntry(Kind kind, Operation operation) {
        Account account = open(kind);
        account.deposit(BigDecimal.ONE, NEXT_DAY);
        assertEquals(3, post(account, operation, BigDecimal.ONE, NEXT_DAY).id());
    }

    @ParameterizedTest
    @MethodSource("accountOperations")
    void explicitFarFutureDateDoesNotDependOnTheSystemClock(Kind kind, Operation operation) {
        LocalDate date = LocalDate.of(2100, 1, 1);
        assertEquals(date, post(open(kind), operation, BigDecimal.ONE, date).date());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void historyKeepsChronologicalPostingOrderAndRunningBalances(Kind kind) {
        Account account = open(kind);
        account.deposit(new BigDecimal("10.00"), NEXT_DAY);
        account.withdraw(new BigDecimal("20.00"), NEXT_DAY);
        account.deposit(new BigDecimal("0.01"), NEXT_DAY.plusDays(1));
        List<Transaction> history = account.getTransactionHistory();
        assertAll(() -> assertEquals(List.of(1L, 2L, 3L, 4L), history.stream().map(Transaction::id).toList()),
                  () -> assertEquals(List.of(TransactionType.OPENING, TransactionType.DEPOSIT,
                          TransactionType.WITHDRAWAL, TransactionType.DEPOSIT),
                          history.stream().map(Transaction::type).toList()),
                  () -> assertEquals(List.of(new BigDecimal("250.00"), new BigDecimal("260.00"),
                          new BigDecimal("240.00"), new BigDecimal("240.01")),
                          history.stream().map(Transaction::balanceAfter).toList()),
                  () -> assertEquals(history.get(3).balanceAfter(), account.getBalance()));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void historySnapshotRejectsExternalMutation(Kind kind) {
        Account account = open(kind);
        assertThrows(UnsupportedOperationException.class, () -> account.getTransactionHistory().clear());
        assertEquals(1, account.getTransactionHistory().size());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void previouslyRetrievedHistoryRemainsUnchangedAfterPosting(Kind kind) {
        Account account = open(kind);
        List<Transaction> old = account.getTransactionHistory();
        account.deposit(BigDecimal.ONE, NEXT_DAY);
        assertAll(() -> assertEquals(1, old.size()),
                  () -> assertEquals(new BigDecimal("250.00"), old.get(0).balanceAfter()),
                  () -> assertEquals(2, account.getTransactionHistory().size()));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void failedLaterDatedWithdrawalDoesNotAdvanceLedgerDateOrSequence(Kind kind) {
        Account account = open(kind);
        assertThrows(InsufficientFundsException.class,
                () -> account.withdraw(new BigDecimal("999"), OPENED.plusDays(10)));
        assertEquals(2, account.withdraw(BigDecimal.ONE, NEXT_DAY).id());
    }

    @ParameterizedTest
    @MethodSource("accountOperations")
    void rejectedBackdateDoesNotConsumeAnIdentifier(Kind kind, Operation operation) {
        Account account = open(kind);
        assertThrows(InvalidPostingDateException.class,
                () -> post(account, operation, BigDecimal.ONE, OPENED.minusDays(1)));
        assertEquals(2, post(account, operation, BigDecimal.ONE, NEXT_DAY).id());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void replenishingAnAccountAtItsFloorRestoresSpendableBalance(Kind kind) {
        Account account = open(kind);
        account.withdraw(account.getAvailableBalance(), NEXT_DAY);
        account.deposit(new BigDecimal("0.01"), NEXT_DAY);
        account.withdraw(new BigDecimal("0.01"), NEXT_DAY);
        assertEquals(account.getMinimumBalance(), account.getBalance());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void veryLargeDepositAndWithdrawalRoundTripExactly(Kind kind) {
        Account account = open(kind);
        BigDecimal large = new BigDecimal("123456789012345678901234567890.12");
        account.deposit(large, NEXT_DAY);
        account.withdraw(large, NEXT_DAY);
        assertEquals(new BigDecimal("250.00"), account.getBalance());
    }

    @Test
    void accountLedgersKeepIndependentSequencesAndState() {
        Account first = new CheckingAccount("A1", OWNER, BigDecimal.ZERO, OPENED);
        Account second = new SavingsAccount("A2", OWNER, new BigDecimal("100"), OPENED);
        first.deposit(BigDecimal.ONE, NEXT_DAY);
        first.deposit(BigDecimal.ONE, NEXT_DAY);
        Transaction entry = second.deposit(BigDecimal.ONE, NEXT_DAY);
        assertAll(() -> assertEquals(2, entry.id()),
                  () -> assertEquals("A2", entry.accountId()),
                  () -> assertEquals(new BigDecimal("2.00"), first.getBalance()),
                  () -> assertEquals(new BigDecimal("101.00"), second.getBalance()));
    }
}

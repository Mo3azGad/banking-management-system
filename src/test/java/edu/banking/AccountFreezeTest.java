package edu.banking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class AccountFreezeTest {
    private enum Kind { CHECKING, SAVINGS }
    private enum Operation { DEPOSIT, WITHDRAWAL }
    private static final Customer OWNER = new Customer("C1", "Ada");
    private static final LocalDate DATE = LocalDate.of(2024, 2, 29);

    private Account account(Kind kind) {
        return switch (kind) {
            case CHECKING -> new CheckingAccount(" A1 ", OWNER, new BigDecimal("250"), DATE);
            case SAVINGS -> new SavingsAccount(" A1 ", OWNER, new BigDecimal("250"), DATE);
        };
    }

    private Transaction post(Account account, Operation operation, BigDecimal amount, LocalDate date) {
        return operation == Operation.DEPOSIT ? account.deposit(amount, date) : account.withdraw(amount, date);
    }

    private static Stream<Arguments> postingCases() {
        return Stream.of(Kind.values()).flatMap(kind -> Stream.of(Operation.values())
                .map(operation -> Arguments.of(kind, operation)));
    }

    private static Stream<Arguments> invalidPostingCases() {
        return postingCases().flatMap(args -> Stream.of(
                Arguments.of(args.get()[0], args.get()[1], null, DATE),
                Arguments.of(args.get()[0], args.get()[1], BigDecimal.ZERO, DATE),
                Arguments.of(args.get()[0], args.get()[1], new BigDecimal("-1"), DATE),
                Arguments.of(args.get()[0], args.get()[1], new BigDecimal("1.001"), DATE),
                Arguments.of(args.get()[0], args.get()[1], BigDecimal.ONE, null),
                Arguments.of(args.get()[0], args.get()[1], BigDecimal.ONE, DATE.minusDays(1))));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void newlyConstructedAccountIsActive(Kind kind) {
        assertFalse(account(kind).isFrozen());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void firstFreezeTransitionsAnActiveAccountToFrozen(Kind kind) {
        Account account = account(kind);
        account.freeze();
        assertTrue(account.isFrozen());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void freezingPreservesFinancialStateAndExistingHistory(Kind kind) {
        Account account = account(kind);
        account.deposit(BigDecimal.TEN, DATE);
        account.withdraw(BigDecimal.ONE, DATE.plusDays(1));
        List<Transaction> before = account.getTransactionHistory();
        BigDecimal balance = account.getBalance();
        BigDecimal available = account.getAvailableBalance();
        account.freeze();
        assertAll(() -> assertEquals(balance, account.getBalance()),
                  () -> assertEquals(available, account.getAvailableBalance()),
                  () -> assertEquals(before, account.getTransactionHistory()),
                  () -> assertEquals(3, account.getTransactionHistory().get(2).id()),
                  () -> assertEquals(DATE.plusDays(1), account.getTransactionHistory().get(2).date()));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void repeatedFreezeThrowsTypedFailureAndPreservesState(Kind kind) {
        Account account = account(kind);
        account.freeze();
        List<Transaction> before = account.getTransactionHistory();
        var error = assertThrows(AccountFrozenException.class, account::freeze);
        assertAll(() -> assertEquals("Account A1 is frozen.", error.getMessage()),
                  () -> assertTrue(account.isFrozen()),
                  () -> assertEquals(new BigDecimal("250.00"), account.getBalance()),
                  () -> assertEquals(before, account.getTransactionHistory()));
    }

    @ParameterizedTest
    @MethodSource("postingCases")
    void frozenAccountRejectsValidPostingsWithoutChangingLedger(Kind kind, Operation operation) {
        Account account = account(kind);
        List<Transaction> before = account.getTransactionHistory();
        account.freeze();
        var error = assertThrows(AccountFrozenException.class,
                () -> post(account, operation, BigDecimal.ONE, DATE.plusDays(10)));
        assertAll(() -> assertEquals("Account A1 is frozen.", error.getMessage()),
                  () -> assertEquals(new BigDecimal("250.00"), account.getBalance()),
                  () -> assertEquals(before, account.getTransactionHistory()),
                  () -> assertTrue(account.isFrozen()));
    }

    @ParameterizedTest
    @MethodSource("postingCases")
    void freezingAfterSuccessfulPostingsBlocksFurtherPostings(Kind kind, Operation operation) {
        Account account = account(kind);
        account.deposit(BigDecimal.TEN, DATE);
        account.withdraw(BigDecimal.ONE, DATE.plusDays(1));
        List<Transaction> before = account.getTransactionHistory();
        account.freeze();
        assertThrows(AccountFrozenException.class,
                () -> post(account, operation, BigDecimal.ONE, DATE.plusDays(2)));
        assertAll(() -> assertEquals(new BigDecimal("259.00"), account.getBalance()),
                  () -> assertEquals(before, account.getTransactionHistory()));
    }

    @ParameterizedTest
    @MethodSource("invalidPostingCases")
    void frozenFailureTakesPrecedenceOverInvalidPostingInput(Kind kind, Operation operation,
                                                            BigDecimal amount, LocalDate date) {
        Account account = account(kind);
        account.freeze();
        List<Transaction> before = account.getTransactionHistory();
        assertThrows(AccountFrozenException.class, () -> post(account, operation, amount, date));
        assertAll(() -> assertEquals(new BigDecimal("250.00"), account.getBalance()),
                  () -> assertEquals(before, account.getTransactionHistory()));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void frozenFailureTakesPrecedenceOverInsufficientFunds(Kind kind) {
        Account account = account(kind);
        account.freeze();
        assertThrows(AccountFrozenException.class, () -> account.withdraw(new BigDecimal("999"), DATE));
        assertEquals(new BigDecimal("250.00"), account.getBalance());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void frozenHistoryRemainsReadableAndRejectsExternalMutation(Kind kind) {
        Account account = account(kind);
        account.deposit(BigDecimal.ONE, DATE);
        account.freeze();
        List<Transaction> readable = account.getTransactionHistory();
        assertThrows(UnsupportedOperationException.class, readable::clear);
        assertAll(() -> assertEquals(2, readable.size()),
                  () -> assertEquals(new BigDecimal("251.00"), readable.get(1).balanceAfter()),
                  () -> assertEquals(readable, account.getTransactionHistory()));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void freezingDoesNotInvalidateAnEarlierHistorySnapshot(Kind kind) {
        Account account = account(kind);
        List<Transaction> before = account.getTransactionHistory();
        account.freeze();
        assertEquals(before, account.getTransactionHistory());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void failedPostingDoesNotImplicitlyFreezeAnActiveAccount(Kind kind) {
        Account account = account(kind);
        assertThrows(InsufficientFundsException.class, () -> account.withdraw(new BigDecimal("999"), DATE));
        assertFalse(account.isFrozen());
        assertEquals(2, account.deposit(BigDecimal.ONE, DATE).id());
    }

    @Test
    void freezingThroughBankLookupIsVisibleInTheRegistrySnapshot() {
        Bank bank = new Bank();
        Customer owner = bank.registerCustomer("C1", "Ada");
        Account account = bank.openCheckingAccount("A1", "C1", BigDecimal.ZERO, DATE);
        List<Account> snapshot = bank.getAccounts();
        bank.findAccount("A1").freeze();
        assertAll(() -> assertTrue(account.isFrozen()),
                  () -> assertTrue(snapshot.get(0).isFrozen()),
                  () -> assertSame(account, bank.findAccount("A1")),
                  () -> assertSame(owner, account.getOwner()),
                  () -> assertEquals(DATE, account.getOpenedOn()),
                  () -> assertEquals(List.of(account), bank.getAccounts()));
    }

    @Test
    void freezingOneAccountDoesNotBlockAnotherAccountOfTheSameOwner() {
        Bank bank = new Bank();
        bank.registerCustomer("C1", "Ada");
        Account frozen = bank.openCheckingAccount("A1", "C1", BigDecimal.ZERO, DATE);
        Account active = bank.openSavingsAccount("A2", "C1", new BigDecimal("100"), DATE);
        frozen.freeze();
        active.deposit(BigDecimal.ONE, DATE);
        assertAll(() -> assertFalse(active.isFrozen()),
                  () -> assertEquals(new BigDecimal("101.00"), active.getBalance()),
                  () -> assertEquals(new BigDecimal("0.00"), frozen.getBalance()));
    }
}

package edu.banking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class MonthlyInterestTest {
    private enum Kind { CHECKING, SAVINGS }
    private static final Customer OWNER = new Customer("C1", "Ada");
    private static final LocalDate OPENED = LocalDate.of(2024, 1, 15);
    private static final LocalDate JANUARY = LocalDate.of(2024, 1, 31);
    private static final LocalDate FEBRUARY = LocalDate.of(2024, 2, 29);

    private Account standard(Kind kind) {
        return kind == Kind.CHECKING ? new CheckingAccount("A1", OWNER, new BigDecimal("1000"), OPENED)
                : new SavingsAccount("A1", OWNER, new BigDecimal("1000"), OPENED);
    }

    private Account custom(Kind kind, InterestPolicy policy) {
        return kind == Kind.CHECKING ? new CheckingAccount("A1", OWNER, new BigDecimal("1000"), OPENED, policy)
                : new SavingsAccount("A1", OWNER, new BigDecimal("1000"), OPENED, policy);
    }

    private static Stream<Arguments> invalidDates() {
        return Stream.of(Kind.values()).flatMap(kind -> Stream.of(
                LocalDate.of(2024, 1, 30), LocalDate.of(2024, 2, 28),
                LocalDate.of(2024, 3, 30), LocalDate.of(2024, 4, 29))
                .map(date -> Arguments.of(kind, date)));
    }

    private static Stream<Arguments> invalidResults() {
        return Stream.of(Kind.values()).flatMap(kind -> Stream.<BigDecimal>of(null,
                new BigDecimal("-0.01"), new BigDecimal("0.001"))
                .map(value -> Arguments.of(kind, value)));
    }

    @ParameterizedTest
    @CsvSource({"CHECKING, 1.00, 1001.00", "SAVINGS, 3.00, 1003.00"})
    void defaultPolicyPostsTheCorrectRateAndOneCompleteInterestEntry(Kind kind, BigDecimal interest, BigDecimal after) {
        Account account = standard(kind);
        List<Transaction> before = account.getTransactionHistory();
        Transaction entry = account.applyMonthlyInterest(JANUARY);
        assertAll(() -> assertEquals(new Transaction(2, "A1", TransactionType.INTEREST, interest,
                          after, JANUARY, OptionalLong.empty()), entry),
                  () -> assertEquals(after, account.getBalance()),
                  () -> assertEquals(List.of(before.get(0), entry), account.getTransactionHistory()),
                  () -> assertSame(entry, account.getTransactionHistory().get(1)),
                  () -> assertEquals(1, before.size()));
    }

    @ParameterizedTest
    @CsvSource({"CHECKING, 1002.00", "SAVINGS, 1006.01"})
    void nextCalendarMonthUsesTheCurrentBalanceIncludingPriorInterest(Kind kind, BigDecimal after) {
        Account account = standard(kind);
        account.applyMonthlyInterest(JANUARY);
        assertEquals(3, account.applyMonthlyInterest(FEBRUARY).id());
        assertEquals(after, account.getBalance());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void repeatedMonthRejectsWithoutChangingBalanceOrHistory(Kind kind) {
        Account account = standard(kind);
        account.applyMonthlyInterest(JANUARY);
        List<Transaction> before = account.getTransactionHistory();
        BigDecimal balance = account.getBalance();
        var error = assertThrows(InterestAlreadyAppliedException.class,
                () -> account.applyMonthlyInterest(JANUARY));
        assertAll(() -> assertEquals("Interest already applied for account A1 in 2024-01.", error.getMessage()),
                  () -> assertEquals(balance, account.getBalance()),
                  () -> assertEquals(before, account.getTransactionHistory()));
        assertEquals(3, account.applyMonthlyInterest(FEBRUARY).id());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void ordinarySameDayPostingsDoNotPermitReapplicationOrRecallThePolicy(Kind kind) {
        AtomicInteger calls = new AtomicInteger();
        Account account = custom(kind, balance -> { calls.incrementAndGet(); return BigDecimal.ONE; });
        account.applyMonthlyInterest(JANUARY);
        account.deposit(BigDecimal.TEN, JANUARY);
        account.withdraw(BigDecimal.ONE, JANUARY);
        List<Transaction> before = account.getTransactionHistory();
        assertThrows(InterestAlreadyAppliedException.class, () -> account.applyMonthlyInterest(JANUARY));
        assertAll(() -> assertEquals(1, calls.get()),
                  () -> assertEquals(before, account.getTransactionHistory()));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void zeroCreditIsRecordedAndStillProtectsTheProcessedMonth(Kind kind) {
        Account account = custom(kind, new FixedRateInterestPolicy(BigDecimal.ZERO));
        Transaction entry = account.applyMonthlyInterest(JANUARY);
        assertAll(() -> assertEquals(new BigDecimal("0.00"), entry.amount()),
                  () -> assertEquals(new BigDecimal("1000.00"), account.getBalance()),
                  () -> assertEquals(2, account.getTransactionHistory().size()));
        assertThrows(InterestAlreadyAppliedException.class, () -> account.applyMonthlyInterest(JANUARY));
    }

    @Test
    void roundedZeroCheckingInterestUsesHalfEvenAndMarksTheMonth() {
        Account account = new CheckingAccount("A1", OWNER, new BigDecimal("5.00"), OPENED);
        assertEquals(new BigDecimal("0.00"), account.applyMonthlyInterest(JANUARY).amount());
        assertThrows(InterestAlreadyAppliedException.class, () -> account.applyMonthlyInterest(JANUARY));
    }

    @Test
    void zeroBalanceCheckingAccountReceivesAZeroInterestRecord() {
        Account account = new CheckingAccount("A1", OWNER, BigDecimal.ZERO, OPENED);
        Transaction entry = account.applyMonthlyInterest(JANUARY);
        assertAll(() -> assertEquals(new BigDecimal("0.00"), entry.amount()),
                  () -> assertEquals(new BigDecimal("0.00"), entry.balanceAfter()));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void nullPolicyIsRejectedByTheInjectedConstructor(Kind kind) {
        var error = assertThrows(InvalidBankingInputException.class, () -> custom(kind, null));
        assertEquals("Interest policy must not be null.", error.getMessage());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void injectedPolicyReceivesThePrePostingBalanceAndOverridesTheDefaultAlgorithm(Kind kind) {
        AtomicReference<BigDecimal> seen = new AtomicReference<>();
        Account account = custom(kind, balance -> { seen.set(balance); return new BigDecimal("7.0000"); });
        Transaction result = account.applyMonthlyInterest(JANUARY);
        assertAll(() -> assertEquals(new BigDecimal("1000.00"), seen.get()),
                  () -> assertEquals(new BigDecimal("7.00"), result.amount()),
                  () -> assertEquals(new BigDecimal("1007.00"), account.getBalance()),
                  () -> assertEquals(standard(kind).getMonthlyInterestRate(), account.getMonthlyInterestRate()));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void missingInterestDateDoesNotCallThePolicyOrReserveAnId(Kind kind) {
        AtomicInteger calls = new AtomicInteger();
        Account account = custom(kind, balance -> { calls.incrementAndGet(); return BigDecimal.ONE; });
        List<Transaction> before = account.getTransactionHistory();
        assertThrows(InvalidBankingInputException.class, () -> account.applyMonthlyInterest(null));
        assertAll(() -> assertEquals(0, calls.get()),
                  () -> assertEquals(before, account.getTransactionHistory()));
        assertEquals(2, account.applyMonthlyInterest(JANUARY).id());
    }

    @ParameterizedTest
    @MethodSource("invalidDates")
    void nonMonthEndDateRejectsWithoutChangingStateOrCallingThePolicy(Kind kind, LocalDate date) {
        AtomicInteger calls = new AtomicInteger();
        Account account = custom(kind, balance -> { calls.incrementAndGet(); return BigDecimal.ONE; });
        List<Transaction> before = account.getTransactionHistory();
        var error = assertThrows(InvalidPostingDateException.class, () -> account.applyMonthlyInterest(date));
        assertAll(() -> assertEquals("Monthly interest date must be the last day of its month.", error.getMessage()),
                  () -> assertEquals(0, calls.get()),
                  () -> assertEquals(before, account.getTransactionHistory()),
                  () -> assertEquals(new BigDecimal("1000.00"), account.getBalance()));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void monthEndBeforeOpeningCannotApplyInterest(Kind kind) {
        Account account = standard(kind);
        List<Transaction> before = account.getTransactionHistory();
        assertThrows(InvalidPostingDateException.class,
                () -> account.applyMonthlyInterest(LocalDate.of(2023, 12, 31)));
        assertEquals(before, account.getTransactionHistory());
        assertEquals(2, account.applyMonthlyInterest(JANUARY).id());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void monthEndBeforeLatestDepositDoesNotReserveTheMonthOrAdvanceTheLedger(Kind kind) {
        Account account = standard(kind);
        account.deposit(BigDecimal.ONE, LocalDate.of(2024, 2, 1));
        List<Transaction> before = account.getTransactionHistory();
        assertThrows(InvalidPostingDateException.class, () -> account.applyMonthlyInterest(JANUARY));
        assertEquals(before, account.getTransactionHistory());
        assertEquals(3, account.applyMonthlyInterest(FEBRUARY).id());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void monthEndBeforeLatestWithdrawalIsRejected(Kind kind) {
        Account account = standard(kind);
        account.withdraw(BigDecimal.ONE, LocalDate.of(2024, 2, 1));
        List<Transaction> before = account.getTransactionHistory();
        BigDecimal balance = account.getBalance();
        assertThrows(InvalidPostingDateException.class, () -> account.applyMonthlyInterest(JANUARY));
        assertAll(() -> assertEquals(before, account.getTransactionHistory()),
                  () -> assertEquals(balance, account.getBalance()));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void frozenStatusPrecedesValidOrMissingInterestDate(Kind kind) {
        AtomicInteger calls = new AtomicInteger();
        Account account = custom(kind, balance -> { calls.incrementAndGet(); return BigDecimal.ONE; });
        account.freeze();
        List<Transaction> before = account.getTransactionHistory();
        assertThrows(AccountFrozenException.class, () -> account.applyMonthlyInterest(JANUARY));
        assertThrows(AccountFrozenException.class, () -> account.applyMonthlyInterest(null));
        assertAll(() -> assertEquals(0, calls.get()),
                  () -> assertEquals(before, account.getTransactionHistory()),
                  () -> assertEquals(new BigDecimal("1000.00"), account.getBalance()));
    }

    @ParameterizedTest
    @MethodSource("invalidResults")
    void invalidPolicyResultPreservesTheMonthAndSequenceForAValidRetry(Kind kind, BigDecimal invalid) {
        AtomicInteger calls = new AtomicInteger();
        Account account = custom(kind, balance -> calls.getAndIncrement() == 0 ? invalid : BigDecimal.ONE);
        List<Transaction> before = account.getTransactionHistory();
        assertThrows(InvalidBankingInputException.class, () -> account.applyMonthlyInterest(JANUARY));
        assertAll(() -> assertEquals(before, account.getTransactionHistory()),
                  () -> assertEquals(new BigDecimal("1000.00"), account.getBalance()));
        Transaction retried = account.applyMonthlyInterest(JANUARY);
        assertAll(() -> assertEquals(2, retried.id()),
                  () -> assertEquals(new BigDecimal("1.00"), retried.amount()));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void domainPolicyFailurePropagatesWithoutPublishingOrReservingTheMonth(Kind kind) {
        AtomicInteger calls = new AtomicInteger();
        BankingException failure = new InvalidBankingInputException("Policy rejected this input.");
        Account account = custom(kind, balance -> {
            if (calls.getAndIncrement() == 0) { throw failure; }
            return BigDecimal.ONE;
        });
        List<Transaction> before = account.getTransactionHistory();
        assertSame(failure, assertThrows(BankingException.class, () -> account.applyMonthlyInterest(JANUARY)));
        assertEquals(before, account.getTransactionHistory());
        assertEquals(2, account.applyMonthlyInterest(JANUARY).id());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void unexpectedPolicyFailureIsWrappedWithItsCauseAndAllowsRetry(Kind kind) {
        AtomicInteger calls = new AtomicInteger();
        RuntimeException cause = new IllegalStateException("Policy service unavailable.");
        Account account = custom(kind, balance -> {
            if (calls.getAndIncrement() == 0) { throw cause; }
            return BigDecimal.ZERO;
        });
        List<Transaction> before = account.getTransactionHistory();
        var error = assertThrows(InterestCalculationException.class, () -> account.applyMonthlyInterest(JANUARY));
        assertAll(() -> assertSame(cause, error.getCause()),
                  () -> assertEquals("Interest policy failed for account A1.", error.getMessage()),
                  () -> assertEquals(before, account.getTransactionHistory()),
                  () -> assertEquals(new BigDecimal("1000.00"), account.getBalance()));
        assertEquals(2, account.applyMonthlyInterest(JANUARY).id());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void successfulZeroInterestInvalidatesAnOlderPreparedPlan(Kind kind) {
        Account account = custom(kind, new FixedRateInterestPolicy(BigDecimal.ZERO));
        Account.PreparedPosting plan = account.prepareDeposit(BigDecimal.ONE, OPENED);
        account.applyMonthlyInterest(JANUARY);
        assertThrows(StalePostingException.class, plan::commit);
        assertEquals(2, account.getTransactionHistory().size());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void rejectedInterestDoesNotInvalidateAnExistingPreparedPlan(Kind kind) {
        Account account = custom(kind, balance -> null);
        Account.PreparedPosting plan = account.prepareDeposit(BigDecimal.ONE, OPENED);
        assertThrows(InvalidBankingInputException.class, () -> account.applyMonthlyInterest(JANUARY));
        assertEquals(2, plan.commit().id());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void theSameMonthNumberInADifferentYearIsNotADuplicate(Kind kind) {
        Account account = standard(kind);
        account.applyMonthlyInterest(JANUARY);
        Transaction later = account.applyMonthlyInterest(LocalDate.of(2025, 1, 31));
        assertAll(() -> assertEquals(3, later.id()),
                  () -> assertEquals(3, account.getTransactionHistory().size()),
                  () -> assertEquals(LocalDate.of(2025, 1, 31), later.date()));
    }

    @Test
    void firstMonthMayApplyOnTheOpeningMonthEndWithoutProration() {
        Account account = new SavingsAccount("A1", OWNER, new BigDecimal("1000"), JANUARY);
        assertEquals(new BigDecimal("3.00"), account.applyMonthlyInterest(JANUARY).amount());
    }

    @Test
    void bankRegisteredAccountsUseTheirDefaultInjectedPolicies() {
        Bank bank = new Bank();
        bank.registerCustomer("C1", "Ada");
        bank.openCheckingAccount("C", "C1", new BigDecimal("1000"), OPENED);
        bank.openSavingsAccount("S", "C1", new BigDecimal("1000"), OPENED);
        bank.findAccount("C").applyMonthlyInterest(JANUARY);
        bank.findAccount("S").applyMonthlyInterest(JANUARY);
        assertAll(() -> assertEquals(new BigDecimal("1001.00"), bank.findAccount("C").getBalance()),
                  () -> assertEquals(new BigDecimal("1003.00"), bank.findAccount("S").getBalance()));
    }
}

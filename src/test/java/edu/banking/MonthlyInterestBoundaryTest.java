package edu.banking;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class MonthlyInterestBoundaryTest {
    private enum Kind { CHECKING, SAVINGS }
    private enum Failure { NULL, NEGATIVE, SUB_CENT, DOMAIN, UNEXPECTED }
    private static final Customer OWNER = new Customer("C1", "Ada");
    private static final LocalDate OPENED = LocalDate.of(2024, 1, 1);
    private static final LocalDate JANUARY = LocalDate.of(2024, 1, 31);
    private static final LocalDate FEBRUARY = LocalDate.of(2024, 2, 29);
    private static final LocalDate MARCH = LocalDate.of(2024, 3, 31);

    private Account standard(Kind kind, BigDecimal balance, LocalDate openedOn) {
        return kind == Kind.CHECKING ? new CheckingAccount("A1", OWNER, balance, openedOn)
                : new SavingsAccount("A1", OWNER, balance, openedOn);
    }

    private Account custom(Kind kind, InterestPolicy policy) {
        return kind == Kind.CHECKING ? new CheckingAccount("A1", OWNER, new BigDecimal("1000"), OPENED, policy)
                : new SavingsAccount("A1", OWNER, new BigDecimal("1000"), OPENED, policy);
    }

    private static Stream<Arguments> calendarBoundaries() {
        return Stream.of(Kind.values()).flatMap(kind -> Stream.of(
                LocalDate.of(2000, 2, 29), LocalDate.of(2100, 2, 28),
                LocalDate.of(2023, 2, 28), LocalDate.of(2024, 12, 31))
                .map(date -> Arguments.of(kind, date)));
    }

    private static Stream<Arguments> policyFailures() {
        return Stream.of(Kind.values()).flatMap(kind -> Stream.of(Failure.values())
                .map(failure -> Arguments.of(kind, failure)));
    }

    private static BigDecimal fail(Failure failure) {
        return switch (failure) {
            case NULL -> null;
            case NEGATIVE -> new BigDecimal("-0.01");
            case SUB_CENT -> new BigDecimal("0.001");
            case DOMAIN -> throw new InvalidBankingInputException("Policy rejected the balance.");
            case UNEXPECTED -> throw new IllegalStateException("Policy unavailable.");
        };
    }

    @ParameterizedTest
    @CsvSource({
        "CHECKING, 1004.99, 1.00, 1005.99", "CHECKING, 1005.00, 1.00, 1006.00",
        "CHECKING, 1005.01, 1.01, 1006.02", "CHECKING, 1014.99, 1.01, 1016.00",
        "CHECKING, 1015.00, 1.02, 1016.02", "CHECKING, 1015.01, 1.02, 1016.03",
        "SAVINGS, 1004.99, 3.01, 1008.00", "SAVINGS, 1005.00, 3.02, 1008.02",
        "SAVINGS, 1005.01, 3.02, 1008.03", "SAVINGS, 1014.99, 3.04, 1018.03",
        "SAVINGS, 1015.00, 3.04, 1018.04", "SAVINGS, 1015.01, 3.05, 1018.06"
    })
    void postedCreditsAndBalancesRespectHalfEvenTiesAndTheirNeighbors(Kind kind, BigDecimal balance,
                                                                    BigDecimal interest, BigDecimal after) {
        Account account = standard(kind, balance, OPENED);
        Transaction entry = account.applyMonthlyInterest(JANUARY);
        assertAll(() -> assertEquals(new Transaction(2, "A1", TransactionType.INTEREST,
                          interest, after, JANUARY, OptionalLong.empty()), entry),
                  () -> assertEquals(after, account.getBalance()),
                  () -> assertEquals(entry, account.getTransactionHistory().get(1)));
    }

    @ParameterizedTest
    @MethodSource("calendarBoundaries")
    void calendarMonthEndsIncludeCommonFebruaryAndGregorianCenturyRules(Kind kind, LocalDate monthEnd) {
        Account account = standard(kind, new BigDecimal("1000"), monthEnd.withDayOfMonth(1));
        Transaction entry = account.applyMonthlyInterest(monthEnd);
        assertAll(() -> assertEquals(monthEnd, entry.date()),
                  () -> assertEquals(2, entry.id()),
                  () -> assertEquals(TransactionType.INTEREST, entry.type()));
    }

    @ParameterizedTest
    @CsvSource({"CHECKING, 1.00, 1002.00", "SAVINGS, 3.01, 1006.01"})
    void skippingFebruaryCreditsOnlyMarchAndDoesNotAllowBackfilling(Kind kind, BigDecimal marchCredit,
                                                                 BigDecimal after) {
        Account account = standard(kind, new BigDecimal("1000"), OPENED);
        Transaction january = account.applyMonthlyInterest(JANUARY);
        List<Transaction> prior = account.getTransactionHistory();
        Transaction march = account.applyMonthlyInterest(MARCH);
        assertAll(() -> assertEquals(marchCredit, march.amount()),
                  () -> assertEquals(after, account.getBalance()),
                  () -> assertEquals(List.of(prior.get(0), january, march), account.getTransactionHistory()),
                  () -> assertEquals(3, march.id()),
                  () -> assertEquals(2, prior.size()));
        List<Transaction> beforeRejection = account.getTransactionHistory();
        assertThrows(InvalidPostingDateException.class, () -> account.applyMonthlyInterest(FEBRUARY));
        assertEquals(beforeRejection, account.getTransactionHistory());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void anOldProcessedMonthFailsChronologyBeforeDuplicateDetectionOrCalculation(Kind kind) {
        AtomicInteger calls = new AtomicInteger();
        Account account = custom(kind, balance -> { calls.incrementAndGet(); return BigDecimal.ONE; });
        account.applyMonthlyInterest(JANUARY);
        account.applyMonthlyInterest(MARCH);
        List<Transaction> before = account.getTransactionHistory();
        var error = assertThrows(InvalidPostingDateException.class, () -> account.applyMonthlyInterest(JANUARY));
        assertAll(() -> assertEquals("Posting date 2024-01-31 must not precede latest posting date 2024-03-31.",
                                    error.getMessage()),
                  () -> assertEquals(2, calls.get()),
                  () -> assertEquals(before, account.getTransactionHistory()),
                  () -> assertEquals(new BigDecimal("1002.00"), account.getBalance()));
    }

    @ParameterizedTest
    @MethodSource("policyFailures")
    void failedLaterMonthKeepsPriorMarkersAndAllowsAnEarlierMonthThenRetry(Kind kind, Failure failure) {
        AtomicInteger calls = new AtomicInteger();
        Account account = custom(kind, balance -> calls.incrementAndGet() == 2 ? fail(failure) : BigDecimal.ONE);
        Transaction january = account.applyMonthlyInterest(JANUARY);
        List<Transaction> before = account.getTransactionHistory();
        Class<? extends BankingException> expected = failure == Failure.UNEXPECTED
                ? InterestCalculationException.class : InvalidBankingInputException.class;
        assertThrows(expected, () -> account.applyMonthlyInterest(MARCH));
        assertAll(() -> assertEquals(before, account.getTransactionHistory()),
                  () -> assertEquals(new BigDecimal("1001.00"), account.getBalance()));
        assertThrows(InterestAlreadyAppliedException.class, () -> account.applyMonthlyInterest(JANUARY));
        Transaction february = account.applyMonthlyInterest(FEBRUARY);
        Transaction march = account.applyMonthlyInterest(MARCH);
        assertAll(() -> assertEquals(3, february.id()),
                  () -> assertEquals(4, march.id()),
                  () -> assertEquals(new BigDecimal("1003.00"), account.getBalance()),
                  () -> assertEquals(List.of(before.get(0), january, february, march), account.getTransactionHistory()),
                  () -> assertEquals(4, calls.get()),
                  () -> assertEquals(2, before.size()));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void realFixedPolicyArithmeticFailureDoesNotAdvancePostingChronology(Kind kind) {
        InterestPolicy policy = new FixedRateInterestPolicy(new BigDecimal(BigInteger.ONE, Integer.MAX_VALUE));
        Account account = custom(kind, policy);
        List<Transaction> before = account.getTransactionHistory();
        var error = assertThrows(InvalidBankingInputException.class, () -> account.applyMonthlyInterest(MARCH));
        assertAll(() -> assertEquals("Interest calculation exceeds supported decimal precision.", error.getMessage()),
                  () -> assertEquals(before, account.getTransactionHistory()),
                  () -> assertEquals(new BigDecimal("1000.00"), account.getBalance()));
        Transaction deposit = account.deposit(BigDecimal.ONE, OPENED.plusDays(1));
        assertAll(() -> assertEquals(2, deposit.id()),
                  () -> assertEquals(OPENED.plusDays(1), deposit.date()),
                  () -> assertEquals(new BigDecimal("1001.00"), account.getBalance()));
    }

    @Test
    void roundedZeroMonthsDoNotAccumulateHiddenFractionsOrPermitRepeatCredits() {
        Account account = new CheckingAccount("A1", OWNER, new BigDecimal("5.00"), OPENED);
        List<LocalDate> dates = List.of(JANUARY, FEBRUARY, MARCH, LocalDate.of(2024, 4, 30));
        for (int i = 0; i < dates.size(); i++) {
            LocalDate date = dates.get(i);
            Transaction entry = account.applyMonthlyInterest(date);
            assertEquals(new BigDecimal("0.00"), entry.amount());
            assertEquals(i + 2L, entry.id());
            assertThrows(InterestAlreadyAppliedException.class, () -> account.applyMonthlyInterest(date));
        }
        assertAll(() -> assertEquals(new BigDecimal("5.00"), account.getBalance()),
                  () -> assertEquals(5, account.getTransactionHistory().size()));
    }
}

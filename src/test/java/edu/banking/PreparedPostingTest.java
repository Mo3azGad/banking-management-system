package edu.banking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class PreparedPostingTest {
    private enum Kind { CHECKING, SAVINGS }
    private enum Operation { DEPOSIT, WITHDRAWAL }
    private static final Customer OWNER = new Customer("C1", "Ada");
    private static final LocalDate DATE = LocalDate.of(2024, 2, 29);

    private Account account(Kind kind) {
        return switch (kind) {
            case CHECKING -> new CheckingAccount("A1", OWNER, new BigDecimal("250"), DATE);
            case SAVINGS -> new SavingsAccount("A1", OWNER, new BigDecimal("250"), DATE);
        };
    }

    private Account.PreparedPosting prepare(Account account, Operation operation, BigDecimal amount, LocalDate date) {
        return operation == Operation.DEPOSIT ? account.prepareDeposit(amount, date)
                : account.prepareWithdrawal(amount, date);
    }

    private static Stream<Arguments> cases() {
        return Stream.of(Kind.values()).flatMap(kind -> Stream.of(Operation.values())
                .map(operation -> Arguments.of(kind, operation)));
    }

    private static Stream<Arguments> invalidInputs() {
        return cases().flatMap(args -> Stream.of(
                Arguments.of(args.get()[0], args.get()[1], null, DATE),
                Arguments.of(args.get()[0], args.get()[1], BigDecimal.ZERO, DATE),
                Arguments.of(args.get()[0], args.get()[1], new BigDecimal("-0.01"), DATE),
                Arguments.of(args.get()[0], args.get()[1], new BigDecimal("1.001"), DATE),
                Arguments.of(args.get()[0], args.get()[1], BigDecimal.ONE, null)));
    }

    @ParameterizedTest
    @MethodSource("cases")
    void preparingAndValidatingDoesNotPublishFinancialState(Kind kind, Operation operation) {
        Account account = account(kind);
        List<Transaction> before = account.getTransactionHistory();
        Account.PreparedPosting plan = prepare(account, operation, new BigDecimal("10.000"), DATE.plusDays(1));
        plan.validate();
        TransactionType type = operation == Operation.DEPOSIT ? TransactionType.DEPOSIT : TransactionType.WITHDRAWAL;
        BigDecimal after = new BigDecimal(operation == Operation.DEPOSIT ? "260.00" : "240.00");
        assertAll(() -> assertEquals(new BigDecimal("250.00"), account.getBalance()),
                  () -> assertEquals(before, account.getTransactionHistory()),
                  () -> assertFalse(account.isFrozen()),
                  () -> assertEquals(new Transaction(2, "A1", type, new BigDecimal("10.00"), after,
                          DATE.plusDays(1), OptionalLong.empty()), plan.transaction()));
    }

    @ParameterizedTest
    @MethodSource("cases")
    void commitPublishesExactlyThePreparedFactsAndPreservesOldSnapshot(Kind kind, Operation operation) {
        Account account = account(kind);
        List<Transaction> old = account.getTransactionHistory();
        Account.PreparedPosting plan = prepare(account, operation, BigDecimal.ONE, DATE);
        Transaction entry = plan.commit();
        assertAll(() -> assertSame(plan.transaction(), entry),
                  () -> assertEquals(entry.balanceAfter(), account.getBalance()),
                  () -> assertEquals(List.of(old.get(0), entry), account.getTransactionHistory()),
                  () -> assertEquals(1, old.size()));
        assertThrows(UnsupportedOperationException.class, () -> account.getTransactionHistory().clear());
    }

    @ParameterizedTest
    @MethodSource("invalidInputs")
    void rejectedPreparationPreservesBalanceHistoryAndExistingValidPlan(Kind kind, Operation operation,
                                                                      BigDecimal amount, LocalDate date) {
        Account account = account(kind);
        List<Transaction> before = account.getTransactionHistory();
        Account.PreparedPosting valid = prepare(account, operation, BigDecimal.ONE, DATE);
        assertThrows(InvalidBankingInputException.class, () -> prepare(account, operation, amount, date));
        assertAll(() -> assertEquals(new BigDecimal("250.00"), account.getBalance()),
                  () -> assertEquals(before, account.getTransactionHistory()));
        assertEquals(2, valid.commit().id());
    }

    @ParameterizedTest
    @MethodSource("cases")
    void backdatedPreparationDoesNotInvalidateAValidPlan(Kind kind, Operation operation) {
        Account account = account(kind);
        Account.PreparedPosting valid = prepare(account, operation, BigDecimal.ONE, DATE);
        assertThrows(InvalidPostingDateException.class,
                () -> prepare(account, operation, BigDecimal.ONE, DATE.minusDays(1)));
        assertEquals(2, valid.commit().id());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void insufficientFundsPreparationPreservesStateAndSequence(Kind kind) {
        Account account = account(kind);
        List<Transaction> before = account.getTransactionHistory();
        assertThrows(InsufficientFundsException.class,
                () -> account.prepareWithdrawal(account.getAvailableBalance().add(new BigDecimal("0.01")), DATE));
        assertEquals(before, account.getTransactionHistory());
        assertEquals(2, account.prepareDeposit(BigDecimal.ONE, DATE).commit().id());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void preparedWithdrawalMayCommitExactlyAtTheFloor(Kind kind) {
        Account account = account(kind);
        account.prepareWithdrawal(account.getAvailableBalance(), DATE).commit();
        assertEquals(account.getMinimumBalance(), account.getBalance());
    }

    @ParameterizedTest
    @MethodSource("cases")
    void frozenAccountRejectsPreparationBeforeInputValidation(Kind kind, Operation operation) {
        Account account = account(kind);
        account.freeze();
        List<Transaction> before = account.getTransactionHistory();
        assertThrows(AccountFrozenException.class, () -> prepare(account, operation, null, null));
        assertEquals(before, account.getTransactionHistory());
    }

    @ParameterizedTest
    @MethodSource("cases")
    void freezingAfterPreparationPreventsValidationAndCommit(Kind kind, Operation operation) {
        Account account = account(kind);
        Account.PreparedPosting plan = prepare(account, operation, BigDecimal.ONE, DATE.plusDays(1));
        List<Transaction> before = account.getTransactionHistory();
        account.freeze();
        assertThrows(AccountFrozenException.class, plan::validate);
        assertThrows(AccountFrozenException.class, plan::commit);
        assertAll(() -> assertEquals(new BigDecimal("250.00"), account.getBalance()),
                  () -> assertEquals(before, account.getTransactionHistory()),
                  () -> assertTrue(account.isFrozen()));
    }

    @ParameterizedTest
    @MethodSource("cases")
    void frozenStatusTakesPrecedenceWhenAPreparedPlanIsAlsoStale(Kind kind, Operation operation) {
        Account account = account(kind);
        Account.PreparedPosting plan = prepare(account, operation, BigDecimal.ONE, DATE);
        account.deposit(BigDecimal.TEN, DATE);
        account.freeze();
        List<Transaction> before = account.getTransactionHistory();
        assertThrows(AccountFrozenException.class, plan::validate);
        assertThrows(AccountFrozenException.class, plan::commit);
        assertAll(() -> assertEquals(new BigDecimal("260.00"), account.getBalance()),
                  () -> assertEquals(before, account.getTransactionHistory()));
    }

    @ParameterizedTest
    @MethodSource("cases")
    void committedPlanCannotBeValidatedOrCommittedAgain(Kind kind, Operation operation) {
        Account account = account(kind);
        Account.PreparedPosting plan = prepare(account, operation, BigDecimal.ONE, DATE);
        plan.commit();
        List<Transaction> beforeRetry = account.getTransactionHistory();
        var error = assertThrows(StalePostingException.class, plan::validate);
        assertThrows(StalePostingException.class, plan::commit);
        assertAll(() -> assertEquals("Prepared posting for account A1 no longer matches its ledger.", error.getMessage()),
                  () -> assertEquals(beforeRetry, account.getTransactionHistory()),
                  () -> assertEquals(beforeRetry.get(1).balanceAfter(), account.getBalance()));
    }

    @ParameterizedTest
    @MethodSource("cases")
    void interveningDepositInvalidatesAPreparedPlanWithoutOverwritingNewState(Kind kind, Operation operation) {
        Account account = account(kind);
        Account.PreparedPosting plan = prepare(account, operation, BigDecimal.ONE, DATE);
        account.deposit(BigDecimal.TEN, DATE);
        List<Transaction> afterDeposit = account.getTransactionHistory();
        assertThrows(StalePostingException.class, plan::commit);
        assertAll(() -> assertEquals(new BigDecimal("260.00"), account.getBalance()),
                  () -> assertEquals(afterDeposit, account.getTransactionHistory()));
        assertEquals(3, account.deposit(BigDecimal.ONE, DATE).id());
    }

    @ParameterizedTest
    @MethodSource("cases")
    void interveningWithdrawalInvalidatesAPreparedPlan(Kind kind, Operation operation) {
        Account account = account(kind);
        Account.PreparedPosting plan = prepare(account, operation, BigDecimal.ONE, DATE);
        account.withdraw(BigDecimal.TEN, DATE);
        List<Transaction> afterWithdrawal = account.getTransactionHistory();
        assertThrows(StalePostingException.class, plan::commit);
        assertAll(() -> assertEquals(new BigDecimal("240.00"), account.getBalance()),
                  () -> assertEquals(afterWithdrawal, account.getTransactionHistory()));
    }

    @ParameterizedTest
    @MethodSource("cases")
    void returningToTheSameBalanceDoesNotReviveAnOldPlan(Kind kind, Operation operation) {
        Account account = account(kind);
        Account.PreparedPosting plan = prepare(account, operation, BigDecimal.ONE, DATE);
        account.deposit(BigDecimal.ONE, DATE);
        account.withdraw(BigDecimal.ONE, DATE);
        assertThrows(StalePostingException.class, plan::commit);
        assertAll(() -> assertEquals(new BigDecimal("250.00"), account.getBalance()),
                  () -> assertEquals(3, account.getTransactionHistory().size()));
    }

    @ParameterizedTest
    @MethodSource("cases")
    void preparationDoesNotReserveAnIdentifierOrAdvanceTheDate(Kind kind, Operation operation) {
        Account account = account(kind);
        Account.PreparedPosting future = prepare(account, operation, BigDecimal.ONE, DATE.plusDays(10));
        Transaction actual = account.deposit(BigDecimal.ONE, DATE);
        assertAll(() -> assertEquals(2, future.transaction().id()),
                  () -> assertEquals(2, actual.id()),
                  () -> assertEquals(DATE, actual.date()));
        assertThrows(StalePostingException.class, future::commit);
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void competingPlansForOneAccountCannotBothCommit(Kind kind) {
        Account account = account(kind);
        Account.PreparedPosting first = account.prepareDeposit(BigDecimal.TEN, DATE);
        Account.PreparedPosting second = account.prepareWithdrawal(BigDecimal.TEN, DATE);
        first.validate();
        second.validate();
        first.commit();
        assertThrows(StalePostingException.class, second::commit);
        assertAll(() -> assertEquals(new BigDecimal("260.00"), account.getBalance()),
                  () -> assertEquals(2, account.getTransactionHistory().size()));
    }

    @ParameterizedTest
    @MethodSource("cases")
    void rejectedPublicPostingDoesNotInvalidateAnExistingPlan(Kind kind, Operation operation) {
        Account account = account(kind);
        Account.PreparedPosting plan = prepare(account, operation, BigDecimal.ONE, DATE);
        assertThrows(InsufficientFundsException.class, () -> account.withdraw(new BigDecimal("999"), DATE));
        assertEquals(2, plan.commit().id());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void planIsBoundToItsOriginatingObjectEvenWhenAnotherAccountHasTheSameId(Kind kind) {
        Account owner = account(kind);
        Account other = account(kind);
        Account.PreparedPosting plan = owner.prepareDeposit(BigDecimal.TEN, DATE);
        other.deposit(BigDecimal.ONE, DATE);
        plan.commit();
        assertAll(() -> assertEquals(new BigDecimal("260.00"), owner.getBalance()),
                  () -> assertEquals(new BigDecimal("251.00"), other.getBalance()));
    }

    @Test
    void failedSecondPreparationLeavesTheFirstAccountUncommitted() {
        Account first = account(Kind.CHECKING);
        Account second = account(Kind.SAVINGS);
        List<Transaction> firstBefore = first.getTransactionHistory();
        List<Transaction> secondBefore = second.getTransactionHistory();
        Account.PreparedPosting firstPlan = first.prepareWithdrawal(BigDecimal.TEN, DATE);
        second.freeze();
        assertThrows(AccountFrozenException.class, () -> second.prepareDeposit(BigDecimal.TEN, DATE));
        firstPlan.validate();
        assertAll(() -> assertEquals(firstBefore, first.getTransactionHistory()),
                  () -> assertEquals(secondBefore, second.getTransactionHistory()),
                  () -> assertEquals(new BigDecimal("250.00"), first.getBalance()));
    }

    @Test
    void prevalidationDetectsAStaleSecondPlanBeforeTheFirstAccountIsCommitted() {
        Account first = account(Kind.CHECKING);
        Account second = account(Kind.SAVINGS);
        Account.PreparedPosting firstPlan = first.prepareDeposit(BigDecimal.ONE, DATE);
        Account.PreparedPosting secondPlan = second.prepareDeposit(BigDecimal.ONE, DATE);
        second.withdraw(BigDecimal.ONE, DATE);
        firstPlan.validate();
        assertThrows(StalePostingException.class, secondPlan::validate);
        assertAll(() -> assertEquals(new BigDecimal("250.00"), first.getBalance()),
                  () -> assertEquals(1, first.getTransactionHistory().size()),
                  () -> assertEquals(new BigDecimal("249.00"), second.getBalance()));
    }

    @Test
    void distinctPrevalidatedAccountPlansDoNotInvalidateEachOther() {
        Account first = account(Kind.CHECKING);
        Account second = account(Kind.SAVINGS);
        Account.PreparedPosting one = first.prepareDeposit(BigDecimal.ONE, DATE);
        Account.PreparedPosting two = second.prepareDeposit(BigDecimal.TEN, DATE);
        one.validate();
        two.validate();
        one.commit();
        two.commit();
        assertAll(() -> assertEquals(new BigDecimal("251.00"), first.getBalance()),
                  () -> assertEquals(new BigDecimal("260.00"), second.getBalance()),
                  () -> assertEquals(TransactionType.DEPOSIT, one.transaction().type()),
                  () -> assertEquals(TransactionType.DEPOSIT, two.transaction().type()));
    }
}

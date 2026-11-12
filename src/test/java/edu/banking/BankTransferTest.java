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
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class BankTransferTest {
    private enum Kind { CHECKING, SAVINGS }
    private enum Side { SOURCE, TARGET }
    private static final LocalDate DATE = LocalDate.of(2024, 2, 29);
    private record Fixture(Bank bank, Account source, Account target) { }
    private record Snapshot(BigDecimal sourceBalance, BigDecimal targetBalance,
                            List<Transaction> sourceHistory, List<Transaction> targetHistory) {
        static Snapshot of(Fixture fixture) {
            return new Snapshot(fixture.source().getBalance(), fixture.target().getBalance(),
                    fixture.source().getTransactionHistory(), fixture.target().getTransactionHistory());
        }
    }

    private Account open(Bank bank, Kind kind, String id, String customer, String balance, LocalDate date) {
        return kind == Kind.CHECKING ? bank.openCheckingAccount(id, customer, new BigDecimal(balance), date)
                : bank.openSavingsAccount(id, customer, new BigDecimal(balance), date);
    }

    private Fixture fixture(Kind sourceKind, Kind targetKind) {
        Bank bank = new Bank();
        bank.registerCustomer("C1", "Ada");
        bank.registerCustomer("C2", "Grace");
        return new Fixture(bank, open(bank, sourceKind, "S", "C1", "250", DATE.minusDays(1)),
                open(bank, targetKind, "T", "C2", "150", DATE));
    }

    private static Stream<Arguments> combinations() {
        return Stream.of(Kind.values()).flatMap(source -> Stream.of(Kind.values())
                .map(target -> Arguments.of(source, target)));
    }

    private static Stream<Arguments> invalidAmounts() {
        return combinations().flatMap(args -> Stream.<BigDecimal>of(null, BigDecimal.ZERO,
                new BigDecimal("-0.01"), new BigDecimal("1.001"))
                .map(amount -> Arguments.of(args.get()[0], args.get()[1], amount)));
    }

    private static Stream<Arguments> invalidIdentifiers() {
        return Stream.of(Side.values()).flatMap(side -> Stream.<String>of(null, "", " ", "\t\n", "\u2003")
                .map(id -> Arguments.of(side, id)));
    }

    private static Stream<Arguments> frozenAccounts() {
        return combinations().flatMap(args -> Stream.of(Side.values())
                .map(side -> Arguments.of(args.get()[0], args.get()[1], side)));
    }

    private void assertUnchanged(Fixture fixture, Snapshot before) {
        assertEquals(before, Snapshot.of(fixture));
    }

    private void assertFirstValidTransferStillUsesFirstReference(Fixture fixture) {
        List<Transaction> receipt = fixture.bank().transfer("S", "T", BigDecimal.ONE, DATE);
        assertAll(() -> assertEquals(OptionalLong.of(1), receipt.get(0).transferReference()),
                  () -> assertEquals(2, receipt.get(0).id()),
                  () -> assertEquals(2, receipt.get(1).id()));
    }

    @ParameterizedTest
    @MethodSource("combinations")
    void transferConservesMoneyAndPostsOneLinkedEntryPerAccount(Kind sourceKind, Kind targetKind) {
        Fixture f = fixture(sourceKind, targetKind);
        Snapshot before = Snapshot.of(f);
        List<Transaction> receipt = f.bank().transfer(" S ", " T ", new BigDecimal("10.0100"), DATE);
        Transaction out = new Transaction(2, "S", TransactionType.TRANSFER_OUT, new BigDecimal("10.01"),
                new BigDecimal("239.99"), DATE, OptionalLong.of(1));
        Transaction in = new Transaction(2, "T", TransactionType.TRANSFER_IN, new BigDecimal("10.01"),
                new BigDecimal("160.01"), DATE, OptionalLong.of(1));
        assertAll(() -> assertEquals(List.of(out, in), receipt),
                  () -> assertEquals(new BigDecimal("239.99"), f.source().getBalance()),
                  () -> assertEquals(new BigDecimal("160.01"), f.target().getBalance()),
                  () -> assertEquals(new BigDecimal("400.00"), f.source().getBalance().add(f.target().getBalance())),
                  () -> assertEquals(List.of(before.sourceHistory().get(0), out), f.source().getTransactionHistory()),
                  () -> assertEquals(List.of(before.targetHistory().get(0), in), f.target().getTransactionHistory()),
                  () -> assertSame(receipt.get(0), f.source().getTransactionHistory().get(1)),
                  () -> assertSame(receipt.get(1), f.target().getTransactionHistory().get(1)));
    }

    @ParameterizedTest
    @MethodSource("combinations")
    void exactAvailableBalanceMayBeTransferredLeavingTheSourceFloor(Kind sourceKind, Kind targetKind) {
        Fixture f = fixture(sourceKind, targetKind);
        f.bank().transfer("S", "T", f.source().getAvailableBalance(), DATE);
        assertAll(() -> assertEquals(f.source().getMinimumBalance(), f.source().getBalance()),
                  () -> assertEquals(new BigDecimal("400.00"), f.source().getBalance().add(f.target().getBalance())));
    }

    @ParameterizedTest
    @MethodSource("combinations")
    void oneCentOverAvailableFundsPreservesBothAccountsAndSequences(Kind sourceKind, Kind targetKind) {
        Fixture f = fixture(sourceKind, targetKind);
        Snapshot before = Snapshot.of(f);
        assertThrows(InsufficientFundsException.class, () -> f.bank().transfer("S", "T",
                f.source().getAvailableBalance().add(new BigDecimal("0.01")), DATE));
        assertUnchanged(f, before);
        assertFirstValidTransferStillUsesFirstReference(f);
    }

    @ParameterizedTest
    @MethodSource("combinations")
    void accountAlreadyAtItsFloorCannotTransferOneCent(Kind sourceKind, Kind targetKind) {
        Fixture f = fixture(sourceKind, targetKind);
        f.source().withdraw(f.source().getAvailableBalance(), DATE);
        Snapshot before = Snapshot.of(f);
        assertThrows(InsufficientFundsException.class,
                () -> f.bank().transfer("S", "T", new BigDecimal("0.01"), DATE));
        assertUnchanged(f, before);
    }

    @ParameterizedTest
    @MethodSource("invalidAmounts")
    void invalidTransferAmountPreservesBothAccountsAndReferenceSequence(Kind sourceKind, Kind targetKind,
                                                                     BigDecimal amount) {
        Fixture f = fixture(sourceKind, targetKind);
        Snapshot before = Snapshot.of(f);
        assertThrows(InvalidBankingInputException.class, () -> f.bank().transfer("S", "T", amount, DATE));
        assertUnchanged(f, before);
        assertFirstValidTransferStillUsesFirstReference(f);
    }

    @ParameterizedTest
    @MethodSource("invalidIdentifiers")
    void missingAccountIdentifierRejectsTransferWithoutMutation(Side side, String id) {
        Fixture f = fixture(Kind.CHECKING, Kind.SAVINGS);
        Snapshot before = Snapshot.of(f);
        assertThrows(InvalidBankingInputException.class,
                () -> f.bank().transfer(side == Side.SOURCE ? id : "S", side == Side.TARGET ? id : "T",
                        BigDecimal.ONE, DATE));
        assertUnchanged(f, before);
        assertFirstValidTransferStillUsesFirstReference(f);
    }

    @ParameterizedTest
    @EnumSource(Side.class)
    void unregisteredAccountRejectsTransferWithoutMutation(Side side) {
        Fixture f = fixture(Kind.CHECKING, Kind.SAVINGS);
        Snapshot before = Snapshot.of(f);
        assertThrows(AccountNotFoundException.class,
                () -> f.bank().transfer(side == Side.SOURCE ? "missing" : "S",
                        side == Side.TARGET ? "missing" : "T", BigDecimal.ONE, DATE));
        assertUnchanged(f, before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"S", " S "})
    void normalizedSameAccountTransferIsRejected(String target) {
        Fixture f = fixture(Kind.CHECKING, Kind.SAVINGS);
        Snapshot before = Snapshot.of(f);
        var error = assertThrows(InvalidBankingInputException.class,
                () -> f.bank().transfer("S", target, BigDecimal.ONE, DATE));
        assertEquals("Transfer requires two distinct accounts.", error.getMessage());
        assertUnchanged(f, before);
        assertFirstValidTransferStillUsesFirstReference(f);
    }

    @ParameterizedTest
    @MethodSource("combinations")
    void missingPostingDatePreservesBothLedgers(Kind sourceKind, Kind targetKind) {
        Fixture f = fixture(sourceKind, targetKind);
        Snapshot before = Snapshot.of(f);
        assertThrows(InvalidBankingInputException.class, () -> f.bank().transfer("S", "T", BigDecimal.ONE, null));
        assertUnchanged(f, before);
        assertFirstValidTransferStillUsesFirstReference(f);
    }

    @ParameterizedTest
    @MethodSource("combinations")
    void dateBeforeSourceOpeningPreservesBothLedgers(Kind sourceKind, Kind targetKind) {
        Fixture f = fixture(sourceKind, targetKind);
        Snapshot before = Snapshot.of(f);
        assertThrows(InvalidPostingDateException.class,
                () -> f.bank().transfer("S", "T", BigDecimal.ONE, DATE.minusDays(2)));
        assertUnchanged(f, before);
    }

    @ParameterizedTest
    @MethodSource("combinations")
    void dateValidForSourceButBeforeTargetOpeningCannotPartiallyDebit(Kind sourceKind, Kind targetKind) {
        Fixture f = fixture(sourceKind, targetKind);
        Snapshot before = Snapshot.of(f);
        assertThrows(InvalidPostingDateException.class,
                () -> f.bank().transfer("S", "T", BigDecimal.ONE, DATE.minusDays(1)));
        assertUnchanged(f, before);
        assertFirstValidTransferStillUsesFirstReference(f);
    }

    @ParameterizedTest
    @MethodSource("frozenAccounts")
    void eitherFrozenAccountBlocksTransferWithoutChangingEitherLedger(Kind sourceKind, Kind targetKind, Side side) {
        Fixture f = fixture(sourceKind, targetKind);
        Account frozen = side == Side.SOURCE ? f.source() : f.target();
        frozen.freeze();
        Snapshot before = Snapshot.of(f);
        assertThrows(AccountFrozenException.class, () -> f.bank().transfer("S", "T", BigDecimal.ONE, DATE));
        assertUnchanged(f, before);
        assertTrue(frozen.isFrozen());
        Account active = side == Side.SOURCE ? f.target() : f.source();
        f.bank().openCheckingAccount("U", "C1", BigDecimal.ZERO, DATE);
        List<Transaction> recovery = f.bank().transfer(active.getId(), "U", BigDecimal.ONE, DATE);
        assertAll(() -> assertEquals(OptionalLong.of(1), recovery.get(0).transferReference()),
                  () -> assertEquals(2, recovery.get(0).id()));
    }

    @ParameterizedTest
    @EnumSource(Side.class)
    void backdateRelativeToEitherLatestPostingPreservesBothAccounts(Side side) {
        Fixture f = fixture(Kind.CHECKING, Kind.SAVINGS);
        Account later = side == Side.SOURCE ? f.source() : f.target();
        later.deposit(BigDecimal.ONE, DATE.plusDays(2));
        Snapshot before = Snapshot.of(f);
        assertThrows(InvalidPostingDateException.class,
                () -> f.bank().transfer("S", "T", BigDecimal.ONE, DATE.plusDays(1)));
        assertUnchanged(f, before);
        List<Transaction> valid = f.bank().transfer("S", "T", BigDecimal.ONE, DATE.plusDays(2));
        assertEquals(OptionalLong.of(1), valid.get(0).transferReference());
    }

    @Test
    void bothFrozenAccountsRemainReadableAfterTransferRejection() {
        Fixture f = fixture(Kind.CHECKING, Kind.SAVINGS);
        f.source().freeze();
        f.target().freeze();
        Snapshot before = Snapshot.of(f);
        assertThrows(AccountFrozenException.class, () -> f.bank().transfer("S", "T", BigDecimal.ONE, DATE));
        assertUnchanged(f, before);
    }

    @Test
    void failedLaterDatedTransferDoesNotAdvanceSourceDateOrSequence() {
        Fixture f = fixture(Kind.CHECKING, Kind.SAVINGS);
        f.target().freeze();
        Snapshot before = Snapshot.of(f);
        assertThrows(AccountFrozenException.class,
                () -> f.bank().transfer("S", "T", BigDecimal.ONE, DATE.plusDays(10)));
        assertUnchanged(f, before);
        assertEquals(2, f.source().withdraw(BigDecimal.ONE, DATE).id());
    }

    @Test
    void transferReferencesAreIndependentOfLocalPostingSequenceNumbers() {
        Fixture f = fixture(Kind.CHECKING, Kind.SAVINGS);
        f.source().deposit(BigDecimal.ONE, DATE);
        f.source().deposit(BigDecimal.ONE, DATE);
        List<Transaction> first = f.bank().transfer("S", "T", BigDecimal.ONE, DATE);
        List<Transaction> reverse = f.bank().transfer("T", "S", BigDecimal.ONE, DATE);
        assertAll(() -> assertEquals(4, first.get(0).id()),
                  () -> assertEquals(2, first.get(1).id()),
                  () -> assertEquals(OptionalLong.of(1), first.get(0).transferReference()),
                  () -> assertEquals(OptionalLong.of(1), first.get(1).transferReference()),
                  () -> assertEquals(3, reverse.get(0).id()),
                  () -> assertEquals(5, reverse.get(1).id()),
                  () -> assertEquals(OptionalLong.of(2), reverse.get(0).transferReference()),
                  () -> assertEquals(OptionalLong.of(2), reverse.get(1).transferReference()));
    }

    @Test
    void rejectedTransferAfterSuccessDoesNotLeaveAGapInTransferReferences() {
        Fixture f = fixture(Kind.CHECKING, Kind.SAVINGS);
        f.bank().transfer("S", "T", BigDecimal.ONE, DATE);
        Snapshot before = Snapshot.of(f);
        assertThrows(InsufficientFundsException.class,
                () -> f.bank().transfer("S", "T", new BigDecimal("999"), DATE));
        assertUnchanged(f, before);
        assertEquals(OptionalLong.of(2), f.bank().transfer("S", "T", BigDecimal.ONE, DATE).get(0).transferReference());
    }

    @Test
    void transferReceiptAndPreviouslyRetrievedHistoriesAreImmutableSnapshots() {
        Fixture f = fixture(Kind.CHECKING, Kind.SAVINGS);
        Snapshot old = Snapshot.of(f);
        List<Transaction> receipt = f.bank().transfer("S", "T", BigDecimal.ONE, DATE);
        assertThrows(UnsupportedOperationException.class, receipt::clear);
        f.bank().transfer("S", "T", BigDecimal.ONE, DATE);
        assertAll(() -> assertEquals(1, old.sourceHistory().size()),
                  () -> assertEquals(1, old.targetHistory().size()),
                  () -> assertEquals(new BigDecimal("249.00"), receipt.get(0).balanceAfter()),
                  () -> assertEquals(new BigDecimal("151.00"), receipt.get(1).balanceAfter()));
    }

    @Test
    void transferAllowsDifferentAccountsOwnedByTheSameCustomer() {
        Fixture f = fixture(Kind.CHECKING, Kind.SAVINGS);
        Account sameOwner = f.bank().openSavingsAccount("U", "C1", new BigDecimal("100"), DATE);
        f.bank().transfer("S", "U", new BigDecimal("0.01"), DATE);
        assertAll(() -> assertSame(f.source().getOwner(), sameOwner.getOwner()),
                  () -> assertEquals(new BigDecimal("100.01"), sameOwner.getBalance()));
    }

    @Test
    void caseSensitiveDistinctAccountIdsMayTransfer() {
        Bank bank = new Bank();
        bank.registerCustomer("C1", "Ada");
        bank.openCheckingAccount("A", "C1", BigDecimal.ONE, DATE);
        bank.openCheckingAccount("a", "C1", BigDecimal.ZERO, DATE);
        List<Transaction> receipt = bank.transfer("A", "a", BigDecimal.ONE, DATE);
        assertAll(() -> assertEquals("A", receipt.get(0).accountId()),
                  () -> assertEquals("a", receipt.get(1).accountId()));
    }

    @ParameterizedTest
    @EnumSource(Side.class)
    void lookupDoesNotFoldAccountIdCase(Side side) {
        Fixture f = fixture(Kind.CHECKING, Kind.SAVINGS);
        Snapshot before = Snapshot.of(f);
        assertThrows(AccountNotFoundException.class, () -> f.bank().transfer(
                side == Side.SOURCE ? "s" : "S", side == Side.TARGET ? "t" : "T", BigDecimal.ONE, DATE));
        assertUnchanged(f, before);
    }

    @Test
    void accountRegisteredOnlyAtAnotherBankCannotParticipate() {
        Fixture f = fixture(Kind.CHECKING, Kind.SAVINGS);
        Bank other = new Bank();
        other.registerCustomer("C1", "Other");
        Account foreign = other.openCheckingAccount("foreign", "C1", BigDecimal.TEN, DATE);
        Snapshot before = Snapshot.of(f);
        assertThrows(AccountNotFoundException.class, () -> f.bank().transfer("S", "foreign", BigDecimal.ONE, DATE));
        assertUnchanged(f, before);
        assertEquals(new BigDecimal("10.00"), foreign.getBalance());
    }

    @Test
    void anotherBanksMatchingIdDoesNotRedirectTheRegisteredRecipient() {
        Fixture f = fixture(Kind.CHECKING, Kind.SAVINGS);
        Bank other = new Bank();
        other.registerCustomer("C1", "Other");
        Account foreign = other.openCheckingAccount("T", "C1", BigDecimal.ZERO, DATE);
        f.bank().transfer("S", "T", BigDecimal.ONE, DATE);
        assertAll(() -> assertEquals(new BigDecimal("151.00"), f.target().getBalance()),
                  () -> assertEquals(new BigDecimal("0.00"), foreign.getBalance()));
    }

    @Test
    void separateBanksMaintainIndependentTransferReferences() {
        Fixture first = fixture(Kind.CHECKING, Kind.SAVINGS);
        Fixture second = fixture(Kind.SAVINGS, Kind.CHECKING);
        first.bank().transfer("S", "T", BigDecimal.ONE, DATE);
        assertEquals(OptionalLong.of(1), second.bank().transfer("S", "T", BigDecimal.ONE, DATE).get(0).transferReference());
    }

    @Test
    void veryLargeTransferRoundTripPreservesEveryCent() {
        Fixture f = fixture(Kind.CHECKING, Kind.SAVINGS);
        BigDecimal large = new BigDecimal("123456789012345678901234567890.12");
        f.source().deposit(large, DATE);
        Snapshot before = Snapshot.of(f);
        f.bank().transfer("S", "T", large, DATE);
        f.bank().transfer("T", "S", large, DATE);
        assertAll(() -> assertEquals(before.sourceBalance(), f.source().getBalance()),
                  () -> assertEquals(before.targetBalance(), f.target().getBalance()));
    }

    @Test
    void successfulTransferInvalidatesOldPreparedPlansOnBothAccounts() {
        Fixture f = fixture(Kind.CHECKING, Kind.SAVINGS);
        Account.PreparedPosting sourcePlan = f.source().prepareDeposit(BigDecimal.ONE, DATE);
        Account.PreparedPosting targetPlan = f.target().prepareDeposit(BigDecimal.ONE, DATE);
        f.bank().transfer("S", "T", BigDecimal.ONE, DATE);
        Snapshot after = Snapshot.of(f);
        assertThrows(StalePostingException.class, sourcePlan::commit);
        assertThrows(StalePostingException.class, targetPlan::commit);
        assertUnchanged(f, after);
    }

    @Test
    void failedTargetPreparationDoesNotInvalidateASourcePlan() {
        Fixture f = fixture(Kind.CHECKING, Kind.SAVINGS);
        Account.PreparedPosting plan = f.source().prepareDeposit(BigDecimal.ONE, DATE);
        f.target().freeze();
        assertThrows(AccountFrozenException.class, () -> f.bank().transfer("S", "T", BigDecimal.ONE, DATE));
        assertEquals(2, plan.commit().id());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    void internalTransferPreparersRejectInvalidReferencesWithoutPublication(long reference) {
        Fixture f = fixture(Kind.CHECKING, Kind.SAVINGS);
        Snapshot before = Snapshot.of(f);
        assertThrows(InvalidBankingInputException.class,
                () -> f.source().prepareTransferOut(BigDecimal.ONE, DATE, reference));
        assertThrows(InvalidBankingInputException.class,
                () -> f.target().prepareTransferIn(BigDecimal.ONE, DATE, reference));
        assertUnchanged(f, before);
    }
}

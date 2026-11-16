package edu.banking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class BankScenarioTest {
    private enum Kind { CHECKING, SAVINGS }
    private enum Side { SOURCE, TARGET }
    private static final LocalDate OPENED = LocalDate.of(2024, 1, 1);
    private static final LocalDate JANUARY = LocalDate.of(2024, 1, 31);
    private static final LocalDate FEBRUARY = LocalDate.of(2024, 2, 29);

    private Bank bank() {
        Bank bank = new Bank();
        bank.registerCustomer("C1", "Ada");
        bank.registerCustomer("C2", "Grace");
        return bank;
    }

    private Account open(Bank bank, Kind kind, String id, String owner, String balance) {
        return kind == Kind.CHECKING
                ? bank.openCheckingAccount(id, owner, new BigDecimal(balance), OPENED)
                : bank.openSavingsAccount(id, owner, new BigDecimal(balance), OPENED);
    }

    private BigDecimal total(Bank bank) {
        return bank.getAccounts().stream().map(Account::getBalance).reduce(new BigDecimal("0.00"), BigDecimal::add);
    }

    // Independently reconstruct every running balance from signed ledger facts, through public reads.
    private void assertLedgersReconcile(Bank bank) {
        for (Account account : bank.getAccounts()) {
            List<Transaction> history = account.getTransactionHistory();
            BigDecimal running = new BigDecimal("0.00");
            assertEquals(TransactionType.OPENING, history.get(0).type());
            for (int i = 0; i < history.size(); i++) {
                Transaction entry = history.get(i);
                if (i > 0) { assertNotEquals(TransactionType.OPENING, entry.type()); }
                running = switch (entry.type()) {
                    case OPENING, DEPOSIT, TRANSFER_IN, INTEREST -> running.add(entry.amount());
                    case WITHDRAWAL, TRANSFER_OUT -> running.subtract(entry.amount());
                };
                assertEquals(i + 1L, entry.id());
                assertEquals(account.getId(), entry.accountId());
                assertEquals(running, entry.balanceAfter());
                assertTrue(running.compareTo(account.getMinimumBalance()) >= 0);
                if (i > 0) { assertFalse(entry.date().isBefore(history.get(i - 1).date())); }
            }
            assertEquals(running, account.getBalance());
        }
    }

    private static Stream<Arguments> interestSides() {
        return Stream.of(Kind.values()).flatMap(source -> Stream.of(Kind.values())
                .flatMap(target -> Stream.of(Side.values()).map(side -> Arguments.of(source, target, side))));
    }

    @Test
    void twoMonthWorkflowReconcilesExternalFlowsInterestAndPairedTransfers() {
        Bank bank = bank();
        Account checking = open(bank, Kind.CHECKING, "C", "C1", "500");
        Account savings = open(bank, Kind.SAVINGS, "S", "C2", "1000");
        checking.deposit(new BigDecimal("150"), LocalDate.of(2024, 1, 5));
        checking.deposit(new BigDecimal("50"), LocalDate.of(2024, 1, 6));
        savings.withdraw(new BigDecimal("100"), LocalDate.of(2024, 1, 10));
        List<Transaction> first = bank.transfer("C", "S", new BigDecimal("250"), LocalDate.of(2024, 1, 20));
        assertEquals(new BigDecimal("1600.00"), total(bank));
        assertLedgersReconcile(bank);

        Transaction checkingJanuary = bank.findAccount("C").applyMonthlyInterest(JANUARY);
        Transaction savingsJanuary = bank.findAccount("S").applyMonthlyInterest(JANUARY);
        List<Transaction> januarySnapshot = checking.getTransactionHistory();
        List<Transaction> second = bank.transfer("S", "C", new BigDecimal("53.45"), LocalDate.of(2024, 2, 10));
        assertEquals(new BigDecimal("1603.90"), total(bank));
        assertLedgersReconcile(bank);
        Transaction checkingFebruary = checking.applyMonthlyInterest(FEBRUARY);
        Transaction savingsFebruary = savings.applyMonthlyInterest(FEBRUARY);

        assertAll(() -> assertEquals(new BigDecimal("0.45"), checkingJanuary.amount()),
                  () -> assertEquals(new BigDecimal("3.45"), savingsJanuary.amount()),
                  () -> assertEquals(new BigDecimal("0.50"), checkingFebruary.amount()),
                  () -> assertEquals(new BigDecimal("3.30"), savingsFebruary.amount()),
                  () -> assertEquals(new BigDecimal("504.40"), checking.getBalance()),
                  () -> assertEquals(new BigDecimal("1103.30"), savings.getBalance()),
                  // Opening 1500 + deposits 200 - withdrawal 100 + interest 7.70.
                  () -> assertEquals(new BigDecimal("1607.70"), total(bank)),
                  () -> assertEquals(5, januarySnapshot.size()),
                  () -> assertSame(bank.findCustomer("C1"), checking.getOwner()),
                  () -> assertSame(bank.findCustomer("C2"), savings.getOwner()));
        for (int i = 0; i < 2; i++) {
            List<Transaction> receipt = List.of(first, second).get(i);
            assertEquals(TransactionType.TRANSFER_OUT, receipt.get(0).type());
            assertEquals(TransactionType.TRANSFER_IN, receipt.get(1).type());
            assertEquals(i + 1L, receipt.get(0).transferReference().orElseThrow());
            assertEquals(receipt.get(0).transferReference(), receipt.get(1).transferReference());
            assertEquals(receipt.get(0).amount(), receipt.get(1).amount());
            assertEquals(receipt.get(0).date(), receipt.get(1).date());
            assertNotEquals(receipt.get(0).id(), receipt.get(1).id());
            for (Transaction leg : receipt) {
                assertEquals(leg, bank.findAccount(leg.accountId()).getTransactionHistory().get((int) leg.id() - 1));
            }
        }
        assertLedgersReconcile(bank);
    }

    @ParameterizedTest
    @CsvSource({"CHECKING, CHECKING, 0.90, 1.10", "CHECKING, SAVINGS, 0.90, 3.30",
                "SAVINGS, CHECKING, 2.70, 1.10", "SAVINGS, SAVINGS, 2.70, 3.30"})
    void transferBeforeInterestChangesBothCalculationBalances(Kind sourceKind, Kind targetKind,
                                                            BigDecimal sourceCredit, BigDecimal targetCredit) {
        Bank bank = bank();
        Account source = open(bank, sourceKind, "S", "C1", "1000");
        Account target = open(bank, targetKind, "T", "C2", "1000");
        bank.transfer("S", "T", new BigDecimal("100"), JANUARY);
        assertEquals(sourceCredit, source.applyMonthlyInterest(JANUARY).amount());
        assertEquals(targetCredit, target.applyMonthlyInterest(JANUARY).amount());
        assertEquals(new BigDecimal("2000.00").add(sourceCredit).add(targetCredit), total(bank));
        assertLedgersReconcile(bank);
    }

    @ParameterizedTest
    @CsvSource({"CHECKING, CHECKING, 1.00, 1.00", "CHECKING, SAVINGS, 1.00, 3.00",
                "SAVINGS, CHECKING, 3.00, 1.00", "SAVINGS, SAVINGS, 3.00, 3.00"})
    void sameDayTransferAfterInterestCannotReopenEitherProcessedMonth(Kind sourceKind, Kind targetKind,
                                                                    BigDecimal sourceCredit, BigDecimal targetCredit) {
        Bank bank = bank();
        Account source = open(bank, sourceKind, "S", "C1", "1000");
        Account target = open(bank, targetKind, "T", "C2", "1000");
        assertEquals(sourceCredit, source.applyMonthlyInterest(JANUARY).amount());
        assertEquals(targetCredit, target.applyMonthlyInterest(JANUARY).amount());
        List<Transaction> sourceSnapshot = source.getTransactionHistory();
        BigDecimal before = total(bank);
        bank.transfer("S", "T", new BigDecimal("100"), JANUARY);
        List<Transaction> sourceAfter = source.getTransactionHistory();
        List<Transaction> targetAfter = target.getTransactionHistory();
        assertThrows(InterestAlreadyAppliedException.class, () -> source.applyMonthlyInterest(JANUARY));
        assertThrows(InterestAlreadyAppliedException.class, () -> target.applyMonthlyInterest(JANUARY));
        assertAll(() -> assertEquals(before, total(bank)),
                  () -> assertEquals(sourceAfter, source.getTransactionHistory()),
                  () -> assertEquals(targetAfter, target.getTransactionHistory()),
                  () -> assertEquals(2, sourceSnapshot.size()));
        assertLedgersReconcile(bank);
    }

    @ParameterizedTest
    @MethodSource("interestSides")
    void interestOnEitherSideBlocksBackdatedTransferWithoutConsumingAReference(Kind sourceKind,
                                                                            Kind targetKind, Side side) {
        Bank bank = bank();
        Account source = open(bank, sourceKind, "S", "C1", "1000");
        Account target = open(bank, targetKind, "T", "C2", "1000");
        Account advanced = side == Side.SOURCE ? source : target;
        advanced.applyMonthlyInterest(JANUARY);
        List<Transaction> sourceBefore = source.getTransactionHistory();
        List<Transaction> targetBefore = target.getTransactionHistory();
        assertThrows(InvalidPostingDateException.class,
                () -> bank.transfer("S", "T", BigDecimal.ONE, JANUARY.minusDays(1)));
        assertEquals(sourceBefore, source.getTransactionHistory());
        assertEquals(targetBefore, target.getTransactionHistory());
        assertLedgersReconcile(bank);
        assertThrows(InterestAlreadyAppliedException.class, () -> advanced.applyMonthlyInterest(JANUARY));
        List<Transaction> receipt = bank.transfer("S", "T", BigDecimal.ONE, JANUARY);
        assertAll(() -> assertEquals(1, receipt.get(0).transferReference().orElseThrow()),
                  () -> assertEquals(sourceBefore.size() + 1L, receipt.get(0).id()),
                  () -> assertEquals(targetBefore.size() + 1L, receipt.get(1).id()));
        assertLedgersReconcile(bank);
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void interestBecomesTransferableWhileTheOriginalFloorStillApplies(Kind kind) {
        Bank bank = bank();
        Account source = open(bank, kind, "S", "C1", "100");
        open(bank, Kind.CHECKING, "T", "C2", "1000");
        source.applyMonthlyInterest(JANUARY);
        BigDecimal available = source.getAvailableBalance();
        List<Transaction> before = source.getTransactionHistory();
        BigDecimal totalBefore = total(bank);
        assertThrows(InsufficientFundsException.class,
                () -> bank.transfer("S", "T", available.add(new BigDecimal("0.01")), JANUARY));
        assertEquals(before, source.getTransactionHistory());
        List<Transaction> receipt = bank.transfer("S", "T", available, JANUARY);
        assertAll(() -> assertEquals(source.getMinimumBalance(), source.getBalance()),
                  () -> assertEquals(available, receipt.get(0).amount()),
                  () -> assertEquals(1, receipt.get(0).transferReference().orElseThrow()),
                  () -> assertEquals(3, receipt.get(0).id()),
                  () -> assertEquals(totalBefore, total(bank)));
        assertLedgersReconcile(bank);
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void freezingAfterTransfersAndInterestProtectsHistoryAndIsolatesOtherAccounts(Kind sourceKind) {
        Bank bank = bank();
        Account frozen = open(bank, sourceKind, "S", "C1", "1000");
        Account active = open(bank, Kind.CHECKING, "T", "C2", "1000");
        bank.transfer("S", "T", new BigDecimal("100"), JANUARY);
        frozen.applyMonthlyInterest(JANUARY);
        active.applyMonthlyInterest(JANUARY);
        List<Transaction> frozenBefore = frozen.getTransactionHistory();
        List<Transaction> activeBefore = active.getTransactionHistory();
        BigDecimal before = total(bank);
        bank.findAccount("S").freeze();
        assertThrows(AccountFrozenException.class, () -> frozen.deposit(BigDecimal.ONE, FEBRUARY));
        assertThrows(AccountFrozenException.class, () -> frozen.withdraw(BigDecimal.ONE, FEBRUARY));
        assertThrows(AccountFrozenException.class, () -> frozen.applyMonthlyInterest(FEBRUARY));
        assertThrows(AccountFrozenException.class, () -> bank.transfer("S", "T", BigDecimal.ONE, FEBRUARY));
        assertThrows(AccountFrozenException.class, () -> bank.transfer("T", "S", BigDecimal.ONE, FEBRUARY));
        assertAll(() -> assertEquals(frozenBefore, bank.findAccount("S").getTransactionHistory()),
                  () -> assertEquals(activeBefore, active.getTransactionHistory()),
                  () -> assertEquals(before, total(bank)),
                  () -> assertTrue(frozen.isFrozen()),
                  () -> assertFalse(active.isFrozen()));
        assertLedgersReconcile(bank);

        Transaction credit = active.applyMonthlyInterest(FEBRUARY);
        bank.openCheckingAccount("U", "C2", BigDecimal.ZERO, OPENED);
        List<Transaction> receipt = bank.transfer("T", "U", BigDecimal.ONE, FEBRUARY);
        assertAll(() -> assertEquals(2, receipt.get(0).transferReference().orElseThrow()),
                  () -> assertEquals(before.add(credit.amount()), total(bank)),
                  () -> assertEquals(frozenBefore, frozen.getTransactionHistory()));
        assertLedgersReconcile(bank);
    }
}

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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class BankAccountTest {
    private enum Kind { CHECKING, SAVINGS }
    private static final LocalDate DATE = LocalDate.of(2024, 2, 29);
    private final Bank bank = new Bank();

    private Account open(Bank target, Kind kind, String accountId, String customerId,
                         BigDecimal balance, LocalDate date) {
        return switch (kind) {
            case CHECKING -> target.openCheckingAccount(accountId, customerId, balance, date);
            case SAVINGS -> target.openSavingsAccount(accountId, customerId, balance, date);
        };
    }

    private Account open(Kind kind, String id, String owner) {
        return open(bank, kind, id, owner, new BigDecimal("250"), DATE);
    }

    private static Stream<Arguments> missingIdentities() {
        return Stream.of(Kind.values()).flatMap(kind -> Stream.<String>of(null, "", " ", "\t\n", "\u2003")
                .map(value -> Arguments.of(kind, value)));
    }

    private static Stream<Arguments> invalidOpeningState() {
        return Stream.of(Kind.values()).flatMap(kind -> Stream.of(
                Arguments.of(kind, null, DATE),
                Arguments.of(kind, new BigDecimal("-0.01"), DATE),
                Arguments.of(kind, new BigDecimal("100.001"), DATE),
                Arguments.of(kind, new BigDecimal("250"), null)));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void openingPublishesTheReturnedAccountWithTheCanonicalRegisteredOwner(Kind kind) {
        Customer owner = bank.registerCustomer(" C1 ", "Ada");
        Account account = open(kind, " A1 ", " C1 ");
        assertAll(() -> assertEquals("A1", account.getId()),
                  () -> assertSame(owner, account.getOwner()),
                  () -> assertEquals(new BigDecimal("250.00"), account.getBalance()),
                  () -> assertEquals(DATE, account.getOpenedOn()),
                  () -> assertSame(account, bank.findAccount("A1")),
                  () -> assertEquals(List.of(account), bank.getAccounts()));
    }

    @Test
    void checkingOpeningReturnsCheckingAccountAndAcceptsZeroBalance() {
        bank.registerCustomer("C1", "Ada");
        CheckingAccount account = bank.openCheckingAccount("A1", "C1", BigDecimal.ZERO, DATE);
        assertAll(() -> assertEquals(new BigDecimal("0.00"), account.getBalance()),
                  () -> assertEquals(new BigDecimal("0.001"), account.getMonthlyInterestRate()));
    }

    @Test
    void savingsOpeningReturnsSavingsAccountAndAcceptsExactMinimum() {
        bank.registerCustomer("C1", "Ada");
        SavingsAccount account = bank.openSavingsAccount("A1", "C1", new BigDecimal("100"), DATE);
        assertAll(() -> assertEquals(new BigDecimal("100.00"), account.getBalance()),
                  () -> assertEquals(new BigDecimal("0.003"), account.getMonthlyInterestRate()));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void bankOpeningCreatesExactlyOneOpeningLedgerEntry(Kind kind) {
        bank.registerCustomer("C1", "Ada");
        Account account = open(kind, "A1", "C1");
        assertEquals(List.of(new Transaction(1, "A1", TransactionType.OPENING,
                new BigDecimal("250.00"), new BigDecimal("250.00"), DATE, OptionalLong.empty())),
                account.getTransactionHistory());
    }

    @ParameterizedTest
    @MethodSource("missingIdentities")
    void rejectsMissingAccountIdWithoutChangingEitherRegistry(Kind kind, String id) {
        Customer owner = bank.registerCustomer("C1", "Ada");
        assertThrows(InvalidBankingInputException.class, () -> open(kind, id, "C1"));
        assertAll(() -> assertTrue(bank.getAccounts().isEmpty()),
                  () -> assertEquals(List.of(owner), bank.getCustomers()));
    }

    @ParameterizedTest
    @MethodSource("missingIdentities")
    void rejectsMissingCustomerIdWithoutRegisteringAnAccount(Kind kind, String customerId) {
        bank.registerCustomer("C1", "Ada");
        assertThrows(InvalidBankingInputException.class, () -> open(kind, "A1", customerId));
        assertTrue(bank.getAccounts().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void rejectsUnknownOwnerWithoutRegisteringAnAccount(Kind kind) {
        var error = assertThrows(CustomerNotFoundException.class, () -> open(kind, "A1", "missing"));
        assertAll(() -> assertEquals("Customer not found: missing", error.getMessage()),
                  () -> assertTrue(bank.getAccounts().isEmpty()),
                  () -> assertTrue(bank.getCustomers().isEmpty()));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void ownerIdentifiersAreCaseSensitive(Kind kind) {
        bank.registerCustomer("C1", "Ada");
        assertThrows(CustomerNotFoundException.class, () -> open(kind, "A1", "c1"));
        assertTrue(bank.getAccounts().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void registrationAtAnotherBankDoesNotAuthorizeOpeningHere(Kind kind) {
        Bank other = new Bank();
        other.registerCustomer("C1", "Ada");
        assertThrows(CustomerNotFoundException.class, () -> open(kind, "A1", "C1"));
        assertTrue(bank.getAccounts().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void rejectedUnknownOwnerDoesNotReserveTheAccountId(Kind kind) {
        assertThrows(CustomerNotFoundException.class, () -> open(kind, "A1", "C1"));
        bank.registerCustomer("C1", "Ada");
        assertSame(open(kind, "A1", "C1"), bank.findAccount("A1"));
    }

    @ParameterizedTest
    @MethodSource("invalidOpeningState")
    void constructorRejectionPreservesRegistriesAndLeavesIdReusable(Kind kind, BigDecimal balance, LocalDate date) {
        Customer owner = bank.registerCustomer("C1", "Ada");
        Account existing = open(kind, "original", "C1");
        List<Transaction> ledger = existing.getTransactionHistory();
        assertThrows(InvalidBankingInputException.class,
                () -> open(bank, kind, "A1", "C1", balance, date));
        assertAll(() -> assertEquals(List.of(existing), bank.getAccounts()),
                  () -> assertEquals(List.of(owner), bank.getCustomers()),
                  () -> assertEquals(ledger, existing.getTransactionHistory()));
        Account retry = open(kind, "A1", "C1");
        assertAll(() -> assertEquals(1, retry.getTransactionHistory().get(0).id()),
                  () -> assertSame(retry, bank.findAccount("A1")));
    }

    @Test
    void savingsBelowMinimumIsRejectedWithoutReservingId() {
        bank.registerCustomer("C1", "Ada");
        assertThrows(InvalidBankingInputException.class,
                () -> bank.openSavingsAccount("A1", "C1", new BigDecimal("99.99"), DATE));
        assertTrue(bank.getAccounts().isEmpty());
        assertNotNull(bank.openSavingsAccount("A1", "C1", new BigDecimal("100"), DATE));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void duplicateOpeningPreservesTheOriginalOwnerBalanceAndLedger(Kind kind) {
        Customer originalOwner = bank.registerCustomer("C1", "Ada");
        bank.registerCustomer("C2", "Grace");
        Account original = open(kind, "A1", "C1");
        original.deposit(BigDecimal.ONE, DATE);
        List<Transaction> history = original.getTransactionHistory();
        var error = assertThrows(DuplicateAccountException.class, () -> open(kind, " A1 ", "C2"));
        assertAll(() -> assertEquals("Account already registered: A1", error.getMessage()),
                  () -> assertEquals(List.of(original), bank.getAccounts()),
                  () -> assertSame(originalOwner, bank.findAccount("A1").getOwner()),
                  () -> assertEquals(new BigDecimal("251.00"), original.getBalance()),
                  () -> assertEquals(history, original.getTransactionHistory()));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void accountIdentifiersAreUniqueAcrossCheckingAndSavings(Kind firstType) {
        bank.registerCustomer("C1", "Ada");
        Account original = open(firstType, "A1", "C1");
        Kind otherType = firstType == Kind.CHECKING ? Kind.SAVINGS : Kind.CHECKING;
        assertThrows(DuplicateAccountException.class, () -> open(otherType, "A1", "C1"));
        assertSame(original, bank.findAccount("A1"));
    }

    @Test
    void customerMayOwnSeveralAccountsOfBothTypes() {
        Customer owner = bank.registerCustomer("C1", "Ada");
        Account checking = open(Kind.CHECKING, "A1", "C1");
        Account savings = open(Kind.SAVINGS, "A2", "C1");
        assertAll(() -> assertEquals(List.of(checking, savings), bank.getAccounts()),
                  () -> assertSame(owner, checking.getOwner()),
                  () -> assertSame(owner, savings.getOwner()));
    }

    @Test
    void differentCustomersCanOwnDistinctAccounts() {
        Customer first = bank.registerCustomer("C1", "Ada");
        Customer second = bank.registerCustomer("C2", "Grace");
        Account checking = open(Kind.CHECKING, "A1", "C1");
        Account savings = open(Kind.SAVINGS, "A2", "C2");
        assertAll(() -> assertSame(first, checking.getOwner()),
                  () -> assertSame(second, savings.getOwner()));
    }

    @Test
    void customerAndAccountIdentifiersHaveSeparateNamespaces() {
        bank.registerCustomer("same", "Ada");
        assertEquals("same", open(Kind.CHECKING, "same", "same").getId());
    }

    @Test
    void accountIdentifiersAreCaseSensitiveWithinOneBank() {
        bank.registerCustomer("C1", "Ada");
        Account upper = open(Kind.CHECKING, "A1", "C1");
        Account lower = open(Kind.CHECKING, "a1", "C1");
        assertAll(() -> assertSame(upper, bank.findAccount("A1")),
                  () -> assertSame(lower, bank.findAccount("a1")));
    }

    @Test
    void lookupRejectsUnregisteredAccount() {
        var error = assertThrows(AccountNotFoundException.class, () -> bank.findAccount("missing"));
        assertEquals("Account not found: missing", error.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "\u2003"})
    void lookupRejectsMissingAccountIdentifier(String id) {
        assertThrows(InvalidBankingInputException.class, () -> bank.findAccount(id));
    }

    @Test
    void lookupStripsSurroundingWhitespace() {
        bank.registerCustomer("C1", "Ada");
        Account account = open(Kind.CHECKING, "A1", "C1");
        assertSame(account, bank.findAccount(" A1 "));
    }

    @Test
    void lookupDoesNotFoldIdentifierCase() {
        bank.registerCustomer("C1", "Ada");
        open(Kind.CHECKING, "A1", "C1");
        assertThrows(AccountNotFoundException.class, () -> bank.findAccount("a1"));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void lookupRetainsTheRegisteredPostingStateAndSequence(Kind kind) {
        bank.registerCustomer("C1", "Ada");
        Account opened = open(kind, "A1", "C1");
        bank.findAccount("A1").deposit(new BigDecimal("5.00"), DATE);
        Transaction entry = bank.findAccount("A1").withdraw(BigDecimal.ONE, DATE);
        assertAll(() -> assertEquals(new BigDecimal("254.00"), opened.getBalance()),
                  () -> assertEquals(3, entry.id()),
                  () -> assertSame(entry, opened.getTransactionHistory().get(2)));
    }

    @Test
    void newBankHasAnEmptyAccountSnapshot() {
        assertTrue(bank.getAccounts().isEmpty());
    }

    @Test
    void accountSnapshotPreservesOpeningOrderAcrossTypesAndOwners() {
        bank.registerCustomer("C1", "Ada");
        bank.registerCustomer("C2", "Grace");
        Account first = open(Kind.SAVINGS, "Z", "C2");
        Account second = open(Kind.CHECKING, "A", "C1");
        assertEquals(List.of(first, second), bank.getAccounts());
    }

    @Test
    void accountSnapshotRejectsExternalMembershipChanges() {
        bank.registerCustomer("C1", "Ada");
        Account original = open(Kind.CHECKING, "A1", "C1");
        assertThrows(UnsupportedOperationException.class, () -> bank.getAccounts().clear());
        assertEquals(List.of(original), bank.getAccounts());
    }

    @Test
    void priorAccountSnapshotDoesNotGainLaterAccounts() {
        bank.registerCustomer("C1", "Ada");
        Account first = open(Kind.CHECKING, "A1", "C1");
        List<Account> snapshot = bank.getAccounts();
        open(Kind.SAVINGS, "A2", "C1");
        assertEquals(List.of(first), snapshot);
    }

    @Test
    void accountSnapshotContainsTheRegisteredInstancesRatherThanCopies() {
        bank.registerCustomer("C1", "Ada");
        Account account = open(Kind.CHECKING, "A1", "C1");
        bank.getAccounts().get(0).deposit(BigDecimal.ONE, DATE);
        assertEquals(new BigDecimal("251.00"), account.getBalance());
    }

    @Test
    void separateBanksHaveIndependentAccountNamespacesAndCanonicalOwners() {
        Bank other = new Bank();
        Customer local = bank.registerCustomer("C1", "Ada");
        Customer remote = other.registerCustomer("C1", "Grace");
        Account first = open(Kind.CHECKING, "A1", "C1");
        Account second = open(other, Kind.CHECKING, "A1", "C1", BigDecimal.ZERO, DATE);
        assertAll(() -> assertSame(local, first.getOwner()),
                  () -> assertSame(remote, second.getOwner()),
                  () -> assertSame(first, bank.findAccount("A1")),
                  () -> assertSame(second, other.findAccount("A1")));
    }

    @Test
    void directlyConstructedAccountIsNotImplicitlyRegistered() {
        Customer owner = bank.registerCustomer("C1", "Ada");
        new CheckingAccount("A1", owner, BigDecimal.ZERO, DATE);
        assertThrows(AccountNotFoundException.class, () -> bank.findAccount("A1"));
    }
}

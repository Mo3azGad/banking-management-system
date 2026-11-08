package edu.banking;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class BankTest {
    private final Bank bank = new Bank();

    @Test
    void registrationMakesCustomerAvailableForLookup() {
        Customer customer = bank.registerCustomer("C1", "Ada");
        assertSame(customer, bank.findCustomer("C1"));
    }

    @Test
    void duplicateRegistrationKeepsOriginalCustomer() {
        Customer original = bank.registerCustomer("C1", "Ada");
        var error = assertThrows(DuplicateCustomerException.class,
                () -> bank.registerCustomer(" C1 ", "Grace"));
        assertAll(() -> assertEquals("Customer already registered: C1", error.getMessage()),
                  () -> assertEquals(List.of(original), bank.getCustomers()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void rejectsInvalidRegistrationIdentifierWithoutChangingRegistry(String id) {
        assertThrows(InvalidBankingInputException.class, () -> bank.registerCustomer(id, "Ada"));
        assertTrue(bank.getCustomers().isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void rejectsInvalidRegistrationNameWithoutChangingRegistry(String name) {
        assertThrows(InvalidBankingInputException.class, () -> bank.registerCustomer("C1", name));
        assertTrue(bank.getCustomers().isEmpty());
    }

    @Test
    void lookupStripsSurroundingWhitespace() {
        Customer customer = bank.registerCustomer(" C1 ", "Ada");
        assertSame(customer, bank.findCustomer(" C1 "));
    }

    @Test
    void lookupRejectsUnregisteredIdentifier() {
        var error = assertThrows(CustomerNotFoundException.class, () -> bank.findCustomer("missing"));
        assertEquals("Customer not found: missing", error.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void lookupRejectsMissingIdentifier(String id) {
        assertThrows(InvalidBankingInputException.class, () -> bank.findCustomer(id));
    }

    @Test
    void customerIdentifiersAreCaseSensitive() {
        bank.registerCustomer("C1", "Ada");
        assertThrows(CustomerNotFoundException.class, () -> bank.findCustomer("c1"));
    }

    @Test
    void permitsCustomersToShareAName() {
        bank.registerCustomer("C1", "Ada");
        bank.registerCustomer("C2", "Ada");
        assertEquals(2, bank.getCustomers().size());
    }

    @Test
    void newBankHasEmptyCustomerSnapshot() {
        assertTrue(bank.getCustomers().isEmpty());
    }

    @Test
    void customerSnapshotPreservesRegistrationOrder() {
        Customer first = bank.registerCustomer("C2", "Grace");
        Customer second = bank.registerCustomer("C1", "Ada");
        assertEquals(List.of(first, second), bank.getCustomers());
    }

    @Test
    void customerSnapshotRejectsExternalModification() {
        bank.registerCustomer("C1", "Ada");
        assertThrows(UnsupportedOperationException.class, () -> bank.getCustomers().clear());
        assertEquals(1, bank.getCustomers().size());
    }

    @Test
    void customerSnapshotDoesNotChangeAfterLaterRegistration() {
        List<Customer> snapshot = bank.getCustomers();
        bank.registerCustomer("C1", "Ada");
        assertTrue(snapshot.isEmpty());
    }
}

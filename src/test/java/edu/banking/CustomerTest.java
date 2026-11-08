package edu.banking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CustomerTest {
    @Test
    void createsCustomerWithProvidedIdentity() {
        Customer customer = new Customer("C1", "Ada Lovelace");
        assertAll(() -> assertEquals("C1", customer.getId()),
                  () -> assertEquals("Ada Lovelace", customer.getName()));
    }

    @Test
    void stripsSurroundingWhitespaceFromIdentity() {
        Customer customer = new Customer(" C1 ", " Ada Lovelace ");
        assertAll(() -> assertEquals("C1", customer.getId()),
                  () -> assertEquals("Ada Lovelace", customer.getName()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "\u2003"})
    void rejectsMissingCustomerIdentifier(String id) {
        var error = assertThrows(InvalidBankingInputException.class,
                () -> new Customer(id, "Ada"));
        assertEquals("Customer identifier must not be blank.", error.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "\u2003"})
    void rejectsMissingCustomerName(String name) {
        var error = assertThrows(InvalidBankingInputException.class,
                () -> new Customer("C1", name));
        assertEquals("Customer name must not be blank.", error.getMessage());
    }
}

package edu.banking;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory bank facade. Currently implements customer management; account integration arrives later.
 * Instances are intended for single-threaded use.
 */
public final class Bank {
    private final Map<String, Customer> customers = new LinkedHashMap<>();

    /** Registers a unique identifier; a failed registration leaves the registry unchanged. */
    public Customer registerCustomer(String id, String name) {
        Customer customer = new Customer(id, name);
        if (customers.containsKey(customer.getId())) {
            throw new DuplicateCustomerException("Customer already registered: " + customer.getId());
        }
        customers.put(customer.getId(), customer);
        return customer;
    }

    /** Looks up a customer, rejecting missing identities instead of returning null. */
    public Customer findCustomer(String id) {
        String normalizedId = BankingInput.text(id, "Customer identifier");
        Customer customer = customers.get(normalizedId);
        if (customer == null) {
            throw new CustomerNotFoundException("Customer not found: " + normalizedId);
        }
        return customer;
    }

    /** Returns an immutable snapshot in registration order, empty for a new bank. */
    public List<Customer> getCustomers() {
        return List.copyOf(customers.values());
    }
}

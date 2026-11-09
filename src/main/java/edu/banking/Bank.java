package edu.banking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory bank facade for customers and registered accounts.
 * Instances are intended for single-threaded use.
 */
public final class Bank {
    private final Map<String, Customer> customers = new LinkedHashMap<>();
    private final Map<String, Account> accounts = new LinkedHashMap<>();

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

    /** Opens and registers a checking account for a customer registered at this bank.
     * Rejection leaves both registries unchanged and does not reserve the account identifier.
     */
    public CheckingAccount openCheckingAccount(String accountId, String customerId,
                                               BigDecimal openingBalance, LocalDate openedOn) {
        String normalizedId = availableAccountId(accountId);
        Customer owner = findCustomer(customerId);
        CheckingAccount account = new CheckingAccount(normalizedId, owner, openingBalance, openedOn);
        accounts.put(normalizedId, account);
        return account;
    }

    /** Opens and registers a savings account, including its minimum opening-balance rule.
     * Rejection leaves both registries unchanged and does not reserve the account identifier.
     */
    public SavingsAccount openSavingsAccount(String accountId, String customerId,
                                             BigDecimal openingBalance, LocalDate openedOn) {
        String normalizedId = availableAccountId(accountId);
        Customer owner = findCustomer(customerId);
        SavingsAccount account = new SavingsAccount(normalizedId, owner, openingBalance, openedOn);
        accounts.put(normalizedId, account);
        return account;
    }

    /** Returns the registered instance, or throws when no account has this identifier. */
    public Account findAccount(String accountId) {
        String normalizedId = BankingInput.text(accountId, "Account identifier");
        Account account = accounts.get(normalizedId);
        if (account == null) {
            throw new AccountNotFoundException("Account not found: " + normalizedId);
        }
        return account;
    }

    /** Immutable membership snapshot in opening order; contained accounts retain their identity. */
    public List<Account> getAccounts() {
        return List.copyOf(accounts.values());
    }

    private String availableAccountId(String accountId) {
        String normalizedId = BankingInput.text(accountId, "Account identifier");
        if (accounts.containsKey(normalizedId)) {
            throw new DuplicateAccountException("Account already registered: " + normalizedId);
        }
        return normalizedId;
    }
}

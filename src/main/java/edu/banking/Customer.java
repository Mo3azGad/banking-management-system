package edu.banking;

/** Immutable customer identity. Names are not unique; identifiers are case-sensitive. */
public final class Customer {
    private final String id;
    private final String name;

    public Customer(String id, String name) {
        this.id = BankingInput.text(id, "Customer identifier");
        this.name = BankingInput.text(name, "Customer name");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}

package edu.banking;

/** Immutable customer identity. Names are not unique; identifiers are case-sensitive. */
public final class Customer {
    private final String id;
    private final String name;

    public Customer(String id, String name) {
        this.id = requireText(id, "Customer identifier");
        this.name = requireText(name, "Customer name");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // Package-private so shared input validation does not become a public service API.
    static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidBankingInputException(field + " must not be blank.");
        }
        return value.strip();
    }
}

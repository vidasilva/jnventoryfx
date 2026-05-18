package br.com.vidasilva.jnventoryfx.model;

public enum UserRole {
    ADMIN("Admin"),
    MANAGER("Manager"),
    CASHIER("Cashier"),
    WAREHOUSE("Warehouse");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}

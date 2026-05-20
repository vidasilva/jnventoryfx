package br.com.vidasilva.jnventoryfx.security;

public enum Permission {
    VIEW_INVENTORY("view inventory"),
    REGISTER_PART("register car parts"),
    SELL_PART("register sales"),
    VIEW_SUPPLIERS("view suppliers"),
    MANAGE_SUPPLIERS("manage suppliers"),
    UPDATE_WAREHOUSE("update warehouse data"),
    VIEW_USERS("view users"),
    MANAGE_USERS("manage users"),
    VIEW_AUDIT_LOGS("view audit logs");

    private final String label;

    Permission(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

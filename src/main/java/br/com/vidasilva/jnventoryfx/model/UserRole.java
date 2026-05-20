package br.com.vidasilva.jnventoryfx.model;

import br.com.vidasilva.jnventoryfx.security.Permission;

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

    public boolean hasPermission(Permission permission) {
        if (permission == null) {
            return false;
        }

        return switch (this) {
            case ADMIN -> true;
            case MANAGER -> switch (permission) {
                case VIEW_INVENTORY, REGISTER_PART, SELL_PART, VIEW_SUPPLIERS, MANAGE_SUPPLIERS, UPDATE_WAREHOUSE -> true;
                case VIEW_USERS, MANAGE_USERS, VIEW_AUDIT_LOGS -> false;
            };
            case CASHIER -> switch (permission) {
                case VIEW_INVENTORY, SELL_PART -> true;
                case REGISTER_PART, VIEW_SUPPLIERS, MANAGE_SUPPLIERS, UPDATE_WAREHOUSE, VIEW_USERS, MANAGE_USERS, VIEW_AUDIT_LOGS -> false;
            };
            case WAREHOUSE -> switch (permission) {
                case VIEW_INVENTORY, UPDATE_WAREHOUSE -> true;
                case REGISTER_PART, SELL_PART, VIEW_SUPPLIERS, MANAGE_SUPPLIERS, VIEW_USERS, MANAGE_USERS, VIEW_AUDIT_LOGS -> false;
            };
        };
    }

    @Override
    public String toString() {
        return label;
    }
}

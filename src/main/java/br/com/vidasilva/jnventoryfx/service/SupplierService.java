package br.com.vidasilva.jnventoryfx.service;

import br.com.vidasilva.jnventoryfx.model.Supplier;
import br.com.vidasilva.jnventoryfx.repository.SupplierRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SupplierService {
    private static final SupplierRepository SUPPLIER_REPOSITORY = new SupplierRepository();
    private static final ObservableList<Supplier> SUPPLIERS = FXCollections.observableArrayList(SUPPLIER_REPOSITORY.findAll());

    public ObservableList<Supplier> getSuppliers() {
        return SUPPLIERS;
    }

    public Supplier registerSupplier(String name, String phone, String email, String address, String notes) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("Supplier name is required.");
        }

        Supplier supplier = SUPPLIER_REPOSITORY.insert(
                name.trim(),
                safe(phone),
                safe(email),
                safe(address),
                safe(notes)
        );

        SUPPLIERS.add(supplier);
        return supplier;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

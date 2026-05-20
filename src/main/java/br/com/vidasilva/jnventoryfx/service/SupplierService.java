package br.com.vidasilva.jnventoryfx.service;

import br.com.vidasilva.jnventoryfx.model.Supplier;
import br.com.vidasilva.jnventoryfx.repository.SupplierRepository;
import br.com.vidasilva.jnventoryfx.security.AuthorizationService;
import br.com.vidasilva.jnventoryfx.security.Permission;
import br.com.vidasilva.jnventoryfx.validation.Validator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SupplierService {
    private static final SupplierRepository SUPPLIER_REPOSITORY = new SupplierRepository();
    private static final ObservableList<Supplier> SUPPLIERS = FXCollections.observableArrayList(SUPPLIER_REPOSITORY.findAll());

    public ObservableList<Supplier> getSuppliers() {
        return SUPPLIERS;
    }

    public Supplier registerSupplier(String name, String phone, String email, String address, String notes) {
        AuthorizationService.require(Permission.MANAGE_SUPPLIERS);

        String validatedName = Validator.requiredText(name, 120, "Supplier name");
        String validatedPhone = Validator.optionalText(phone, 40, "Supplier phone");
        String validatedEmail = Validator.optionalEmail(email, "Supplier email");
        String validatedAddress = Validator.optionalText(address, 180, "Supplier address");
        String validatedNotes = Validator.optionalText(notes, 500, "Supplier notes");

        Supplier supplier = SUPPLIER_REPOSITORY.insert(
                validatedName,
                validatedPhone,
                validatedEmail,
                validatedAddress,
                validatedNotes
        );

        SUPPLIERS.add(supplier);
        AuditService.record(
                "REGISTER_SUPPLIER",
                "SUPPLIER",
                String.valueOf(supplier.getId()),
                "SUCCESS",
                "Registered supplier " + supplier.getName() + "."
        );
        return supplier;
    }
}

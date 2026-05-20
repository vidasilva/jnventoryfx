package br.com.vidasilva.jnventoryfx.service;

import br.com.vidasilva.jnventoryfx.model.CarPart;
import br.com.vidasilva.jnventoryfx.model.Supplier;
import br.com.vidasilva.jnventoryfx.model.WarehouseAddress;
import br.com.vidasilva.jnventoryfx.repository.CarPartRepository;
import br.com.vidasilva.jnventoryfx.repository.WarehouseAddressRepository;
import br.com.vidasilva.jnventoryfx.security.AuthorizationService;
import br.com.vidasilva.jnventoryfx.security.Permission;
import br.com.vidasilva.jnventoryfx.validation.Validator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InventoryService {
    private static final Pattern WAREHOUSE_ADDRESS_PATTERN = Pattern.compile("^([A-Za-z])[-\\s]?(\\d+)[-\\s]?(\\d+)(?:[-\\s]?(\\d+))?$");
    private static final CarPartRepository CAR_PART_REPOSITORY = new CarPartRepository();
    private static final WarehouseAddressRepository WAREHOUSE_ADDRESS_REPOSITORY = new WarehouseAddressRepository();
    private static final ObservableList<CarPart> PARTS = FXCollections.observableArrayList(CAR_PART_REPOSITORY.findAll());

    public ObservableList<CarPart> getParts() {
        return PARTS;
    }

    public CarPart registerPart(
            String name,
            String manufacturer,
            String compatibleVehicles,
            Supplier supplier,
            double unitPrice,
            int quantity,
            String warehouseAddressText,
            int maxCapacity,
            int lowCapacityWarningTriggerLevel
    ) {
        AuthorizationService.require(Permission.REGISTER_PART);
        ValidatedPartInput input = validatePart(name, manufacturer, compatibleVehicles, supplier, unitPrice, quantity, warehouseAddressText, maxCapacity, lowCapacityWarningTriggerLevel);

        WarehouseAddress warehouseAddress = createWarehouseAddressFromText(
                input.warehouseAddressText(),
                input.maxCapacity(),
                input.lowCapacityWarningTriggerLevel()
        );
        WarehouseAddress savedWarehouseAddress = WAREHOUSE_ADDRESS_REPOSITORY.saveOrUpdate(warehouseAddress);

        CarPart part = CAR_PART_REPOSITORY.insert(
                input.name(),
                input.manufacturer(),
                input.compatibleVehicles(),
                input.supplier(),
                input.unitPrice(),
                input.quantity(),
                savedWarehouseAddress
        );

        PARTS.add(part);
        AuditService.record(
                "REGISTER_PART",
                "CAR_PART",
                String.valueOf(part.getId()),
                "SUCCESS",
                "Registered car part " + part.getName() + " at " + part.getWarehouseAddressLabel() + "."
        );
        return part;
    }

    public void sellPart(int partId, int saleQuantity) {
        AuthorizationService.require(Permission.SELL_PART);
        Validator.positiveInt(saleQuantity, "Sale quantity");

        CarPart part = findById(partId);

        if (part == null) {
            throw new IllegalArgumentException("Part not found.");
        }

        if (part.getQuantity() < saleQuantity) {
            throw new IllegalArgumentException("Not enough stock for this sale.");
        }

        CAR_PART_REPOSITORY.registerSale(part, saleQuantity);
        part.setQuantity(part.getQuantity() - saleQuantity);
        AuditService.record(
                "SELL_PART",
                "CAR_PART",
                String.valueOf(part.getId()),
                "SUCCESS",
                "Sold " + saleQuantity + " unit(s) of " + part.getName() + "."
        );
    }

    public ObservableList<CarPart> searchParts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return PARTS;
        }

        String normalizedQuery = query.trim().toLowerCase();
        ObservableList<CarPart> results = FXCollections.observableArrayList();

        for (CarPart part : PARTS) {
            boolean matches = String.valueOf(part.getId()).contains(normalizedQuery)
                    || part.getName().toLowerCase().contains(normalizedQuery)
                    || part.getManufacturer().toLowerCase().contains(normalizedQuery)
                    || part.getCompatibleVehicles().toLowerCase().contains(normalizedQuery)
                    || part.getSupplierName().toLowerCase().contains(normalizedQuery)
                    || part.getWarehouseAddressLabel().toLowerCase().contains(normalizedQuery);

            if (matches) {
                results.add(part);
            }
        }

        return results;
    }

    public void updateWarehouseData(int partId, String warehouseAddressText, int maxCapacity, int lowCapacityWarningTriggerLevel) {
        AuthorizationService.require(Permission.UPDATE_WAREHOUSE);

        Validator.requiredText(warehouseAddressText, "Warehouse address");
        Validator.positiveInt(maxCapacity, "Max capacity");
        Validator.betweenInclusive(lowCapacityWarningTriggerLevel, 0, maxCapacity, "Low stock warning");

        CarPart part = findById(partId);

        if (part == null) {
            throw new IllegalArgumentException("Part not found.");
        }

        if (part.getQuantity() > maxCapacity) {
            throw new IllegalArgumentException("Max capacity cannot be lower than the current quantity.");
        }

        WarehouseAddress warehouseAddress = createWarehouseAddressFromText(
                warehouseAddressText,
                maxCapacity,
                lowCapacityWarningTriggerLevel
        );
        WarehouseAddress savedWarehouseAddress = WAREHOUSE_ADDRESS_REPOSITORY.saveOrUpdate(warehouseAddress);

        CAR_PART_REPOSITORY.updateWarehouseAddress(partId, savedWarehouseAddress.getId());
        part.setWarehouseAddress(savedWarehouseAddress);
        AuditService.record(
                "UPDATE_WAREHOUSE",
                "CAR_PART",
                String.valueOf(part.getId()),
                "SUCCESS",
                "Updated warehouse data for " + part.getName() + " to " + part.getWarehouseAddressLabel() + "."
        );
    }

    public CarPart findById(int partId) {
        for (CarPart part : PARTS) {
            if (part.getId() == partId) {
                return part;
            }
        }

        return null;
    }

    private ValidatedPartInput validatePart(
            String name,
            String manufacturer,
            String compatibleVehicles,
            Supplier supplier,
            double unitPrice,
            int quantity,
            String warehouseAddressText,
            int maxCapacity,
            int lowCapacityWarningTriggerLevel
    ) {
        String validatedName = Validator.requiredText(name, 120, "Part name");
        String validatedManufacturer = Validator.optionalText(manufacturer, 120, "Manufacturer");
        String validatedCompatibleVehicles = Validator.optionalText(compatibleVehicles, 500, "Compatible vehicles");

        if (supplier == null) {
            throw new IllegalArgumentException("Supplier is required.");
        }

        Validator.nonNegativeMoney(unitPrice, "Unit price");
        Validator.nonNegativeInt(quantity, "Quantity");
        Validator.positiveInt(maxCapacity, "Max capacity");

        if (quantity > maxCapacity) {
            throw new IllegalArgumentException("Quantity cannot be greater than max capacity.");
        }

        Validator.betweenInclusive(lowCapacityWarningTriggerLevel, 0, maxCapacity, "Low stock warning");
        String validatedWarehouseAddressText = Validator.requiredText(warehouseAddressText, 40, "Warehouse address");
        createWarehouseAddressFromText(validatedWarehouseAddressText, maxCapacity, lowCapacityWarningTriggerLevel);

        return new ValidatedPartInput(
                validatedName,
                validatedManufacturer,
                validatedCompatibleVehicles,
                supplier,
                unitPrice,
                quantity,
                validatedWarehouseAddressText,
                maxCapacity,
                lowCapacityWarningTriggerLevel
        );
    }

    private WarehouseAddress createWarehouseAddressFromText(
            String warehouseAddressText,
            int maxCapacity,
            int lowCapacityWarningTriggerLevel
    ) {
        Matcher matcher = WAREHOUSE_ADDRESS_PATTERN.matcher(warehouseAddressText.trim());

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Warehouse address must use the format A-01-03 or A-01-03-04.");
        }

        char street = Character.toUpperCase(matcher.group(1).charAt(0));
        int building = Integer.parseInt(matcher.group(2));
        int level = Integer.parseInt(matcher.group(3));
        int apto = matcher.group(4) == null ? 0 : Integer.parseInt(matcher.group(4));

        return new WarehouseAddress(
                0,
                street,
                building,
                level,
                apto,
                maxCapacity,
                lowCapacityWarningTriggerLevel
        );
    }

    private record ValidatedPartInput(
            String name,
            String manufacturer,
            String compatibleVehicles,
            Supplier supplier,
            double unitPrice,
            int quantity,
            String warehouseAddressText,
            int maxCapacity,
            int lowCapacityWarningTriggerLevel
    ) {
    }
}

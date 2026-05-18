package br.com.vidasilva.jnventoryfx.service;

import br.com.vidasilva.jnventoryfx.model.CarPart;
import br.com.vidasilva.jnventoryfx.model.Supplier;
import br.com.vidasilva.jnventoryfx.repository.CarPartRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class InventoryService {
    private static final CarPartRepository CAR_PART_REPOSITORY = new CarPartRepository();
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
            String warehouseAddress,
            int maxCapacity,
            int lowCapacityWarningTriggerLevel
    ) {
        validatePart(name, supplier, unitPrice, quantity, warehouseAddress, maxCapacity, lowCapacityWarningTriggerLevel);

        CarPart part = CAR_PART_REPOSITORY.insert(
                name.trim(),
                safe(manufacturer),
                safe(compatibleVehicles),
                supplier,
                unitPrice,
                quantity,
                warehouseAddress.trim(),
                maxCapacity,
                lowCapacityWarningTriggerLevel
        );

        PARTS.add(part);
        return part;
    }

    public void sellPart(int partId, int saleQuantity) {
        if (saleQuantity <= 0) {
            throw new IllegalArgumentException("Sale quantity must be greater than zero.");
        }

        CarPart part = findById(partId);

        if (part == null) {
            throw new IllegalArgumentException("Part not found.");
        }

        if (part.getQuantity() < saleQuantity) {
            throw new IllegalArgumentException("Not enough stock for this sale.");
        }

        CAR_PART_REPOSITORY.registerSale(part, saleQuantity);
        part.setQuantity(part.getQuantity() - saleQuantity);
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
                    || part.getWarehouseAddress().toLowerCase().contains(normalizedQuery);

            if (matches) {
                results.add(part);
            }
        }

        return results;
    }

    public void updateWarehouseData(int partId, String warehouseAddress, int maxCapacity, int lowCapacityWarningTriggerLevel) {
        if (warehouseAddress == null || warehouseAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("Warehouse address is required.");
        }

        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Max capacity must be greater than zero.");
        }

        if (lowCapacityWarningTriggerLevel < 0 || lowCapacityWarningTriggerLevel > maxCapacity) {
            throw new IllegalArgumentException("Low stock warning must be between zero and max capacity.");
        }

        CarPart part = findById(partId);

        if (part == null) {
            throw new IllegalArgumentException("Part not found.");
        }

        if (part.getQuantity() > maxCapacity) {
            throw new IllegalArgumentException("Max capacity cannot be lower than the current quantity.");
        }

        String normalizedAddress = warehouseAddress.trim();
        CAR_PART_REPOSITORY.updateWarehouseData(partId, normalizedAddress, maxCapacity, lowCapacityWarningTriggerLevel);

        part.setWarehouseAddress(normalizedAddress);
        part.setMaxCapacity(maxCapacity);
        part.setLowCapacityWarningTriggerLevel(lowCapacityWarningTriggerLevel);
    }

    public CarPart findById(int partId) {
        for (CarPart part : PARTS) {
            if (part.getId() == partId) {
                return part;
            }
        }

        return null;
    }

    private void validatePart(
            String name,
            Supplier supplier,
            double unitPrice,
            int quantity,
            String warehouseAddress,
            int maxCapacity,
            int lowCapacityWarningTriggerLevel
    ) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("Part name is required.");
        }

        if (supplier == null) {
            throw new IllegalArgumentException("Supplier is required.");
        }

        if (unitPrice < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative.");
        }

        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }

        if (isBlank(warehouseAddress)) {
            throw new IllegalArgumentException("Warehouse address is required.");
        }

        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Max capacity must be greater than zero.");
        }

        if (quantity > maxCapacity) {
            throw new IllegalArgumentException("Quantity cannot be greater than max capacity.");
        }

        if (lowCapacityWarningTriggerLevel < 0 || lowCapacityWarningTriggerLevel > maxCapacity) {
            throw new IllegalArgumentException("Low stock warning must be between zero and max capacity.");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

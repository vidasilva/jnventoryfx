package br.com.vidasilva.jnventoryfx.model;

public class CarPart {
    private int id;
    private String name;
    private String manufacturer;
    private String compatibleVehicles;
    private Supplier supplier;
    private double unitPrice;
    private int quantity;
    private WarehouseAddress warehouseAddress;

    public CarPart(
            int id,
            String name,
            String manufacturer,
            String compatibleVehicles,
            Supplier supplier,
            double unitPrice,
            int quantity,
            WarehouseAddress warehouseAddress
    ) {
        this.id = id;
        this.name = name;
        this.manufacturer = manufacturer;
        this.compatibleVehicles = compatibleVehicles;
        this.supplier = supplier;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.warehouseAddress = warehouseAddress;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getCompatibleVehicles() {
        return compatibleVehicles;
    }

    public void setCompatibleVehicles(String compatibleVehicles) {
        this.compatibleVehicles = compatibleVehicles;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public String getSupplierName() {
        return supplier == null ? "No supplier" : supplier.getName();
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public WarehouseAddress getWarehouseAddress() {
        return warehouseAddress;
    }

    public void setWarehouseAddress(WarehouseAddress warehouseAddress) {
        this.warehouseAddress = warehouseAddress;
    }

    public String getWarehouseAddressLabel() {
        return warehouseAddress == null ? "No location" : warehouseAddress.getFormattedAddress();
    }

    public int getMaxCapacity() {
        return warehouseAddress == null ? 0 : warehouseAddress.getMaxCapacity();
    }

    public int getLowCapacityWarningTriggerLevel() {
        return warehouseAddress == null ? 0 : warehouseAddress.getLowCapacityWarningTriggerLevel();
    }

    public String getStockStatus() {
        if (quantity <= getLowCapacityWarningTriggerLevel()) {
            return "LOW STOCK";
        }

        if (quantity >= getMaxCapacity()) {
            return "FULL";
        }

        return "OK";
    }
}

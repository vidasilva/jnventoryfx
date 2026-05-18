package br.com.vidasilva.jnventoryfx.model;

public class CarPart {
    private int id;
    private String name;
    private String manufacturer;
    private String compatibleVehicles;
    private Supplier supplier;
    private double unitPrice;
    private int quantity;
    private String warehouseAddress;
    private int maxCapacity;
    private int lowCapacityWarningTriggerLevel;

    public CarPart(
            int id,
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
        this.id = id;
        this.name = name;
        this.manufacturer = manufacturer;
        this.compatibleVehicles = compatibleVehicles;
        this.supplier = supplier;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.warehouseAddress = warehouseAddress;
        this.maxCapacity = maxCapacity;
        this.lowCapacityWarningTriggerLevel = lowCapacityWarningTriggerLevel;
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

    public String getWarehouseAddress() {
        return warehouseAddress;
    }

    public void setWarehouseAddress(String warehouseAddress) {
        this.warehouseAddress = warehouseAddress;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public int getLowCapacityWarningTriggerLevel() {
        return lowCapacityWarningTriggerLevel;
    }

    public void setLowCapacityWarningTriggerLevel(int lowCapacityWarningTriggerLevel) {
        this.lowCapacityWarningTriggerLevel = lowCapacityWarningTriggerLevel;
    }

    public String getStockStatus() {
        if (quantity <= lowCapacityWarningTriggerLevel) {
            return "LOW STOCK";
        }

        if (quantity >= maxCapacity) {
            return "FULL";
        }

        return "OK";
    }
}

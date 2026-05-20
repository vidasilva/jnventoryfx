package br.com.vidasilva.jnventoryfx.model;

public class WarehouseAddress {

    private int id;
    private char street;
    private int building;
    private int level;
    private int apto;
    private int maxCapacity;
    private int lowCapacityWarningTriggerLevel;

    public WarehouseAddress(
            int id,
            char street,
            int building,
            int level,
            int apto,
            int maxCapacity,
            int lowCapacityWarningTriggerLevel
    ) {
        this.id = id;
        this.street = street;
        this.building = building;
        this.level = level;
        this.apto = apto;
        this.maxCapacity = maxCapacity;
        this.lowCapacityWarningTriggerLevel = lowCapacityWarningTriggerLevel;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public char getStreet() {
        return street;
    }

    public void setStreet(char street) {
        this.street = Character.toUpperCase(street);
    }

    public int getBuilding() {
        return building;
    }

    public void setBuilding(int building) {
        this.building = building;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getApto() {
        return apto;
    }

    public void setApto(int apto) {
        this.apto = apto;
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

    public String getFormattedAddress() {
        if (apto <= 0) {
            return "%s-%02d-%02d".formatted(street, building, level);
        }

        return "%s-%02d-%02d-%02d".formatted(street, building, level, apto);
    }

    @Override
    public String toString() {
        return getFormattedAddress();
    }
}

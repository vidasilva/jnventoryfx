package br.com.vidasilva.jnventoryfx.repository;

import br.com.vidasilva.jnventoryfx.database.Database;
import br.com.vidasilva.jnventoryfx.model.WarehouseAddress;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class WarehouseAddressRepository {

    public List<WarehouseAddress> findAll() {
        String sql = """
                SELECT id, street, building, level, apto, max_capacity, low_capacity_warning_trigger_level
                FROM warehouse_addresses
                ORDER BY street, building, level, apto
                """;

        List<WarehouseAddress> addresses = new ArrayList<>();

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                addresses.add(mapWarehouseAddress(resultSet));
            }

            return addresses;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load warehouse addresses from SQLite.", exception);
        }
    }

    public WarehouseAddress findById(int id) {
        String sql = """
                SELECT id, street, building, level, apto, max_capacity, low_capacity_warning_trigger_level
                FROM warehouse_addresses
                WHERE id = ?
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapWarehouseAddress(resultSet) : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not find warehouse address in SQLite.", exception);
        }
    }

    public WarehouseAddress findByCoordinates(char street, int building, int level, int apto) {
        String sql = """
                SELECT id, street, building, level, apto, max_capacity, low_capacity_warning_trigger_level
                FROM warehouse_addresses
                WHERE street = ? AND building = ? AND level = ? AND apto = ?
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, String.valueOf(Character.toUpperCase(street)));
            statement.setInt(2, building);
            statement.setInt(3, level);
            statement.setInt(4, apto);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapWarehouseAddress(resultSet) : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not find warehouse address in SQLite.", exception);
        }
    }

    public WarehouseAddress saveOrUpdate(WarehouseAddress address) {
        WarehouseAddress existingAddress = findByCoordinates(
                address.getStreet(),
                address.getBuilding(),
                address.getLevel(),
                address.getApto()
        );

        if (existingAddress == null) {
            return insert(address);
        }

        updateCapacity(
                existingAddress.getId(),
                address.getMaxCapacity(),
                address.getLowCapacityWarningTriggerLevel()
        );

        existingAddress.setMaxCapacity(address.getMaxCapacity());
        existingAddress.setLowCapacityWarningTriggerLevel(address.getLowCapacityWarningTriggerLevel());
        return existingAddress;
    }

    public WarehouseAddress insert(WarehouseAddress address) {
        String sql = """
                INSERT INTO warehouse_addresses (
                    street,
                    building,
                    level,
                    apto,
                    max_capacity,
                    low_capacity_warning_trigger_level
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, String.valueOf(Character.toUpperCase(address.getStreet())));
            statement.setInt(2, address.getBuilding());
            statement.setInt(3, address.getLevel());
            statement.setInt(4, address.getApto());
            statement.setInt(5, address.getMaxCapacity());
            statement.setInt(6, address.getLowCapacityWarningTriggerLevel());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    address.setId(generatedKeys.getInt(1));
                    Database.persistEncryptedCopy();
                    return address;
                }
            }

            throw new IllegalStateException("Warehouse address was inserted, but SQLite did not return an ID.");
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save warehouse address to SQLite.", exception);
        }
    }

    public void updateCapacity(int warehouseAddressId, int maxCapacity, int lowCapacityWarningTriggerLevel) {
        String sql = """
                UPDATE warehouse_addresses
                SET max_capacity = ?,
                    low_capacity_warning_trigger_level = ?
                WHERE id = ?
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, maxCapacity);
            statement.setInt(2, lowCapacityWarningTriggerLevel);
            statement.setInt(3, warehouseAddressId);

            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated == 0) {
                throw new IllegalArgumentException("Warehouse address not found.");
            }
            Database.persistEncryptedCopy();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not update warehouse address in SQLite.", exception);
        }
    }

    private WarehouseAddress mapWarehouseAddress(ResultSet resultSet) throws SQLException {
        return new WarehouseAddress(
                resultSet.getInt("id"),
                resultSet.getString("street").charAt(0),
                resultSet.getInt("building"),
                resultSet.getInt("level"),
                resultSet.getInt("apto"),
                resultSet.getInt("max_capacity"),
                resultSet.getInt("low_capacity_warning_trigger_level")
        );
    }
}

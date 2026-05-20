package br.com.vidasilva.jnventoryfx.repository;

import br.com.vidasilva.jnventoryfx.database.Database;
import br.com.vidasilva.jnventoryfx.model.CarPart;
import br.com.vidasilva.jnventoryfx.model.Supplier;
import br.com.vidasilva.jnventoryfx.model.WarehouseAddress;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CarPartRepository {

    public List<CarPart> findAll() {
        String sql = """
                SELECT
                    cp.id,
                    cp.name,
                    cp.manufacturer,
                    cp.compatible_vehicles,
                    cp.unit_price,
                    cp.quantity,
                    s.id AS supplier_id,
                    s.name AS supplier_name,
                    s.phone AS supplier_phone,
                    s.email AS supplier_email,
                    s.address AS supplier_address,
                    s.notes AS supplier_notes,
                    wa.id AS warehouse_address_id,
                    wa.street AS warehouse_street,
                    wa.building AS warehouse_building,
                    wa.level AS warehouse_level,
                    wa.apto AS warehouse_apto,
                    wa.max_capacity AS warehouse_max_capacity,
                    wa.low_capacity_warning_trigger_level AS warehouse_low_capacity_warning_trigger_level
                FROM car_parts cp
                JOIN suppliers s ON s.id = cp.supplier_id
                JOIN warehouse_addresses wa ON wa.id = cp.warehouse_address_id
                ORDER BY cp.id
                """;

        List<CarPart> parts = new ArrayList<>();

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                parts.add(mapCarPart(resultSet));
            }

            return parts;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load car parts from SQLite.", exception);
        }
    }

    public CarPart insert(
            String name,
            String manufacturer,
            String compatibleVehicles,
            Supplier supplier,
            double unitPrice,
            int quantity,
            WarehouseAddress warehouseAddress
    ) {
        String sql = """
                INSERT INTO car_parts (
                    name,
                    manufacturer,
                    compatible_vehicles,
                    supplier_id,
                    unit_price,
                    quantity,
                    warehouse_address_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, name);
            statement.setString(2, manufacturer);
            statement.setString(3, compatibleVehicles);
            statement.setInt(4, supplier.getId());
            statement.setDouble(5, unitPrice);
            statement.setInt(6, quantity);
            statement.setInt(7, warehouseAddress.getId());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    CarPart carPart = new CarPart(
                            generatedKeys.getInt(1),
                            name,
                            manufacturer,
                            compatibleVehicles,
                            supplier,
                            unitPrice,
                            quantity,
                            warehouseAddress
                    );
                    Database.persistEncryptedCopy();
                    return carPart;
                }
            }

            throw new IllegalStateException("Car part was inserted, but SQLite did not return an ID.");
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save car part to SQLite.", exception);
        }
    }

    public void registerSale(CarPart part, int saleQuantity) {
        String updatePartSql = """
                UPDATE car_parts
                SET quantity = ?
                WHERE id = ?
                """;

        String insertSaleSql = """
                INSERT INTO sales (car_part_id, quantity, unit_price_at_sale)
                VALUES (?, ?, ?)
                """;

        int newQuantity = part.getQuantity() - saleQuantity;

        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);

            try (PreparedStatement updatePartStatement = connection.prepareStatement(updatePartSql);
                 PreparedStatement insertSaleStatement = connection.prepareStatement(insertSaleSql)) {

                updatePartStatement.setInt(1, newQuantity);
                updatePartStatement.setInt(2, part.getId());
                updatePartStatement.executeUpdate();

                insertSaleStatement.setInt(1, part.getId());
                insertSaleStatement.setInt(2, saleQuantity);
                insertSaleStatement.setDouble(3, part.getUnitPrice());
                insertSaleStatement.executeUpdate();

                connection.commit();
                Database.persistEncryptedCopy();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not register sale in SQLite.", exception);
        }
    }

    public void updateWarehouseAddress(int partId, int warehouseAddressId) {
        String sql = """
                UPDATE car_parts
                SET warehouse_address_id = ?
                WHERE id = ?
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, warehouseAddressId);
            statement.setInt(2, partId);

            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated == 0) {
                throw new IllegalArgumentException("Part not found.");
            }
            Database.persistEncryptedCopy();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not update part warehouse address in SQLite.", exception);
        }
    }

    private CarPart mapCarPart(ResultSet resultSet) throws SQLException {
        Supplier supplier = new Supplier(
                resultSet.getInt("supplier_id"),
                resultSet.getString("supplier_name"),
                resultSet.getString("supplier_phone"),
                resultSet.getString("supplier_email"),
                resultSet.getString("supplier_address"),
                resultSet.getString("supplier_notes")
        );

        WarehouseAddress warehouseAddress = new WarehouseAddress(
                resultSet.getInt("warehouse_address_id"),
                resultSet.getString("warehouse_street").charAt(0),
                resultSet.getInt("warehouse_building"),
                resultSet.getInt("warehouse_level"),
                resultSet.getInt("warehouse_apto"),
                resultSet.getInt("warehouse_max_capacity"),
                resultSet.getInt("warehouse_low_capacity_warning_trigger_level")
        );

        return new CarPart(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("manufacturer"),
                resultSet.getString("compatible_vehicles"),
                supplier,
                resultSet.getDouble("unit_price"),
                resultSet.getInt("quantity"),
                warehouseAddress
        );
    }
}

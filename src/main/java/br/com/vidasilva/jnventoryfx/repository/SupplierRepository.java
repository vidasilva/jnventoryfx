package br.com.vidasilva.jnventoryfx.repository;

import br.com.vidasilva.jnventoryfx.database.Database;
import br.com.vidasilva.jnventoryfx.model.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SupplierRepository {

    public List<Supplier> findAll() {
        String sql = """
                SELECT id, name, phone, email, address, notes
                FROM suppliers
                ORDER BY name
                """;

        List<Supplier> suppliers = new ArrayList<>();

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                suppliers.add(mapSupplier(resultSet));
            }

            return suppliers;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load suppliers from SQLite.", exception);
        }
    }

    public Supplier insert(String name, String phone, String email, String address, String notes) {
        String sql = """
                INSERT INTO suppliers (name, phone, email, address, notes)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, name);
            statement.setString(2, phone);
            statement.setString(3, email);
            statement.setString(4, address);
            statement.setString(5, notes);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new Supplier(generatedKeys.getInt(1), name, phone, email, address, notes);
                }
            }

            throw new IllegalStateException("Supplier was inserted, but SQLite did not return an ID.");
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save supplier to SQLite.", exception);
        }
    }

    private Supplier mapSupplier(ResultSet resultSet) throws SQLException {
        return new Supplier(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("phone"),
                resultSet.getString("email"),
                resultSet.getString("address"),
                resultSet.getString("notes")
        );
    }
}

package br.com.vidasilva.jnventoryfx.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private static final String DATABASE_URL = "jdbc:sqlite:jnventoryfx.db";
    private static boolean initialized = false;

    private Database() {
    }

    public static synchronized Connection connect() throws SQLException {
        initialize();
        Connection connection = DriverManager.getConnection(DATABASE_URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            Class.forName("org.sqlite.JDBC");

            try (Connection connection = DriverManager.getConnection(DATABASE_URL);
                 Statement statement = connection.createStatement()) {

                statement.execute("PRAGMA foreign_keys = ON");

                statement.execute("""
                        CREATE TABLE IF NOT EXISTS suppliers (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            name TEXT NOT NULL,
                            phone TEXT NOT NULL DEFAULT '',
                            email TEXT NOT NULL DEFAULT '',
                            address TEXT NOT NULL DEFAULT '',
                            notes TEXT NOT NULL DEFAULT ''
                        )
                        """);

                statement.execute("""
                        CREATE TABLE IF NOT EXISTS users (
                            email TEXT PRIMARY KEY,
                            username TEXT NOT NULL,
                            password TEXT NOT NULL,
                            role TEXT NOT NULL
                        )
                        """);

                statement.execute("""
                        CREATE TABLE IF NOT EXISTS car_parts (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            name TEXT NOT NULL,
                            manufacturer TEXT NOT NULL DEFAULT '',
                            compatible_vehicles TEXT NOT NULL DEFAULT '',
                            supplier_id INTEGER NOT NULL,
                            unit_price REAL NOT NULL,
                            quantity INTEGER NOT NULL,
                            warehouse_address TEXT NOT NULL,
                            max_capacity INTEGER NOT NULL,
                            low_capacity_warning_trigger_level INTEGER NOT NULL,
                            FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON UPDATE CASCADE ON DELETE RESTRICT
                        )
                        """);

                statement.execute("""
                        CREATE TABLE IF NOT EXISTS sales (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            car_part_id INTEGER NOT NULL,
                            quantity INTEGER NOT NULL,
                            unit_price_at_sale REAL NOT NULL,
                            sold_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (car_part_id) REFERENCES car_parts(id) ON UPDATE CASCADE ON DELETE RESTRICT
                        )
                        """);

                seedSuppliers(connection);
                seedUsers(connection);
                seedCarParts(connection);
            }

            initialized = true;
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("SQLite JDBC driver was not found. Check your pom.xml dependency.", exception);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not initialize SQLite database.", exception);
        }
    }

    private static void seedSuppliers(Connection connection) throws SQLException {
        if (countRows(connection, "suppliers") > 0) {
            return;
        }

        String sql = """
                INSERT INTO suppliers (name, phone, email, address, notes)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            insertSupplier(statement, "AutoPrime Distribuidora", "+55 51 90000-1000", "sales@autoprime.local", "Rua das Peças, 120", "Brake and suspension parts");
            insertSupplier(statement, "MotorSul Imports", "+55 51 90000-2000", "orders@motorsul.local", "Av. Industrial, 350", "Imported engine components");
        }
    }

    private static void insertSupplier(PreparedStatement statement, String name, String phone, String email, String address, String notes) throws SQLException {
        statement.setString(1, name);
        statement.setString(2, phone);
        statement.setString(3, email);
        statement.setString(4, address);
        statement.setString(5, notes);
        statement.executeUpdate();
    }

    private static void seedUsers(Connection connection) throws SQLException {
        if (countRows(connection, "users") > 0) {
            return;
        }

        String sql = """
                INSERT INTO users (username, email, password, role)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            insertUser(statement, "Admin", "admin@inventory.local", "admin123", "ADMIN");
            insertUser(statement, "Manager", "manager@inventory.local", "manager123", "MANAGER");
            insertUser(statement, "Cashier", "cashier@inventory.local", "cashier123", "CASHIER");
            insertUser(statement, "Warehouse", "warehouse@inventory.local", "warehouse123", "WAREHOUSE");
        }
    }

    private static void insertUser(PreparedStatement statement, String username, String email, String password, String role) throws SQLException {
        statement.setString(1, username);
        statement.setString(2, email);
        statement.setString(3, password);
        statement.setString(4, role);
        statement.executeUpdate();
    }

    private static void seedCarParts(Connection connection) throws SQLException {
        if (countRows(connection, "car_parts") > 0) {
            return;
        }

        Integer supplierAId = findSupplierIdByName(connection, "AutoPrime Distribuidora");
        Integer supplierBId = findSupplierIdByName(connection, "MotorSul Imports");

        if (supplierAId == null || supplierBId == null) {
            return;
        }

        String sql = """
                INSERT INTO car_parts (
                    name,
                    manufacturer,
                    compatible_vehicles,
                    supplier_id,
                    unit_price,
                    quantity,
                    warehouse_address,
                    max_capacity,
                    low_capacity_warning_trigger_level
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            insertPart(statement, "Brake Pad Set", "Bosch", "Civic 2016-2021, Corolla 2015-2019", supplierAId, 189.90, 24, "A-01-03", 50, 10);
            insertPart(statement, "Oil Filter", "Mann", "Onix 2018-2024, Prisma 2017-2020", supplierBId, 39.90, 8, "B-02-01", 80, 12);
            insertPart(statement, "Spark Plug", "NGK", "Gol 2014-2022, Fox 2015-2021", supplierAId, 29.90, 120, "C-04-02", 120, 20);
        }
    }

    private static void insertPart(
            PreparedStatement statement,
            String name,
            String manufacturer,
            String compatibleVehicles,
            int supplierId,
            double unitPrice,
            int quantity,
            String warehouseAddress,
            int maxCapacity,
            int lowCapacityWarningTriggerLevel
    ) throws SQLException {
        statement.setString(1, name);
        statement.setString(2, manufacturer);
        statement.setString(3, compatibleVehicles);
        statement.setInt(4, supplierId);
        statement.setDouble(5, unitPrice);
        statement.setInt(6, quantity);
        statement.setString(7, warehouseAddress);
        statement.setInt(8, maxCapacity);
        statement.setInt(9, lowCapacityWarningTriggerLevel);
        statement.executeUpdate();
    }

    private static int countRows(Connection connection, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private static Integer findSupplierIdByName(Connection connection, String name) throws SQLException {
        String sql = "SELECT id FROM suppliers WHERE name = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt("id") : null;
            }
        }
    }
}

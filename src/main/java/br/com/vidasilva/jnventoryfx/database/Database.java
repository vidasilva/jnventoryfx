package br.com.vidasilva.jnventoryfx.database;

import br.com.vidasilva.jnventoryfx.security.DatabaseEncryptionService;
import br.com.vidasilva.jnventoryfx.security.PasswordHasher;
import org.sqlite.mc.SQLiteMCSqlCipherConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Database {
    private static final Path DATABASE_FILE = Path.of("jnventoryfx.db");
    private static final Path LEGACY_RUNTIME_DIRECTORY = Path.of(".jnventoryfx-runtime");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE.toAbsolutePath().normalize();
    private static final byte[] SQLITE_PLAINTEXT_HEADER = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);
    private static final List<String> MIGRATED_TABLES = List.of(
            "suppliers",
            "users",
            "password_reset_tokens",
            "warehouse_addresses",
            "car_parts",
            "sales",
            "audit_logs"
    );
    private static boolean initialized = false;

    private Database() {
    }

    public static synchronized Connection connect() throws SQLException {
        initialize();
        return openEncryptedConnection();
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            Class.forName("org.sqlite.JDBC");
            removeLegacyRuntimeDirectory();
            migratePlaintextDatabaseIfNeeded();

            try (Connection connection = openEncryptedConnection()) {
                prepareSchemaAndSeedData(connection);
            }

            initialized = true;
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("SQLCipher-capable SQLite JDBC driver was not found. Check your pom.xml dependency.", exception);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not initialize SQLite database.", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not prepare SQLCipher encrypted SQLite storage.", exception);
        }
    }

    public static synchronized void persistEncryptedCopy() {
        // SQLCipher encrypts pages directly in the live SQLite file, so there is no decrypted runtime copy to persist.
    }

    private static Connection openEncryptedConnection() throws SQLException {
        String databaseKey;
        try {
            databaseKey = DatabaseEncryptionService.loadSqlCipherKey();
        } catch (Exception exception) {
            throw new SQLException("Could not load SQLCipher database key.", exception);
        }

        var properties = SQLiteMCSqlCipherConfig.getDefault()
                .withKey(databaseKey)
                .build()
                .toProperties();

        Connection connection = DriverManager.getConnection(DATABASE_URL, properties);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("SELECT count(*) FROM sqlite_master");
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Keep the original failure; closing is cleanup only.
            }

            throw new SQLException("Could not unlock the SQLCipher database. Check JNVENTORYFX_DB_KEY or delete the incompatible local demo database.", exception);
        }

        return connection;
    }

    private static void migratePlaintextDatabaseIfNeeded() throws Exception {
        if (!Files.exists(DATABASE_FILE) || !isPlaintextSqliteDatabase(DATABASE_FILE)) {
            return;
        }

        Path plaintextSource = Path.of(DATABASE_FILE + ".plaintext-migration-source");
        Files.deleteIfExists(plaintextSource);
        Files.move(DATABASE_FILE, plaintextSource);

        boolean migrated = false;
        try (Connection source = DriverManager.getConnection("jdbc:sqlite:" + plaintextSource.toAbsolutePath().normalize());
             Connection destination = openEncryptedConnection()) {

            createSchema(destination);
            copyLegacyPlaintextData(source, destination);
            migrateLegacyUserTable(destination);
            migrateLegacyWarehouseData(destination);
            migrateLegacyUserPasswords(destination);
            seedSuppliers(destination);
            seedUsers(destination);
            seedWarehouseAddresses(destination);
            seedCarParts(destination);
            migrated = true;
        } finally {
            if (migrated) {
                Files.deleteIfExists(plaintextSource);
            } else {
                Files.deleteIfExists(DATABASE_FILE);
                Files.move(plaintextSource, DATABASE_FILE);
            }
        }
    }

    private static boolean isPlaintextSqliteDatabase(Path path) {
        try {
            if (Files.size(path) < SQLITE_PLAINTEXT_HEADER.length) {
                return false;
            }

            byte[] header = Files.readAllBytes(path);
            if (header.length < SQLITE_PLAINTEXT_HEADER.length) {
                return false;
            }

            for (int index = 0; index < SQLITE_PLAINTEXT_HEADER.length; index++) {
                if (header[index] != SQLITE_PLAINTEXT_HEADER[index]) {
                    return false;
                }
            }

            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private static void prepareSchemaAndSeedData(Connection connection) throws SQLException {
        createSchema(connection);
        migrateLegacyUserTable(connection);
        migrateLegacyWarehouseData(connection);
        migrateLegacyUserPasswords(connection);
        seedSuppliers(connection);
        seedUsers(connection);
        seedWarehouseAddresses(connection);
        seedCarParts(connection);
    }

    private static void createSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode = DELETE");

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
                        role TEXT NOT NULL,
                        must_change_password INTEGER NOT NULL DEFAULT 0
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS password_reset_tokens (
                        user_email TEXT PRIMARY KEY,
                        token_hash TEXT NOT NULL,
                        expires_at TEXT NOT NULL,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_email) REFERENCES users(email) ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS warehouse_addresses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        street TEXT NOT NULL,
                        building INTEGER NOT NULL,
                        level INTEGER NOT NULL,
                        apto INTEGER NOT NULL DEFAULT 0,
                        max_capacity INTEGER NOT NULL,
                        low_capacity_warning_trigger_level INTEGER NOT NULL,
                        UNIQUE (street, building, level, apto)
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
                        warehouse_address_id INTEGER NOT NULL,
                        FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON UPDATE CASCADE ON DELETE RESTRICT,
                        FOREIGN KEY (warehouse_address_id) REFERENCES warehouse_addresses(id) ON UPDATE CASCADE ON DELETE RESTRICT
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

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS audit_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        event_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        actor_username TEXT NOT NULL DEFAULT '',
                        actor_email TEXT NOT NULL DEFAULT '',
                        actor_role TEXT NOT NULL DEFAULT '',
                        action TEXT NOT NULL,
                        target_type TEXT NOT NULL DEFAULT '',
                        target_id TEXT NOT NULL DEFAULT '',
                        status TEXT NOT NULL DEFAULT '',
                        details TEXT NOT NULL DEFAULT ''
                    )
                    """);
        }
    }

    private static void copyLegacyPlaintextData(Connection source, Connection destination) throws SQLException {
        try (Statement statement = destination.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");
        }

        try {
            for (String tableName : MIGRATED_TABLES) {
                copyTableIfPresent(source, destination, tableName);
            }
        } finally {
            try (Statement statement = destination.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
        }
    }

    private static void copyTableIfPresent(Connection source, Connection destination, String tableName) throws SQLException {
        if (!tableExists(source, tableName) || !tableExists(destination, tableName)) {
            return;
        }

        List<String> sharedColumns = sharedColumns(source, destination, tableName);
        if (sharedColumns.isEmpty()) {
            return;
        }

        String columns = String.join(", ", sharedColumns);
        String placeholders = String.join(", ", sharedColumns.stream().map(column -> "?").toList());
        String selectSql = "SELECT " + columns + " FROM " + tableName;
        String insertSql = "INSERT OR IGNORE INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";

        try (PreparedStatement selectStatement = source.prepareStatement(selectSql);
             ResultSet resultSet = selectStatement.executeQuery();
             PreparedStatement insertStatement = destination.prepareStatement(insertSql)) {

            int columnCount = sharedColumns.size();
            while (resultSet.next()) {
                for (int index = 1; index <= columnCount; index++) {
                    insertStatement.setObject(index, resultSet.getObject(index));
                }
                insertStatement.executeUpdate();
            }
        }
    }

    private static List<String> sharedColumns(Connection source, Connection destination, String tableName) throws SQLException {
        Set<String> sourceColumns = new LinkedHashSet<>(tableColumns(source, tableName));
        List<String> destinationColumns = tableColumns(destination, tableName);
        List<String> sharedColumns = new ArrayList<>();

        for (String column : destinationColumns) {
            if (sourceColumns.contains(column)) {
                sharedColumns.add(column);
            }
        }

        return sharedColumns;
    }

    private static List<String> tableColumns(Connection connection, String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        String sql = "PRAGMA table_info(" + tableName + ")";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                columns.add(resultSet.getString("name"));
            }
        }

        return columns;
    }

    private static void removeLegacyRuntimeDirectory() {
        try {
            if (!Files.exists(LEGACY_RUNTIME_DIRECTORY)) {
                return;
            }

            try (var paths = Files.walk(LEGACY_RUNTIME_DIRECTORY)) {
                paths.sorted((first, second) -> second.compareTo(first))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception ignored) {
                                // A stale runtime file should not prevent the SQLCipher database from opening.
                            }
                        });
            }
        } catch (Exception ignored) {
            // Startup can continue; the new database flow no longer uses this directory.
        }
    }

    private static void migrateLegacyUserTable(Connection connection) throws SQLException {
        if (!tableExists(connection, "users")) {
            return;
        }

        if (!columnExists(connection, "users", "must_change_password")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE users ADD COLUMN must_change_password INTEGER NOT NULL DEFAULT 0");
            }
        }
    }

    private static void migrateLegacyUserPasswords(Connection connection) throws SQLException {
        if (!tableExists(connection, "users") || !columnExists(connection, "users", "password")) {
            return;
        }

        String selectSql = """
                SELECT email, password
                FROM users
                """;

        try (PreparedStatement selectStatement = connection.prepareStatement(selectSql);
             ResultSet resultSet = selectStatement.executeQuery()) {

            while (resultSet.next()) {
                String email = resultSet.getString("email");
                String password = resultSet.getString("password");

                if (!PasswordHasher.isHashed(password)) {
                    updateUserPasswordHash(connection, email, PasswordHasher.hash(password));
                }
            }
        }
    }

    private static void updateUserPasswordHash(Connection connection, String email, String passwordHash) throws SQLException {
        String sql = """
                UPDATE users
                SET password = ?
                WHERE email = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setString(2, email);
            statement.executeUpdate();
        }
    }

    private static void migrateLegacyWarehouseData(Connection connection) throws SQLException {
        if (!tableExists(connection, "car_parts")) {
            return;
        }

        boolean hasWarehouseAddressId = columnExists(connection, "car_parts", "warehouse_address_id");
        boolean hasLegacyWarehouseAddress = columnExists(connection, "car_parts", "warehouse_address");
        boolean hasLegacyMaxCapacity = columnExists(connection, "car_parts", "max_capacity");
        boolean hasLegacyLowLevel = columnExists(connection, "car_parts", "low_capacity_warning_trigger_level");

        if (!hasWarehouseAddressId) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE car_parts ADD COLUMN warehouse_address_id INTEGER");
            }
        }

        if (!hasLegacyWarehouseAddress || !hasLegacyMaxCapacity || !hasLegacyLowLevel) {
            return;
        }

        String selectSql = """
                SELECT id, warehouse_address, max_capacity, low_capacity_warning_trigger_level
                FROM car_parts
                WHERE warehouse_address_id IS NULL
                """;

        try (PreparedStatement selectStatement = connection.prepareStatement(selectSql);
             ResultSet resultSet = selectStatement.executeQuery()) {

            while (resultSet.next()) {
                ParsedWarehouseAddress parsedAddress = parseWarehouseAddress(
                        resultSet.getString("warehouse_address"),
                        resultSet.getInt("max_capacity"),
                        resultSet.getInt("low_capacity_warning_trigger_level")
                );

                int warehouseAddressId = findOrCreateWarehouseAddress(connection, parsedAddress);
                updateCarPartWarehouseAddressId(connection, resultSet.getInt("id"), warehouseAddressId);
            }
        }

        rebuildLegacyCarPartsTable(connection);
    }

    private static void rebuildLegacyCarPartsTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");
            statement.execute("DROP TABLE IF EXISTS car_parts_new");
            statement.execute("""
                    CREATE TABLE car_parts_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        manufacturer TEXT NOT NULL DEFAULT '',
                        compatible_vehicles TEXT NOT NULL DEFAULT '',
                        supplier_id INTEGER NOT NULL,
                        unit_price REAL NOT NULL,
                        quantity INTEGER NOT NULL,
                        warehouse_address_id INTEGER NOT NULL,
                        FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON UPDATE CASCADE ON DELETE RESTRICT,
                        FOREIGN KEY (warehouse_address_id) REFERENCES warehouse_addresses(id) ON UPDATE CASCADE ON DELETE RESTRICT
                    )
                    """);
            statement.execute("""
                    INSERT INTO car_parts_new (
                        id,
                        name,
                        manufacturer,
                        compatible_vehicles,
                        supplier_id,
                        unit_price,
                        quantity,
                        warehouse_address_id
                    )
                    SELECT
                        id,
                        name,
                        manufacturer,
                        compatible_vehicles,
                        supplier_id,
                        unit_price,
                        quantity,
                        warehouse_address_id
                    FROM car_parts
                    WHERE warehouse_address_id IS NOT NULL
                    """);
            statement.execute("DROP TABLE car_parts");
            statement.execute("ALTER TABLE car_parts_new RENAME TO car_parts");
            statement.execute("PRAGMA foreign_keys = ON");
        }
    }

    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        String sql = """
                SELECT name
                FROM sqlite_master
                WHERE type = 'table' AND name = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        String sql = "PRAGMA table_info(" + tableName + ")";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
        }

        return false;
    }

    private static ParsedWarehouseAddress parseWarehouseAddress(
            String address,
            int maxCapacity,
            int lowCapacityWarningTriggerLevel
    ) {
        if (address == null || address.trim().isEmpty()) {
            return new ParsedWarehouseAddress('Z', 0, 0, 0, maxCapacity, lowCapacityWarningTriggerLevel);
        }

        String[] pieces = address.trim().toUpperCase().split("[-\\s]+");
        char street = pieces[0].charAt(0);
        int building = pieces.length > 1 ? parsePositiveNumber(pieces[1]) : 0;
        int level = pieces.length > 2 ? parsePositiveNumber(pieces[2]) : 0;
        int apto = pieces.length > 3 ? parsePositiveNumber(pieces[3]) : 0;

        return new ParsedWarehouseAddress(street, building, level, apto, maxCapacity, lowCapacityWarningTriggerLevel);
    }

    private static int parsePositiveNumber(String value) {
        try {
            return Integer.parseInt(value.replaceAll("\\D", ""));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static int findOrCreateWarehouseAddress(Connection connection, ParsedWarehouseAddress address) throws SQLException {
        Integer existingAddressId = findWarehouseAddressId(connection, address.street(), address.building(), address.level(), address.apto());

        if (existingAddressId != null) {
            updateWarehouseAddressCapacity(
                    connection,
                    existingAddressId,
                    address.maxCapacity(),
                    address.lowCapacityWarningTriggerLevel()
            );
            return existingAddressId;
        }

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

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, String.valueOf(address.street()));
            statement.setInt(2, address.building());
            statement.setInt(3, address.level());
            statement.setInt(4, address.apto());
            statement.setInt(5, address.maxCapacity());
            statement.setInt(6, address.lowCapacityWarningTriggerLevel());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new IllegalStateException("Warehouse address was inserted, but SQLite did not return an ID.");
    }

    private static Integer findWarehouseAddressId(
            Connection connection,
            char street,
            int building,
            int level,
            int apto
    ) throws SQLException {
        String sql = """
                SELECT id
                FROM warehouse_addresses
                WHERE street = ? AND building = ? AND level = ? AND apto = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, String.valueOf(Character.toUpperCase(street)));
            statement.setInt(2, building);
            statement.setInt(3, level);
            statement.setInt(4, apto);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt("id") : null;
            }
        }
    }

    private static void updateWarehouseAddressCapacity(
            Connection connection,
            int warehouseAddressId,
            int maxCapacity,
            int lowCapacityWarningTriggerLevel
    ) throws SQLException {
        String sql = """
                UPDATE warehouse_addresses
                SET max_capacity = ?,
                    low_capacity_warning_trigger_level = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, maxCapacity);
            statement.setInt(2, lowCapacityWarningTriggerLevel);
            statement.setInt(3, warehouseAddressId);
            statement.executeUpdate();
        }
    }

    private static void updateCarPartWarehouseAddressId(
            Connection connection,
            int carPartId,
            int warehouseAddressId
    ) throws SQLException {
        String sql = """
                UPDATE car_parts
                SET warehouse_address_id = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, warehouseAddressId);
            statement.setInt(2, carPartId);
            statement.executeUpdate();
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
                INSERT INTO users (username, email, password, role, must_change_password)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            insertUser(statement, "Admin", "admin@inventory.local", PasswordHasher.hash("admin123"), "ADMIN");
            insertUser(statement, "Manager", "manager@inventory.local", PasswordHasher.hash("manager123"), "MANAGER");
            insertUser(statement, "Cashier", "cashier@inventory.local", PasswordHasher.hash("cashier123"), "CASHIER");
            insertUser(statement, "Warehouse", "warehouse@inventory.local", PasswordHasher.hash("warehouse123"), "WAREHOUSE");
        }
    }

    private static void insertUser(PreparedStatement statement, String username, String email, String password, String role) throws SQLException {
        statement.setString(1, username);
        statement.setString(2, email);
        statement.setString(3, password);
        statement.setString(4, role);
        statement.setInt(5, 0);
        statement.executeUpdate();
    }

    private static void seedWarehouseAddresses(Connection connection) throws SQLException {
        if (countRows(connection, "warehouse_addresses") > 0) {
            return;
        }

        findOrCreateWarehouseAddress(connection, new ParsedWarehouseAddress('A', 1, 3, 0, 50, 10));
        findOrCreateWarehouseAddress(connection, new ParsedWarehouseAddress('B', 2, 1, 0, 80, 12));
        findOrCreateWarehouseAddress(connection, new ParsedWarehouseAddress('C', 4, 2, 0, 120, 20));
    }

    private static void seedCarParts(Connection connection) throws SQLException {
        if (countRows(connection, "car_parts") > 0) {
            return;
        }

        Integer supplierAId = findSupplierIdByName(connection, "AutoPrime Distribuidora");
        Integer supplierBId = findSupplierIdByName(connection, "MotorSul Imports");
        Integer addressAId = findWarehouseAddressId(connection, 'A', 1, 3, 0);
        Integer addressBId = findWarehouseAddressId(connection, 'B', 2, 1, 0);
        Integer addressCId = findWarehouseAddressId(connection, 'C', 4, 2, 0);

        if (supplierAId == null || supplierBId == null || addressAId == null || addressBId == null || addressCId == null) {
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
                    warehouse_address_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            insertPart(statement, "Brake Pad Set", "Bosch", "Civic 2016-2021, Corolla 2015-2019", supplierAId, 189.90, 24, addressAId);
            insertPart(statement, "Oil Filter", "Mann", "Onix 2018-2024, Prisma 2017-2020", supplierBId, 39.90, 8, addressBId);
            insertPart(statement, "Spark Plug", "NGK", "Gol 2014-2022, Fox 2015-2021", supplierAId, 29.90, 120, addressCId);
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
            int warehouseAddressId
    ) throws SQLException {
        statement.setString(1, name);
        statement.setString(2, manufacturer);
        statement.setString(3, compatibleVehicles);
        statement.setInt(4, supplierId);
        statement.setDouble(5, unitPrice);
        statement.setInt(6, quantity);
        statement.setInt(7, warehouseAddressId);
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

    private record ParsedWarehouseAddress(
            char street,
            int building,
            int level,
            int apto,
            int maxCapacity,
            int lowCapacityWarningTriggerLevel
    ) {
        private ParsedWarehouseAddress {
            street = Character.toUpperCase(street);
        }
    }
}

# JnventoryFX

**JnventoryFX** is a JavaFX prototype for a car parts retail inventory system.
It was built as a desktop inventory application for managing car parts, suppliers, sales, users, warehouse locations, audit logs, and role-based access.

This is still a **prototype**, not a finished production business system. The goal is to show a realistic layered JavaFX application structure for a portfolio project.

## Features

### Authentication and Security

- User sign-in and sign-up screen
- Password hashing with PBKDF2-SHA256 and per-user salts
- Admin-created accounts with generated temporary passwords
- First-login password change requirement for temporary passwords
- Email-based password recovery using short-lived reset codes
- Development email outbox fallback when SMTP is not configured
- Centralized password validation rules
- Role-based permission checks in the service layer
- Audit logs for authentication, password recovery, user management, inventory actions, sales, supplier changes, warehouse updates, and permission failures
- SQLCipher-compatible SQLite page-level database encryption
- No decrypted runtime SQLite database copy
- Predefined demo users for testing

### Car Parts Inventory

- Register new car parts
- View existing parts in a table
- Search parts by name, ID, supplier, compatible vehicle, or warehouse location
- Update stock after sales
- Track part price, quantity, capacity, and stock warning state

### Sales

- Register a part sale
- Automatically reduce stock quantity after a successful sale
- Prevent sale quantities greater than available stock
- Audit successful sales

### Suppliers

- Register supplier information
- Validate supplier name and optional supplier email
- Store supplier name, phone, email, address, and notes
- Link parts to suppliers

### User Roles

| Role | Purpose |
| --- | --- |
| `ADMIN` | Full system access, user management, password reset email triggers, and audit logs |
| `MANAGER` | Inventory and supplier management |
| `CASHIER` | Sales-focused access |
| `WAREHOUSE` | Warehouse and stock-location tools |

### Warehouse Tools

- Find a product in the warehouse
- Change a product's warehouse address
- Set maximum storage capacity
- Set low-stock warning trigger level
- Identify parts with low stock

### Validation Layer

Validation is centralized in the `validation` package instead of being scattered through controllers.

The current validation layer covers:

- Required text fields
- Maximum text lengths
- Required and optional email format checks
- Non-negative money values
- Positive and non-negative integer rules
- Range checks for stock warning values
- Password policy checks
- Warehouse address format checks

## Tech Stack

- Java 21
- JavaFX 21
- FXML
- Maven
- SQLite
- SQLCipher-compatible SQLite JDBC driver
- JDBC
- MVC-inspired project structure

## Project Structure

```text
src/main/java/br/com/vidasilva/jnventoryfx/
├── App.java
├── controller/
│   ├── Controller.java
│   └── DashboardController.java
├── database/
│   └── Database.java
├── model/
│   ├── AuditLog.java
│   ├── CarPart.java
│   ├── Supplier.java
│   ├── User.java
│   ├── UserRole.java
│   └── WarehouseAddress.java
├── repository/
│   ├── AuditLogRepository.java
│   ├── CarPartRepository.java
│   ├── SupplierRepository.java
│   ├── UserRepository.java
│   └── WarehouseAddressRepository.java
├── security/
│   ├── AuthorizationService.java
│   ├── DatabaseEncryptionService.java
│   ├── PasswordHasher.java
│   └── Permission.java
├── service/
│   ├── AuditService.java
│   ├── EmailService.java
│   ├── InventoryService.java
│   ├── Session.java
│   ├── SupplierService.java
│   └── UserService.java
└── validation/
    ├── ValidationException.java
    └── Validator.java
```

```text
src/main/resources/br/com/vidasilva/jnventoryfx/view/
├── welcome-auth.fxml
└── inventory-dashboard.fxml
```

## Requirements

Before running the project, make sure you have:

- JDK 21 installed
- Maven installed

On Arch Linux:

```bash
sudo pacman -S jdk21-openjdk maven
sudo archlinux-java set java-21-openjdk
```

Check your versions:

```bash
java -version
javac -version
mvn -version
```

## Running the Project

Clone the repository:

```bash
git clone https://github.com/vidasilva/jnventoryfx.git
cd jnventoryfx
```

Set a database encryption key outside the source code:

```bash
export JNVENTORYFX_DB_KEY='use-a-long-random-secret-here'
```

Run the application:

```bash
mvn clean javafx:run
```

## Demo Login Credentials

The application seeds demo users into the encrypted SQLite database. Passwords are stored as PBKDF2-SHA256 hashes, but the demo login passwords stay the same for testing.

These demo passwords exist only so the prototype is easy to test. In a real deployment, default passwords should not live in source code.

| Role | Email | Password |
| --- | --- | --- |
| Admin | `admin@inventory.local` | `admin123` |
| Manager | `manager@inventory.local` | `manager123` |
| Cashier | `cashier@inventory.local` | `cashier123` |
| Warehouse | `warehouse@inventory.local` | `warehouse123` |

## Database Encryption

The app uses SQLite for local persistence, but the database file is opened through a SQLCipher-compatible JDBC driver using page-level encryption.

The persistent file is:

```text
jnventoryfx.db
```

Unlike the previous AES-wrapped prototype approach, the app does **not** decrypt the database into `.jnventoryfx-runtime/` while running. The live database file is the encrypted SQLCipher-compatible SQLite file.

When upgrading from an older prototype build, the app detects a plain SQLite `jnventoryfx.db`, migrates its rows into a SQLCipher-compatible encrypted database, and deletes the plaintext migration source after a successful conversion. That keeps existing demo data without keeping a decrypted runtime database file lying around like an embarrassing receipt.

For real use, set the database key outside the source code:

```bash
export JNVENTORYFX_DB_KEY='use-a-long-random-secret-here'
```

If `JNVENTORYFX_DB_KEY` is not set, the app creates a local development key file:

```text
.jnventoryfx-dev-key
```

That fallback is for demos only. A real deployment should load the database key from a proper secret-management system or protected environment configuration.

To reset the prototype data, stop the app and delete:

```text
jnventoryfx.db
.jnventoryfx-dev-key
```

Then run the app again.

## Email-Based Password Recovery

Password recovery is now code-based and email-driven:

1. The user clicks **Forgot password?** on the sign-in screen.
2. The user enters their account email.
3. The app generates a short-lived reset code.
4. The reset code is stored only as a hash.
5. The app sends the reset code by SMTP email.
6. The user enters the code and chooses a new password.
7. The reset code is cleared after successful use.

Admins can also trigger a reset email for a selected user from the **Users / Roles** tab, but the admin does not receive or see the reset code.

### SMTP Configuration

Configure SMTP with environment variables:

```bash
export JNVENTORYFX_SMTP_HOST='smtp.example.com'
export JNVENTORYFX_SMTP_PORT='587'
export JNVENTORYFX_SMTP_USERNAME='smtp-user@example.com'
export JNVENTORYFX_SMTP_PASSWORD='smtp-password-or-app-password'
export JNVENTORYFX_SMTP_FROM='no-reply@example.com'
export JNVENTORYFX_SMTP_STARTTLS='true'
```

For SMTP over SSL, use:

```bash
export JNVENTORYFX_SMTP_SSL='true'
export JNVENTORYFX_SMTP_PORT='465'
```

If SMTP is not configured, the app writes the reset email to:

```text
dev-mail-outbox/
```

That fallback exists so the portfolio prototype can be tested locally without a real mail server.

## Audit Logs

`ADMIN` users can open the **Audit Logs** tab to view recent events.

The app records events such as:

- Successful and failed sign-in attempts
- Public sign-up account creation
- Admin-created users
- Password recovery requests
- Password recovery completion
- First-login password changes
- Permission denials
- Car part registration
- Sales
- Supplier registration
- Warehouse updates

## Prototype Limitations

The previous limitations around plain-text passwords, no real permission enforcement, no audit logs, no advanced validation, no password recovery, and no encrypted database have been addressed for prototype purposes.

Remaining non-production caveats:

- No installer or packaged release yet.
- UI styling is still intentionally simple.
- No automated unit/integration test suite yet.

## Security Notes

- Passwords are hashed with PBKDF2-SHA256 before being stored.
- Each password hash uses a per-user random salt.
- Older plain-text demo passwords are automatically migrated to hashes on startup.
- Role permissions are checked before protected service-layer actions, not only by disabling dashboard controls.
- Users created from the public sign-up form are created as `CASHIER` users.
- Only `ADMIN` users can create accounts with selected roles from the dashboard.
- Admin-created accounts receive a generated temporary password that is shown once and never stored as plain text.
- Admin-created accounts are marked with `must_change_password = true` until the user replaces the temporary password.
- Password reset codes are stored as hashes and expire after 15 minutes.
- Password reset emails can be sent by SMTP, with a local development outbox fallback for demos.
- Permission failures and password recovery events are written to the audit log.
- The persistent database is encrypted at the SQLite page level through the SQLCipher-compatible JDBC driver.
- The app no longer creates a decrypted runtime SQLite database file.

## Future Improvements

Planned improvements include:

- Better dashboard styling
- Product image support
- Barcode support
- Sale history screen
- Supplier history and purchase orders
- Stock movement reports
- Warehouse capacity reports
- Export to CSV or PDF
- Unit and integration tests
- Application packaging for Linux

## Screens / Main Flow

```text
Welcome/Auth Screen
        ↓
Login / Sign Up / Email Password Recovery
        ↓
Inventory Dashboard
        ↓
Parts / Sales / Suppliers / Users / Warehouse Tools / Audit Logs
```

## Purpose

This project was created as a learning prototype for a desktop inventory system using JavaFX, Maven, SQLite, SQLCipher-compatible encryption, and a layered Java structure.

It is meant to show practical use of:

- Java OOP
- JavaFX controllers
- FXML views
- Service classes
- Repository classes
- SQLite persistence
- Password hashing
- Email-based password recovery
- Role-based authorization
- Centralized validation
- Audit logging
- SQLCipher-compatible encrypted persistence
- Basic desktop application architecture

## Author

**Vitor Davi Gomes da Silva**

- GitHub: [@vidasilva](https://github.com/vidasilva)
- Project repository: [JnventoryFX](https://github.com/vidasilva/jnventoryfx)

## License

This project is currently provided as a prototype for learning and demonstration purposes.
A formal license can be added later.

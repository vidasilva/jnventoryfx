# JnventoryFX

**JnventoryFX** is a JavaFX prototype for a car parts retail inventory system.  
It was built as a desktop inventory application for managing car parts, suppliers, sales, users, and warehouse locations.

This project is currently a **prototype**, not a production-ready business system. It focuses on demonstrating the core structure, interface flow, and basic data operations of an inventory management application.

## Features

### Authentication

- User sign-in and sign-up screen
- Basic role-based access structure
- Predefined demo users for testing

### Car Parts Inventory

- Register new car parts
- View existing parts in a table
- Search parts by name, SKU, category, supplier, or warehouse location
- Update stock after sales
- Track part price and quantity

### Sales

- Register a part sale
- Automatically reduce stock quantity after a successful sale
- Prevent sale quantities greater than available stock

### Suppliers

- Register supplier information
- Store supplier name, contact person, phone, email, and address
- Link parts to suppliers

### User Roles

The prototype includes the following role model:

| Role | Purpose |
| --- | --- |
| `ADMIN` | Full system access |
| `MANAGER` | Inventory and supplier management |
| `CASHIER` | Sales-focused access |
| `WAREHOUSE` | Warehouse and stock-location tools |

### Warehouse Tools

- Find a product in the warehouse
- Change a product's warehouse address
- Set maximum storage capacity
- Set low-stock warning trigger level
- Identify parts with low stock

## Tech Stack

- Java 21
- JavaFX 21
- FXML
- Maven
- SQLite
- JDBC
- MVC-inspired project structure

## Project Structure

```text
src/main/java/br/com/vidasilva/jnventoryfx/
├── App.java
├── controller/
│   ├── Controller.java
│   └── InventoryDashboardController.java
├── database/
│   └── Database.java
├── model/
│   ├── CarPart.java
│   ├── Supplier.java
│   ├── User.java
│   └── UserRole.java
├── repository/
│   ├── CarPartRepository.java
│   ├── SupplierRepository.java
│   └── UserRepository.java
└── service/
    ├── InventoryService.java
    ├── Session.java
    ├── SupplierService.java
    └── UserService.java
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

On Arch Linux, for example:

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
cd jnventoryfx/app
```

Run the application:

```bash
mvn clean javafx:run
```

## Demo Login Credentials

The application seeds demo users into the SQLite database.

| Role | Email | Password |
| --- | --- | --- |
| Admin | `admin@inventory.local` | `admin123` |
| Manager | `manager@inventory.local` | `manager123` |
| Cashier | `cashier@inventory.local` | `cashier123` |
| Warehouse | `warehouse@inventory.local` | `warehouse123` |

## Database

The app uses SQLite for local persistence.

When the application runs, it creates a local database file:

```text
jnventoryfx.db
```

The database is automatically initialized with:

- Users
- Suppliers
- Sample car parts
- Sales table structure

To reset the prototype data, stop the app and delete:

```text
jnventoryfx.db
```

Then run the app again.

## Prototype Limitations

This is not a finished production application. Current limitations include:

- Passwords are stored in plain text
- No advanced validation layer
- No password recovery
- No encrypted database
- No detailed audit logs
- No real permission enforcement beyond the basic role model
- UI styling is still minimal
- No installer or packaged release yet

These limitations are intentional for the prototype stage. The current goal is to demonstrate structure, workflow, and basic inventory behavior.

## Future Improvements

Planned improvements include:

- Password hashing
- Stronger role-based permissions
- Better dashboard styling
- Product image support
- Barcode support
- Sale history screen
- Supplier history and purchase orders
- Stock movement logs
- Warehouse capacity reports
- Export to CSV or PDF
- Unit and integration tests
- Application packaging for Linux

## Screens / Main Flow

The current application flow is:

```text
Welcome/Auth Screen
        ↓
Login / Sign Up
        ↓
Inventory Dashboard
        ↓
Parts / Sales / Suppliers / Users / Warehouse Tools
```

## Purpose

This project was created as a learning prototype for a desktop inventory system using JavaFX, Maven, SQLite, and a layered Java structure.

It is meant to show practical use of:

- Java OOP
- JavaFX controllers
- FXML views
- Service classes
- Repository classes
- SQLite persistence
- Basic desktop application architecture

## Author

**Vitor Davi Gomes da Silva**

- GitHub: [@vidasilva](https://github.com/vidasilva)
- Project repository: [JnventoryFX](https://github.com/vidasilva/jnventoryfx)

## License

This project is currently provided as a prototype for learning and demonstration purposes.  
A formal license can be added later.

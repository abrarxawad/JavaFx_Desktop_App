# LifeLink — Blood Bank & Donor Navigation System

> Java 25 + JavaFX 25 + SQLite

LifeLink is a healthcare-focused desktop application for managing blood donation operations, donor matching, inventory tracking, and emergency blood request workflows.

This project uses SQLite as its database backend, which means the app creates and manages the `lifelink.db` database automatically when it starts. There is no MySQL installation required for normal use.

---

## Features

- Secure login with role-based access
- Donor registration and profile management
- Recipient and hospital request workflows
- Blood bank stock tracking
- Matching and recommendation flows
- Admin and dashboard views for each user role
- Professional JavaFX UI with a healthcare-oriented dark theme

---

## Quick Start

### 1. Install the runtime

Make sure Java 25 is installed and available on your system.

### 2. Build the project

From the project root:

```bash
mvn clean compile
```

To launch the application:

```bash
mvn javafx:run
```

### 3. Database setup

The project uses SQLite, so the app will create the database automatically on first launch.

- Database file: `lifelink.db`
- Configuration file: `src/main/resources/config/database.properties`

You do not need MySQL or any external database server.

---

## Default Admin Access

Use the default administrator account to access the full admin workflow:

- Username: `admin`
- Password: `Admin@1234`

After logging in, the app routes the user to the appropriate dashboard based on the account role.

---

## How to Access All Features

### Admin access

1. Start the app.
2. Log in with the default admin account above.
3. The administrator dashboard gives access to user oversight and operational controls.

### Donor access

1. Open the app and select the registration option.
2. Create a donor profile.
3. Log in with the new donor account.
4. Use the donor dashboard to view availability, update information, and manage donation status.

### Recipient / Hospital / Blood Bank access

1. Register a matching account for that role.
2. Sign in to the corresponding dashboard.
3. Use the workflow screens for requests, matching, and blood inventory operations.

The role-based navigation is handled automatically by `LoginController`, which directs each user to the correct FXML dashboard.

---

## Project Architecture

```text
JavaFX UI (FXML + CSS)
       ↓
Controller layer
       ↓
DAO layer
       ↓
SQLite database (lifelink.db)
```

### Key technologies

- Java 25
- JavaFX 25
- SQLite JDBC
- Maven
- BCrypt password hashing
- SLF4J + Logback
- JUnit 5

---

## Notes

- The app is designed to initialize the SQLite database automatically at startup.
- The previous MySQL instructions were outdated and have been corrected to reflect the actual database configuration.
- If the database file is missing, restart the app and the initializer will recreate it.

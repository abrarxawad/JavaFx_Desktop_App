# LifeLink — Blood Bank & Donor Navigation System

> **Advanced Programming Laboratory Project**
> Developed with Java 26, JavaFX 21, and MySQL 8.x

LifeLink is a centralized blood donation and emergency blood-management platform connecting donors, recipients, hospitals, and blood banks. 

This repository contains the completed project skeleton (Phases 1-3) along with the first UI screens (Login), ready to be loaded into IntelliJ IDEA.

---

## 🚀 Quick Start Guide

### 1. Database Setup
You must initialize the database before running the application, as the login screen authenticates against MySQL.

1. Open MySQL Workbench (or your preferred MySQL client, e.g., phpMyAdmin, command line).
2. Connect using the username `root` (no password, as configured).
3. Open the file `database/schema.sql` located in this project folder.
4. Execute the entire script. It will create the `lifelink` database, all 12 tables, and insert a default admin user.

### 2. Opening in IntelliJ IDEA
This project is built using Maven. Since IntelliJ has built-in Maven support, you don't need Maven installed on your system.

1. Open IntelliJ IDEA.
2. Select **File > Open...** and select the `JavaFx Assinment` folder (the folder containing the `pom.xml`).
3. Click **"Trust Project"** if prompted.
4. IntelliJ will automatically detect the Maven `pom.xml` and start downloading dependencies (JavaFX, MySQL Connector, BCrypt, SLF4J, JUnit). 
   - *Wait for the progress bar at the bottom right to finish.*
5. Ensure your Project SDK is set to Java 21 or higher (**File > Project Structure > Project > SDK**).

### 3. Running the Application
1. In the Project tool window on the left, navigate to:
   `src/main/java/com/lifelink/Main.java`
2. Right-click on `Main` and select **Run 'Main.main()'**.
3. The JavaFX Login window should appear.

---

## 🔐 Default Admin Account
A default admin account is created by the `schema.sql` script:

* **Username:** `admin`
* **Password:** `Admin@1234`

You can use these credentials to log in on the start screen. (The application will route you to the placeholder Admin Dashboard).

---

## 🏗 Project Architecture

This project follows a strict MVC and layered architecture to enforce clean separation of concerns and prevent SQL injection.

```text
UI (JavaFX / FXML)
       ↓
Controller (e.g. LoginController)
       ↓
Service Layer (Validation & Business Logic)
       ↓
DAO (e.g. UserDAO) — All SQL goes here
       ↓
DatabaseManager (Singleton Connection Pool)
       ↓
MySQL Database
```

### Key Technologies Utilized
* **Java Concurrency:** Heavy operations (hashing, JDBC queries) are offloaded to background threads using `javafx.concurrent.Task` to keep the UI responsive.
* **Design Patterns:** Singleton (DatabaseManager, SessionManager), DAO, MVC, Builder (BloodRequest), Factory.
* **Security:** BCrypt for password hashing. `PreparedStatement` for all database interactions.
* **UI/UX:** Modern dark healthcare theme with glassmorphism effects (CSS), entirely free of inline styles.

---

## 📝 Next Steps
We have completed Phases 1 through 3, plus the Phase 4 and 5 foundations:
- [x] Phase 1: SRS & Architecture Plan
- [x] Phase 2: Database Schema & ER Design 
- [x] Phase 3: Maven Project Skeleton
- [x] Phase 4: JavaFX App Shell + Global CSS
- [x] Phase 5: Authentication (Login Screen + Controller)

When you are ready, I can continue with **Phase 6: User & Role Management** (building the registration form and the admin user dashboard).

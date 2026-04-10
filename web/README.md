# Railway Reservation Web Version

This web version includes:
- Frontend: HTML, CSS, JavaScript
- Backend: Java (HttpServer + JDBC)
- Database: MySQL

## 1) Create database and tables

Run in MySQL:

```sql
source web/database/schema.sql;
```

Or manually execute the SQL from `web/database/schema.sql`.

## 2) Compile backend

From project root:

```powershell
javac -cp .;mysql-connector-j-9.6.0.jar web\\backend\\ApiServer.java
```

## 3) Run backend server

```powershell
java -cp .;mysql-connector-j-9.6.0.jar;web\\backend ApiServer
```

Server URL:
- `http://localhost:8080`

## 4) Optional DB config with environment variables

If your DB settings are different, set:
- `RAILWAY_DB_URL` (default: `jdbc:mysql://localhost:3306/railway`)
- `RAILWAY_DB_USER` (default: `root`)
- `RAILWAY_DB_PASSWORD` (default: `12345`)

Example (PowerShell):

```powershell
$env:RAILWAY_DB_URL="jdbc:mysql://localhost:3306/railway"
$env:RAILWAY_DB_USER="root"
$env:RAILWAY_DB_PASSWORD="your_password"
```

## Features implemented

- Register user
- Login user
- Book ticket (source, destination, date, seats)
- View all bookings for logged-in user
- Logout

Notes:
- Passwords are stored as SHA-256 hashes (not plain text).
- Frontend and API are served from the same Java server.

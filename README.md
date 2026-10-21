# Wallet Service

A digital wallet and payments REST backend built with Spring Boot and Oracle. Users can register, log in, deposit money, transfer to other users and view their transaction history. Admins can freeze and unfreeze accounts.

## Tech Stack
Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA (Hibernate), Oracle Database, Lombok, Maven

## Features
- User registration with BCrypt password hashing and an automatic wallet per user
- JWT authentication with role-based access (USER and ADMIN)
- Deposit money and check balance
- Atomic fund transfers between users using `@Transactional`
- Optimistic locking (`@Version`) on wallets to prevent double-spending under concurrent requests
- Paginated transaction history (sent and received)
- Admin APIs to freeze and unfreeze accounts; frozen users cannot log in or transact
- Request validation and global exception handling with clear error responses
- `BigDecimal` for all money values

## Design Decisions
- **Atomic transfers:** debit, credit and the ledger row are saved in one transaction, so they all succeed or all roll back.
- **Optimistic locking:** if two requests change the same wallet at the same time, the second fails with HTTP 409 instead of overwriting the balance.
- **JWT, stateless:** the logged-in user comes from the token, never from the URL.
- **Ledger:** every deposit and transfer is stored as a transaction row.

## API
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | /api/auth/register | Public | Register a user |
| POST | /api/auth/login | Public | Log in, returns a JWT |
| POST | /api/wallet/deposit | User | Deposit money |
| GET | /api/wallet/balance | User | Get balance |
| POST | /api/wallet/transfer | User | Transfer to another user by email |
| GET | /api/wallet/transactions | User | Paginated history (`?page=0&size=10`) |
| PATCH | /api/admin/users/{id}/freeze | Admin | Freeze a user |
| PATCH | /api/admin/users/{id}/unfreeze | Admin | Unfreeze a user |

Send the token as `Authorization: Bearer <token>`.

## Setup
1. Create an Oracle user in your pluggable database (XEPDB1):
   `CREATE USER wallet_user IDENTIFIED BY <your_password>;`
   `GRANT CONNECT, RESOURCE TO wallet_user;`
   `ALTER USER wallet_user QUOTA UNLIMITED ON USERS;`
2. Set the database URL, username and password in `src/main/resources/application.properties`.
3. Set the `JWT_SECRET` environment variable (at least 32 characters), or use the default for local testing only.
4. Run `WalletServiceApplication`. The app starts on port 8080 and creates the tables automatically.
5. To create an admin, update a user's role in the `app_users` table to `ADMIN`.

## Features
- Register / login with JWT (stateless Spring Security, BCrypt), roles USER / ADMIN
- Deposit, balance, transfer between users, paginated transaction history
- Transfers are atomic (`@Transactional`) and protected from concurrent updates with optimistic locking (`@Version`)
- **Idempotent transfers**: send an `Idempotency-Key` header; a retried request returns the original result and never moves money twice
- Admin can freeze / unfreeze accounts
- Global exception handling with proper HTTP status codes (400, 401, 403, 404, 409)
- Unit tests with JUnit 5 and Mockito (`mvn test`)
- Swagger UI: http://localhost:8080/swagger-ui.html

## Run
1. Create Oracle user (see `oracle-setup.sql`)
2. `mvn spring-boot:run` (or run from IntelliJ)
3. Open Swagger UI, call `/api/auth/register` and `/api/auth/login`, click **Authorize** and paste the token

## Run with Docker
```
docker build -t wallet-service .
docker run -p 8080:8080 -e DB_URL=jdbc:oracle:thin:@host.docker.internal:1521/XEPDB1 -e JWT_SECRET=change-me-change-me-change-me-change-me wallet-service
```

## Tech
Java 21, Spring Boot 3, Spring Security, Spring Data JPA, Hibernate, Oracle, JWT, JUnit 5, Mockito, Swagger/OpenAPI, Docker, Maven

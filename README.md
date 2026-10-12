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

## Planned
Idempotency keys for transfers, unit test
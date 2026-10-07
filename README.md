# Wallet Service

A digital wallet and payments REST backend built with Spring Boot and Oracle.

## Tech Stack
Java 21, Spring Boot 3, Spring Data JPA (Hibernate), Spring Security, Oracle DB, Lombok, Maven

## Features (so far)
- User registration with BCrypt password hashing
- Automatic wallet creation per user
- Deposit money and check balance
- Transaction ledger for every deposit
- Request validation and global exception handling

## Coming next
- JWT authentication with USER/ADMIN roles
- Atomic fund transfers with `@Transactional` and optimistic locking
- Idempotency keys, transaction history, unit tests, Docker

## Setup
1. Create the Oracle user (see `oracle-setup.sql` or run):
   `CREATE USER wallet_user IDENTIFIED BY <password>;` and grant CONNECT, RESOURCE.
2. Set the DB URL, username and password in `src/main/resources/application.properties`.
3. Run `WalletServiceApplication`. The app starts on port 8080.

## API
| Method | Endpoint | Description |
|---|---|---|
| POST | /api/auth/register | Register a user |
| POST | /api/wallet/{userId}/deposit | Deposit money |
| GET | /api/wallet/{userId}/balance | Get balance |
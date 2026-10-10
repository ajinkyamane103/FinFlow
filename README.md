# FinFlow

FinFlow is a personal-finance API for authenticated user profiles, self-owned bank accounts, and a simple transaction ledger. It is a learning/prototype application, not regulated banking infrastructure.

## Technology

- Java 21 and Spring Boot 4.1.1
- Spring Web MVC, Security, Data JPA, and Jakarta Validation
- PostgreSQL 18 for development/runtime; H2 only for automated tests
- JJWT 0.12.6 (all three JJWT modules use the same version)
- BCrypt password hashing and stateless signed JWT bearer authentication
- Maven Wrapper

## Prerequisites

- JDK 21 (configure both IntelliJ Project SDK and Maven runner JDK to 21)
- PostgreSQL 18 running locally
- A PostgreSQL database and user with access to it

Create a local database (choose a strong password rather than the example placeholder):

```sql
CREATE USER finflow_app WITH PASSWORD 'choose-a-local-password';
CREATE DATABASE finflow OWNER finflow_app;
```

The default database URL is `jdbc:postgresql://localhost:5432/finflow`. Hibernate uses `ddl-auto=update` for local development and will create/update the application tables. Never point this setting at a production database without an explicit migration strategy.

## Environment variables on Windows

No JWT or database password is committed to the project. Configure these environment variables before starting the application:

```powershell
$env:JWT_SECRET = "replace-with-a-random-secret-of-at-least-32-bytes"
$env:DB_USERNAME = "finflow_app"
$env:DB_PASSWORD = "choose-a-local-password"
# Optional when the default local PostgreSQL URL is not correct:
$env:DB_URL = "jdbc:postgresql://localhost:5432/finflow"
```

Generate a random local secret with a password manager or a cryptographically secure tool, for example:

```powershell
$bytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

`JWT_SECRET` must be at least 32 bytes. The application fails clearly at startup if it is absent, too short, or if the expiration setting is invalid. Use a separately managed value in production; do not put it in Git, `application.properties`, Postman collections, or logs.

For a persistent Windows user variable, use `setx JWT_SECRET "..."`, then restart IntelliJ/your terminal. In IntelliJ, the preferred per-project setup is **Run → Edit Configurations → Spring Boot → FinFlowApplication → Environment variables**, then add `JWT_SECRET`, `DB_USERNAME`, `DB_PASSWORD`, and optionally `DB_URL`.

## Run

The Maven project directory is the nested `finflow` folder, which contains `mvnw.cmd`. From `C:\Users\Ajinkya Mane\Downloads\finflow`, run:

```powershell
Set-Location .\finflow
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd spring-boot:run
```

In IntelliJ, open `finflow/pom.xml`, select JDK 21, reload Maven, configure the environment variables above, and run `FinFlowApplication`.

## Authentication and errors

The API is stateless: no form login, HTTP Basic, or server sessions are enabled. Send a token as:

```http
Authorization: Bearer <accessToken>
```

Public routes are `GET /api/health`, `POST /api/auth/register`, and `POST /api/auth/login`. All other API routes require a valid bearer token.

Error bodies have JSON fields including `timestamp`, `status`, `error`, `message`, `path`, and (for validation errors) `fieldErrors`.

- Invalid request body/validation: `400`
- Missing, malformed, expired, tampered, or invalid credentials: `401`
- Authenticated but not authorized: `403`
- Duplicate/conflicting state, including insufficient funds: `409`
- Missing resource or another user's account: `404`
- Unexpected failure: `500` without implementation details

CORS is intentionally not enabled because no browser client origin is currently specified. If a browser front end is added, configure only its known HTTPS origin(s), methods, and headers; do not use wildcard origins with credentials.

## API endpoints

| Method | Route | Description |
| --- | --- | --- |
| GET | `/api/health` | Public health response |
| POST | `/api/auth/register` | Register a user; email is normalized and password BCrypt-hashed |
| POST | `/api/auth/login` | Return `{ accessToken, tokenType: "Bearer", expiresIn }` |
| GET | `/api/users/me` | Authenticated user profile; no password hash |
| POST | `/api/accounts` | Create a self-owned account |
| GET | `/api/accounts` | List the caller's accounts |
| GET | `/api/accounts/{accountId}` | Get one caller-owned account |
| POST | `/api/accounts/{accountId}/deposits` | Add money and write a ledger entry |
| POST | `/api/accounts/{accountId}/withdrawals` | Withdraw with sufficient-funds and locking checks |
| POST | `/api/accounts/transfers` | Transfer between the caller's same-currency accounts atomically |
| GET | `/api/accounts/{accountId}/transactions?page=0&size=20&sort=createdAt,desc` | Paginated transaction history |
| GET | `/api/dashboard` | Accounts, per-currency balance totals, and recent transactions |

All money fields are `BigDecimal` values (send them as JSON numbers or strings with up to four fractional digits). There is no client endpoint to edit an account balance directly. The account's initial balance is recorded as an `OPENING_BALANCE` ledger entry.

For a retry-safe deposit, withdrawal, or transfer, supply a unique `Idempotency-Key` header (maximum 100 characters). FinFlow records a SHA-256 fingerprint of the operation (type, accounts, amount, and description): an exact retry returns the original ledger result, while reusing the key for a different request returns `409 Conflict`. A database unique constraint also prevents concurrent duplicate requests from applying twice.

## Postman smoke test

1. `POST http://localhost:8080/api/auth/register`

```json
{
  "fullName": "Ada Lovelace",
  "email": "ada@example.com",
  "password": "Password123"
}
```

2. `POST http://localhost:8080/api/auth/login`

```json
{
  "email": "ada@example.com",
  "password": "Password123"
}
```

Copy `accessToken` into a Postman collection variable named `token`.

3. `GET http://localhost:8080/api/test/protected` with `Authorization: Bearer {{token}}` returns `200`. Without that header it returns `401`; with `Bearer not-a-jwt` it also returns `401`.

4. `POST http://localhost:8080/api/accounts` with `Authorization: Bearer {{token}}`:

```json
{
  "name": "Primary",
  "accountType": "CHECKING",
  "currency": "USD",
  "initialBalance": "100.00"
}
```

5. `POST http://localhost:8080/api/accounts/{accountId}/deposits` with `Authorization: Bearer {{token}}` and `Idempotency-Key: deposit-001`:

```json
{
  "amount": "25.00",
  "description": "Example deposit"
}
```

6. `POST http://localhost:8080/api/accounts/transfers` with the same authorization and a new `Idempotency-Key`:

```json
{
  "sourceAccountId": 1,
  "destinationAccountId": 2,
  "amount": "10.00",
  "description": "Move to savings"
}
```

## Tests

Automated tests use an in-memory H2 database and never require PostgreSQL. They cover registration, duplicate registration, BCrypt storage, login/password rejection, JWT signature/expiry/malformed-token behavior, `400`/`401`/`403`/`409` responses, account ownership, ledger writes, idempotency, transfer atomic behavior, dashboard data, and insufficient funds.

```powershell
Set-Location "C:\Users\Ajinkya Mane\Downloads\finflow\finflow"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean compile
.\mvnw.cmd clean test
```

## Current limitations

- This is not a regulated bank, payment processor, or production financial system.
- No external bank connections, card processing, foreign-exchange conversion, statements, audit export, or email verification are implemented.
- Account creation accepts an initial balance for setup only; real systems need controlled funding workflows.
- H2 tests give fast coverage. Before production, add PostgreSQL-backed integration tests, schema migrations (for example, Flyway), operational monitoring, rate limits, token rotation/revocation policy, and independent security review.

# FinFlow Full-Stack Personal Finance Management
![FinFlow Dashboard](docs/finflowdashboard.png)
FinFlow is a full-stack personal finance management application built with React, Spring Boot, and PostgreSQL. It allows authenticated users to manage financial accounts, perform deposits and withdrawals, transfer money between their own accounts, and view transaction history through a responsive web interface.

**Project status:** Functional local prototype. Not a regulated banking or payment-processing system.

## Features

### Authentication and Security
- User registration and login.
- JWT bearer-token authentication.
- BCrypt password hashing.
- Protected frontend routes.
- Authenticated user profile.
- User-specific account access.
- Automatic logout when authentication becomes invalid.

### Account Management
- Create and list financial accounts.
- Support account types and currencies accepted by the backend.
- Initialize accounts with an opening balance.
- View account balances.
- Maintain a transaction ledger.

### Financial Transactions
- Deposit money into an account.
- Withdraw money subject to available-balance checks.
- Transfer money between eligible accounts owned by the same user.
- View transaction history with pagination.
- Use idempotency keys to prevent duplicate financial operations when supported by the API.
- Refresh account and dashboard data after successful operations.

### Dashboard
- View account summaries.
- Review balances grouped by currency.
- See recent transactions.
- Navigate to account and transaction management.

### User Interface
- Responsive React interface.
- Dashboard layout with sidebar navigation.
- Login and registration pages.
- Account and transaction forms.
- Loading, validation, error, and success states.

## Technology Stack

| Component | Technology |
|---|---|
| Frontend | React, JavaScript, Vite |
| Styling | Tailwind CSS |
| Backend | Java 21, Spring Boot 4.1.1 |
| REST API | Spring Web MVC |
| Security | Spring Security, JWT, BCrypt |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL 18 |
| Testing database | H2 |
| Build tool | Maven Wrapper |
| API testing | Postman |
| Version control | Git and GitHub |

## Architecture

FinFlow follows a layered full-stack architecture. The React frontend communicates with the Spring Boot REST API using HTTP and JSON. Spring Security validates JWT tokens, while the service and persistence layers interact with PostgreSQL.

![FinFlow Full-Stack Architecture](Doc/FinFlow%20Full-Stack%20Architecture%20Diagram.png)

**Request flow:**
1. The user interacts with the React frontend.
2. The frontend sends API requests to Spring Boot, including a JWT bearer token for protected endpoints.
3. Spring Security validates authentication and authorization.
4. Controllers and services process the request.
5. Spring Data JPA accesses PostgreSQL.
6. The API returns a JSON response to the frontend.

The React application communicates with the Spring Boot API over HTTP. The backend validates authentication and authorization, applies business rules, and persists financial records in PostgreSQL.

The browser never connects directly to PostgreSQL.

## Repository Structure

```text
FinFlow/
├── README.md
├── .gitignore
├── finflow/
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── .mvn/
│   └── src/
│       ├── main/
│       └── test/
└── finflow-frontend/
    ├── package.json
    ├── package-lock.json
    ├── .env.example
    ├── vite.config.js
    └── src/
```

## Prerequisites

Install the following software:

- JDK 21.
- Node.js 20 or newer.
- npm.
- PostgreSQL.
- Git.

## Local Setup

### 1. Clone the repository

```powershell
git clone https://github.com/ajinkyamane103/FinFlow.git
cd FinFlow
```

### 2. Configure PostgreSQL

Start PostgreSQL and create a database for local development.

```sql
CREATE USER finflow_app WITH PASSWORD 'choose-your-own-strong-password';
CREATE DATABASE finflow OWNER finflow_app;
```

If you already have a local database and user, you can use those instead. Configure the credentials through environment variables.

### 3. Configure the backend

Open a PowerShell terminal in the repository root:

```powershell
cd .\finflow

$env:DB_URL = "jdbc:postgresql://localhost:5432/finflow"
$env:DB_USERNAME = "finflow_app"
$env:DB_PASSWORD = Read-Host "Enter PostgreSQL password"
$env:JWT_SECRET = Read-Host "Enter your JWT secret"
```

Use a cryptographically random JWT secret that satisfies the backend's minimum length requirement. Keep it private and do not commit it to Git.

Configure the backend to read the environment variables:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/finflow}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

app.jwt.secret=${JWT_SECRET}
app.jwt.expiration-ms=3600000
```

Preserve the other required properties already present in `application.properties`. The example above illustrates the database and JWT settings; it is not a replacement for the complete configuration.

Start the backend:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend runs on `http://localhost:8080` by default.

Health check:

```http
GET http://localhost:8080/api/health
```

Environment variables configured in PowerShell apply to that terminal session. Configure them separately in IntelliJ if you launch the backend from the IDE.

### 4. Configure and run the frontend

Open a second PowerShell terminal from the repository root:

```powershell
cd .\finflow-frontend
Copy-Item .env.example .env
npm install
npm run dev
```

Open the local URL printed by Vite, normally:

`http://localhost:5173`

The checked-in frontend environment example uses `/api`, and the development server is configured to proxy API requests to the backend. Keep this configuration consistent with `vite.config.js`.

Never put a database password or JWT signing secret in a frontend environment variable. Vite variables prefixed with `VITE_` are exposed to browser code.

## API Endpoints

The following endpoints are documented by the backend. Protected endpoints require a valid JWT bearer token.

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/health` | Health check |
| POST | `/api/auth/register` | Register a user |
| POST | `/api/auth/login` | Authenticate and obtain a token |
| GET | `/api/users/me` | Retrieve the authenticated profile |
| GET | `/api/dashboard` | Retrieve dashboard data |
| POST | `/api/accounts` | Create an account |
| GET | `/api/accounts` | List the user's accounts |
| GET | `/api/accounts/{accountId}` | Retrieve an account |
| POST | `/api/accounts/{accountId}/deposits` | Deposit funds |
| POST | `/api/accounts/{accountId}/withdrawals` | Withdraw funds |
| POST | `/api/accounts/transfers` | Transfer between eligible accounts |
| GET | `/api/accounts/{accountId}/transactions?page=0&size=20&sort=createdAt,desc` | Retrieve paginated transaction history |

The exact request and response formats are defined by the backend controllers and DTOs. Consult the backend source code for field-level details.

## Authentication

After successful login, the API returns an access token. Send it in the authorization header for protected requests:

```http
Authorization: Bearer <accessToken>
```

The frontend handles token use and invalid-session responses. Never commit real access tokens or share them publicly.

## Testing

### Backend tests

From the repository root:

```powershell
cd .\finflow

.\mvnw.cmd clean compile
.\mvnw.cmd clean test
```

The automated backend tests use H2, an in-memory database. Passing those tests does not, by itself, verify every workflow against PostgreSQL.

### Frontend build

From the repository root:

```powershell
cd .\finflow-frontend

npm install
npm run build
```

### Manual integration testing

Verify the following in the running application:

1. Register a new user and log in.
2. Create an account and confirm its opening balance.
3. Deposit money and verify the updated balance.
4. Withdraw money and verify the updated balance.
5. Transfer funds between two eligible accounts.
6. Verify the transaction history.
7. Refresh the page and confirm data persists.
8. Log out and confirm protected pages require authentication.
9. Confirm one user cannot access another user's accounts.
10. Confirm insufficient funds and duplicate requests are handled correctly.

## Security and Configuration

- Never commit database passwords, JWT secrets, access tokens, or private keys.
- Use environment variables for sensitive configuration.
- Keep `.env` files out of Git.
- Review the entire Git history if a secret was previously committed.
- Use HTTPS and restricted CORS origins for deployment.
- Use database migrations instead of relying on automatic schema updates for production.
- Add PostgreSQL-backed integration tests and independent security review before any production financial use.

## Current Limitations

FinFlow is a learning and portfolio project, not regulated banking infrastructure.

It does not provide external bank integrations, payment-card processing, real-world money movement, foreign-exchange conversion, email verification, or a complete production banking security and compliance framework.

Dashboard metrics and charts are limited to the data exposed by the backend. No exchange rates or unsupported financial statistics are fabricated.

## Future Improvements

- Deploy the frontend and backend.
- Add automated PostgreSQL integration tests.
- Introduce database migrations with Flyway.
- Add CI with GitHub Actions.
- Add rate limiting and a documented token revocation strategy.
- Improve monitoring, logging, and operational error reporting.
- Add screenshots and a live demo link after deployment.

## Author

**Ajinkya Mane**

GitHub: https://github.com/ajinkyamane103

Project repository: https://github.com/ajinkyamane103/FinFlow

---

*Built as a full-stack software engineering portfolio project using React, Java, Spring Boot, and PostgreSQL.*

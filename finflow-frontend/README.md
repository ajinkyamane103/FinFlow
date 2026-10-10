# FinFlow frontend

The FinFlow frontend is a responsive React and Vite application for the existing Spring Boot API. It uses real API data only: there is no browser-side financial data store or simulated transfer flow.

## Included

- JWT sign-in, registration, protected routes, session-only token storage, and automatic sign-out after a `401` response.
- Responsive dark-sidebar application layout with accessible navigation and form focus states.
- Dashboard populated by `/api/dashboard`, account management, and real account ledgers.
- Deposit, withdrawal, and transfer confirmations. Each logical money operation receives one generated `Idempotency-Key`; the same key is retained if that submitted operation is retried from the dialog.
- Transaction pagination from the backend's Spring `Page` response.
- Profile details from the authenticated `/api/users/me` endpoint.

## Prerequisites

- Node.js 20 or newer (Node 24 was used for the build verification).
- The FinFlow backend running at `http://localhost:8080`.

## Run locally on Windows

From the repository parent directory:

```powershell
Set-Location .\finflow\
.\mvnw.cmd spring-boot:run
```

In a second PowerShell window:

```powershell
Set-Location 'C:\Users\Ajinkya Mane\Downloads\finflow\finflow-frontend'
Copy-Item .env.example .env
npm install
npm run dev
```

Open the URL printed by Vite (normally `http://localhost:5173`). The development server proxies `/api` to `http://localhost:8080`, so the browser makes same-origin requests and the backend's secure CORS policy does not need to be loosened.

## Environment

`VITE_API_BASE_URL` controls the API base URL. The checked-in `.env.example` uses `/api`, which is the recommended local-development value because Vite proxies it to the backend.

For a deployment where the frontend and backend are served through the same origin, keep `/api`. A direct value such as `http://localhost:8080/api` is supported, but it requires an explicitly configured restricted CORS policy on the backend; do not add wildcard origins or credentialed wildcard CORS.

Never place JWT secrets, passwords, or database credentials in a Vite environment file: every `VITE_` value is exposed to the browser.

## API contract used

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/auth/register` | Register with `fullName`, `email`, and `password` |
| POST | `/api/auth/login` | Get the `accessToken` bearer token |
| GET | `/api/users/me` | Get the signed-in profile |
| GET | `/api/dashboard` | Get account summaries and recent ledger entries |
| GET/POST | `/api/accounts` | List or create accounts |
| POST | `/api/accounts/{id}/deposits` | Deposit, with `Idempotency-Key` |
| POST | `/api/accounts/{id}/withdrawals` | Withdraw, with `Idempotency-Key` |
| POST | `/api/accounts/transfers` | Transfer, with `Idempotency-Key` |
| GET | `/api/accounts/{id}/transactions` | Paginated account ledger |

## Checks

```powershell
npm run build
```

The production build was verified successfully. A local proxy smoke test also returned `200` from `http://127.0.0.1:5173/api/health` while the backend was running.

## Current API limitations reflected in the UI

- The backend exposes balances by currency, not exchange rates, cash-flow totals, or historical chart data. The UI therefore does not fabricate a combined multi-currency total, income/expense statistic, or chart.
- The API does not provide profile editing, password changes, account deletion, or transaction search/filter endpoints, so the frontend intentionally does not offer those actions.

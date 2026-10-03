# Fraud Detection analyst console

The analyst console uses React, TypeScript, Vite and Bun. Start the API and local Keycloak with the repository's Quarkus dev setup, then run:

```powershell
bun install
bun run dev
```

Open `http://localhost:5175`. The development realm includes `fraud-analyst / fraud-analyst` and `admin / admin`; both are local-only identities. The console requires `FRAUD_ANALYST` or `ADMIN` and uses the API's role checks as the source of authorization.

For another API origin, set `VITE_API_URL` before starting Vite. In production, configure the Keycloak client redirect and allowed web origin to match the deployed console URL, set `FRONTEND_ORIGIN` on the API, and provide the production OIDC configuration. Do not use local development credentials in a deployed environment.

```powershell
bun run format
bun run format:check
bun run build
```

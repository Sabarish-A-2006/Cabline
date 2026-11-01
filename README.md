# Cabline — Cab booking

A full stack ride booking application with a Java 17 / Spring Boot API, React client, and PostgreSQL option. The code is split into HTTP controllers, services for business rules and integrations, JPA domain models, and repositories. Rider, driver, and admin screens use the same authenticated API.

## Run locally

Requirements: Java 17+, Maven 3.9+, Node.js 22+, and npm.

Start the API in one terminal:

```bash
mvn spring-boot:run
```

Start the web client in another:

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). The API uses an in-memory H2 database by default; its data resets when the API stops. `GET /api/health` reports API health.

To run the full stack with PostgreSQL and the production web server:

```bash
cp .env.example .env   # optional: set admin and provider credentials
docker compose up --build
```

Open [http://localhost:3000](http://localhost:3000). Docker Compose starts PostgreSQL, the API, and the web client. Use `docker compose down -v` to remove the local database volume.

## Run checks

```bash
mvn test
cd frontend
npm run build
```

The Java tests cover application startup, validation, fare and promo calculations, the rider-to-driver booking flow, demo checkout, and PDF receipt generation. The frontend production build checks the React application and Stripe checkout components.

## First ride

1. Create a rider account from the web page. Driver accounts can also register from the same screen.
2. Sign in as a driver in a second browser or private window, then turn availability on. Allow location access to help match nearby requests.
3. As a rider, enter pickup and destination. Add a distance if Google Maps is not configured; otherwise the server estimates a driving route. Review the fare and confirm the ride.
4. Accept the offer as a driver, start the trip, and mark it complete. The rider can pay in demo mode, rate the ride, and download a PDF receipt.
5. Submit a support request from **Help & support**. Admins can review the service dashboard and support queue.

Public registration accepts `RIDER` and `DRIVER` roles. Admin accounts are bootstrapped at API startup when both `APP_ADMIN_EMAIL` and `APP_ADMIN_PASSWORD` are set; the password must be at least 10 characters. Do not expose admin credentials in client-side configuration.

## Optional integrations

Integrations are switched on by environment variables. The app runs without provider credentials; route estimates then need a distance, and payments use a local demo confirmation.

| Variable | Purpose |
| --- | --- |
| `APP_JWT_SECRET` | JWT signing key; use a random secret of at least 32 UTF-8 bytes outside local development. |
| `FRONTEND_URL` | Client origin used for CORS, WebSocket connections, and Google sign-in redirects. |
| `APP_CORS_ALLOWED_ORIGIN_PATTERNS` | Comma-separated browser origins allowed by API CORS and ride WebSockets. The local defaults support localhost and `192.168.*`; set explicit origins for deployed environments. |
| `GOOGLE_MAPS_API_KEY` | Enables Google Routes API driving distance, duration, and route polyline. Enable the Routes API and restrict the key. |
| `STRIPE_SECRET_KEY` | Enables Stripe PaymentIntent checkout. |
| `STRIPE_PUBLISHABLE_KEY` | Stripe publishable key returned to the client for Stripe Elements. |
| `STRIPE_WEBHOOK_SECRET` | Verifies `payment_intent.succeeded` events at `/api/webhooks/stripe`. |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Enables Google OAuth rider sign-in at `/oauth2/authorization/google`. The web button also requires `VITE_GOOGLE_OAUTH_ENABLED=true` at client build time. Set the Google OAuth callback URL to `{FRONTEND_URL}/login/oauth2/code/google`. Existing non-rider accounts cannot sign in through Google. |
| `MAIL_ENABLED`, `MAIL_FROM` | Turns on payment receipt email after configuring `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, and SMTP TLS/auth settings. |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | PostgreSQL connection settings when running with `SPRING_PROFILES_ACTIVE=postgres`. |
| `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` | Creates the initial admin account if that email does not already exist. |

For local Stripe webhooks, forward test events to the API with the Stripe CLI:

```bash
stripe listen --forward-to localhost:8080/api/webhooks/stripe
```

Set the displayed signing secret as `STRIPE_WEBHOOK_SECRET`. A live Stripe payment is only marked paid after a verified webhook. Without Stripe keys, **Pay now** opens the clearly labeled demo confirmation instead.

For Google OAuth during local development, configure the callback `http://localhost:5173/login/oauth2/code/google`, set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` for the API, and set `VITE_GOOGLE_OAUTH_ENABLED=true` in `frontend/.env.local`. For Docker Compose, use callback `http://localhost:3000/login/oauth2/code/google` and set `VITE_GOOGLE_OAUTH_ENABLED=true` before building.

## Main API

All protected API requests use `Authorization: Bearer <accessToken>`.

| Endpoint | Access | Purpose |
| --- | --- | --- |
| `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` | Public / signed-in | Account creation, sign-in, and current account. |
| `POST /api/routes/estimate`, `POST /api/rides` | Rider | Calculate fare / route and request a ride. |
| `GET /api/rides/me`, `POST /api/rides/{id}/cancel`, `POST /api/rides/{id}/rating` | Rider or driver, as applicable | Ride history, cancellation, and rating. |
| `GET /api/drivers/me/offers`, `PATCH /api/drivers/me/availability` | Driver | Review offers and set online status and location. |
| `POST /api/driver-offers/{id}/accept`, `POST /api/driver-offers/{id}/decline` | Driver | Accept or decline ride offers. |
| `POST /api/rides/{id}/start`, `POST /api/rides/{id}/complete` | Assigned driver | Update a trip and create its pending payment. |
| `GET /api/payments/me`, `POST /api/payments/{id}/checkout`, `GET /api/payments/{id}/receipt` | Rider | Payment status, checkout setup, and paid PDF receipt. |
| `POST /api/support/tickets`, `GET /api/support/tickets/me` | Signed-in | Create and review support requests. |
| `/api/admin/**` | Admin | Service metrics, accounts, rides, promo codes, and support queue. |

Ride status changes are published to `/topic/rides/{rideId}` via the `/ws` STOMP endpoint. Clients connect with their bearer token and can subscribe only to rides they participate in.

## Configuration notes

The default fare is INR 50 plus INR 18 per kilometre; edit `app.fare` in `src/main/resources/application.yml` to change it. `GOOGLE_MAPS_API_KEY` is used only by the server. Stripe and SMTP secrets belong in the server environment, never in the React app. Use test-mode Stripe keys until the account and webhook are ready. When mail is enabled, the payment confirmation email includes the PDF receipt. The Docker Compose passwords and JWT secret are local-development defaults and must be replaced before deployment.

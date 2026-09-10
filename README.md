# High-Performance URL Shortener Engine (Spring Boot)

A production-grade, highly scalable, and secure URL Shortener backend built on the Spring Boot 3 framework.

---

## Key Features & Architecture

### 1. Core Functional Features (MVP)
- **Base62 URL Shortener Engine**: High-speed mathematical alphanumeric encoder (`[0-9, a-z, A-Z]`) transforming 64-bit integer IDs into compact short tokens.
- **Distributed Snowflake ID Generator**: Employs a 64-bit distributed Snowflake algorithm (timestamp + datacenter ID + worker ID + sequence) that guarantees collision-free short token generation across distributed server instances.
- **High-Speed Redirection Engine**:
  - `302 Found`: Default temporary redirect optimized for live click tracking and telemetry (sets `Cache-Control: no-cache, no-store, must-revalidate` to prevent client caching).
  - `301 Moved Permanently`: Permanent redirect (sets `Cache-Control: public, max-age=...`) for high-throughput static destinations.
- **Collision Resolution & DB Constraints**: Enforces unique database indexes on the `short_url` column (`urls` table) with O(1) indexed lookups.

### 2. User & Link Customisation
- **Branded Custom Aliases**: Allows users to customize their short URLs (e.g., `/my-promo-code`).
- **Reserved-Keywords Filter**: Shields system endpoints and routes (`api`, `auth`, `admin`, `login`, `register`, `v1`, `swagger`, `health`, `metrics`, `qr`, `gate`, etc.) from being claimed as aliases.
- **Time-To-Live (TTL) & Click Threshold Expiration**:
  - Expiration by timestamp (`expiryDate`).
  - Automatic deactivation after reaching maximum click thresholds (`maxClicks` or one-time use).
  - Scheduled background scanner (`CleanupService`) deactivating expired links hourly.
- **User Accounts & Management (JWT)**:
  - User registration and login issuing HMAC-SHA256 JWT tokens.
  - Stateless Bearer authentication via `JwtAuthenticationFilter`.
  - Link management endpoints: list owned links (`/my-links`), update destination/aliases, toggle active status, and revoke links.

### 3. Analytics & Telemetry (Asynchronous Event Processing)
- **Real-Time Click Counter**: Atomic click tracking on short URLs.
- **Thread-Safe Asynchronous Event Pipeline**: Captures immutable `ClickEvent` payloads synchronously in the web thread to eliminate servlet request recycling bugs, then delegating heavy processing to `@Async` background threads.
- **Traffic Demographics Breakdown**:
  - **Browser Agent Detection**: Microsoft Edge, Google Chrome, Mozilla Firefox, Apple Safari, Opera, Samsung Internet.
  - **Operating System Detection**: Windows, macOS, Linux, Android, iOS, iPadOS, ChromeOS.
  - **Device Classification**: Mobile, Tablet, Desktop, Bot / Web Crawler.
  - **GeoIP & Referrer Aggregation**: Resolves country data from network hints / GeoIP and aggregates referrer domains.
  - **Time Series Breakdown**: Daily and hourly click distribution charts.
- **Automatic QR Code Generation**:
  - Embedded Base64 QR code data returned upon URL creation.
  - Dedicated downloadable image streaming endpoint (`GET /api/v1/url/{shortUrl}/qr?download=true`).

### 4. System Security & Abuse Prevention
- **Malicious Link Scanner (Threat Intelligence)**:
  - Supports Google Safe Browsing API v4 (`threatMatches:find`) for malware, phishing, and unwanted software detection.
  - Built-in heuristic and domain blacklist scanner blocking deceptive raw IP hosts, suspicious TLDs, and known malicious patterns.
- **Layer 7 Rate Limiting**:
  - Bucket4j Token Bucket algorithm protecting against DDoS and automated link spamming:
    - URL Redirection: 120 requests / min per IP.
    - URL Creation: 30 requests / hour per IP.
    - Authentication: 10 requests / min per IP (mitigating credential stuffing and brute force).
  - Emits HTTP `429 Too Many Requests` with `Retry-After` headers.
- **Password-Protected Access & Interstitial Gate Page**:
  - Passwords hashed with BCrypt.
  - Interstitial HTML Gate Page rendered when accessing protected links in a web browser.
  - Programmatic access supported via `X-Password` header or `POST /api/v1/url/{shortUrl}/verify`.

---

## API Documentation

### Authentication (`/api/v1/auth`)
| Method | Endpoint | Description | Public / Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Register user | Public |
| `POST` | `/api/v1/auth/login` | Login and receive JWT Bearer token | Public |
| `GET` | `/api/v1/auth/me` | Current user profile | Bearer Token |

### URL Operations (`/api/v1/url`)
| Method | Endpoint | Description | Public / Auth |
| :--- | :--- | :--- | :--- |
| `GET` | `/r/{shortUrl}` | High-speed redirect (301 or 302) | Public |
| `GET` | `/api/v1/url/{shortUrl}` | Public redirect alternative | Public |
| `GET` | `/gate/{shortUrl}` | Interstitial gate HTML page | Public |
| `POST` | `/api/v1/url/{shortUrl}/verify` | Verify password and unlock URL | Public |
| `GET` | `/api/v1/url/{shortUrl}/qr` | Stream/Download QR Code PNG | Public |
| `POST` | `/api/v1/url/save` | Create short URL | Bearer Token |
| `POST` | `/api/v1/url/save/batch` | Batch create short URLs | Bearer Token |
| `GET` | `/api/v1/url/my-links` | List all URLs for current user | Bearer Token |
| `GET` | `/api/v1/url/get/{id}` | Get URL details | Bearer Token |
| `PUT` | `/api/v1/url/update/{id}` | Update URL configuration | Bearer Token |
| `PATCH` | `/api/v1/url/{id}/toggle-status` | Toggle active/deactive status | Bearer Token |
| `DELETE` | `/api/v1/url/delete/{id}` | Delete URL and associated telemetry | Bearer Token |
| `GET` | `/api/v1/url/{id}/analytics` | Raw click event logs | Bearer Token |
| `GET` | `/api/v1/url/{id}/analytics/summary`| Telemetry demographic summary | Bearer Token |

---

## Quick Start & Running Tests

### Prerequisites
- Java 21+ (`JAVA_HOME` pointing to JDK 21+)
- Maven 3.9+
- PostgreSQL (or in-memory H2 for testing/offline mode)

### Running Automated Tests
```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.7/libexec/openjdk.jdk/Contents/Home mvn test
```

### Running the Application
```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.7/libexec/openjdk.jdk/Contents/Home mvn spring-boot:run
```

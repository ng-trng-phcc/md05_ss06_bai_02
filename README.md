# Rikkei DevOps - Session 06 - Exercise 02

Banking Account Service REST API with PostgreSQL persistence, Spring Data JPA,
Bean Validation, HikariCP, Docker Compose, and evidence handoff.

## Relationship to Exercise 01

This project intentionally keeps the Exercise 01 Compose project identity:

```text
Compose project: rikkei-ss06-banking01
Database volume: rikkei-ss06-banking01_banking-db-data
Database:        banking_db
```

Keeping the same project identity means the volume `banking-db-data` resolves to
the existing named volume instead of creating a new empty database.

The previous `banking-app` service was replaced by `account-service`. The
`core-db` service, port mappings, database name, and volume remain the same.

Before the Account entity was created, the existing `banking_db` public schema
contained 0 tables. Hibernate `ddl-auto=update` then created the `accounts`
table. No existing tables were dropped or recreated.

Do not run `docker compose down -v`; the named volume must be preserved.

## Stack

| Component | Detail |
|---|---|
| Application | `account-service` |
| Java | 21 |
| Spring Boot | 4.1.0 |
| Build | Gradle 9.5.1 wrapper |
| Persistence | Spring Data JPA / Hibernate 7.4.1 |
| Database | PostgreSQL 17 Alpine (`core-db`) |
| Host ports | `5432:5432` and `8080:8080` |
| Compose project | `rikkei-ss06-banking01` |

## API

### Create account

`POST /api/accounts`

```json
{
  "accountNumber": "ACC100001",
  "ownerName": "Nguyen Van A",
  "initialBalance": 1000000
}
```

Returns `201 Created`, a `Location` header, and the persisted account.

### Get account

`GET /api/accounts/{id}`

Returns `200 OK` for an existing account and `404 Not Found` otherwise.

### List accounts

`GET /api/accounts`

Returns the database-backed account list.

### Error statuses

| Status | Meaning |
|---|---|
| `201` | Account created |
| `200` | Account retrieved or listed |
| `400` | Invalid JSON or validation failure |
| `404` | Account not found |
| `409` | Duplicate account number |
| `500` | Unexpected server error without stack trace exposure |

## Configuration

The datasource is injected through Compose:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://core-db:5432/banking_db
SPRING_DATASOURCE_USERNAME=${POSTGRES_USER}
SPRING_DATASOURCE_PASSWORD=${POSTGRES_PASSWORD}
```

Create the local `.env` from the placeholder file:

```bash
cp .env.example .env
# Replace the password placeholder
chmod 600 .env
```

`.env` is ignored by Git and is not included in the image build context.

## Build

```bash
./gradlew clean bootJar --no-daemon
docker build -t account-service:latest .
```

The Dockerfile is multi-stage. It builds `account-service.jar` inside the
builder image and copies it into the JRE runtime image as `app.jar`.

## Deploy

```bash
docker compose config -q
docker compose up -d --build --remove-orphans
docker compose ps
docker compose logs --no-color account-service
```

Expected services:

```text
core-db          healthy
account-service  running
```

The old `banking-app` service is absent.

## Verified results

- `core-db`: PostgreSQL 17.11, healthy, `0.0.0.0:5432->5432/tcp`
- `account-service`: running, `0.0.0.0:8080->8080/tcp`
- HikariCP: `BankingHikariPool - Start completed.`
- Hibernate: `Hibernate ORM core version 7.4.1.Final`
- JPA: `Initialized JPA EntityManagerFactory for persistence unit 'default'`
- Database: `banking_db`
- JDBC check: `SELECT 1 => 1; current_database() => banking_db`
- Schema check: `Schema verification succeeded: accounts table exists`
- No `Connection Refused`, `APPLICATION FAILED`, `ERROR`, or `FATAL` entries

### Automated API matrix

| Test | Result |
|---|---|
| Create account | `201 Created` |
| Get created account | `200 OK` |
| List accounts | `200 OK` |
| Negative initial balance | `400 Bad Request` |
| Missing owner name | `400 Bad Request` |
| Duplicate account number | `409 Conflict` |
| Unknown account ID | `404 Not Found` |

Automated verification account:

```text
ID:            2
Account number: ACC-E02-API-20261115172152
Owner:         Nguyen Van A
Balance:       1000000.00
Created at:    2026-11-15T17:21:52.938821Z
```

The PostgreSQL row matched the API response for ID, account number, owner name,
balance, and `created_at` at microsecond precision.

### Database schema

```text
accounts
- id             bigint identity, primary key
- account_number varchar(50), not null
- owner_name     varchar(255), not null
- balance        numeric(19,2), not null
- created_at     timestamp(6) with time zone, not null

Constraints:
- accounts_pkey: PRIMARY KEY (id)
- uk_accounts_account_number: UNIQUE (account_number)
```

## Postman evidence

The importable collection is:

```text
postman/md05_ss06_bai_02.postman_collection.json
```

Postman Desktop 11.94.0 was used to send the request. The captured screenshot
shows `201 Created` and the response containing account ID `3`.

Reproduction steps:

1. Import the collection into Postman Desktop.
2. Open `Create account`.
3. Confirm `Content-Type: application/json`.
4. Confirm the collection variable `accountNumber` is
   `ACC-E02-PM-20261115-01`; it is intentionally fresh and not present in the
   database yet.
5. Send the request once.
6. Verify `201 Created`, the `Location` header, and the JSON response with a
   generated `id` and `createdAt`.
7. Capture the full Postman window showing the request and response.

Captured screenshot:

```text
homework/session_06/exercise_02/postman_create_account.png
```

Captured result: `201 Created`, account ID `3`, account number
`ACC-E02-PM-20261115-01`.

Do not run the collection runner first, because that would create the account
before the screenshot request is sent.

## Database client evidence

DBeaver is not installed in this environment. The PostgreSQL result was
captured with IntelliJ IDEA Ultimate 2026.1.3 using its bundled Database tool.
This is IntelliJ IDEA evidence, not DBeaver branding.

Connection parameters used:

```text
Host:     localhost
Port:     5432
Database: banking_db
Username: banking_user
Password: read from the local .env file; do not expose it in screenshots
```

After creating the Postman account, run this read-only query:

```sql
SELECT id, account_number, owner_name, balance, created_at
FROM accounts
WHERE account_number = 'ACC-E02-PM-20261115-01';
```

Saved screenshot:

```text
homework/session_06/exercise_02/intellij_database_account_record.png
```

The screenshot shows the executed query and the result grid for account ID `3`.
It is a genuine database-client screenshot, not a terminal capture.

## Evidence status

- Postman screenshot: captured and saved at
  `homework/session_06/exercise_02/postman_create_account.png`.
- Database client screenshot: captured with IntelliJ IDEA Ultimate and saved at
  `homework/session_06/exercise_02/intellij_database_account_record.png`.
- DBeaver screenshot: not captured; DBeaver is not installed. IntelliJ IDEA was
  used as the available GUI database client.
- GitHub Release `ss06-bai-02`: created from the evidence commit.

## Runtime cleanup

Evidence capture is complete. The application stack was stopped without deleting
the named volume:

```bash
docker compose down
```

The named volume `rikkei-ss06-banking01_banking-db-data` is retained.

Host PostgreSQL remains inactive; start it with:

```bash
sudo systemctl start postgresql
```

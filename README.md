# Trie-Based Web Scraping Application

A Spring Boot REST API application that scrapes real-time data from user-defined URLs, indexes keywords using a custom Trie data structure, and provides fast prefix-based search and autocomplete functionality.

---

## Tech Stack

| Technology      | Version | Purpose               |
| --------------- | ------- | --------------------- |
| Java            | 21      | Programming language  |
| Spring Boot     | 3.3.x   | Application framework |
| Spring Data JPA | -       | Database ORM          |
| PostgreSQL      | -       | Relational database   |
| Jsoup           | 1.17.2  | Web scraping          |
| Lombok          | -       | Boilerplate reduction |
| JUnit 5         | -       | Unit testing          |
| Mockito         | -       | Mocking in tests      |
| Maven           | -       | Build tool            |

---

## Prerequisites

Ensure the following are installed on your machine:

- **Java 21**
- **Maven**
- **PostgreSQL** (running locally on port `5432`)

---

## Setup & Installation

### 1. Clone the repository

```
git clone https://github.com/Pranesh2288/trie-scraper
cd trie-scraper
```

### 2. Create the PostgreSQL database

Connect to PostgreSQL and run:

```sql
CREATE DATABASE trie_scraper;
```

To reset the database at any time:

```bash
psql -U postgres -d trie_scraper -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

### 3. Configure application properties

Open `src/main/resources/application.properties` and update the following:

```properties
server.port=8080

spring.datasource.url=jdbc:postgresql://localhost:5432/trie_scraper
spring.datasource.username=postgres
spring.datasource.password=YOUR_POSTGRES_PASSWORD
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.open-in-view=false

logging.level.org.hibernate.SQL=OFF
logging.level.org.hibernate.orm.jdbc.bind=OFF

spring.application.name=trie-scraper
```

---

## Configuration

| Property                        | Description                                                 |
| ------------------------------- | ----------------------------------------------------------- |
| `spring.datasource.url`         | PostgreSQL connection URL                                   |
| `spring.datasource.username`    | PostgreSQL username                                         |
| `spring.datasource.password`    | PostgreSQL password                                         |
| `spring.jpa.hibernate.ddl-auto` | Set to `update` — auto-creates/updates tables on startup    |
| `spring.jpa.open-in-view`       | Set to `false` — prevents open session in view anti-pattern |

> Tables are created automatically on first startup by Hibernate.

---

## Running the Application

```bash
mvn spring-boot:run
```

The application starts on **http://localhost:8080**

On startup you should see:

```
Trie initialized with X entries.
Tomcat started on port 8080
Scheduler running at: <current timestamp>
```

---

## API Documentation

### 1. Initiate Scraping

Scrapes the provided URLs for the given keywords and saves matched content to the database. If no schedule is provided, scraping runs immediately.

**Endpoint:**

```
POST /api/v1/scrape
```

**Request Body:**

| Field      | Type            | Required | Description                                                                   |
| ---------- | --------------- | -------- | ----------------------------------------------------------------------------- |
| `urls`     | `List<String>`  | ✅ Yes   | One or more URLs to scrape                                                    |
| `keywords` | `List<String>`  | ✅ Yes   | Keywords to search within page content                                        |
| `schedule` | `LocalDateTime` | ❌ No    | Future time to run (e.g. `2026-12-01T09:00:00`). If omitted, runs immediately |

**Example Request:**

```json
{
  "urls": [
    "https://en.wikipedia.org/wiki/Technology",
    "https://en.wikipedia.org/wiki/Innovation"
  ],
  "keywords": ["technology", "innovation"],
  "schedule": "2026-12-01T10:00:00"
}
```

**Example Response:** `200 OK`

```json
{
  "status": "success",
  "message": "Scraping initiated successfully.",
  "jobId": "49ae0dc2-00af-47ad-a846-ed3be566dbfe",
  "scheduledAt": "2026-12-01T10:00:00"
}
```

---

### 2. Search by Prefix

Performs a prefix-based keyword search through all scraped data using the Trie. Returns all pages where a matching keyword was found.

**Endpoint:**

```
POST /api/v1/search
```

**Request Body:**

| Field    | Type     | Required | Description                                 |
| -------- | -------- | -------- | ------------------------------------------- |
| `prefix` | `String` | ✅ Yes   | Keyword prefix to search (case-insensitive) |
| `limit`  | `int`    | ❌ No    | Max results to return (default: 5)          |

**Example Request:**

```json
{
  "prefix": "tech",
  "limit": 5
}
```

**Example Response:** `200 OK`

```json
{
  "status": "success",
  "results": [
    {
      "url": "https://en.wikipedia.org/wiki/Technology",
      "matchedContent": "...technology trends are shaping the future...",
      "timestamp": "2026-03-23T12:15:28.332192"
    }
  ]
}
```

---

### 3. Check Job Status

Retrieves the current status and metadata of a scraping job using its job ID.

**Endpoint:**

```
GET /api/v1/status/{jobId}
```

**Example Request:**

```
GET http://localhost:8080/api/v1/status/49ae0dc2-00af-47ad-a846-ed3be566dbfe
```

**Example Response:** `200 OK`

```json
{
  "status": "COMPLETED",
  "jobId": "49ae0dc2-00af-47ad-a846-ed3be566dbfe",
  "urlsScraped": ["https://en.wikipedia.org/wiki/Technology"],
  "keywordsFound": ["technology", "innovation"],
  "dataSize": "900 B",
  "finishedAt": "2026-03-23T12:15:30"
}
```

**Job Status Values:**

| Status        | Description                          |
| ------------- | ------------------------------------ |
| `PENDING`     | Job created, waiting to be processed |
| `IN_PROGRESS` | Scraping is currently running        |
| `COMPLETED`   | Scraping finished successfully       |
| `FAILED`      | Scraping encountered an error        |

---

## Error Handling

All errors return a consistent JSON response:

```json
{
  "status": "error",
  "message": "Error message here",
  "timestamp": "2026-03-23T12:00:00"
}
```

| Scenario                         | HTTP Status                 |
| -------------------------------- | --------------------------- |
| Empty or missing URLs / keywords | `400 Bad Request`           |
| Blank search prefix              | `400 Bad Request`           |
| Job ID not found                 | `404 Not Found`             |
| Unexpected server error          | `500 Internal Server Error` |

---

## Running Tests

```bash
mvn clean test
```

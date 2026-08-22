# Book Catalogue

A small Spring Boot service and responsive browser UI for searching, filtering, adding, and removing books. The catalogue is seeded with 1,200 records and uses server-side filtering and pagination, so the browser only receives the current page.

## Requirements

- Java 21
- Maven 3.9+

No Node.js, npm, Docker, or external database is required. The frontend is served directly by Spring Boot.

## Run the application

From the project root, run:

```bash
mvn spring-boot:run
```

When the log says that Tomcat has started on port 8080, open:

```text
http://localhost:8080
```

Stop the application with `Ctrl+C` in the terminal.

The application uses an in-memory H2 database. Data is reset and the 1,200 sample books are recreated whenever the application restarts.

## Use the frontend

- Type in **Search** to find a partial title or author match.
- Choose a **Genre** from the dropdown to apply an exact genre filter.
- Search and genre can be used together.
- Use **Previous** and **Next** to move through server-provided pages.
- Select **Add book** to create a catalogue entry.
- Select **Delete** beside a book and confirm in the dialog to remove it.

Search changes are debounced briefly to avoid sending a request for every keystroke; genre changes apply immediately. Only the current page of 20 books is kept in browser memory.

## Run the tests

```bash
mvn test
```

The test suite contains:

- Repository integration tests using real JPQL and H2.
- HTTP integration tests covering listing, filtering, pagination, validation, creation, deletion, and error responses.
- Focused service unit tests covering normalization, mapping, creation, and deletion behaviour.

## Architecture

```text
Browser UI
   |
   | HTTP/JSON
   v
BookController
   v
BookService
   v
BookRepository
   v
H2
```

Backend code is grouped by feature under `com.gustavo.books.catalogue`. The frontend is intentionally lightweight:

```text
src/main/resources/static/
├── index.html          Page structure and accessible form
├── css/styles.css      Responsive presentation
└── js/
    ├── api.js          HTTP requests and API error handling
    └── app.js          UI state, rendering, and interactions
```

Serving the frontend from Spring Boot keeps the exercise to one process, avoids CORS configuration, and makes setup quick for reviewers.

## API

### List books

```http
GET /api/books?search=river&genre=Fantasy&page=0&size=20
```

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 1200,
  "totalPages": 60
}
```

The default page size is 20 and the maximum accepted page size is 100. Results are sorted by title and then ID unless a sort is supplied.

### Add a book

```http
POST /api/books
Content-Type: application/json
```

```json
{
  "title": "Kindred",
  "author": "Octavia Butler",
  "genre": "Science Fiction",
  "publicationYear": 1979
}
```

### Remove a book

```http
DELETE /api/books/{id}
```

Successful deletion returns `204 No Content`. An unknown ID returns `404 Not Found`.

## Key decisions and trade-offs

- **H2 in memory:** simple to run and sufficient for the exercise; data is intentionally not durable.
- **Server-side pagination:** supports 1,000+ records without loading the entire collection into the browser.
- **Explicit page response:** avoids exposing Spring Data's internal `PageImpl` JSON representation as the API contract.
- **Vanilla JavaScript:** keeps setup small for a backend-focused exercise; a larger UI would benefit from a component framework.
- **Refetch after mutations:** creation and deletion reload the authoritative server page rather than maintaining complex optimistic state.
- **Deterministic sample data:** makes the initial volume reproducible without a large SQL file.

## AI usage

AI assistance was used for requirements review, architecture discussion, incremental code review, sample-data generation, and test/frontend generation. The reconstructed working log is available in [docs/ai-transcript.md](docs/ai-transcript.md).

# AI Assistance Transcript and Decision Log

> This document is a reconstructed working log of the Codex conversation used during the exercise. It is not a raw platform export. Messages are condensed for readability while preserving the requests, recommendations, implementation work, verification results, and decisions made.

## Context

- Project: Books catalogue technical exercise
- Backend: Java 21, Spring Boot, Spring Data JPA, H2
- AI assistant: OpenAI Codex
- Working session started: 16 August 2026
- Developer implemented the core application code.
- Codex was used for requirements analysis, architecture discussion, code review, sample-data generation, and test generation.

## 1. Requirements and architecture

### Developer request

The developer asked Codex to act as a senior Java/Spring Boot engineer throughout the implementation. He explicitly asked Codex not to implement the entire exercise, to favour pragmatic choices suitable for a 3-4 hour task, explain architectural decisions, challenge questionable choices, and maintain a decision log for the interview.

The developer had already created the `Book` model and `BookRepository` interface.

### Codex response

Codex reviewed the supplied exercise PDF and the initial repository. It identified the main requirements:

- Spring Boot backend with a lightweight UI.
- Server-side listing, searching, filtering, sorting, and pagination for 1,000+ records.
- Create and delete operations.
- Lightweight persistence and realistic seed data.
- Clear run instructions and meaningful Git history.
- Transparent use of AI.

Codex recommended a deliberately small layered structure:

```text
HTTP request -> Controller -> Service -> Repository -> H2
                    |
              Request/response DTOs
```

Suggested package structure:

```text
catalogue/
  Book.java
  BookRepository.java
  BookService.java
  BookController.java
  BookRequest.java
  BookResponse.java
  BookNotFoundException.java

common/
  ApiExceptionHandler.java

config/
  SampleDataLoader.java
```

Codex advised against adding hexagonal architecture, authentication, caching, Docker, or other infrastructure that was not needed for the exercise.

### Initial issues found

Codex found two problems in the first `Book` entity:

- It imported Spring Data's `org.springframework.data.annotation.Id` instead of `jakarta.persistence.Id`.
- It applied `@NotBlank` to the generated `Long` ID.

The developer corrected these issues and later replaced Lombok-generated entity behaviour with explicit constructors and accessors.

### Decisions recorded

- Use H2 as an embedded/in-memory database.
- Use conventional controller/service/repository layers.
- Keep JPA entities separate from API request and response DTOs.
- Use database-side pagination and filtering.
- Search across title and author, case-insensitively.
- Filter by genre using an exact, case-insensitive comparison.
- Use deterministic ordering for stable pagination.
- Return `201 Created` for creation and `204 No Content` for deletion.
- Return `404 Not Found` when deleting an unknown book.

## 2. Sample data

### Developer request

> Generate the SampleDataLoader file.

### Codex work

Codex implemented `SampleDataLoader` using `ApplicationRunner`. The loader:

- Creates 1,200 books.
- Uses deterministic title, author, genre, and publication-year combinations.
- Checks `bookRepository.count()` before inserting, preventing duplicate seeding.
- Uses `saveAll()` to keep the implementation straightforward.

Codex explained that deterministic data makes demonstrations and tests reproducible and avoids maintaining a very large SQL seed file.

### Verification

Codex ran the Maven test suite using a workspace-local Maven repository because the machine-level Maven configuration attempted to use an unwritable `C:\.m2` directory.

Result:

```text
Tests run: 1
Failures: 0
Errors: 0
BUILD SUCCESS
```

The application context created the schema and inserted the sample records successfully.

## 3. Controller, service, repository, and exception review

### Developer request

The developer added `BookService`, `BookController`, request/response DTOs, `BookNotFoundException`, and repository search support, then asked Codex to review the changes.

### Codex review

Codex considered the overall structure appropriately small and readable. Positive observations included:

- Thin controller methods.
- Constructor injection.
- DTOs separating the API from persistence.
- `@Valid` request validation.
- Class-level `@Transactional(readOnly = true)` with write transactions on create/delete.
- Mapping `Page<Book>` to `Page<BookResponse>` without losing pagination metadata.
- Database-side search through Spring Data JPA.

Codex identified three gaps:

1. `BookNotFoundException` was a plain runtime exception, so it would produce `500` rather than `404`.
2. The API had search and pagination but did not yet expose the required genre filter.
3. Pagination had no explicit maximum size or deterministic default sort.

### Developer changes

The developer added `ApiExceptionHandler` using `@RestControllerAdvice`. It maps `BookNotFoundException` to:

```json
{
  "error": "Book not found: 123"
}
```

with HTTP status `404 Not Found`.

The developer also configured:

```yaml
spring:
  data:
    web:
      pageable:
        default-page-size: 20
        max-page-size: 100
```

and added default sorting by `title` followed by `id`.

### Filtering refinement

An intermediate implementation added `findByGenreIgnoreCase`, but the method was not connected to the controller or service and could not combine search with genre.

Codex challenged that design and recommended one explicit JPQL query with optional parameters. The developer updated the repository so that:

- `search` can be absent or can match title/author.
- `genre` can be absent or can match genre.
- Both conditions can be applied together.

The service normalizes blank values to `null` and trims nonblank values before calling the repository.

### Final listing contract

```http
GET /api/books?search=night&genre=Fantasy&page=0&size=20&sort=title,asc
```

The resulting decisions were:

- Default page size: 20.
- Maximum page size: 100.
- Default ordering: title, then ID.
- Blank search/filter parameters are treated as absent.
- Search is a case-insensitive partial match on title or author.
- Genre is a case-insensitive exact match.
- Search and genre compose in the same database query.

### Verification

Codex repeatedly ran the Maven tests after the developer's changes. The application context and repository query parsed successfully.

Codex also reminded the developer to re-stage files marked `AM` before committing, because the staged versions were older skeletons than the working copies.

## 4. Test strategy discussion

### Developer request

The developer asked for a prompt that could be sent to Codex to generate meaningful tests rather than tests written only for coverage.

After shortening the initial prompt, the agreed testing request was:

> Add a small, meaningful test suite for the backend changes we just made. Focus on behavior, not coverage: repository search/filter/pagination and API creation/deletion/error behaviour. Keep tests deterministic, use real H2/JPA where useful, avoid unnecessary mocks, run the suite, and do not commit.

### Testing decisions

- Prefer integration tests for repository queries and HTTP contracts.
- Use real H2 and Spring Data JPA for query behaviour.
- Use MockMvc for request validation, JSON serialization, exception mapping, and persistence flow.
- Clear seed data before each integration test and arrange only the records relevant to the scenario.
- Avoid tests for constructors, getters, record accessors, and framework annotations.
- Add isolated unit tests only where they verify meaningful service decisions.

## 5. Repository integration tests generated by Codex

Codex created `BookRepositoryIntegrationTests` with five scenarios:

1. Search matches titles case-insensitively.
2. Search matches authors case-insensitively.
3. Genre filtering is case-insensitive.
4. Search and genre must both match.
5. Pagination returns the requested slice with correct totals, and unmatched filters return an empty page.

These tests execute the real JPQL query against H2 rather than mocking the repository.

## 6. API integration tests generated by Codex

Codex replaced the original context-only test with `BookApiIntegrationTests`. The API suite verifies:

- Valid creation returns `201`, returns the created representation, and persists it.
- A blank required field returns `400` and does not persist a book.
- Deleting an existing book returns `204` and removes it.
- Deleting an unknown book returns `404` with the expected error body.
- Listing books uses the default title ordering.
- Listing books combines trimmed search and genre filters.
- Listing books returns the requested page and correct pagination metadata.

The tests exercise the complete MVC -> service -> repository -> H2 path.

## 7. Service unit tests generated by Codex

The developer asked for meaningful unit tests in addition to integration tests.

Codex added `BookServiceTests`, using Mockito only at the repository boundary. The tests verify:

- Search and genre values are trimmed before repository access.
- Blank filter values are converted to `null`.
- Books are mapped correctly to response DTOs.
- A create request is mapped to an entity and the persisted ID is returned.
- An existing book is passed to the repository for deletion.
- A missing book throws `BookNotFoundException` and never calls `delete`.

Codex intentionally did not add isolated controller, DTO, entity-accessor, or exception-handler unit tests because those behaviours were already covered more meaningfully by the integration suite.

### Final test result at the end of the session

```text
Tests run: 17
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

## 8. Issues intentionally left for follow-up

### Stable paginated response

When the GET integration tests executed, Spring warned that directly serializing `PageImpl` does not guarantee a stable JSON structure.

The production code was not changed because the developer had asked Codex to add tests without changing application behaviour. The recommended follow-up is to return an explicit pagination response DTO before the frontend treats the current JSON structure as a permanent contract.

### Flyway versus Hibernate schema ownership

Flyway is enabled but contains no migrations, while Hibernate uses `ddl-auto: create-drop`. Codex recommended choosing one schema owner. For this time-boxed exercise, removing Flyway and keeping Hibernate schema creation would be the simpler choice; alternatively, add a deliberate migration and stop Hibernate from creating the schema.

### Open EntityManager in View

Spring logs that Open EntityManager in View is enabled. Because DTO mapping happens in the service transaction and the API does not need lazy loading during serialization, setting `spring.jpa.open-in-view: false` would make the transaction boundary explicit.

### Input normalization on creation

`@NotBlank` rejects whitespace-only values but does not automatically trim valid values. Trimming title, author, and genre before persistence would prevent values such as `" Fantasy "` from behaving unexpectedly during exact genre filtering.

## 9. Consolidated architecture decision record

| Decision | Reason |
|---|---|
| Conventional layered architecture | Easy to navigate and extend during live pairing |
| H2 embedded database | Simple startup and sufficient for the exercise |
| JPA repository with explicit JPQL | Keeps combined optional filters readable |
| Request and response DTOs | Avoids exposing the persistence model as the API contract |
| Thin service layer | Provides a clear extension point without overengineering |
| Server-side pagination | Required for responsive handling of 1,000+ records |
| Page size capped at 100 | Prevents clients from bypassing pagination |
| Sort by title then ID | Produces deterministic pages |
| Deterministic 1,200-record seed set | Reproducible UI demonstrations and testing |
| Central exception handler | Consistent HTTP error mapping and room for extension |
| Real H2 integration tests | Proves JPQL and persistence behaviour rather than mocks |
| MockMvc API integration tests | Proves validation, status codes, JSON, service, and persistence together |
| Mockito only for service unit tests | Isolates actual service decisions without duplicating framework tests |

## 10. How AI output was applied

- The developer retained ownership of the architecture and implemented the core entity, repository, service, controller, DTO, filter, and exception-handling changes.
- Codex reviewed each increment and highlighted functional or architectural gaps.
- Codex directly generated the deterministic sample-data loader after the developer explicitly requested that file.
- Codex directly generated the repository, API integration, listing, and service unit tests after the developer explicitly requested tests.
- The developer iteratively updated production code in response to review findings.
- Codex ran the complete test suite after meaningful changes and reported the results.
- No commits were created by Codex.

This use of AI was intentionally collaborative: architecture and trade-offs were discussed first, production implementation remained primarily developer-led, and generated code was limited to explicitly requested supporting components and tests.

## 11. Frontend implementation

The developer explained that he did not have much frontend experience and explicitly asked Codex to implement the UI and explain how to run it.

Codex recommended and implemented a vanilla JavaScript frontend served from Spring Boot. This kept the project to one process and avoided Node.js, a separate frontend build, and CORS configuration. The UI includes:

- A responsive book table that becomes stacked cards on narrow screens.
- Debounced title/author search.
- A styled genre dropdown whose filter composes with search.
- Previous/next server-side pagination.
- An accessible add-book dialog with browser validation.
- A styled, accessible deletion confirmation dialog followed by a server refresh.
- Loading, empty, error, form-error, and success states.

The oversized introductory heading was later removed and the Add book action was moved from the global header into a compact catalogue toolbar. This keeps the action close to the collection it changes and gives the page a more task-focused hierarchy. On small screens, the button expands to the toolbar width.
- Cancellation of stale list requests with `AbortController`.

Before connecting the UI, Codex replaced direct `PageImpl` serialization with an explicit `PageResponse` containing `items`, `page`, `size`, `totalItems`, and `totalPages`. The existing HTTP tests were updated to enforce this stable contract.

Codex verified JavaScript syntax, ran all 17 tests successfully, started the complete application locally, and checked that the page, static assets, paginated filtering endpoint, creation, and deletion all responded correctly.

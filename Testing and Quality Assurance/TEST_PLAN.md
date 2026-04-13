# Test Plan & Strategy
## SOEN 345 – Cloud-based Ticket Reservation Application
**Course:** SOEN 345 – Software Testing, Verification and Quality Assurance  
**Term:** Winter 2026  

---

## 1. Introduction

This document describes the test plan and strategy for the Ticket Reservation Application developed as part of the SOEN 345 group project. The application is a cloud-based Java system allowing users to browse events, make reservations, and receive email confirmations, while administrators manage events.

---

## 2. Scope

### 2.1 In Scope
- Unit testing of all model classes (`User`, `Admin`, `Event`, `Reservation`)
- Unit testing of `Server` helper/utility methods (`parseString`, `parseLong`, `escapeJson`)
- Integration testing of `Database` class against the live Firebase REST API
- Basic validation of HTTP routing logic in `Server`
- CACC testing for Database, EmailService, Reservation, Server, and User
- CFG testing for `HandleEvents` routing logic

### 2.2 Out of Scope
- Frontend UI testing (the HTML/CSS frontend is a thin static client)
- Email delivery verification (SMTP is mocked/skipped when not configured)
- Load and performance testing (beyond the scope of this phase)
- Security penetration testing

---

## 3. Test Objectives

- Verify that all model classes correctly store, expose, and serialize their data
- Verify that `Admin` always inherits the `"admin"` role from `User`
- Verify that `Reservation` automatically timestamps creation
- Verify that `Database` methods correctly communicate with Firebase (CRUD operations)
- Verify that `Server` helper methods parse and sanitize JSON correctly
- Ensure no regressions are introduced during development via CI/CD
- Ensure correctness of decision logic using CACC testing
- Ensure structural correctness of event-handling logic using CFG testing

---

## 4. Software Development Method

The team adopted **Scrum** as the software development methodology. The project was broken into two-week sprints, with progress reports submitted on Moodle at each sprint boundary. Each sprint included planning, development, testing, and review phases.

---

## 5. Test Strategy

### 5.1 Testing Levels

| Level | Description | Tools |
|---|---|---|
| Unit Testing | Test individual classes and methods in isolation | JUnit 5 |
| Integration Testing | Test `Database` class against the live Firebase API | JUnit 5 |
| System Testing | Manual end-to-end testing via the web UI and API | Browser + Postman |
| CACC Testing | Clause-level decision testing for Database, EmailService, Reservation, Server, User | JUnit 5 |
| CFG Testing | Structural path testing for `HandleEvents` | JUnit 5 |

### 5.2 Test Types

**Unit Tests** cover all model classes and server utility methods. These tests have no external dependencies — they run entirely in memory and are fast and deterministic.

**Integration Tests** (DatabaseTest) connect to the live Firebase Realtime Database. Each test cleans up after itself (deletes any data it creates) to keep the database in a known state. These tests require a valid `config.properties` file with Firebase credentials.

**Acceptance / Functional Tests** are conducted manually by running the server and interacting with the web UI and REST API to verify end-to-end user flows (registration, login, browsing events, making reservations, cancelling).

**CACC Tests** were created for all modules containing multi-clause boolean logic:
- Database (query filtering, existence checks)
- EmailService (SMTP enabled/disabled, null checks)
- Reservation (timestamp logic, field validation)
- Server (authorization, JSON parsing conditions)
- User (role assignment, constructor logic)

CACC ensures each clause independently affects the decision outcome.

**CFG Tests** were created specifically for the `HandleEvents` routing logic, covering:
- Node coverage
- Edge coverage
- Basis path coverage (cyclomatic complexity)

### 5.3 Test Approach

- Tests are written in JUnit 5 and placed in the `src/test/` package
- All tests follow the **Arrange → Act → Assert** pattern
- Each test method tests a single behavior and has a descriptive name
- Integration tests use `@BeforeEach` / `@AfterEach` for setup and cleanup
- Private server methods are tested via Java Reflection
- CACC clause tables were created for each multi-clause decision
- CFG path sets were derived from the `HandleEvents` control flow graph

---

## 6. Tools & Environment

| Tool | Purpose |
|---|---|
| JUnit 5 | Unit and integration test framework |
| IntelliJ IDEA | IDE used for development and running tests |
| GitHub | Version control and code collaboration |
| GitHub Actions | CI/CD pipeline — automatically runs tests on every push and pull request |
| Firebase Realtime Database | Cloud database (integration test target) |
| Postman | Manual API testing |
| Draw.io / IntelliJ Diagrams | Used for CFG diagrams for `HandleEvents` |

---

## 7. CI/CD Pipeline

GitHub Actions is configured to run all JUnit tests automatically on every push to `main` and on every pull request. The workflow:
1. Checks out the repository
2. Sets up JDK 17
3. Compiles the project
4. Runs all tests with the JUnit console launcher
5. Reports pass/fail status on the pull request

This ensures no broken code is merged into the main branch.

---

## 8. Test Cases

### 8.1 Unit Tests – User

| ID | Test Method | Description | Expected Result |
|---|---|---|---|
| UT-U01 | `testEmailPasswordConstructor` | Create user with email and password | Email, password, and role="user" stored correctly |
| UT-U02 | `testEmailOnlyConstructor` | Create user with email only | Email stored, password is null, role="user" |
| UT-U03 | `testPhoneNumberConstructor` | Create user with phone number | Phone stored, email is null, role="user" |
| UT-U04 | `testFullConstructor` | Create user with all fields | All four fields stored correctly |
| UT-U05 | `testDefaultRoleIsUser` | Check default role across 3 constructors | All return "user" |
| UT-U06 | `testSetRole` | Change role via setRole() | Role updated to new value |
| UT-U07 | `testSetUserId` | Assign a userId | userId returned by getter matches set value |
| UT-U08 | `testToJsonContainsEmail` | Serialize user to JSON | JSON string contains correct email field |
| UT-U09 | `testToJsonContainsRole` | Serialize user to JSON | JSON string contains role="user" |
| UT-U10 | `testToJsonContainsPassword` | Serialize user to JSON | JSON string contains password |
| UT-U11 | `testToJsonWithNullEmail` | Serialize user with null email | JSON contains email="" (no null pointer exception) |
| UT-U12 | `testToJsonWithNullPassword` | Serialize user with null password | JSON contains password="" |

### 8.2 Unit Tests – Admin

| ID | Test Method | Description | Expected Result |
|---|---|---|---|
| UT-A01 | `testEmailPasswordConstructorSetsAdminRole` | Create admin with email+password | Role is "admin" |
| UT-A02 | `testEmailOnlyConstructorSetsAdminRole` | Create admin with email only | Role is "admin" |
| UT-A03 | `testPhoneConstructorSetsAdminRole` | Create admin with phone number | Role is "admin" |
| UT-A04 | `testEmailIsStoredCorrectly` | Email field from User superclass | Email matches input |
| UT-A05 | `testPasswordIsStoredCorrectly` | Password field from User superclass | Password matches input |
| UT-A06 | `testPhoneNumberIsStoredCorrectly` | Phone field from User superclass | Phone matches input |
| UT-A07 | `testToJsonContainsAdminRole` | JSON serialization | JSON contains role="admin" |
| UT-A08 | `testToJsonContainsEmail` | JSON serialization | JSON contains correct email |
| UT-A09 | `testSetUserIdInherited` | setUserId() inherited from User | userId getter returns set value |

### 8.3 Unit Tests – Event

| ID | Test Method | Description | Expected Result |
|---|---|---|---|
| UT-E01 | `testFullConstructor` | Create event with all 4 fields | All fields stored correctly |
| UT-E02 | `testConstructorWithoutId` | Create event without eventId | eventId is null |
| UT-E03 | `testSetEventId` | Assign eventId | Getter returns assigned value |
| UT-E04 | `testGetDate` | Date getter | Returns correct timestamp |
| UT-E05 | `testGetLocation` | Location getter | Returns correct location |
| UT-E06 | `testGetCategory` | Category getter | Returns correct category |
| UT-E07 | `testToJsonContainsDate` | JSON serialization | JSON contains date field |
| UT-E08 | `testToJsonContainsLocation` | JSON serialization | JSON contains location field |
| UT-E09 | `testToJsonContainsCategory` | JSON serialization | JSON contains category field |
| UT-E10 | `testToJsonStructure` | JSON format check | Starts with `{` and ends with `}` |
| UT-E11 | `testToJsonDoesNotContainEventId` | Firebase node body check | eventId is not part of the stored JSON body |

### 8.4 Unit Tests – Reservation

| ID | Test Method | Description | Expected Result |
|---|---|---|---|
| UT-R01 | `testShortConstructor` | Create reservation with 3 fields | Fields stored, reservedAt auto-set within test window |
| UT-R02 | `testFullConstructor` | Create reservation with all 5 fields | All fields stored correctly |
| UT-R03 | `testSetReservationId` | Assign reservationId | Getter returns assigned value |
| UT-R04 | `testReservedAtIsPositive` | Auto-timestamp | reservedAt is greater than 0 |
| UT-R05 | `testToJsonContainsUserId` | JSON serialization | JSON contains userId |
| UT-R06 | `testToJsonContainsEventId` | JSON serialization | JSON contains eventId |
| UT-R07 | `testToJsonContainsUserEmail` | JSON serialization | JSON contains userEmail |
| UT-R08 | `testToJsonWithNullEmail` | Null email safety | JSON contains userEmail="" (no NPE) |
| UT-R09 | `testToJsonContainsReservedAt` | JSON serialization | JSON contains reservedAt timestamp |
| UT-R10 | `testToJsonStructure` | JSON format check | Starts with `{` and ends with `}` |

### 8.5 Unit Tests – Server Helpers

| ID | Test Method | Description | Expected Result |
|---|---|---|---|
| UT-S01 | `testParseStringBasic` | Extract string from JSON | Correct value returned |
| UT-S02 | `testParseStringMissingKeyReturnsEmpty` | Key not in JSON | Returns empty string |
| UT-S03 | `testParseStringEmptyValue` | Key exists with empty value | Returns empty string |
| UT-S04 | `testParseStringWithWhitespace` | Key-value has spaces around colon | Returns correct value |
| UT-S05 | `testParseLongBasic` | Extract long from JSON | Correct numeric value returned |
| UT-S06 | `testParseLongMissingKeyReturnsZero` | Key not in JSON | Returns 0 |
| UT-S07 | `testParseLongPhoneNumber` | Large phone number | Parsed correctly as long |
| UT-S08 | `testParseLongZeroValue` | Date = 0 | Returns 0 |
| UT-S09 | `testEscapeJsonNull` | Null input | Returns empty string |
| UT-S10 | `testEscapeJsonPlainString` | No special characters | String unchanged |
| UT-S11 | `testEscapeJsonDoubleQuote` | Input contains `"` | Escaped as `\"` |
| UT-S12 | `testEscapeJsonBackslash` | Input contains `\` | Escaped as `\\` |
| UT-S13 | `testEscapeJsonNewline` | Input contains newline | Escaped as `\n` |
| UT-S14 | `testEscapeJsonCarriageReturn` | Input contains `\r` | Escaped as `\r` |
| UT-S15 | `testEscapeJsonTab` | Input contains tab | Escaped as `\t` |
| UT-S16 | `testEscapeJsonMixedSpecialChars` | Multiple special characters | All escaped correctly |

### 8.6 Integration Tests – Database

| ID | Test Method | Type | Description | Expected Result |
|---|---|---|---|---|
| IT-D01 | `testAddEvent` | Integration | Add and delete an event via Firebase | HTTP 200 for both operations |
| IT-D02 | `testGetAllEventsReturnsJson` | Integration | Retrieve all events | Returns "null" or valid JSON object |
| IT-D03 | `testUpdateEvent` | Integration | Add event, update it, delete | HTTP 200 for update and delete |
| IT-D04 | `testDeleteEvent` | Integration | Add and immediately delete event | HTTP 200 for delete |
| IT-D05 | `testAddUser` | Integration | Add a new user | Returns non-empty Firebase key |
| IT-D06 | `testGetAllUsersReturnsJson` | Integration | Retrieve all users | Returns "null" or valid JSON object |
| IT-D07 | `testGetUserByEmail` | Integration | Query user by email | Response contains the queried email |
| IT-D08 | `testAddReservation` | Integration | Add and delete a reservation | Returns non-empty Firebase key |
| IT-D09 | `testGetAllReservationsReturnsJson` | Integration | Retrieve all reservations | Returns "null" or valid JSON object |
| IT-D10 | `testGetReservationsByUser` | Integration | Filter reservations by userId | Response contains the userId |
| IT-D11 | `testDeleteReservation` | Integration | Add and delete a reservation | HTTP 200 for delete |
| IT-D12 | `testGetReservation` | Integration | Fetch a single reservation by ID | Response contains correct userId |

### 8.7 Manual / Functional Test Cases

| ID | Scenario | Steps | Expected Result |
|---|---|---|---|
| FT-01 | User Registration | POST `/api/auth/register` with valid email and password | HTTP 201, userId returned |
| FT-02 | Duplicate Registration | POST `/api/auth/register` with already-registered email | HTTP 409 Conflict |
| FT-03 | Registration Missing Fields | POST `/api/auth/register` without email or password | HTTP 400 Bad Request |
| FT-04 | User Login | POST `/api/auth/login` with correct credentials | HTTP 200, role returned |
| FT-05 | Invalid Login | POST `/api/auth/login` with wrong password | HTTP 401 Unauthorized |
| FT-06 | Browse Events | GET `/api/events` | HTTP 200, JSON list of events |
| FT-07 | Admin Add Event | POST `/api/events` with role="admin" | HTTP 201, eventId returned |
| FT-08 | Non-Admin Add Event | POST `/api/events` with role="user" | HTTP 403 Forbidden |
| FT-09 | Admin Edit Event | PUT `/api/events/{id}` with role="admin" | HTTP 200, status="updated" |
| FT-10 | Non-Admin Edit Event | PUT `/api/events/{id}` with role="user" | HTTP 403 Forbidden |
| FT-11 | Admin Cancel Event | DELETE `/api/events/{id}?role=admin` | HTTP 200, status="deleted" |
| FT-12 | Non-Admin Cancel Event | DELETE `/api/events/{id}?role=user` | HTTP 403 Forbidden |
| FT-13 | Make Reservation | POST `/api/reservations` with userId, eventId | HTTP 201, reservationId returned |
| FT-14 | Duplicate Reservation | POST `/api/reservations` for same event by same user | HTTP 409 Conflict |
| FT-15 | Cancel Reservation | DELETE `/api/reservations/{id}` | HTTP 200, status="cancelled" |
| FT-16 | View My Reservations | GET `/api/reservations?userId={id}` | HTTP 200, reservations for that user only |

### 8.8 CACC Test Cases 

| ID | Component | Description | Expected Result |
|---|---|---|---|
| CACC-DB01 | Database | Clause coverage for filtering and CRUD decision logic | Each clause independently affects decision outcome |
| CACC-ES01 | EmailService | SMTP-enabled, null-check, and fallback logic | Correct behavior for all clause combinations |
| CACC-R01 | Reservation | Timestamp and field validation clauses | Correct timestamping and null handling |
| CACC-S01 | Server | Authorization and JSON parsing clauses | Correct allow/deny and parsing behavior |
| CACC-U01 | User | Role assignment and constructor clause logic | Correct role and field initialization |

### 8.9 CFG Test Cases 

| ID | Method | Description | Expected Result |
|---|---|---|---|
| CFG-H01 | `HandleEvents` | Node coverage | All nodes executed at least once |
| CFG-H02 | `HandleEvents` | Edge coverage | All edges executed |
| CFG-H03 | `HandleEvents` | Basis path coverage | All independent paths executed |

---

## 9. Entry and Exit Criteria

### Entry Criteria
- Source code compiles without errors
- `config.properties` is present and contains valid Firebase credentials
- JUnit 5 is available on the classpath

### Exit Criteria
- All unit tests pass (0 failures)
- All integration tests pass against Firebase
- All manual functional test cases have been executed and documented
- CI/CD pipeline reports a green build on the `main` branch
- All CACC tests achieve full clause coverage
- All CFG tests achieve basis path coverage for `HandleEvents`

---

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Firebase API downtime | Low | High | Retry tests; keep unit tests independent of Firebase |
| SMTP credentials not configured | Medium | Low | EmailService degrades gracefully with console output |
| Test data polluting Firebase | Medium | Medium | Each integration test deletes its own data in cleanup |
| Private method access via reflection | Low | Low | If Java security manager blocks it, refactor to package-private |
| CACC clause explosion | Low | Medium | Limit to meaningful clause combinations |
| CFG path explosion in HandleEvents | Medium | Medium | Use cyclomatic complexity to limit required paths |
---

## 11. Version Control & Collaboration

All source code and test files are managed in a **GitHub** repository. The team uses feature branches and pull requests for code review. **GitHub Actions** runs the full test suite on every push and pull request, enforcing that the `main` branch always stays in a passing state.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

A small Spring Boot 4 + PostgreSQL REST API (one read endpoint) built from the
Amigoscode course. `README.md` covers running it; `docs/ARCHITECTURE.md` covers
*why* it is wired the way it is and the non-obvious behaviour — read it before
changing the persistence layer, the compose setup, or the seeder.

## Commands

```bash
./mvnw spring-boot:run          # run the app; starts Postgres via compose.yaml, leaves it up
./mvnw test                     # full test suite (see Postgres note below)
./mvnw test -Dtest=DataSeederTest                       # one test class
./mvnw test -Dtest='SoftwareEngineerServiceTest#getAllSoftwareEngineers_returnsEmptyListWhenRepositoryIsEmpty'  # one method
./mvnw package                  # build the executable jar (compose support is stripped from it)
```

`./mvnw test` also writes the JaCoCo report (the `report` goal is bound to the
`test` phase) — see Coverage below.

There is no separate linter — the compiler plus Lombok/`spring-boot-configuration-processor`
annotation processing (wired in `pom.xml`) is the whole static-check story.

### Tests need Postgres already running

`spring.docker.compose.skip.in-tests` is `true`, so `./mvnw test` does **not**
start the database. `ApplicationTests` and `SoftwareEngineerRepositoryTest` are
`@SpringBootTest` and hit real Postgres on host port **5332**. Start it first:

```bash
docker compose up -d db && ./mvnw test
```

A red `@SpringBootTest` with a connection error is an environment problem (DB
down), not a test to rewrite.

### Coverage

JaCoCo runs during `test` (`prepare-agent`) and `report` writes to
`target/site/jacoco/` (`index.html`, `jacoco.csv`). Generated code
(`com.amigoscode.codegen`) is excluded. Coverage is a diagnostic here, not a
gate — do not add tests whose only purpose is moving the number.

## Architecture

`Controller → Service → Spring Data `JpaRepository` → Hibernate → Postgres`.
The service is a deliberate seam that is currently a pass-through; a write path
would go through it (see "Extending this" in `docs/ARCHITECTURE.md`).

Non-obvious pieces, all explained in `docs/ARCHITECTURE.md`:

- **`SoftwareEngineer.techStack`** is `@ElementCollection` + `@CollectionTable`
  (`software_engineer_tech_stack` side table), fetched `EAGER` on purpose so
  serialisation does not depend on `open-in-view`.
- **`id`** is `@GeneratedValue(strategy = UUID)` — assigned on persist, `null`
  before. `equals`/`hashCode` cover the generated `id`, which is fragile for a
  JPA entity but safe here because instances are never hashed before saving.
- **`DataSeeder`** (`CommandLineRunner`) seeds two engineers only when
  `count() == 0`. With `ddl-auto=update` + the `postgres_data` volume, rows
  survive restarts, so the guard is load-bearing.
- **Schema is `ddl-auto=update`** — no migration files. A renamed/removed field
  leaves a stale column. Not for a shared database.
- **Docker Compose lifecycle**: `spring-boot-docker-compose` drives the DB in
  dev, is stripped from the jar, and is skipped in tests — which is why explicit
  `spring.datasource.*` properties exist even though dev never uses them. The
  `app` service is behind the `full` compose profile so IDE runs don't rebuild it.

### Known dead config (don't "fix" without reason)

- `src/main/resources/graphql-client/` **must exist** (tracked `.gitkeep`) or the
  `graphqlcodegen-maven-plugin` fails the build before tests run. The plugin is
  configured against an empty dir and generates nothing.
- `spring-modulith-starter-core` is a dependency but no modules are defined.

## Spring Boot 4.1.1 — not the Boot 3 test API

Copy test setup from the existing tests; several imports moved in Boot 4:

| Need | This repo uses | NOT |
| --- | --- | --- |
| Controller slice | `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` | `...boot.test.autoconfigure.web.servlet.WebMvcTest` |
| Mock a bean in a slice | `org.springframework.test.context.bean.override.mockito.MockitoBean` | `@MockBean` |
| Test starter | `spring-boot-starter-webmvc-test` (on the classpath) | `spring-boot-starter-test` |

A test referencing `@MockBean` or the `web.servlet` package will not compile here.

### Test conventions

- **Unit**: `@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks`, AssertJ,
  BDDMockito (`given(...).willReturn(...)`). Models: `SoftwareEngineerServiceTest`,
  `DataSeederTest`.
- **Controller slice**: `@WebMvcTest(X.class)`, `@MockitoBean` the service,
  `jsonPath` + Hamcrest. Model: `SoftwareEngineerControllerTest`.
- **JPA**: `@SpringBootTest` + `@Transactional` (rollback). Model:
  `SoftwareEngineerRepositoryTest`.
- Test names: `methodUnderTest_expectedBehaviour`.
- Tests that pin questionable-but-real behaviour carry a Javadoc pointing at
  `docs/ARCHITECTURE.md` "Known rough edges" and framing the test as documenting,
  not endorsing (e.g. `SoftwareEngineerControllerTest.getEngineers_propagatesServiceExceptionUnhandled`).

## Git pre-push hook

`.githooks/pre-push` runs two Claude Code subagents against the commits being
pushed. Activate once per clone: `git config core.hooksPath .githooks`.

1. **`unit-tester`** runs `./mvnw test` + prints a coverage table. Blocks the
   push only on a real test failure (`TEST_RESULT: FAIL`); a coverage number
   never blocks; if Postgres is down it reports `SKIP` and the push proceeds.
   Runs against the working tree, so it is gated: only when the push touches
   `src/`/`pom.xml`, the tree is clean, and `HEAD` is a pushed tip.
2. **`code-reviewer`** reviews the diff. Blocks only on a 🔴 blocker
   (`REVIEW_RESULT: BLOCK`).

Fails open (missing `claude` CLI, timeout, unreadable verdict → push allowed).
Bypass: `SKIP_TESTS=1`, `SKIP_CODE_REVIEW=1`, or `git push --no-verify`.

Subagent definitions live in `.claude/agents/`. When changing behaviour that the
hook or the agents describe, update the agent `.md` and `.githooks/README.md` too.

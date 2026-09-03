# Contributing

Thanks for your interest in improving this project. This is a small Spring Boot
REST API, and the workflow below keeps changes easy to review.

## Getting set up

- JDK 21
- Docker with Compose v2 (`docker compose`, not `docker-compose`)

See `README.md` for the full quick-start and `docs/ARCHITECTURE.md` for *why*
the persistence layer, compose setup, and seeder are wired the way they are.

## Making a change

1. Fork the repository and create a branch off `main`.
2. Make your change, keeping it focused — a bug fix shouldn't carry unrelated
   refactors or cleanup.
3. Start Postgres and run the tests before opening a PR:

   ```bash
   docker compose up -d db && ./mvnw test
   ```

4. Activate the pre-push hook once per clone so the local checks run
   automatically:

   ```bash
   git config core.hooksPath .githooks
   ```

5. Open a pull request against `main` describing what changed and why.

## Code style and conventions

- There's no separate linter — the compiler plus the Lombok /
  `spring-boot-configuration-processor` annotation processing is the whole
  static-check story.
- Follow the existing test conventions (`methodUnderTest_expectedBehaviour`
  naming, `@ExtendWith(MockitoExtension.class)` for unit tests, `@WebMvcTest`
  for controller slices, `@SpringBootTest` + `@Transactional` for JPA tests —
  see `CLAUDE.md` for the full breakdown).
- Spring Boot 4 moved several test imports from the Boot 3 API (e.g.
  `MockitoBean` instead of `@MockBean`) — copy setup from the existing tests
  rather than from older Boot 3 examples.

## Reporting bugs and requesting features

Open a GitHub issue with a clear description, steps to reproduce (for bugs),
and any relevant logs or error output.

## Reporting security issues

Please do not open a public issue for security vulnerabilities — see
`SECURITY.md` for how to report them privately.

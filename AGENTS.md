# Contributor and Agent Guide

This guide is reusable across repositories. Adapt the preferred technical defaults
to each project's requirements. The coverage requirement applies to every project.

## Communication

- Address Vithor by his preferred spelling. Use Brazilian Portuguese in conversation and English for code, identifiers, comments, and project documentation, unless the project requires otherwise.
- State what was found, what changed, and what was verified. Call out assumptions and limitations. Check examples before presenting them as evidence.

## Making Changes

- Read the repository instructions, existing code, tests, and configuration before changing behavior. Preserve work you did not make.
- Prefer focused changes that fit the existing architecture. Assign responsibilities by what a component does. Keep business rules separate from orchestration, transport, and storage when those boundaries exist.
- Favor clear names and straightforward implementations. Remove confirmed dead code and unused imports; avoid speculative abstractions and unrelated cleanup.
- Apply Clean Code and SOLID where they make responsibilities clearer: keep classes cohesive, methods focused, dependencies explicit, and comments reserved for decisions the code cannot explain. Prefer meaningful domain names over generic services and utility classes.
- Use design patterns to solve a concrete problem. Explain the responsibility or trade-off they address before adding indirection. Do not assume a pattern used in one project belongs in the next.
- Preserve public contracts unless the task calls for changing them. Update the README or relevant documentation when setup, behavior, contracts, or architectural decisions change.

## Preferred Backend Defaults

- Prefer hexagonal architecture for business services: domain for invariants and business rules, application for use-case orchestration and ports, and adapters for delivery, persistence, and external integrations. Keep the core independent of frameworks and wire implementations at the composition root.
- For Java backends, prefer Java 17, Spring Boot 4.x, and Maven with a versioned Maven Wrapper. Respect the versions and constraints of an existing repository. Prefer explicit Java over Lombok and YAML for Spring Boot application configuration.
- If a Maven project has no wrapper, generate it with the official Maven Wrapper plugin and version `mvnw`, `mvnw.cmd`, and the generated `.mvn/wrapper/` files. Pin a compatible Maven version, preserve the executable bit on `mvnw`, and use the wrapper in the README and CI.
- Use Spring Web and Bean Validation for HTTP APIs, Spring Data JPA and Flyway for relational persistence, and H2 where an embedded database fits the local or PoC scope. Use JMS with embedded ActiveMQ when local messaging is a requirement, and Resilience4j where an integration needs retry or circuit breaking. These are options for the problem at hand, not mandatory dependencies.
- Keep channel-specific DTOs and mappings at the boundary. Normalize once where practical, keep canonical representations and invariants in the domain, and validate application inputs at the appropriate level. Prefer lightweight commands and queries with manual builders when they improve construction and readability.
- Keep retry policies at integration boundaries, limit retries to failures that can recover, and do not retry in a failed transaction. Keep technical exception details out of public responses. For APIs targeting Brazilian users, prefer clear, consistent errors in Brazilian Portuguese.

## Reviewing and Verifying

- In reviews, lead with actionable findings ordered by severity. Include file references, the triggering scenario, and the impact. Distinguish defects from trade-offs and style preferences.
- Use the repository's standard build and verification commands. Test relevant success and failure paths, and favor deterministic tests over fixed sleeps.
- For Java projects, prefer JUnit 5, Mockito when a boundary needs a test double, and JaCoCo for measurable coverage. Use fast unit tests for domain rules, orchestration, and mapping. Use integration tests for real framework wiring and contracts at boundaries such as HTTP, migrations, database queries, messaging, and scheduling when present.
- Keep tests readable and focused on observable behavior. Use controlled clocks, isolated data, and bounded waits for asynchronous flows. Avoid mocking the behavior an integration test is meant to prove or adding tests solely to exercise unreachable code.
- Check production and test code for unused imports, deprecated APIs, raw types, unchecked casts, avoidable suppressions, nullability issues, and locale-dependent behavior. Run the repository's configured Sonar or other static analysis when available. Report manual findings as inspection, not as confirmed Sonar issues.
- Require 100% test coverage in every project. Configure the build or CI to fail below 100% for lines, branches, methods or functions, and any additional coverage metrics supported by the project's tooling. Count unit and integration tests together where both contribute to coverage.
- Preserve quality gates. Do not lower thresholds, suppress warnings, or exclude code merely to make a check pass. If coverage cannot be measured or verified, report the gap explicitly.
- Distinguish static inspection from checks actually run. Report failures and checks that could not be completed.

## Git Workflow

- Use Conventional Commits and the `codex/` prefix for agent-created branches unless repository instructions specify otherwise.
- Keep commits focused. Commit, push, or open a pull request only when requested or already authorized for the task.

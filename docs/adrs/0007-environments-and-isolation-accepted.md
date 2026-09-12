# 0007 — Environments and isolation

**Status:** Accepted  
**Date:** 2026-09-10  
**Decision makers:** Terry Yin  
**Consulted:** Codex repository-alignment review

## Context

Donut runs in several environments with different owners and data lifecycles.
Confusing them can destroy persistent data or let concurrent work interfere.

## Decision

Use these environments:

| Environment | Purpose | Ownership and lifecycle |
| --- | --- | --- |
| **Production** (`prod`) | The deployed product | Persistent and operator-owned. Test-only access and destructive test controls are prohibited. |
| **Development** (`dev`, normally `doughnut_development`) | Coding and manual use | Persistent and developer-owned. Automated tests and worktree retirement must not mutate it. |
| **Unit Test** (`test`, normally `doughnut_test` or an isolated worktree unit-test database) | Backend unit-test execution | Disposable and owned by the unit-test command. It must not use Development or E2E data. |
| **E2E** (`e2e`, normally `doughnut_e2e_test` or an isolated worktree E2E database) | Running the application under Cypress | Disposable and owned by the E2E runner. Externally driven reset and testability operations are allowed only here. |

CI and Cloud VMs are execution locations, not additional environments; they
select one of the environments above according to the work being run.

### Rules

- Keep each environment's mutable application data separate.
- Concurrent application stacks use separate application processes and service
  ports. Infrastructure may be shared only when its mutable state remains
  isolated between environments.
- A linked or configured worktree uses a stable identity to select its own Unit
  Test database and, when E2E runs, its own E2E database and application ports.
  An unconfigured primary checkout may use the canonical defaults.
- Worktree retirement may delete only that worktree's disposable Unit Test and
  E2E data; Production and Development state are never disposable.
- If the environment or owner cannot be proven, stop. Never fall back to or
  adopt another environment's resources.

## Consequences

Humans and agents must preserve these environment and worktree boundaries when
changing application, database, test, or lifecycle tooling.

## Related

- [ADR 0006 — Failure handling](./0006-failure-handling-accepted.md)
- Operational guide: [Development setup](../development-setup.md)

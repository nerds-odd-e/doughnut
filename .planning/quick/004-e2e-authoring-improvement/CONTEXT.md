# E2E authoring improvement — context

## Intent

Bring all Cucumber/Cypress feature files (and their steps / page objects as needed) in line with `.cursor/rules/e2e-authoring.mdc`, without changing product behavior.

## Scope

- In scope: `e2e_test/features/**/*.feature`, matching `e2e_test/step_definitions/**`, `e2e_test/start/**` when required for wording, waits, or fluent POs.
- Out of scope: product feature work; full-suite Cypress runs; renaming capabilities by phase number; adding `@focus` / `@only`.

## Shared audit checklist (every phase)

Apply in order; only change what fails the checklist for files in that phase’s group:

1. **Behavior triad** — Each scenario has observable pre / trigger / post; avoid presentation-only scenarios with no meaningful state change after Given.
2. **Domain Gherkin** — No UI language (“click button”, control names) unless the control *is* the domain concept; intention over mechanics.
3. **Focused scenarios** — Split multi-behavior mega-scenarios; prefer `Scenario Outline` for same path + different data.
4. **Steps & POs** — Thin steps; fluent page objects; different user-visible behaviors → different steps (no smart mode flags).
5. **Busy wait** — Actions that start `data-app-busy` call `waitUntilAppIsNotBusy()` in the PO; no hardcoded `cy.wait(ms)`.
6. **Fixtures** — Given + data tables; mock tags when needed; avoid unnecessary `Folder` column when inject order alone establishes parent/child (Folder OK when folder hierarchy is under test).
7. **Assertions** — Clear expected/actual messages in POs; assertive Given/When/Then.
8. **Hygiene** — No `@focus`/`@only`; capability-named files; CLI interactive coverage stays Vitest where tagged `@ignore` for Cypress.

## Verification per phase

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec '<comma-or-glob of group features>'
```

Assume `pnpm sut` is running. Do not run the full E2E suite.

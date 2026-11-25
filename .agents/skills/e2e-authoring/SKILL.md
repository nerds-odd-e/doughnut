---
name: e2e-authoring
description: E2E testing with Cypress and Cucumber, shared browser test guidance, feature files, step definitions, named navigation, assertions, app-busy waits, test execution. Use when writing Cypress/Cucumber E2E tests.
paths:
  - "e2e_test/**/*.feature"
  - "e2e_test/**/*.ts"
---
# E2E Authoring Rules

Use this skill when writing or modifying E2E tests, Cypress/Cucumber feature files, step definitions, page objects, or E2E assertions. For OCR-specific canvas assertions, use the `e2e-ocr` skill.

## Select test guidance

Follow [Open Dough test guidance](../dough-execute-plan/references/test-entry-rules.md)
for boundaries and observable cases. Select [Dough Page Objects](../dough-page-objects/SKILL.md)
using the owning browser feature.

When only a Cucumber step, page object or other support source changes, trace
its changed operation through callers and step wording to the owning browser
feature(s), and select those feature paths. Do this even when `--changed`
lists only support sources: they do not themselves match the feature entry.
Carry each edited support path, owning feature path and caller/step evidence
into the implementation or refactor brief. For new support, identify the
intended owning feature before applying the convention. Report any unresolved
feature owner in the brief and follow the shared unresolved-rule handling.

For example, `conversation.ts`'s “I start a conversation about the note …
inviting AI …” step belongs to
`e2e_test/features/messages/conversation_about_a_note.feature`. Starting instead
at `conversationAboutNotePage.ts`, trace
`replyToConversationAndInviteAiToReply` through
`noteActionsMethods.ts`'s `sendMessageToAI` to that step and
feature. Run the shared selector for the feature; keep the original support
path and this owner evidence in the brief. No feature edit is required.

Terminal-only CLI features use the [CLI helpers](#cli-terminal-helpers).

## Running and debugging

Use [running E2E tests](references/running-e2e.md) for environment assumptions,
checkout origins, focused commands, profiling and debugging.

## Technology And Structure

- Cypress for E2E testing.
- Cucumber plugin for BDD-style tests.
- TypeScript for type safety.
- Mountebank mocks external services.
- `e2e_test/features` contains BDD-style tests.
- `e2e_test/step_definitions` contains step definitions.
- `e2e_test/start/` contains page objects.
- `e2e_test/start/pageObjects/cli/` contains CLI page objects with the `cli.xxx` prefix.
- `e2e_test/support/` contains support files.

## Gherkin

- Group related features in domain-specific folders.
- Use `Scenario Outline` and an `Examples` table to parameterize a scenario.
- Use `@mockBrowserTime` when the scenario needs to control browser time.

## Step Definitions

Use TypeScript and Cucumber parameter types for complex objects.

## Page Objects

- Navigation (ADR 0005). E2E navigation goes through `e2e_test/start/router.ts`
  (`visitNamed` / named `push`). Compile hrefs with `namedLocationHref` /
  `noteShowHref`. Page objects and steps do not call `cy.visit` with SPA path
  strings (`scripts/check_e2e_spa_visit_gate.sh` in CI).
- Signing in only establishes the session; it does not open the app. The first
  `router().push` opens it, page objects that start from the Donut menu use
  `withinDonutMenuItem` (which calls `router().openApp()`), and a re-login
  reloads an app the scenario already opened. A scenario that never touches the
  web UI pays no page load.

  | Intent | Mechanism |
  | --- | --- |
  | Unique trigger **is** in-app navigation | UI |
  | Given-shaped shortcut (including Gherkin `When I visit …` when the unique behavior is **on** that screen) | Named `router.push` after first load |
  | First SPA load, inbound URL, or **explicit remount** | `cy.visit` of href **compiled from the named table** |

- Recall: `visitRecallPage` is remount (`visitNamed('recall')`);
  `navigateToRecallPage` is sidebar UI. Gherkin `When I visit recall`
  stays remount — do not convert it to sidebar.
- Note-level assimilation actions, tracker status, Understanding success, and
  sequence controls live under `[data-testid="note-assimilation-modes"]`
  (`assimilationPage/shared.ts`). Scope those queries to that root. Property-row
  assimilation stays under property-panel helpers
  (`noteRichPropertyAssimilationMethods` / `clickPropertyPanelAction`), not a
  note-level ancestry filter.

## CLI terminal helpers

CLI steps use `cli.xxx` page objects, not `start.xxx`. For the install feature, use `cli.backend()`, `cli.installation()`, and `cli.nonInteractiveOutput()` for spawned `version` / `update` stdout. The `@interactiveCLI`-tagged CLI files (`cli_access_token.feature`, `cli_interactive_mode.feature`, `cli_recall.feature`) and `cli_gmail.feature` are `@ignore` in CI; extend Vitest under `cli/tests/interactive/` for interactive TTY coverage instead of adding a second Cypress PTY harness.

```typescript
Then("I should see {string} in the non-interactive output", (expected: string) => {
  cli.nonInteractiveOutput().expectContains(expected)
})
```

## Waiting until the app is not busy

Product loading UI that means unfinished work marks itself with `data-app-busy` (`LoadingThinBar` from `apiCallWithLoading`, `ContentLoader` for pending page data, `LoadingModal` for `blockUi`). See [frontend loading UI](../frontend/SKILL.md#api).

When the busy marker is the relevant completion signal, call
`waitUntilAppIsNotBusy()` from `e2e_test/start/pageBase.ts` in the page object
before the next step that needs the completed work. When the scenario observes
pending work, return control after the trigger so it can make that observation
and release any held response before the page object checks for completion. Follow
the [shared synchronization guidance](../dough-page-objects/references/convention.md)
for scenarios whose proof includes network behavior.

## Test Data And Service Mocking

Follow [test data and service mocking](references/test-data-and-service-mocking.md) when writing Given setup or external-service fixtures.

## Learning-session scheduling

For commissioned learning-session reports, use `recordedAt` from the
`POST /api/learning-sessions/record` response as the time authority. Check that
each graded tracker's next recall is after that time. Backend tests cover the
exact scheduling arithmetic. The browser clock and the backend day/hour
steps can move separately.

## Before hook order

`@badeball/cypress-cucumber-preprocessor` defaults Before hooks to order `10000`. The shared DB reset Before uses **`order: 0`** so it runs before tagged setup. Hooks that start long-lived side effects (e.g. `@interactiveCLI` PTY at order `2`) must stay **after** that reset; otherwise truncate can block on MySQL locks held by the PTY’s HTTP traffic.

Follow [Open Dough failure guidance](../dough-slice-planning/references/architectural-thinking.md#runtime-failure-handling)
when handling exceptions in steps and page objects.

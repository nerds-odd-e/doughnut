---
name: e2e-authoring
description: E2E testing with Cypress and Cucumber, shared browser test guidance, feature files, step definitions, named navigation, assertions, app-busy waits, test execution. Use when writing Cypress/Cucumber E2E tests.
paths:
  - "e2e_test/**/*.feature"
  - "e2e_test/**/*.ts"
---
# E2E Authoring Rules

Use this skill when writing or modifying E2E tests, Cypress/Cucumber feature files, step definitions, page objects, or E2E assertions. For OCR-specific canvas assertions, use the `e2e-ocr` skill.

## Select browser test guidance

For browser authoring, extension and implicated refactoring, follow
[Dough Page Objects](../dough-page-objects/SKILL.md) with the owning feature
source path. Its shared entry selector decides whether to load the convention;
MakeMe is an independent selection. Other entries retain their practice.

When only a Cucumber step, page object or other support source changes, trace
its changed operation through callers and step wording to the owning browser
feature(s), and select those feature paths. Do this even when `--changed`
lists only support sources: they do not themselves match the feature entry.
Carry each edited support path, owning feature path and caller/step evidence
into the implementation or refactor brief. For new support, identify the
intended owning feature before applying the convention. Follow the shared
unresolved-rule handling if ownership, selection or selected guidance is
unresolved; leave only convention-dependent work unperformed.

For example, `conversation.ts`'s “I start a conversation about the note …
inviting AI …” step belongs to
`e2e_test/features/messages/conversation_about_a_note.feature`. Starting instead
at `conversationAboutNotePage.ts`, trace
`replyToConversationAndInviteAiToReply` through
`noteConversationAndQuestionMethods.ts`'s `sendMessageToAI` to that step and
feature. Run the shared selector for the feature; keep the original support
path and this owner evidence in the brief. No feature edit is required.

Apply browser conventions only to browser work. A terminal-only CLI feature
may select `pageObjects` through its entry but keeps the
[CLI helpers](#cli-terminal-helpers). A frontend unit test's Chromium runtime
alone does not select page objects.

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

## Focus on Behavior

**Behavior** (external behavior) has 3 parts: **pre-condition**, **trigger**, and **post-condition**. Each part is:

  * externally observable
  * of user value
  * typically involves state change.

Avoid e2e test that is only about the presentation of a state and having no state change after setup / given.

## Gherkin

- Write features in Gherkin syntax focused on behaviors that has business value.
- Group related features in domain-specific folders.
- Name feature files and titles by domain/capability, such as `note_creation.feature` or `spaced_repetition.feature`. Never name them by planning sequence number or delivery order.
- Use tags, such as `@usingMockedOpenAiService` or `@mockBrowserTime`, to control test execution.
- Keep scenarios focused and concise.
- Prefer `Scenario Outline` with an `Examples` table when the path and assertion focus are the same and only data varies. Keep separate scenarios when observable behavior differs, with the condition in the step wording (for example slash command, plain line, Enter alone or ESC).

## Step Definitions

- Use TypeScript and reuse steps where practical.
- Use parameter types for complex objects.

```typescript
When("I start a conversation about the note {string}", (noteTopology: string) => {
  start.jumpToNotePage(noteTopology).startAConversationAboutNote()
})
```

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

E2E waits with `waitUntilAppIsNotBusy()` from `e2e_test/start/pageBase.ts` (also `start.waitUntilAppIsNotBusy()`):

```typescript
import { waitUntilAppIsNotBusy } from '../pageBase'

cy.findByRole('button', { name: 'Submit' }).click()
waitUntilAppIsNotBusy()
```

Rules of thumb:

- Call `waitUntilAppIsNotBusy()` in the page object **on the action that starts loading**, before the next step that needs that work’s result.
- Prefer asserting a **user-visible** success outcome when the product provides one; use the waiter for “busy cleared,” not as a substitute for success.
- Do not use hardcoded waits (`cy.wait(ms)`) for this.
- Do not skip the wait and rely on a later navigation’s loading check alone — that races when the next step does not go through busy UI.
- Network `cy.intercept` / `cy.wait('@…')` is optional; it is not the documented default stand-in for this product/E2E pair.

## Test Data And Service Mocking

Follow [test data and service mocking](references/test-data-and-service-mocking.md) when writing Given setup or external-service fixtures.

## Assertions

Error messages must be immediately actionable:

1. Assert with clear domain meaning, not just technical selectors.
2. Show expected and actual values when they differ.
3. Include relevant context, such as page content or API response, when helpful.
4. Given, When, and Then steps should be assertive and fail early.
5. For commissioned learning-session report scheduling, use the UI report
   response's `recordedAt` (`POST /api/learning-sessions/record`) as the time
   authority and assert each graded tracker’s next recall is after that time.
   Do not pin an exact hour spread in E2E; backend tests own scheduling
   arithmetic. Browser `Date.now()` is not that authority when
   `@mockBrowserTime` and day/hour steps move backend time separately.

```typescript
expectQuizScore(expectedScore?: string) {
  if (expectedScore) {
    cy.get("[data-test='quiz-score']").should(($score) => {
      const actualScore = $score.text().trim()
      expect(
        actualScore,
        `Expected quiz score to be ${expectedScore}, but found ${actualScore}`
      ).to.equal(expectedScore)
    })
  }
  return this
}
```

## Before hook order

`@badeball/cypress-cucumber-preprocessor` defaults Before hooks to order `10000`. The shared DB reset Before uses **`order: 0`** so it runs before tagged setup. Hooks that start long-lived side effects (e.g. `@interactiveCLI` PTY at order `2`) must stay **after** that reset; otherwise truncate can block on MySQL locks held by the PTY’s HTTP traffic.

## Pitfalls

- Do not put complex logic in step definitions.
- Do not create long scenarios with many steps.
- Avoid using UI language like "click a button" in Gerhkin. Use intention and domain language. 
- Do not rely on test order.
- Do not use hardcoded waits.
- Do not fire a user action that starts app-busy loading and then assert domain state without `waitUntilAppIsNotBusy()` (or a clearer user-visible success signal) first.
- Failure handling in steps and page objects: ADR 0006.
- Do not mix different levels of abstraction in scenarios.

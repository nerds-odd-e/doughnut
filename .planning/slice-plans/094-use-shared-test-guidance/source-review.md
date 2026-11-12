# Source-only review

Reviewer launch: collaboration.spawn_agent, task `/root/source_review`,
`fork_turns: none`, 2026-11-12. Input was only SEED-126's original Goal,
Scope, key examples and exclusions, supplied in the launch message; no plan,
product, suspected omission or slice detail. The reviewer read no files and
changed nothing. Its numbered commitments are retained below; alternative
surfaces in 23 remain alternatives.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | Obtain MakeMe conventions from installed Open Dough guidance for entries that enable MakeMe. | slice 1 | observed: four local fixture sections → dough-makeme → test-entry-rules → selected convention; baseline selector terminal pass |
| 2 | Obtain browser page-object conventions from installed Open Dough guidance for entries that enable page objects. | slice 2 | observed: E2E Select browser test guidance → dough-page-objects → shared selector → convention for owning feature |
| 3 | Provide one authoritative convention to follow. | slice 2 | observed: five audited local skills route generic conventions to installed owners; one-time whole-guidance sweep |
| 4 | Keep Doughnut-specific integration details beside the relevant stack guidance. | slice 2 | observed: local TS imports, Java persistence/FK closure, Cypress named navigation/busy wait and CLI helpers retained beside stack guidance |
| 5 | Audit unit-testing guidance. | slice 1 | observed: unit-testing Test-data guidance and retained Stable boundary, Focused assertions, Removal proof, FK closure |
| 6 | Audit frontend-testing guidance. | slice 1 | observed: frontend-testing Test-data guidance, SDK mock boundary and self-contained import example |
| 7 | Audit backend-testing guidance. | slice 1 | observed: backend-testing Test-data guidance and Java construction, transaction/refresh and FK-closure link |
| 8 | Audit CLI guidance. | slice 1 | observed: cli Vitest observable behavior, fixture routing and retained terminal helpers |
| 9 | Audit E2E-authoring guidance. | slice 2 | observed: E2E authoring/linked mock guidance audited; running responsibility cohesively extracted with entrypoint/backlink |
| 10 | Include linked guidance in the audit. | slice 2 | observed: 20 local links/anchors across five local skills, shared entries/rules and E2E running/mock references resolve and destinations read |
| 11 | Remove duplicated convention text. | slice 2 | observed: generic MakeMe and browser fluent/ownership/variant prose replaced by routes; final one-time sweep classified canonical/local consumers |
| 12 | Route authors and refactorers to installed Open Dough guidance. | slice 2 | observed: fixture/browser sections explicitly route ordinary authoring, extension and implicated refactoring including support-only --changed |
| 13 | Retain Doughnut-specific imports in their existing homes. | slice 1 | observed: frontend-testing import example and CLI SDK data paragraph; representative TS sources import donut-test-fixtures/makeMe |
| 14 | Retain Java persistence requirements in their existing homes. | slice 1 | observed: backend-testing Java construction matches EntityBuilder.please/inMemoryPlease; TextContentValidatorTest example |
| 15 | Retain Java data-integrity requirements in their existing homes. | slice 1 | observed: unit-testing destructive-operations FK closure and backend-testing real database/refresh rules |
| 16 | Retain Cypress navigation guidance in its existing home. | slice 2 | observed: E2E Page Objects retains router named push/visitNamed, named hrefs, session/openApp and recall remount distinction |
| 17 | Retain Cypress busy-wait guidance in its existing home. | slice 2 | observed: E2E Waiting until app is not busy retains action-owned wait; conversation reply action waits before subsequent observation |
| 18 | Retain CLI-specific helpers in their existing homes. | slice 1 | observed: cli SKILL retained terminal helper, HTTP mock, timing and output assertions |
| 19 | Verify representative frontend MakeMe sources. | slice 1 | observed: frontend noteStore.trashNote.spec.ts import/API-shaped fixture reading plus selector makeMe true/pageObjects false |
| 20 | Verify representative CLI MakeMe sources. | slice 1 | observed: cli contestAndRegenerateMcq.test.ts import/SDK data reading plus selector makeMe true/pageObjects false |
| 21 | Verify representative Java MakeMe sources. | slice 1 | observed: TextContentValidatorTest SpringTestBase/inMemoryPlease reading plus selector makeMe true/pageObjects false |
| 22 | Verify Cucumber browser authoring. | slice 2 | observed: conversation_about_a_note.feature → conversation step → sendMessageToAI → reply action/wait; expected messages caller-owned |
| 23 | Include a change touching only one of these alternative support surfaces: a Cucumber step; a browser page object. | slice 2 | observed: separate step-only and page-object-only walkthroughs and selector commands in /tmp/seed126-slice2-proof.md pass without feature edits |
| 24 | Confirm how the owning feature selects the shared convention before retiring the local rule. | slice 2 | observed: E2E explicit support/owner evidence; selectors show support off and owning browser feature pageObjects true, with new-support owner-first route |
| 25 | Check the scripts entry. | slice 1 | observed: baseline selector scripts/test/run.sh.test and scripts/test/path-routing/path-routing-validation.test.mjs both rules false |
| 26 | Keep browser guidance scoped to browser work. | slice 2 | observed: E2E browser-only applicability and frontend Chromium qualification; CLI feature opt-in read with terminal surface |
| 27 | Preserve per-entry selection. | slice 1 | observed: installed selector resolves concrete test source membership; local routes defer to its handling |
| 28 | Preserve MakeMe as an independent opt-in. | slice 1 | observed: baseline unit sources makeMe true/pageObjects false and feature sources inverse |
| 29 | Preserve page objects as an independent opt-in. | slice 1 | observed: baseline features pageObjects true/makeMe false and support/scripts neither |
| 30 | Retain established practice for CLI terminal helpers. | slice 2 | observed: E2E CLI terminal helpers keeps cli.backend/installation/nonInteractiveOutput and interactive Vitest guidance |
| 31 | Retain established practice for script tests. | slice 1 | observed: unit-testing explicitly retains Bach/Node fixtures; both existing script setup examples inspected |
| 32 | Follow each retained guidance link during review. | slice 2 | observed: all 20 local links/anchors and moved relative links resolve; guidance destination walkthrough complete |
| 33 | Review representative examples. | slice 2 | observed: representative frontend/CLI/Java/script examples from slice1 plus conversation feature/support/terminal examples inspected |
| 34 | Check that shared conventions and local integration together cover the workflow. | slice 2 | observed: shared generic conventions plus retained local imports/persistence/navigation/loading/helpers cover explicit source journeys |
| 35 | Perform a one-time reading sweep for duplicated convention text. | slice 2 | observed: read-only whole-product guidance sweep by browser_guidance/guidance_sweep, 2026-11-12, plus consumer trace; canonical/local examples classified |
| 36 | Include dependent references in that reading sweep. | slice 2 | observed: same sweep covers dependent references and retired E2E headings; all202 slice2 consumers dispositioned, moved executable examples exact |

## Qualifications and journey interpretation returned by the reviewer

The intended user may be a test author or a refactorer. Shared MakeMe applies
to entries enabling it; shared page objects additionally require browser work.
Changes are limited to guidance and applicability routing. Existing builders,
page objects and behavioral coverage supply examples. Support-only selection
must be established through owning features, never folder proximity.

- Frontend: an enabled `*.spec.ts` author follows local guidance to select and
  load shared MakeMe and receives the TypeScript import locally.
- CLI: an enabled `*.test.ts` or `*.test.tsx` author follows the same route,
  receiving the TypeScript import locally; the alternatives remain alternatives.
- Java: an enabled `*Test.java` author selects and loads shared MakeMe through
  local guidance and receives construction and persistence details there.
- Browser feature: an enabled Cucumber feature author follows E2E guidance;
  shared conventions govern browser mechanics, with named navigation and
  app-busy waits locally available.
- Support only: an existing browser step or page object changes with no feature
  edit; author or refactorer identifies owning browser features, checks their
  selection and makes the support-to-owner relationship explicit.
- Other surfaces: a script test or terminal-only CLI Cucumber work changes;
  actual entry and surface are checked, fixture practice and terminal helpers
  remain available without browser abstractions.
- Reviewer: retained links and examples lead to the complete shared-plus-local
  workflow; the one-time reading sweep includes dependent references.

Exclusions returned: no test-suite conversion, builder changes, page-object
changes, product-behavior changes, shared-convention changes or permanent
absence checker.

---
id: SEED-126
status: active
planted: 2026-11-12
scope: story
---

# Use shared MakeMe and page-object guidance

<a id="use-shared-test-guidance"></a>

### Use shared MakeMe and page-object guidance

**Identity:** SEED-126#use-shared-test-guidance
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/094-use-shared-test-guidance/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"72b8b8404acc1577a4f0c7a5339edde05b3950e4264d7b16d27796867ee2d759","plan":"38befc58c75e4ef6f3ecd4d03bb4fc8d4aa16f1ca9e07543b62d357e8fe11dac"}}
```

#### Goal

A Doughnut test author or refactorer gets the MakeMe and browser page-object
conventions from Open Dough for the test entries that enable them, with one
authoritative convention to follow and the Doughnut-specific integration
details still available beside their stack guidance.

#### Scope

- Audit [unit testing](../../.agents/skills/unit-testing/SKILL.md),
  [frontend testing](../../.agents/skills/frontend-testing/SKILL.md),
  [backend testing](../../.agents/skills/backend-testing/SKILL.md),
  [CLI](../../.agents/skills/cli/SKILL.md), and
  [E2E authoring](../../.agents/skills/e2e-authoring/SKILL.md), including
  their linked guidance. Remove duplicated convention text and route to the
  installed Open Dough guidance. Keep Doughnut-specific imports, Java
  persistence and data integrity requirements, Cypress navigation and busy
  waits, and CLI-specific helpers in their existing homes.
- Verify representative frontend, CLI, and Java MakeMe sources and Cucumber
  browser authoring. Include a change that touches only a Cucumber step or
  browser page object and confirm how its owning feature selects the shared
  convention before retiring the local rule. Check the scripts entry and
  keep browser guidance scoped to browser work.
- Limit the change to guidance and any routing needed to preserve applicability.
  Existing builders, page objects, and behavioral test coverage provide the
  examples for verification.
- Shared selection remains per test entry: MakeMe and page objects are
  independent opt-ins. Browser page-object guidance applies to browser work;
  CLI terminal helpers and script tests keep their established practice.
- This story does not convert test suites, change builders or page objects,
  alter product behavior, or change the shared Open Dough conventions.

#### Key examples

- A frontend `*.spec.ts`, CLI `*.test.ts`/`*.test.tsx`, or Java `*Test.java`
  source belongs to a MakeMe-enabled entry → its author follows the local
  testing guidance → the guidance selects and loads shared MakeMe, then supplies
  the TypeScript import or Java construction/persistence details locally.
- A browser Cucumber feature belongs to the page-object-enabled entry → its
  author follows E2E guidance → shared page-object conventions govern browser
  mechanics while Doughnut's named navigation and app-busy waiting rules remain
  available locally.
- Only an existing browser Cucumber step or page object changes, with no feature
  edit → the author or refactorer identifies the owning browser feature(s) and
  checks their entry selection → the same shared convention applies, with the
  support source and owning feature relationship explicit. Folder proximity is
  not treated as a successful selector result.
- A script test, or terminal-only CLI Cucumber work, is being changed → its
  actual entry and surface are checked → script tests retain their fixture
  practice and terminal helpers retain CLI-specific guidance without acquiring
  browser abstractions.
- A reviewer follows each retained guidance link and representative example →
  the shared convention and local integration rules together cover the workflow.
  Acceptance includes a one-time reading sweep for duplicated convention text
  and its dependent references; it adds no permanent absence checker.

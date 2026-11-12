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
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

#### Goal

A Doughnut test author or refactorer gets the MakeMe and browser page-object
conventions from Open Dough for the test entries that enable them.

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

---
id: SEED-071
status: dormant
planted: 2026-10-09
planted_during: owner-requested ownership review and prioritization of Donut retrospective findings
trigger_when: selecting the highest-priority project-specific retrospective follow-ups
scope: medium
---

# SEED-071: Get useful Donut feedback without recurring project setup and removal rework

## Why This Matters

Donut contributors repeatedly lose behavioral feedback to the repository's
frontend formatting gate and repeat removal-proof work that conflicts with
the owner's local deletion rule. Both problems recurred across executions.
Their active evidence and priority assessment live in
[Donut Retrospective Findings](../../DonutRetrospectiveFindings.md).

## Story Decomposition

<a id="e2e-proof-before-final-formatting"></a>

### Run Donut E2E proof before final formatting

**Identity:** SEED-071#e2e-proof-before-final-formatting
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planless","assessment":"ready","reasons":[],"basis":{"document":"1e460f3e8343f4ce1817258beba34bc7294663d859a6f3af21a97494e23d8b1a"}}
```

- **For / why:** Donut contributors get behavioral feedback from the ordinary
  local Cypress wrapper while a frontend change is still being implemented.
- **Goal:** A contributor whose touched frontend files are behaviorally valid
  but not yet Biome-formatted gets scenario feedback from
  `pnpm cy:run --spec <feature>` during implementation, so the
  coordinator-owned final formatting pass no longer blocks slice proof. The
  broader ambition, reliable early feedback for all frontend work, is served
  by this one startup correction.
- **Scope:**
  - **Observed cause (2026-10-09):** the formatting gate is not Donut's
    checker configuration as such. The owner's login shell exports
    `NODE_ENV=production`, and the Nix shell inherits it. Vite treats
    `vite --host` as production when that variable is set, and
    `vite-plugin-checker` then leaves its serve-mode logging for its
    build-mode gate: it spawns `biome check`, `tsc`, and `vue-tsc`, and exits
    the dev server on any error, including a format-only diff. `run-p`'s race
    flag stops the whole SUT group, and the wrapper reports
    `SUT readiness failed`. With `NODE_ENV` unset, the same start stays up:
    the serve-mode checker runs `biome lint`, which does not report
    formatting, and diagnostics go to the terminal (`sut.log`). CI sets no
    `NODE_ENV`, so it already runs in that mode.
  - **Required:** the repository-owned E2E entry point starts the frontend dev
    server in Vite's development mode regardless of the invoking shell's
    `NODE_ENV`, on the Nix shell and on the Cloud VM alike. The local
    development stack (`pnpm dev`) shares the same frontend dev-server start
    and receives the same mode; no separate verification is committed for it.
    Local command guidance that still describes a Biome startup check or a
    format-before-`cy:run` workaround is corrected.
  - **Preserved:** the lint and typecheck gate (`frontend:lint`,
    `lint:changed`, and the check-only commit hook) still rejects the
    unformatted file until it is corrected; the coordinator's single
    `format:changed` pass stays the only routine formatting step. The
    serve-mode checker keeps logging Biome and TypeScript diagnostics to
    `sut.log`, as the existing `vite.config.ts` comment intends. Genuine
    application-readiness failures and scenario failures still fail the run.
  - **Decision:** a type-only error no longer stops the E2E stack; it is
    logged in `sut.log` and rejected by the lint gate. This is the existing CI
    behavior and the stated intent of the checker configuration ("linting and
    type checking should be done separately, not during test execution"). A
    file Vite cannot transform (syntax error, unresolved import) still fails
    the scenario visibly through the browser.
  - **Not committed:** no new formatting or Biome step in the E2E wrapper; the
    wrapper never rewrites source files; no change to the Vitest unit-test
    path, whose own `NODE_ENV` caveat stays in the agent map; no change to the
    owner's shell environment; no retrospective-finding bookkeeping here,
    which story wrap-up owns.
- **Key examples:**
  1. A touched frontend file is valid but unformatted (an extra-space
     `export const` in `src/main.ts`), the shell exports
     `NODE_ENV=production`, and the contributor runs
     `pnpm cy:run --spec e2e_test/features/users/new_user.feature` → the SUT
     becomes healthy, the scenario runs and passes, and the file is left
     exactly as written. Today this run stops in about eight seconds with
     `"frontend:sut" exited with 1` and `SUT readiness failed (exit 1)`
     (reproduced 2026-10-09); the same run with `env -u NODE_ENV` passed one
     scenario in five seconds without touching the file.
  2. The same unformatted file → `pnpm lint:changed` (or `frontend:lint`) →
     still reports `Formatter would have printed the following content` until
     the file is formatted.
  3. A scenario assertion genuinely fails → the run exits non-zero with the
     Cypress failure report, unchanged.
  4. A touched frontend file has a syntax error → Vite cannot serve the module,
     the browser shows the transform error, and the scenario fails visibly.
- **Findings:** [ODF-215 / former DD-211](../../DonutRetrospectiveFindings.md#e2e-proof-formatting).
  Three executions on October 5–7 encountered the startup gate; the last
  repeated it after an earlier delegation-only workaround. All three ran in
  the owner's shell.
- **Value / learning:** Restore reliable early feedback for frontend work,
  including the near-future voice-input stories, and remove one local/CI
  divergence instead of hiding its symptom.
- **Effort hypothesis:** S (30–60 minutes), high confidence: the cause is
  reproduced, the correction is an environment fix in the Node entry points
  with a focused script test, and the E2E proof command already exists.
- **Depends on:** None identified; the commit-hook performance story and
  spoken-title rename story can proceed independently.
- **Safe stopping point:** The ordinary Donut E2E entry point gives behavioral
  feedback during implementation and the existing delivery checks retain
  their authority.

<a id="removals-follow-project-rule"></a>

### Deliver Donut removals under the project's deletion rule

**Identity:** SEED-071#removals-follow-project-rule
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** The Donut owner and contributors can delegate a removal
  without paying for a test or assertion that another agent immediately deletes.
- **Goal:** Refinement, planning, and delivery apply Donut's existing deletion
  and whole-product sweep rule consistently from the first handoff.
- **Scope:** Correct the local context or handoff gap evidenced by DD-205 in
  Donut-authored guidance and conventions. `AGENTS.md` and `CLAUDE.md` already
  carry the rule, including every role; another copy of that text alone does
  not establish the outcome. Keep them in sync when changed. Scope and slices
  express deletion, transitive cleanup, and the whole-product sweep; retained
  proof covers surviving behavior and refusals the product actively enforces.
  Shared Open Dough skills continue to allow negative assertions for actual
  promises; this work applies Donut's local removal convention.
- **Evaluation:** A fresh refinement/planning/delivery handoff for the two
  recorded shapes—deleting the dictation model-rewrite chain and deleting the
  obsolete Development PID-file mechanism—produces deletion and sweep work
  with meaningful surviving-behavior proof from the outset. An active notebook
  authorization refusal retains its refusal proof. Evaluate the handoff's
  artifacts directly rather than adding permanent absence checks to Donut.
- **Findings:** [DD-205](../../DonutRetrospectiveFindings.md#local-removal-proof).
  This is a recurrence after SEED-068#removal-rule-in-guidance, delivered in
  `18b99b6424` and `52460a4077`; two later executions contradicted its intended
  effect. The completed story is historical provenance, not a current backlog
  item or a reason to repeat its implementation.
- **Value / learning:** Eliminate repeated proof churn and keep the owner's
  local removal convention effective across agent roles.
- **Effort hypothesis:** S–M (30–120 minutes), low confidence until refinement
  identifies the handoff gap; the known fix of adding the rule is already done.
- **Depends on:** None identified; independent of the E2E-startup story.
- **Safe stopping point:** The local handoffs consistently produce deletion,
  sweep, and surviving-behavior proof while preserving active refusal coverage.

## Ordering and Scope

The owner requested the highest-priority one or two project-specific finding
groups first. E2E startup ranks first because its three occurrences block all
selected behavioral scenarios before feedback begins. Removal-proof churn ranks
second: two post-correction executions plus the original finding establish
recurrence, but the measured effect is limited to proof written and deleted.
The MinerU environment and unreproduced stale-class findings remain unqueued.
The current Taken story and near-future direction retain their existing scope.

These are candidate stories for refinement and approach selection. This seed
provides no executable plan or implementation authorization.

## Breadcrumbs

- Owner's 2026-10-09 request to separate shared and project findings, confirm
  resolutions, group recurrence, and queue the top one or two local stories.
- [Project findings and ownership review](../../DonutRetrospectiveFindings.md).
- [Shared-process findings](../../DearDough.md).

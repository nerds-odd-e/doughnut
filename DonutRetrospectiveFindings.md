# Donut Retrospective Findings

Findings specific to Donut’s product, repository tooling, or local conventions.
Findings about shared OpenDough skills, supporting guidelines, and skill scripts
remain in [DearDough.md](DearDough.md), including those observed in this project.
Existing finding identifiers and occurrence evidence are preserved.

## DD-073 — The focused frontend proof command does not typecheck, so an implementation agent's green proof still failed the commit gate

This project's focused frontend test command, which `dough-execute-plan`
delegation passes to implementation agents as the literal proof command, is
`pnpm frontend:test <spec>` → `pnpm -C frontend test`, which runs Vitest only.
Type checking lives in a different script: `pnpm -C frontend lint`/`format`
runs `biome check . && vue-tsc --noEmit`. A slice 2 component test written by
the implementation agent called
`mockSdkService(NotebookBooksController, "getBook", null)` where the generated
SDK type is `BookFull | undefined`. Vitest passed, so the agent returned a
green `proof:` block in good faith, the coordinator accepted the proof after
inspecting the assertions, and the type error only surfaced at the
coordinator's `format:changed` step — after the refactor pass had already run
against that code.

### Occurrences

- Execution: SEED-030 story 2 / quick/145-reset-notebook-git-history
  - Timestamp: 2026-09-18, ~17:41 +08:00 (slice 2 delivery, at the selective
    formatting step before staging)
  - Tool: Claude Code
  - Model: claude-opus-5
  - Open Dough release: unknown
  - Evidence: `./scripts/run.sh pnpm format:changed` reported
    `tests/components/notebook/NotebookSettings.resetGitHistory.spec.ts:16:56 - error TS2345: Argument of type 'null' is not assignable to parameter of type 'BookFull | undefined'`
    and `Command failed with exit code 2`, while
    `pnpm frontend:test tests/components/notebook/NotebookSettings.resetGitHistory.spec.ts`
    had passed (1 file, 1 test) both before and after the one-word repair.
    `package.json` line 31 defines `frontend:test` as the Vitest run and line
    29 defines `frontend:lint` as the Biome + `vue-tsc` run.
  - Observed effect: caught before commit by the ordinary wrap-up sequence and
    repaired mechanically (`null` to `undefined`), costing one repair plus a
    spec rerun and a second formatting pass. Nothing reached CI broken.
  - Inference: the accepted-proof gate is weaker than it appears for frontend
    slices, because the proof command an agent is told to run cannot fail on a
    type error that will later block the commit. A delegated agent can
    truthfully report passing focused proof on code that does not compile under
    the project's own gate. Either the focused frontend proof command should
    include the typecheck, or delegation should name the typecheck as part of
    frontend proof rather than leaving it to coordinator wrap-up.

- Execution: SEED-031 story 1 / quick/147-resolve-deleted-failure-reports
  - Timestamp: unknown (2026-09-18, during slice 3 proof acceptance, before
    the coordinator's `format:changed` step)
  - Tool: Claude Code
  - Model: claude-sonnet-5
  - Open Dough release: 0.3.25
  - Evidence: implementation agent's accepted `proof:` block for slice 3
    reported `pnpm frontend:test tests/components/admin/FailureReportList.spec.ts`
    passing (9 tests) and `pnpm frontend:test` (full suite, 1901 tests)
    passing, with no typecheck run. The coordinator independently ran
    `pnpm -C frontend exec vue-tsc --noEmit` before accepting the proof and
    got two real errors at `FailureReportList.vue:219`
    (`TS2322`/`TS18048`) from assigning the new generated
    `FailureReportDeletionResultDto.unresolvedGithubIssueUrls?: Array<string>`
    field (optional) directly to a non-optional `ref<string[]>`.
  - Observed effect: caught only because the coordinator proactively ran the
    typecheck as an extra verification step outside the delegated proof
    commands, not because any routine gate required it at that point; a
    follow-up agent applied a one-line `data?.unresolvedGithubIssueUrls ?? []`
    fix. Had the coordinator not run it early, this would have surfaced later
    at `format:changed`, exactly as in the first occurrence.
  - Inference: same root cause as the first occurrence, recurring on a
    different project/slice — the delegated frontend proof command still does
    not typecheck. The coordinator's own initiative substituted for a missing
    protocol step; nothing in `delegation.md`'s proof-acceptance guidance
    named the typecheck as a required check for a slice that consumes a
    newly-regenerated, optional-by-default SDK field.

---
name: post-change-refactor
description: >-
  Refactor and clean up the uncommitted change before committing. Eliminate
  duplication, rename unclear domain concepts, collapse shotgun surgery when
  another similar change is likely, remove dead / test-only / redundant code,
  split files larger than 250 lines into cohesive modules, and confirm
  related tests still pass. Use after finishing a phase or sub-phase and
  before commit, or whenever the developer asks to clean up the current
  change. Triggers on: refactor change, clean up change, post-change
  refactor, before commit cleanup, tidy current change.
---

<objective>
Clean the **current uncommitted change** so it is cohesive, capability-named,
and free of speculative structure — then hand control back for commit.

Purpose: Local wrap-up gate required by `execute-plan` / `/gsd-execute-phase`
(see `.cursor/rules/gsd-coexistence.mdc`). Structure-only: no new behavior.

Output: Refactored working tree + short summary ending with
`## REFACTOR COMPLETE`. **Do not commit** — the caller commits.
</objective>

<context>
**Mandatory first read:** `.cursor/agent-map.md` (navigation + focused test commands).

**Scope — "the current change":** files touched since the last commit
(staged + unstaged + untracked), plus files that directly depend on them or
are depended on by them. Do **not** sweep unrelated parts of the repo.

Discover scope:

```bash
git status
git diff
git diff --cached
```

Whitespace hygiene: `scripts/check_diff_whitespace.sh` (not raw `git diff --check`) —
excludes generated API artifacts under `packages/generated/doughnut-backend-api/**`
and `open_api_docs.yaml`.

**Git does not use the Nix prefix.** All other repo tooling does:
`CURSOR_DEV=true nix develop -c …`

**Plan justification (decision boundary):**
Keep code justified by the **current change** or the **immediate next**
Behavior/Structure unit in the active plan
(`.planning/phases/*/`, `.planning/quick/*/`, or legacy `ongoing/*.md`).
Anything justified only by a later phase, or by "we might need it later",
is speculative — remove it. No plan → justification comes only from the
current change.

**Invokers:** `execute-plan` (fresh sub-agent before commit), `bug-fixing`,
`test-optimization`, or on-demand developer request.
</context>

<process>

<preflight_gate name="discover_scope">
Run the git discovery commands above. If there is no uncommitted change,
report empty scope and emit `## REFACTOR COMPLETE` with no edits.
</preflight_gate>

Run each check below **in order**. After all pass, return to the caller —
**do not commit** from inside this skill.

<step name="duplication">
- Look for **new** duplication introduced by the change (copy-pasted blocks,
  parallel structures with cosmetic differences).
- Look for duplication the change made **visible** — new code repeats logic
  that already existed elsewhere.
- The same concept in two representations counts as duplication, not just
  literal copies.
- **Action:** collapse onto a single representation. Prefer reusing an
  existing helper in the right layer (service, composable, step definition)
  over inventing a new one.
</step>

<step name="domain_naming">
- Read every new or renamed identifier — files, modules, classes, functions,
  variables, tests, Cypress feature files, fixtures.
- Ask: does the name match what a domain reader expects? Does it match
  Doughnut's ubiquitous language (notes, circles, assessments, etc.)?
- **Action:** rename when intent is unclear, misleading, mixes layers, or
  leaks phase numbers / sequence info. Names describe **capability**, not
  development history (`.cursor/rules/planning.mdc`).
</step>

<step name="shotgun_surgery">
- Shotgun surgery: **one logical concept** forces edits in many places.
- Estimate likelihood of another change of the same shape soon.
- **Action:**
  - **High likelihood** → consolidate behind a single seam (one function,
    one config, one module) so the next change touches one place. Do it now.
  - **Low likelihood** → leave it. Do not preemptively abstract.
</step>

<step name="dead_redundant_code">
Remove aggressively whatever the change introduced or exposed that is not
justified by the current change or the immediate next phase:

- Code with no caller.
- Unreachable branches.
- Pairs of edits that cancel each other (added then worked around, flags
  that never flip).
- Production code only exercised by unit tests — no real caller from a
  controller, mounted Vue component, CLI command, MCP tool, or other entry.
- Unit tests that overlap another test on the same observable surface
  (same input/output, same entry point).
- Tests that pin internal structure rather than observable behavior — prefer
  the test that drives a high-level entry point (controller, mounted
  component, Cypress scenario).

When in doubt, **delete**. The next phase will reintroduce only what it needs.
</step>

<step name="file_size">
For every file touched by the change:

```bash
wc -l <path>
```

- Files **over 250 lines** must be split (applies to test code too).
- Split along **cohesive seams** — one concept per module, not arbitrary
  line cuts.
- Update imports. Keep the public API stable for callers outside the change.
</step>

<step name="confirm_related_tests">
Run **related** tests for the changed files — not the whole suite.
Use `CURSOR_DEV=true nix develop -c …` for all commands except `git`.

| Area touched | Focused command |
|--------------|-----------------|
| Backend Java | `CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut....ClassName"` per affected class (or package pattern). If `backend/src/main/resources/db/migration/` changed → `CURSOR_DEV=true nix develop -c pnpm backend:test` |
| Frontend Vue/TS | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/path/to/File.spec.ts` |
| E2E (only if behavior under test changed) | `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/<name>.feature` |
| CLI | `CURSOR_DEV=true nix develop -c pnpm cli:test` (or narrower path under `cli/`) |
| MCP server | `CURSOR_DEV=true nix develop -c pnpm mcp-server:test` |

Prefer controller-level backend tests and mounted-component / E2E tests over
tests that only exercise internal helpers.

All related tests must pass before returning. If a test breaks because of
the refactor (not the original change), fix it now.
</step>

</process>

<success_criteria>
- Scope limited to the current change (+ direct dependents/dependencies)
- No speculative structure beyond current change / immediate next plan unit
- Duplication, naming, shotgun, dead-code, and 250-line checks applied
- Related focused tests green
- No commit created by this skill
- Final output includes `## REFACTOR COMPLETE`
</success_criteria>

<output>
Report a short summary to the caller, then the completion marker:

1. Which checks led to changes — duplication / naming / shotgun / dead code /
   file size (or "none — already clean").
2. Files renamed, extracted, split, or deleted.
3. Which related tests were run and confirmed passing.

```
## REFACTOR COMPLETE
```

Hand control back. **Do not commit** — the caller commits.
</output>

<out_of_scope>
- Do not redesign code outside the changed files.
- Do not start a new phase or add new behavior — Structure only.
- Do not run the entire test suite or trigger CI.
- Do not regenerate the OpenAPI client unless controller/DTO signatures
  changed as part of this refactor (use `generate-api-client` when needed).
</out_of_scope>

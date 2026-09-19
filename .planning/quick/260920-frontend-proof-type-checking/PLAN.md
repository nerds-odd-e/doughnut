# Require type checking before accepting frontend proof

Status: executed
Authority: owner instruction on 2026-09-19 to refine this accepted correction,
write a slice plan if clear, and remove its retrospective record when fixed.
Source: DD-073 in `DonutRetrospectiveFindings.md` at repository root.
Historical recovery: commit `bfa6ac0b05` contains the finding and both occurrences.
Provenance: SEED-030 story 2 / quick/145-reset-notebook-git-history and
SEED-031 story 1 / quick/147-resolve-deleted-failure-reports.
This plan is the canonical home of the bounded correction; no separate seed.

## Refined outcome

Donut contributors and execution agents can distinguish passing frontend
behavior tests from complete frontend proof. Before accepting that proof,
require both the appropriate behavioral tests and Vue/TypeScript checking to
pass against the same working-tree content, including generated API types.
This moves feedback before acceptance and refactoring rather than discovering
type errors for the first time during formatting or the commit gate.

The two historical occurrences establish real late repair costs. Current
inspection confirms that the gap remains: root `frontend:test` delegates to
Vitest, and `frontend/vitest.config.ts` deliberately disables its checker in
test mode. No production escape is established; lint, formatting and build
already run the typechecker. This is a small, low-urgency process correction.

## Scope and key examples

Include project-owned frontend proof guidance and its command discovery
pointers. Keep one authoritative rule in `.agents/skills/frontend/SKILL.md`;
align `.agents/skills/frontend-testing/SKILL.md` and `.agents/agent-map.md`
with links to that rule. Proof reports must identify both commands and results.
Re-run affected verification if subsequent edits invalidate the evidence.

- A component spec passes but a test fixture violates a generated SDK type:
  behavioral proof alone is incomplete; the typecheck must pass before acceptance.
- A component assigns an optional generated field to a required typed value:
  passing the full Vitest suite still does not complete frontend proof.
- The appropriate tests and typecheck both pass on the candidate content:
  accept frontend proof and continue the ordinary refactoring/delivery gates.
- A contributor iterates on one component: the existing focused Vitest command
  remains available without adding typechecking to every test run.
- After the correction is verified: delete DD-073 and its evaluation from
  `DonutRetrospectiveFindings.md`, without an archived/resolved copy there.

Preserve application behavior, focused file selection, watch/UI test modes,
and existing lint/build/format/commit gates. Exclude new runner scripts,
package aliases, changes to shared OpenDough skills, CI redesign, automatic
proof enforcement, and product/API changes. This guidance correction cannot
mechanically prevent an agent from ignoring the rule.

## Existing solutions and decisions

PFE: `frontend/package.json` already uses `vue-tsc --noEmit` in lint, format,
and build; `frontend/tsconfig.json` covers application and test sources.
Reuse that checker directly:

```sh
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
```

`frontend:verify` already combines tests and a production build, but that build
is unnecessary solely for this correction. Keeping the existing commit gate
alone retains the observed late-feedback loop. Adding a checker to every
Vitest run undermines the deliberate fast iteration path. No new abstraction
or dependency is justified.

This local documentation correction changes no architecture or domain policy.
The ADR index and `.planning/NORTH-STAR.md` were inspected; the notebook
synchronization direction does not constrain this frontend proof correction.
No ADR or North Star change is required. No unresolved scope decisions remain.

## Execution slice

### 1. Accept frontend proof with both behavior and type evidence

Type: Structure
Status: done
Outcome: correct the evidenced proof-acceptance weakness, preserve the fast
behavioral test path, and remove the resolved finding after verification.

Delivered: added a "Frontend proof" rule to `.agents/skills/frontend/SKILL.md`
requiring `pnpm -C frontend exec vue-tsc --noEmit` alongside `frontend:test`
before accepting frontend proof, with reuse-existing-evidence guidance; pointed
`.agents/skills/frontend-testing/SKILL.md` and `.agents/agent-map.md` at that
rule; deleted the DD-073 section from `DonutRetrospectiveFindings.md`. A fresh
post-change-refactor pass found nothing to change (single-source-plus-pointers
shape and doc accuracy already matched target).

Proof:
- `CURSOR_DEV=true nix develop -c pnpm frontend:test` — 339 files / 1901 tests
  passed.
- `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit` — no
  errors.
- Diff inspected: only the three guidance files and the findings file changed;
  `package.json`, `vitest.config.ts`, and delivery gates untouched.

Update the three project-owned guidance locations above. Require the separate
Vue/TypeScript command in addition to behavioral proof before acceptance, with
both results reported for the candidate content. Existing adequate typecheck
evidence from a build/lint run on identical content may be reused; do not repeat
checks merely to obtain a differently named command.

Proof ownership:

- The historical DD-073 occurrences already demonstrate both failure examples;
  retain that provenance here until wrap-up. No synthetic regression suite is
  needed for a documentation correction.
- Inspect the changed guidance against every example above: a Vitest-only
  report must be explicitly insufficient, while focused iteration stays valid.
- Run `CURSOR_DEV=true nix develop -c pnpm frontend:test` and
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`.
  Record literal commands and results. A failure leaves acceptance incomplete;
  diagnose ownership instead of expanding into unrelated frontend fixes.
- Inspect the diff to confirm package scripts, Vitest configuration, and delivery
  gates are unchanged. This proves preservation of focused test invocation.
- Only after those checks pass, delete the entire DD-073 section, occurrences,
  and initial evaluation from the findings file. Retain the file's heading and
  scope introduction for future findings. Check references so no active link
  targets a deleted finding or evaluation anchor. Do not copy the record to an
  archive or add a resolved-finding entry.

Sizing: approximately five minutes of edits and review, plus the existing full
frontend suite and typecheck runtime. Test runtime is an explicit exception to
the five-minute target/ten-minute decomposition limit; avoid splitting one
cohesive guidance correction into per-file slices. Reassess unexpected edit or
diagnosis work exceeding those bounds.

Delivery: follow Jidoka, a fresh post-change-refactor agent, coordinator-only
`./scripts/run.sh pnpm format:changed` once, plan status update, commit with the
check-only hook, and push through `dough-execute-plan`. API generation is not
needed for the intended scope. Keep the correction queued until execution starts.

Safe stopping point: before successful verification, DD-073 remains open.
After delivery, guidance carries the lasting rule and the finding is absent.
Ordinary execution retrospective and story wrap-up remove this spent plan and
its backlog entry; preserve other findings and unrelated work. Git retains the
historical evidence without a new permanent completion record.

## Planning assessment

One concept and one slice; no additional structure or preparatory slice is
needed. No slice-specific design concerns identified. Full-suite/typecheck
runtime has not been measured for this correction and is not a speed promise.

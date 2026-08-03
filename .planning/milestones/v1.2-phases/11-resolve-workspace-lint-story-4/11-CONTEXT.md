# Phase 11: Resolve workspace lint (story 4) - Context

**Gathered:** 2026-08-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Apply Phase 7’s Story 4 verdict (**strengthen**) in the tree so `/lint` matches story 4 acceptance: the check identifies malformed frontmatter, duplicate identities, broken local links, missing indexes, and unsupported path mappings; unknown frontmatter properties stay accepted; findings name the affected file with an actionable explanation; a valid workspace produces a clear successful result. Do not remove the capability. Do not change Terry Yin / Tan Yeong Sheng work (HYG-02). Do not implement Stories 5–6 push behavior.

</domain>

<decisions>
## Implementation Decisions

### Gap coverage (LINT-01)
- **D-01:** Phase 11 closes **all four** TRIAGE Story 4 gaps: (1) **duplicate identities**, (2) **broken local links**, (3) **missing indexes**, and (4) **unsupported path mappings**. Keep already-green OKF behavior: malformed frontmatter, unknown keys accepted/preserved (read-only), actionable `path[:line] severity message` lines, and a clear success result when findings are empty of errors. Partial “one-gap-only” is not enough for LINT-01. — **Reversibility:** costly — shipping a subset leaves LINT-01 incomplete and invites a second Story 4 phase.

### Portable contract on top of OKF
- **D-02:** Keep the existing OKF rule modules (`okfConcept` / `okfIndex` / `okfLog` / report shape) as the base. Add portable-contract checks as additional findings orchestrated from `lintWorkspace` (new helpers OK). Do **not** fork a second CLI command. — **Reversibility:** costly — dual `/lint` surfaces would confuse class labs and E2E.
- **D-03:** Invert the intentional OKF “must not reject” unit proofs that conflict with the portable oracle: **broken local link** (`a link to a concept that is not in the bundle`) and **missing `index.md`** (`one concept … and no index.md`) become **error** findings. Keep “unrecognised `type`” and “keys OKF says nothing about” as non-rejecting. Update E2E `A conformant bundle reports nothing` so its sample workspace is truly portable-valid (today it links `./banana.md` without that file). — **Reversibility:** one-way — published unit/E2E contracts flip from “OKF must not reject X” to “portable lint reports X”.
- **D-04:** Keep the success string **`Workspace follows the OKF format.`** when there are no errors (warnings-only may still show the success line + warning summary, matching current behavior). Capability narrative/E2E feature title may mention the portable knowledge contract, but do not introduce a second success slogan in this phase. — **Reversibility:** reversible — copy can retitle later if product wants “portable” wording.

### Duplicate identities
- **D-05:** Identity key is Phase 8’s **`doughnut_id`**. Duplicate = two or more concept `.md` files (not reserved `index.md` / `log.md`) whose non-empty `doughnut_id` values collide. Report each colliding path with an actionable message. Missing `doughnut_id` is **not** a lint error (local-authored notes may lack it until export/push). — **Reversibility:** costly — push/preview already treat `doughnut_id` as stable identity; lint must stay aligned.

### Broken local links
- **D-06:** Flag **local** Markdown links and `[[wiki]]` targets that do not resolve to an existing workspace `.md` (or otherwise existing local file path the link clearly intends). Do **not** flag absolute `http(s):` URLs or remote `/attachments/…` URLs (Phase 8 keeps attachments remote). Hidden / dot-dir paths stay unscanned as sources (existing `isHidden` behavior). — **Reversibility:** reversible — resolver edge cases can tighten later if semantics hold.

### Missing indexes
- **D-07:** Report missing `index.md` when a directory that contains concept `.md` files (or nested concept-bearing folders that need a listing) lacks an `index.md` at that directory — at least: any non-empty concept-bearing directory without `index.md`, including the workspace root when it has concept files. Empty directories are out of scope for this gap. — **Reversibility:** costly — class acceptance treats missing indexes as a first-class portable-contract finding.

### Unsupported path mappings
- **D-08:** Align with Phase 9 reserved/invalid vocabulary (D-04 there): report unsafe / out-of-tree / empty-segment / non-portable path shapes among workspace `.md` paths, and treat reserved role filenames (`index.md`, `log.md`) / `.doughnut-sync/` consistently (not ordinary concept paths). Reuse or mirror preview reject wording where practical so lint and `/sync --dry-run` stay coherent. — **Reversibility:** costly — drift between lint and preview confuse the portable contract.

### Non-mutation / surface
- **D-09:** `/lint` remains **read-only** — no writes to workspace, `.doughnut-sync/`, or Doughnut. Primary strengthen lands in `cli/src/lint/*` + `lintWorkspace` orchestration; touch `lintSlashCommand` only for doc/help if needed. Do not change Stories 5–6 push modules. Prefer not changing shared `readWorkspace` / `directoryArgument` unless a lint proof is blocked. — **Reversibility:** reversible for help text; costly if shared readers regress sync/export.

### Proof strategy
- **D-10:** Prove via `cli_lint_workspace.feature` (duplicate id, broken local link, missing index, unsupported path, existing malformed-frontmatter / success / warning scenarios stay green or are updated per D-03/D-04) plus `cli/tests/lintWorkspace.test.ts` (and focused helper units) for edge cases. Capability-named tests only — no phase numbers in product/test names.

### Plan / commit sizing (user request)
- **D-11:** Config granularity stays **coarse**. Phase 11 plans/commits must be **slightly larger than Phase 10**: prefer **1 plan** with **1–2 larger tasks** that each land a coherent observable chunk (e.g. all four portable rules + units together; E2E proofs in the same plan). Avoid a separate tiny “E2E-only” plan and avoid per-rule micro-commits. Prefer fewer commits that group related unit+E2E for the same behavior. — **Reversibility:** reversible — planning preference only.

### Claude's Discretion
- Exact finding message wording / severity for portable rules (errors preferred for oracle gaps; warnings only if clearly advisory)
- Link/wiki resolution algorithm details (as long as D-06 holds and E2E proves them)
- Whether index-required applies only to directories that directly contain concepts vs also intermediate path segments
- Exact path-mapping checks beyond Phase 9 alignment (smallest set that closes the TRIAGE gap)
- Whether new helpers live as `okf*.ts` siblings or `portable*.ts` modules under `cli/src/lint/`

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone & requirements
- `.planning/PROJECT.md` — Core value, author skip (HYG-02), keep/strengthen/remove bar
- `.planning/REQUIREMENTS.md` — LINT-01 (this phase); HYG-02 standing constraint
- `.planning/ROADMAP.md` — Phase 11 goal and success criteria
- `.planning/STATE.md` — Current milestone position
- `.planning/config.json` — `granularity: coarse` (D-11: fewer/larger plans & commits)

### Acceptance oracle
- `.planning/notes/2026-07-24-portable-notebook-workspace.md` — Story 4 acceptance examples (4 bullets)

### Published triage (sole action source)
- `.planning/phases/07-publish-triage-decisions/TRIAGE.md` — Story 4 verdict **strengthen**, gap proofs, delete/keep set, entrypoints, Phase 11 finish sketch
- `.planning/phases/07-publish-triage-decisions/07-CONTEXT.md` — D-01..D-04 dossier rules
- `.planning/phases/08-resolve-pull-export-story-1/08-CONTEXT.md` — `doughnut_id` identity contract (D-01/D-02 there)
- `.planning/phases/09-resolve-preview-before-pull-story-2/09-CONTEXT.md` — reserved / invalid-mapping vocabulary (D-04) for path-mapping alignment
- `.planning/phases/10-resolve-incremental-pull-story-3/10-CONTEXT.md` — coarse plan sizing precedent (D-10 there); Phase 11 D-11 goes slightly larger

### Implementation targets (from TRIAGE)
- `cli/src/commands/lintSlashCommand.ts` — `/lint` entry
- `cli/src/lint/lintWorkspace.ts` — orchestration (primary strengthen)
- `cli/src/lint/okfConcept.ts`, `okfIndex.ts`, `okfLog.ts`, `okfProblem.ts` — OKF base rules (keep)
- `cli/src/lint/lintReport.ts`, `bundleFiles.ts` — findings formatting / non-md paths
- `cli/src/sync/readWorkspace.ts`, `directoryArgument.ts` — inputs (shared; change only if blocked)
- `e2e_test/features/cli/cli_lint_workspace.feature` — capability E2E
- `cli/tests/lintWorkspace.test.ts` — unit coverage (flip conflicting must-not-reject cases)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `lintWorkspace` — orchestrates per-path OKF problems + non-md warnings; no duplicate-id / link / missing-index / path-mapping rules yet
- `okfConcept` / `okfIndex` / `okfLog` — malformed frontmatter, index/`okf_version`, log rules (keep)
- `lintReport` — `path[:line]  severity  message` + `Workspace follows the OKF format.` success line
- Green E2E: `cli_lint_workspace.feature` (missing frontmatter, non-md warning, conformant success, multi-finding)
- Green units: `lintWorkspace.test.ts` including intentional “must not reject” broken link + missing index

### Established Patterns
- `/lint` is outside notebook stage; read-only workspace scan via `readWorkspace` + `parseDirectoryArgument`
- Reserved role files: `index.md` / `log.md` (OKF structure of their own; Phase 9 preview aligns)
- Capability E2E under `e2e_test/features/cli/`; CLI units under `cli/tests/`
- Identity key for portable notes: `doughnut_id` (Phase 8)

### Integration Points
- `/lint <dir>` → `lintSlashCommand` → `lintWorkspace`
- Shared readers with sync/export — preserve HYG-02 and Stories 1–3 behavior
- Preview invalid-mapping vocabulary (Phase 9) should stay coherent with new path-mapping findings

</code_context>

<specifics>
## Specific Ideas

- Auto mode (`--auto`) selected all gray areas and recommended defaults (see DISCUSSION-LOG).
- User asked again to make commit granularity slightly bigger: keep `coarse`, and lock D-11 so Phase 11 prefers **1 plan / 1–2 larger tasks** (slightly larger than Phase 10’s 1 plan / 2–3 tasks).
- TRIAGE finish sketch: keep OKF malformed-frontmatter / unknown-key / success-report behavior; add duplicate-identity, broken local link, missing-index, and unsupported path-mapping checks.
- Current units intentionally accept broken links and missing `index.md` — those proofs must be inverted as part of strengthen.

</specifics>

<deferred>
## Deferred Ideas

- Stories 5–6 push dry-run / safe push — Phases 12–13
- Retitling success copy from “OKF format” to “portable knowledge contract” as a product copy change — deferred unless E2E forces it (D-04 keeps current string)
- SEED-001 spelling follow-ons — parked
- Remote-driven deletes / Stories 7–10 portable create-rename-move — out of milestone

</deferred>

---

*Phase: 11-Resolve workspace lint (story 4)*
*Context gathered: 2026-08-03*

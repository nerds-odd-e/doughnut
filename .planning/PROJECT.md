# Doughnut — Portable Workspace Training Cleanup

## What This Is

Doughnut is a Personal Knowledge Management tool combining zettelkasten-style note capture, spaced repetition, and knowledge sharing. After v1.2, the CLI portable Markdown workspace paths for stories 1–5 are class-ready (export identity/links/attachments, sync dry-run + pull, OKF lint, push dry-run); Story 6 mutate push was removed cleanly as WIP.

## Core Value

Keep a healthy mainline for future classes: retain only participant work that matches the portable-workspace stories, has no WIP, and delivers external user value — strengthen near-misses; remove the rest. Never touch Terry or Yeong Sheng changes.

## Current State

**Shipped v1.2 (2026-08-03):** Triaged LIA participant work for portable-workspace stories 1–6; strengthened export/sync/lint/push-dry-run; removed Story 6 mutate-push WIP; class-ready hygiene verified (HYG-01/02/03).

**Shipped v1.1 (2026-07-25):** Accidental-match + overlap spelling recall loop end-to-end.

**Shipped v1.0 (2026-07-23):** Notebook Health lint + Health tab + gated empty-folder purge.

## Next Milestone Goals

Define via `/gsd-new-milestone`. Likely candidates:

- Stories 7–10 (create from local file, rename, move, reconcile deletions)
- Mutating `/push` (Story 6 proper) if/when designed
- SEED-001 spelling follow-ons (MCQ / fuzzy / `Notebook:Title`)

## Requirements

### Validated

- ✓ Spelling recall question type and answer grading — existing
- ✓ Spaced-repetition scheduling with success/failure/partial paths — existing
- ✓ Wiki-link resolution by title or alias — existing
- ✓ Note aliases in frontmatter, indexed — existing
- ✓ Add-link UI (wiki / property / relationship) — existing
- ✓ Notebook Health lint + tab + gated empty-folder purge — v1.0
- ✓ Accidental-match + overlap spelling recall loop — v1.1
- ✓ Baseline portable-workspace direction documented in `.planning/notes/2026-07-24-portable-notebook-workspace.md`
- ✓ Triage keep/strengthen/remove for stories 1–6 (participant-only) — v1.2
- ✓ Export with `doughnut_id`, wiki→MD links, absolute attachment URLs — v1.2
- ✓ `/sync --dry-run` create/update/move/reject with non-mutation — v1.2
- ✓ Mutating `/sync` create/update/move + gated baseline — v1.2
- ✓ OKF `/lint` portable contract (dup ids, broken links, missing indexes, unsafe paths) — v1.2
- ✓ `/push --dry-run` load-only baseline + create/update conflict labels — v1.2
- ✓ Story 6 mutate-push WIP removed cleanly — v1.2
- ✓ Class-ready hygiene (no training WIP; Terry/YS untouched; retained CLI green) — v1.2

### Active

(None — define in `/gsd-new-milestone`)

### Out of Scope

- Stories 7–10 (create from local file, rename, move, reconcile deletions) — not attempted in this class; leave for later
- Terry Yin and Tan Yeong Sheng changes — skip entirely
- Spelling follow-ons (MCQ / fuzzy / `Notebook:Title`) — parked as SEED-001
- Mutating `/push` (Story 6 product path) — removed as WIP in v1.2; redesign later if needed
- Broad unrelated refactors not required by triage decisions

## Context

v1.2 cleaned LIA training participant code against portable-workspace stories 1–6. Strengthen path won for stories 1–5; Story 6 was remove. Retained surface: CLI `/export`, `/sync` (+ `--dry-run`), `/lint`, `/push --dry-run`. Spent phase diaries pruned; see `.planning/milestones/v1.2-ROADMAP.md` and `MILESTONES.md`.

## Constraints

- **Authors:** Do not revert, rewrite, or "clean" commits/changes attributable to Terry Yin or Tan Yeong Sheng
- **Acceptance bar:** Story acceptance examples in the portable-workspace note are the keep/remove oracle
- **WIP:** Incomplete or `@wip` / half-wired work is remove-by-default unless strengthening is cheaper and clearly valuable
- **Stack:** Prefer CLI + E2E capability tests; Behavior/Structure phased delivery; Nix tooling via `CURSOR_DEV=true nix develop -c …`
- **Stop-safe:** After each phase the tree must remain healthier (or no worse) than before

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Include story 5 (push dry-run / conflict preview) in audit | Students landed it; part of the push safety loop with story 6 | ✓ Strengthened (PUSH-01) |
| Skip Terry and Yeong Sheng | Instructors / non-participant work; out of triage | ✓ HYG-02 verified |
| Keep only correct + no WIP + external value | Class-ready mainline over preserving every experiment | ✓ Applied via TRIAGE.md |
| Strengthen minor gaps on valuable work | Prefer keeping usable capability over delete-and-rebuild | ✓ Stories 1–5 |
| Story 6 → remove | No mutate push; `@ignore` cli_push.feature WIP debris | ✓ PUSH-02 removed cleanly |
| v1.1 spelling follow-ons → SEED-001 | Detour for training cleanup; do not lose the idea | ✓ Parked |

<details>
<summary>Pre-v1.2 spelling-era project framing (archived)</summary>

Earlier PROJECT.md framed the active product around spelling accidental-match & overlap (v1.1). See `.planning/milestones/v1.1-ROADMAP.md` and `.planning/MILESTONES.md` for the shipped record. Spelling follow-ons live in SEED-001.

</details>

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-08-03 after v1.2 milestone*

# Roadmap: Doughnut

## Milestones

- ✅ **v1.0 Notebook Lint & Auto-Fix** — Phases 1-7 ([archive](./milestones/v1.0-ROADMAP.md)) — shipped 2026-07-23
- ✅ **v1.1 Spelling Answer Match & Link** — Phases 1-6 ([archive](./milestones/v1.1-ROADMAP.md)) — shipped 2026-07-25
- 🚧 **v1.2 Clean up LIA training participant code** — Phases 7-14 (in progress)

## Phases

<details>
<summary>✅ v1.1 Spelling Answer Match & Link (Phases 1-6) — SHIPPED 2026-07-25</summary>

- [x] Phase 1: Extend Answer outcome API (1/1 plans) — completed 2026-07-23
- [x] Phase 2: Accidental-match grading & penalty (2/2 plans) — completed 2026-07-24
- [x] Phase 3: Reveal both notes after accidental match (3/3 plans) — completed 2026-07-24
- [x] Phase 4: Offer link between notes (3/3 plans) — completed 2026-07-24
- [x] Phase 5: Alias-as-wiki-link overlap declaration (3/3 plans) — completed 2026-07-24
- [x] Phase 6: Overlap "try again, no credit" (4/4 plans) — completed 2026-07-24

</details>

<details>
<summary>✅ v1.0 Notebook Lint & Auto-Fix (Phases 1-7) — SHIPPED 2026-07-23</summary>

- [x] Phase 1: Health lint contract — completed 2026-07-22
- [x] Phase 2: Empty-folder findings — completed 2026-07-22
- [x] Phase 3: Readme-only folder findings — completed 2026-07-22
- [x] Phase 4: Dead-link findings — completed 2026-07-22
- [x] Phase 5: Health tab and Run — completed 2026-07-23
- [x] Phase 6: User-level defaults — completed 2026-07-23
- [x] Phase 7: Gated empty-folder purge — completed 2026-07-23

</details>

### 🚧 v1.2 Clean up LIA training participant code (In Progress)

**Milestone Goal:** Audit and triage LIA participant changes against portable-workspace stories 1–6; keep/strengthen valuable complete work; remove WIP, incorrect, or low-value debris so the tree is class-ready. Never touch Terry Yin or Tan Yeong Sheng changes.

**Constraint (all action phases):** HYG-02 — do not revert, rewrite, or "clean" commits/changes attributable to Terry Yin or Tan Yeong Sheng. Requirement ID maps to Phase 14 for final verification; apply the constraint in Phases 8–13.

**Source stories:** `.planning/notes/2026-07-24-portable-notebook-workspace.md` items 1–6.
**Participant surface:** `cli/` export/lint/push, `e2e_test/features/cli/`, related docs/plans.
**Decision bar:** Keep = correct + no WIP + external user value; Strengthen = minor gaps on valuable work; Remove = WIP/incorrect/non-valuable.

- [x] **Phase 7: Publish triage decisions** - Maintainer has cited keep/strengthen/remove for stories 1–6 (3 plans)
- [ ] **Phase 8: Resolve pull/export (story 1)** - Keep/strengthen or cleanly remove story 1 participant work
- [ ] **Phase 9: Resolve preview-before-pull (story 2)** - Keep/strengthen or cleanly remove story 2 participant work
- [ ] **Phase 10: Resolve incremental pull (story 3)** - Keep/strengthen or cleanly remove story 3 participant work
- [ ] **Phase 11: Resolve workspace lint (story 4)** - Keep/strengthen or cleanly remove story 4 participant work
- [ ] **Phase 12: Resolve push dry-run (story 5)** - Keep/strengthen or cleanly remove story 5 participant work
- [ ] **Phase 13: Resolve safe push (story 6)** - Keep/strengthen or cleanly remove story 6 participant work
- [ ] **Phase 14: Class-ready hygiene verify** - No training WIP left; Terry/YS untouched; targeted tests green

## Phase Details

### Phase 7: Publish triage decisions

**Goal**: Maintainer has a published keep / strengthen / remove decision for each of stories 1–6, citing acceptance examples, based only on non–Terry / non–Yeong Sheng participant work
**Type**: Behavior
**Depends on**: Nothing (first phase of v1.2; v1.1 shipped)
**Requirements**: TRIAGE-01, TRIAGE-02
**Success Criteria** (what must be TRUE):

  1. Every story 1–6 has exactly one recorded decision: keep, strengthen, or remove
  2. Each decision cites the matching acceptance examples from `.planning/notes/2026-07-24-portable-notebook-workspace.md`
  3. Decisions are based only on participant work (authors exclude Terry Yin and Tan Yeong Sheng)
  4. A later maintainer can act on Phases 8–13 from the published triage alone (no re-audit required)

**Plans** (3/3 executed):

- [x] `07-01-PLAN.md` — Wave 1: TRIAGE.md schema + Story 1 dossier
- [x] `07-02-PLAN.md` — Wave 2: Story 2, Story 3 (+ D-03), Story 4 dossiers
- [x] `07-03-PLAN.md` — Wave 3: Story 5, Story 6 (+ D-03), hardened completeness verify, CONTEXT/STATE pointers

**Notes**: Standing constraint HYG-02 applies to all subsequent action phases; verified in Phase 14. Deliverable: `.planning/phases/07-publish-triage-decisions/TRIAGE.md` (docs only; no tree apply). Phases 8–13 act from `TRIAGE.md` alone.

### Phase 8: Resolve pull/export (story 1)

**Goal**: Story 1 pull/export participant work is either healthy against acceptance or cleanly gone
**Type**: Behavior
**Depends on**: Phase 7
**Requirements**: EXP-01
**Success Criteria** (what must be TRUE):

  1. Phase 7’s story 1 decision is applied in the tree
  2. If keep/strengthen: pull/export matches story 1 acceptance (hierarchy, identity frontmatter, indexes, links, no secrets; failed pull not presented as success)
  3. If remove: incomplete/incorrect story 1 path and related debris are gone with no half-wired CLI/E2E left for that path
  4. Terry Yin and Tan Yeong Sheng changes remain untouched (HYG-02 standing constraint)

**Plans**: 1/2 plans executed

Plans:

- [x] 08-01-PLAN.md — Backend zip: doughnut_id merge + wiki→relative MD + absolute attachment URLs (+ units)
- [ ] 08-02-PLAN.md — CLI `/export` E2E proofs for three Story 1 gaps + phase wrap-up

### Phase 9: Resolve preview-before-pull (story 2)

**Goal**: Story 2 preview-before-pull participant work is either healthy against acceptance or cleanly gone
**Type**: Behavior
**Depends on**: Phase 8
**Requirements**: EXP-02
**Success Criteria** (what must be TRUE):

  1. Phase 7’s story 2 decision is applied in the tree
  2. If keep/strengthen: preview reports paths/actions and mutates nothing (Doughnut, workspace, sync metadata)
  3. If remove: story 2 participant path and related debris are gone cleanly
  4. Terry Yin and Tan Yeong Sheng changes remain untouched (HYG-02 standing constraint)

**Plans**: TBD

### Phase 10: Resolve incremental pull (story 3)

**Goal**: Story 3 incremental pull participant work is either healthy against acceptance or cleanly gone
**Type**: Behavior
**Depends on**: Phase 9
**Requirements**: EXP-03
**Success Criteria** (what must be TRUE):

  1. Phase 7’s story 3 decision is applied in the tree
  2. If keep/strengthen: unchanged files stay undisturbed and re-pull is idempotent per story 3 acceptance
  3. If remove: story 3 participant path and related debris are gone cleanly
  4. Terry Yin and Tan Yeong Sheng changes remain untouched (HYG-02 standing constraint)

**Plans**: TBD

### Phase 11: Resolve workspace lint (story 4)

**Goal**: Story 4 `/lint` (or equivalent) participant work is either healthy against acceptance or cleanly gone
**Type**: Behavior
**Depends on**: Phase 10
**Requirements**: LINT-01
**Success Criteria** (what must be TRUE):

  1. Phase 7’s story 4 decision is applied in the tree
  2. If keep/strengthen: lint matches story 4 acceptance (malformed frontmatter, duplicate identities, broken links, missing indexes, actionable findings; valid workspace succeeds)
  3. If remove: story 4 participant path and related debris are gone cleanly
  4. Terry Yin and Tan Yeong Sheng changes remain untouched (HYG-02 standing constraint)

**Plans**: TBD

### Phase 12: Resolve push dry-run (story 5)

**Goal**: Story 5 push dry-run / conflict preview participant work is either healthy against acceptance or cleanly gone
**Type**: Behavior
**Depends on**: Phase 11
**Requirements**: PUSH-01
**Success Criteria** (what must be TRUE):

  1. Phase 7’s story 5 decision is applied in the tree
  2. If keep/strengthen: dry-run distinguishes unchanged / local / remote / divergent and mutates nothing
  3. If remove: story 5 participant path and related debris are gone cleanly
  4. Terry Yin and Tan Yeong Sheng changes remain untouched (HYG-02 standing constraint)

**Plans**: TBD

### Phase 13: Resolve safe push (story 6)

**Goal**: Story 6 safe push participant work is either healthy against acceptance or cleanly gone
**Type**: Behavior
**Depends on**: Phase 12
**Requirements**: PUSH-02
**Success Criteria** (what must be TRUE):

  1. Phase 7’s story 6 decision is applied in the tree
  2. If keep/strengthen: push matches story 6 acceptance (body + supported frontmatter; version-safe; conflicts not silent overwrite; successful push refreshes sync metadata)
  3. If remove: story 6 participant path and related debris are gone cleanly
  4. Terry Yin and Tan Yeong Sheng changes remain untouched (HYG-02 standing constraint)

**Plans**: TBD

### Phase 14: Class-ready hygiene verify

**Goal**: Mainline is class-ready — no leftover training WIP for stories 1–6, instructor authors untouched, retained capabilities proven green
**Type**: Behavior
**Depends on**: Phase 13
**Requirements**: HYG-01, HYG-02, HYG-03
**Success Criteria** (what must be TRUE):

  1. WIP, incorrect, or non-valuable participant code for stories 1–6 is gone (including orphaned tests, unfinished `@wip` scenarios, and spent training plans/docs that no longer describe the tree)
  2. Terry Yin and Tan Yeong Sheng changes remain untouched by this milestone’s removals/rewrites (final HYG-02 verify)
  3. Targeted CLI/unit and relevant CLI E2E for retained capabilities pass
  4. A future class can start from this tree without leftover training debris for stories 1–6

**Plans**: TBD

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1–7 Health lint / purge | v1.0 | 13/13 | Complete | 2026-07-23 |
| 1–6 Spelling match & link | v1.1 | 16/16 | Complete | 2026-07-25 |
| 7. Publish triage decisions | v1.2 | 3/3 | Complete | 2026-08-03 |
| 8. Resolve pull/export (story 1) | v1.2 | 1/2 | In Progress|  |
| 9. Resolve preview-before-pull (story 2) | v1.2 | 0/? | Not started | - |
| 10. Resolve incremental pull (story 3) | v1.2 | 0/? | Not started | - |
| 11. Resolve workspace lint (story 4) | v1.2 | 0/? | Not started | - |
| 12. Resolve push dry-run (story 5) | v1.2 | 0/? | Not started | - |
| 13. Resolve safe push (story 6) | v1.2 | 0/? | Not started | - |
| 14. Class-ready hygiene verify | v1.2 | 0/? | Not started | - |

---
*Last updated: 2026-08-03 after 07-03 — triage published; Phase 7 complete*

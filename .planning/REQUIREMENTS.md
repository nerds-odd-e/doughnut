# Requirements: Notebook Lint & Auto-Fix

**Defined:** 2026-07-22
**Core Value:** From Notebook Settings → Health, a user can run lint and see actionable health findings for their notebook — with optional bulk removal of empty folders when auto-fix is enabled.

## v1 Requirements

### Health surface

- [ ] **HLTH-01**: User can open a **Health** tab on notebook settings (alongside Index/Settings), without a separate `/health` route or findings dialog
- [ ] **HLTH-02**: User can explicitly **Run lint** for the current notebook (on-demand; opening Health does not mutate data)
- [ ] **HLTH-03**: User can review **expandable nested findings** on the Health tab (grouped by rule; dead links nested by note) with an action bar for fix options

### Empty folders

- [ ] **EFOL-01**: Lint reports folders whose **entire subtree has no notes** (recursive emptiness; soft-deleted notes do not count as content)
- [ ] **EFOL-02**: When auto-fix is not selected, user can see the list of empty folders under the empty-folder findings group
- [ ] **EFOL-03**: Lint reports note-empty folders that still have non-blank **readme** (`readmeContent`) as a **separate finding type** (not lumped with fully empty folders)

### Dead wiki links

- [ ] **DLNK-01**: Lint reports dead `[[wiki links]]` in note **body** using the same resolve semantics as the editor (including aliases and qualified `Notebook:Title` links)
- [ ] **DLNK-02**: Lint reports dead wiki links in note **frontmatter / properties** with the same resolve semantics
- [ ] **DLNK-03**: Dead-link findings are **report-only** in v1 (no auto-fix / rewrite)

### Auto-fix

- [ ] **AFIX-01**: Auto-fix is an **optional** run option; lint never deletes or mutates on Run alone
- [ ] **AFIX-02**: The only v1 fix action is bulk **remove empty folders** (no per-folder multi-select)
- [ ] **AFIX-03**: **Fix** is enabled only when the user has selected the bulk “remove empty folders” option
- [ ] **AFIX-04**: Bulk remove deletes only **fully empty** folder trees (no notes in subtree and no meaningful readme); folders in the separate “readme only” finding type are **not** deleted by this fix
- [ ] **AFIX-05**: Empty-folder removal uses a **dedicated purge** path (must not use folder dissolve / promote-children)

### User defaults

- [ ] **DFLT-01**: User can save Health lint/auto-fix option defaults at **user** level (apply across notebooks)
- [ ] **DFLT-02**: Opening Health prefills run options from the user’s saved defaults

## v2 Requirements

Deferred; not in current roadmap.

### Health surface

- **HLTH-10**: Health summary chip / count on notebook chrome
- **HLTH-11**: Click-through from a dead-link finding to focus the first occurrence in the note editor

### Rules

- **EFOL-10**: Empty-note lint rule
- **ORPH-01**: Orphan / unlinked note report
- **SEM-01**: LLM / semantic lint (contradictions, weak content, synthesis suggestions)

### Auto-fix

- **DLNK-10**: Assisted or automatic dead-link repair
- **AFIX-10**: Per-folder multi-select for deletion
- **AFIX-11**: Scheduled or silent auto-fix

### Defaults

- **DFLT-10**: Per-notebook overrides of user defaults

## Out of Scope

| Feature | Reason |
|---------|--------|
| LLM / semantic lint | Mechanical v1 first; cost/latency/trust later |
| Empty-note rule | Explicitly deferred from v1 rule set |
| Dead-link auto-fix | Creating/retargeting needs judgment |
| Per-folder multi-select delete | Bulk option only keeps UX simple and safe |
| Dedicated `/health` route or findings dialog | Stay on Health tab with expandable results |
| Per-notebook defaults | v1 is per-user only |
| Reusing folder dissolve for cleanup | Dissolve promotes children; wrong semantics for empty purge |
| Client-only lint over SPA cache | Must match live graph + `WikiLinkResolver` |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| EFOL-01 | Phase 2 | Pending |
| EFOL-02 | Phase 2 | Pending |
| EFOL-03 | Phase 3 | Pending |
| DLNK-01 | Phase 4 | Pending |
| DLNK-02 | Phase 4 | Pending |
| DLNK-03 | Phase 4 | Pending |
| HLTH-01 | Phase 5 | Pending |
| HLTH-02 | Phase 5 | Pending |
| HLTH-03 | Phase 5 | Pending |
| AFIX-01 | Phase 5 | Pending |
| DFLT-01 | Phase 6 | Pending |
| DFLT-02 | Phase 6 | Pending |
| AFIX-02 | Phase 7 | Pending |
| AFIX-03 | Phase 7 | Pending |
| AFIX-04 | Phase 7 | Pending |
| AFIX-05 | Phase 7 | Pending |

**Coverage:**
- v1 requirements: 16 total
- Mapped to phases: 16
- Unmapped: 0 ✓

---
*Requirements defined: 2026-07-22*
*Last updated: 2026-07-22 after roadmap (traceability)*

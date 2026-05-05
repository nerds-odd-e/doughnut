# Phase 8 — Move and remove folders (sub-phases)

Parent plan: `ongoing/doughnut_wiki_migration_plan.md` (Phase 8).

This document decomposes Phase 8 into **stop-safe** slices per `.cursor/rules/planning.mdc`: each slice is either **behavior** (externally observable, testable) or **structure** (no observable change; existing tests still pass). Sub-phases follow the **E2E-led** rhythm from the phased-planning skill where a slice spans UI + API + persistence.

---

## Product intent (constraints)

- **Move:** Change the moved folder’s `parentFolderId` to another folder in the **same notebook** or to **notebook root** (`null`). Descendant folders stay under the moved subtree; contained notes keep their `folderId` pointing at the same folder rows (no per-note rewrite for a pure move). Enforce **folder name uniqueness among siblings** at the destination (same rule as create/rename in the north star / main Phase 8 note).
- **Remove folder:** Remove the folder row; **notes that lived in that folder** get **`folderId`** set to the removed folder’s **parent** (notebook root if the removed folder was at root). **Naming:** pick one user-facing capability name (e.g. **Remove folder**, **Dissolve folder**) and avoid overloading **Delete** with note deletion; final copy is a small deliverable in the first UI slice that ships the dialog.
- **Child folders when removing:** If the removed folder has **subfolders**, define one rule before implementation: e.g. **promote** subfolders to the removed folder’s parent (same sibling set the notes join), or **require empty** (no subfolders). The parent Phase 8 text does not specify this; the implementing sub-phase should **lock the rule** in E2E + API contract so behavior stays coherent.
- **UI:** One **dialog** hosts both **move** and **remove folder**. Entry: a **sidebar toolbar** control (label can stay neutral, e.g. **Folder…** or **Organize folder**, if **Move** alone is misleading once removal ships). The toolbar control is **visible only when a folder is active** (the folder the user is scoped to / has selected in the sidebar — align wording with existing “active folder” state in the app).

---

## Tests and naming

- **E2E:** Add or extend scenarios in a **capability-named** feature file (e.g. `folder_organization.feature` or the smallest extension of an existing topology/folder feature). Do **not** put phase numbers in feature or scenario titles.
- **Unit / integration:** Uniqueness, cycle prevention (if applicable), “cannot move into self/descendant”, and remove-folder reassignment of `folderId` (and subfolder rule) are good **black-box** tests at the service or controller boundary if cheaper than full E2E for every edge.
- Prefer **one intentionally failing `@wip` E2E** at a time while driving a slice, per planning discipline.

---

## Sub-phases (execution order)

| Id | Type | Summary |
|----|------|--------|
| 8.1 | Behavior | **Move folder (happy path) + entry:** Sidebar toolbar control is **shown only when a folder is active**; hidden when scope is notebook root with no active folder. User opens the **single** dialog from that control, chooses another folder or root in the **same notebook**, confirms; tree and note listing reflect the new parent. **E2E first** (`@wip` → green → remove `@wip`). |
| 8.2 | Behavior | **Move folder — validation and errors:** Same-notebook guard, sibling name clash, illegal destination (self or descendant if the backend forbids cycles). Observable error text or inline feedback; extend the same feature file / focused tests. |
| 8.3 | Behavior | **Remove folder in the same dialog:** Primary postcondition: notes previously in that folder appear under **parent folder or root**; folder no longer appears in the tree. Lock and test the **subfolder policy** chosen in Product intent. Destructive action uses the same confirmation patterns as the rest of the app (no new interaction species unless necessary). Finalize user-visible **move vs remove** labels in this dialog context. |
| 8.4 | Structure (optional) | **Hardening only if needed:** Extract shared validation (sibling uniqueness, tree checks) for folder create/rename/move/remove so behavior stays consistent **without** changing user-visible outcomes. Skip if the codebase already has a single choke point. |

---

## Dependencies and non-goals

- **Depends on:** Phase 7 shipped model (folder placement, no structural note parent). No wiki-link resolution (**Phase 9**) or folder templates (**Phase 10**).
- **Non-goals for Phase 8:** Cross-notebook moves, bulk note moves unrelated to folder removal, slug/path columns (retired).

---

## Phase-complete checklist (Phase 8 closeout)

- All **8.x** behavior slices have **E2E** (or agreed controller-level) coverage for the main path; edge paths covered where risk is high.
- No remaining `@wip` for these scenarios in CI.
- Main plan `ongoing/doughnut_wiki_migration_plan.md`: Phase 8 row marked done when shipped; this file updated or archived per team habit.

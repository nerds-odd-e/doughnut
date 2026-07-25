# Phase 6: User-level defaults - Context

**Gathered:** 2026-07-22
**Status:** Ready for planning

<domain>
## Phase Boundary

**Behavior.** User can **save** Health lint/auto-fix option defaults at **user** level (apply across notebooks) and see them **prefilled** when opening Health on any notebook — without applying fixes or mutating notebook data on open or on save.

Delivers DFLT-01 and DFLT-02. Extends the Phase 5 Health action bar (`Remove empty folders` checkbox). No Fix / purge execution (Phase 7). No per-notebook overrides (v2 DFLT-10). Lint API stays bodyless and report-only; defaults only seed **UI run options**.

</domain>

<decisions>
## Implementation Decisions

### What is persisted (DFLT-01)
- **D-01:** Persist the **one** v1 Health run option that exists on the action bar: **Remove empty folders** (boolean). Do **not** invent a second preference (no separate “enable auto-fix” flag). Phase 7 Fix enablement continues to use this same checkbox for the run.
- **D-02:** Field naming (capability-oriented): Java/API `healthRemoveEmptyFoldersDefault`, DB column `health_remove_empty_folders_default`. Default value for new/unset users: **`false`** (unchecked), matching Phase 5’s initial local state.
- **D-03:** Storage is **per-user on `User`**, not `NotebookSettings` and not a new prefs service. Extend `User` entity + Flyway migration with a boolean column, expose on the serialized `User` (so `GET /api/user` and `CurrentUserInfo` already carry it), and add the field to `UserDTO` so `PATCH /api/user/{user}` can update it via existing `UserController.updateUser` (same pattern as `spaceIntervals` / `dailyAssimilationCount`).

### Prefill when opening Health (DFLT-02)
- **D-04:** When the Health panel is shown (mount / Health tab active), seed the **Remove empty folders** checkbox from the user’s saved default. Prefer injected `currentUser` (already loaded at app start via `CurrentUserInfoController.currentUserInfo`, which returns the full `User` entity). Fall back to `UserController.getUserProfile` only if the preference is not available on the inject.
- **D-05:** Prefill is **UI-only**. Opening Health, loading defaults, or saving defaults must **not** call lint, must **not** run Fix/purge, and must **not** mutate notebook data. Success criterion 3 is mandatory.
- **D-06:** After prefill, the checkbox remains **run-scoped local state**. The user may toggle it for this visit without automatically overwriting saved defaults.

### Save interaction (DFLT-01)
- **D-07:** Use an explicit **“Save as defaults”** control on the Health **action bar** (beside Run lint and the checkbox). Do **not** auto-save on every checkbox toggle — one-off run choices must not silently persist.
- **D-08:** Save lives on the Health panel only. Do **not** add Health defaults to the global User Settings dialog (`UserProfileForm`) in this phase.
- **D-09:** Save PATCHes the current user via `UserController.updateUser` with `apiCallWithLoading`. Because `UserDTO.name` is required, include `name` (and other existing UserDTO fields the update path already maps) from the current profile plus the updated `healthRemoveEmptyFoldersDefault`. After a successful save, update the injected `currentUser` so other notebooks in the same session prefill the new value without a full reload.
- **D-10:** Saving defaults must not call lint or Fix and must not mutate notebook content — only the user preference row.

### Cross-notebook behavior
- **D-11:** Defaults apply across notebooks: save on notebook A, open Health on notebook B → checkbox reflects the saved default. No notebook-scoped override in v1 (DFLT-10 is out of scope).

### Lint / Fix API boundary (unchanged this phase)
- **D-12:** Keep `POST .../health/lint` **bodyless** and report-only. Do **not** send defaults on the lint request. Fix enablement and purge remain Phase 7; this phase only persists and prefills the checkbox that Phase 7 will gate on.

### Verification
- **D-13:** Backend: migration + User/UserDTO/updateUser mapping for the new boolean; focused controller/entity tests that GET profile / PATCH update round-trip the field (default false when unset).
- **D-14:** Frontend unit/component tests: Health panel prefills from user default; Save as defaults PATCHes and updates local/injected user; opening Health alone still does not call lint; save alone does not mutate notebook data.
- **D-15:** Targeted E2E (extend capability-named `notebook_health`): set checkbox → Save as defaults on one notebook → open Health on another notebook → checkbox matches. Tag `@wip` until green; remove `@wip` when scenarios pass. Do not run the full E2E suite unless required.

### Claude's Discretion
- Exact button label (“Save as defaults” vs “Save defaults”) — prefer **“Save as defaults”**.
- Whether prefill runs on panel `onMounted` vs watching tab activation — smallest change that seeds when Health is shown.
- Exact DaisyUI classes for the save control (secondary/ghost/sm) — match action-bar density from Phase 5 UI-SPEC; primary accent stays on **Run lint**.
- Whether `UserDTO` needs `@NotNull` on the new boolean — prefer optional with server default false so partial mental models stay simple; researcher/planner may follow existing DTO style.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and requirements
- `.planning/ROADMAP.md` — Phase 6 goal, Behavior type, success criteria (DFLT-01/02 + no mutate on prefill), UI hint, stop-safe value after Phase 6
- `.planning/REQUIREMENTS.md` — DFLT-01, DFLT-02 (this phase); AFIX-02–05 Phase 7; Out of Scope: per-notebook defaults
- `.planning/PROJECT.md` — Per-user defaults across notebooks; lint ≠ fix; action bar options visible and savable as user defaults

### Prior phase decisions
- `.planning/phases/05-health-tab-and-run/05-CONTEXT.md` — Health tab, action bar, `removeEmptyFolders` UI-only checkbox, bodyless lint, no Fix (Phase 7), deferred defaults to this phase
- `.planning/phases/05-health-tab-and-run/05-UI-SPEC.md` — Action-bar layout, CheckInput contract, primary reserved for Run lint
- `.planning/phases/02-empty-folder-findings/02-CONTEXT.md` — write-auth lint; empty_folders semantics Phase 7 will purge

### Research (user defaults pattern)
- `.planning/research/ARCHITECTURE.md` — Pattern 3: user defaults seed run options; extend `User` / `UserDTO` / `updateUser`; anti-pattern: NotebookSettings for user defaults; defaults must not enable silent mutation
- `.planning/research/SUMMARY.md` — Flyway + User columns; preferences across notebooks
- `.planning/research/PITFALLS.md` — defaults only prefill; Fix still requires bulk option selected for the run (Phase 7)
- `.planning/research/FEATURES.md` — user-level defaults; not per-notebook in v1

### Frontend / API / migration conventions
- `.planning/codebase/CONVENTIONS.md` — `data-testid`, capability naming, `apiCallWithLoading`
- `.cursor/rules/frontend-api.mdc` — generated SDK + `apiCallWithLoading` + error handling
- `.cursor/rules/db-migration.mdc` — Flyway migrations for new User column
- `.cursor/rules/e2e-authoring.mdc` — capability-named features, `@wip`, page objects
- `.cursor/skills/generate-api-client/SKILL.md` — regenerate TS client after OpenAPI/`User`/`UserDTO` change

### Implemented surfaces to extend (must not replace)
- `frontend/src/components/notebook/NotebookHealthPanel.vue` — action bar + local `removeEmptyFolders` ref; add prefill + Save as defaults
- `frontend/src/DoughnutApp.vue` — provides `currentUser` from `CurrentUserInfo`
- `frontend/src/components/toolbars/UserProfileForm.vue` — reference PATCH pattern only; do not add Health fields here
- `backend/src/main/java/com/odde/doughnut/entities/User.java` — add preference column
- `backend/src/main/java/com/odde/doughnut/controllers/dto/UserDTO.java` — add field for PATCH
- `backend/src/main/java/com/odde/doughnut/controllers/UserController.java` — map field in `updateUser`
- `packages/generated/doughnut-backend-api/sdk.gen.ts` — `UserController.getUserProfile` / `updateUser` (regen after backend change)
- `e2e_test/features/notebooks/notebook_health.feature` — extend for defaults cross-notebook

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `NotebookHealthPanel` — action bar with Run lint + `CheckInput` for `removeEmptyFolders` (local `ref(false)` today)
- `User` entity / `UserDTO` / `UserController.updateUser` — established preference columns (`dailyAssimilationCount`, `spaceIntervals`)
- `UserController.getUserProfile` and `CurrentUserInfoController.currentUserInfo` — both return full `User` entity JSON
- Injected `currentUser` (`provide`/`inject` from `DoughnutApp`) — session-wide user without extra fetch
- `UserProfileForm` — PATCH via `apiCallWithLoading` + `UserController.updateUser` (include `name` and existing fields)
- `apiCallWithLoading` — standard loading/errors for Save

### Established Patterns
- User preferences live on `User`, updated through `PATCH /api/user/{id}` with `UserDTO`
- Health is notebook-settings-tab only; findings ephemeral; lint bodyless and report-only
- Frontend API: generated SDK; check `error` before using `data`
- E2E: capability-named `notebook_health`; `@wip` while red

### Integration Points
- Extend `User` + migration + `UserDTO` + `updateUser` mapping
- Regen OpenAPI / TypeScript client so `User` / `UserDto` include the new boolean
- `NotebookHealthPanel` loads default into checkbox; Save PATCHes user and refreshes injected `currentUser`
- Phase 7 will gate Fix on the same run-scoped checkbox (prefilled from this default)

</code_context>

<specifics>
## Specific Ideas

- `--auto` discussion locked: one boolean on `User` for Remove empty folders default; explicit Save as defaults on Health action bar; prefill from `currentUser` on open; no auto-save on toggle; no global User Settings UI; no lint/Fix/notebook mutation on open or save; cross-notebook prefill is the acceptance path.
- Research’s “two booleans” / full rule-registry config is **not** in scope — only the option Phase 5 shipped.

</specifics>

<deferred>
## Deferred Ideas

- Fix button enablement + bulk empty-folder purge — Phase 7 (AFIX-02–05)
- Per-notebook overrides of user defaults — v2 (DFLT-10)
- Health defaults in global User Settings dialog — not needed for v1; Health panel is sufficient
- Auto-save on every checkbox toggle — rejected for this phase (explicit Save preserves one-off runs)
- Lint request body carrying fix options — still not required; Phase 7 may introduce a separate fix endpoint/body

None — discussion stayed within phase scope

</deferred>

---

*Phase: 6-User-level defaults*
*Context gathered: 2026-07-22*

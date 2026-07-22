# Phase 6: User-level defaults - Research

**Researched:** 2026-07-22
**Domain:** Per-user Health run-option defaults (Spring Boot `User` + Flyway + Vue Health panel + OpenAPI TS client)
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Persist the **one** v1 Health run option that exists on the action bar: **Remove empty folders** (boolean). Do **not** invent a second preference (no separate “enable auto-fix” flag). Phase 7 Fix enablement continues to use this same checkbox for the run.
- **D-02:** Field naming (capability-oriented): Java/API `healthRemoveEmptyFoldersDefault`, DB column `health_remove_empty_folders_default`. Default value for new/unset users: **`false`** (unchecked), matching Phase 5’s initial local state.
- **D-03:** Storage is **per-user on `User`**, not `NotebookSettings` and not a new prefs service. Extend `User` entity + Flyway migration with a boolean column, expose on the serialized `User` (so `GET /api/user` and `CurrentUserInfo` already carry it), and add the field to `UserDTO` so `PATCH /api/user/{user}` can update it via existing `UserController.updateUser` (same pattern as `spaceIntervals` / `dailyAssimilationCount`).
- **D-04:** When the Health panel is shown (mount / Health tab active), seed the **Remove empty folders** checkbox from the user’s saved default. Prefer injected `currentUser` (already loaded at app start via `CurrentUserInfoController.currentUserInfo`, which returns the full `User` entity). Fall back to `UserController.getUserProfile` only if the preference is not available on the inject.
- **D-05:** Prefill is **UI-only**. Opening Health, loading defaults, or saving defaults must **not** call lint, must **not** run Fix/purge, and must **not** mutate notebook data. Success criterion 3 is mandatory.
- **D-06:** After prefill, the checkbox remains **run-scoped local state**. The user may toggle it for this visit without automatically overwriting saved defaults.
- **D-07:** Use an explicit **“Save as defaults”** control on the Health **action bar** (beside Run lint and the checkbox). Do **not** auto-save on every checkbox toggle — one-off run choices must not silently persist.
- **D-08:** Save lives on the Health panel only. Do **not** add Health defaults to the global User Settings dialog (`UserProfileForm`) in this phase.
- **D-09:** Save PATCHes the current user via `UserController.updateUser` with `apiCallWithLoading`. Because `UserDTO.name` is required, include `name` (and other existing UserDTO fields the update path already maps) from the current profile plus the updated `healthRemoveEmptyFoldersDefault`. After a successful save, update the injected `currentUser` so other notebooks in the same session prefill the new value without a full reload.
- **D-10:** Saving defaults must not call lint or Fix and must not mutate notebook content — only the user preference row.
- **D-11:** Defaults apply across notebooks: save on notebook A, open Health on notebook B → checkbox reflects the saved default. No notebook-scoped override in v1 (DFLT-10 is out of scope).
- **D-12:** Keep `POST .../health/lint` **bodyless** and report-only. Do **not** send defaults on the lint request. Fix enablement and purge remain Phase 7; this phase only persists and prefills the checkbox that Phase 7 will gate on.
- **D-13:** Backend: migration + User/UserDTO/updateUser mapping for the new boolean; focused controller/entity tests that GET profile / PATCH update round-trip the field (default false when unset).
- **D-14:** Frontend unit/component tests: Health panel prefills from user default; Save as defaults PATCHes and updates local/injected user; opening Health alone still does not call lint; save alone does not mutate notebook data.
- **D-15:** Targeted E2E (extend capability-named `notebook_health`): set checkbox → Save as defaults on one notebook → open Health on another notebook → checkbox matches. Tag `@wip` until green; remove `@wip` when scenarios pass. Do not run the full E2E suite unless required.

### Claude's Discretion
- Exact button label (“Save as defaults” vs “Save defaults”) — prefer **“Save as defaults”**.
- Whether prefill runs on panel `onMounted` vs watching tab activation — smallest change that seeds when Health is shown.
- Exact DaisyUI classes for the save control (secondary/ghost/sm) — match action-bar density from Phase 5 UI-SPEC; primary accent stays on **Run lint**.
- Whether `UserDTO` needs `@NotNull` on the new boolean — prefer optional with server default false so partial mental models stay simple; researcher/planner may follow existing DTO style.

### Deferred Ideas (OUT OF SCOPE)
- Fix button enablement + bulk empty-folder purge — Phase 7 (AFIX-02–05)
- Per-notebook overrides of user defaults — v2 (DFLT-10)
- Health defaults in global User Settings dialog — not needed for v1; Health panel is sufficient
- Auto-save on every checkbox toggle — rejected for this phase (explicit Save preserves one-off runs)
- Lint request body carrying fix options — still not required; Phase 7 may introduce a separate fix endpoint/body
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DFLT-01 | User can save Health lint/auto-fix option defaults at **user** level (apply across notebooks) | Flyway + `User`/`UserDTO`/`updateUser` mapping; Health **Save as defaults** PATCH; cross-notebook E2E |
| DFLT-02 | Opening Health prefills run options from the user’s saved defaults | Prefill checkbox from injected `currentUser.healthRemoveEmptyFoldersDefault` on Health panel show; no lint/fix on open |
</phase_requirements>

## Summary

Phase 6 is a thin **Behavior** slice on top of Phase 5’s Health action bar: persist one boolean preference on `User`, prefill the existing **Remove empty folders** checkbox when Health opens, and save via an explicit **Save as defaults** control. No Fix, no lint body, no notebook mutation on open or save. [VERIFIED: codebase + 06-CONTEXT.md]

The established preference path is already in production shape: `User` columns ↔ Lombok getters on serialized `User` ↔ `UserDTO` ↔ `UserController.updateUser` ↔ frontend `UserController.updateUser` + `apiCallWithLoading`. Health should copy that path for `healthRemoveEmptyFoldersDefault` / `health_remove_empty_folders_default`, then regenerate the OpenAPI TS client. [VERIFIED: User.java, UserDTO.java, UserController.java, UserProfileForm.vue]

**Primary recommendation:** Add Flyway `V300000232__…` boolean column (default `0`), extend `User`/`UserDTO`/`updateUser`, regen client, then in `NotebookHealthPanel` seed from `inject('currentUser')` on mount and PATCH with a full UserDTO-shaped body (name + existing prefs + new boolean), mutating the injected ref after success.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Persist Health default | API / Backend + Database | — | Preference is per logged-in user; Flyway column on `user` + PATCH mapping |
| Authorize user update | API / Backend | — | Existing `authorizationService.assertAuthorization(user)` on `updateUser` |
| Prefill checkbox | Browser / Client | — | UI-only seed from session `currentUser`; no notebook write |
| Save as defaults control | Browser / Client | API / Backend | Explicit CTA → `apiCallWithLoading` → `updateUser` |
| Cross-notebook consistency | Browser / Client + Database | — | Same user row; remount/open Health reads updated inject or reloaded profile |
| Lint / Fix / notebook graph | — (unchanged) | — | Out of scope; lint stays bodyless report-only |

## Project Constraints (from .cursor/rules/)

| Source | Directive planners must honor |
|--------|-------------------------------|
| `planning.mdc` | This phase is **one Behavior**; stop-safe; capability-named product artifacts (no phase numbers in code/tests); targeted E2E only; `@wip` until green; Jidoka / post-change-refactor / commit+push at phase close |
| `general.mdc` | Tooling via `CURSOR_DEV=true nix develop -c …`; assume `pnpm sut` running; keep changes simple |
| `db-migration.mdc` | New SQL under `backend/src/main/resources/db/migration/`; version **>** existing tip; never edit applied migrations; regenerate `docs/database-erd.md` after schema change |
| `backend-code.mdc` | Prefer returning entities; use import statements; verify with `pnpm backend:verify` / `backend:test` when migration involved |
| `backend-testing.mdc` | Prefer controller-boundary tests; use `makeMe`; run full backend unit suite for verification (not a single cherry-picked class only at gate) |
| `frontend-api.mdc` | Generated SDK only; `{ data, error }` pattern; `apiCallWithLoading` for Save; check `!error` before using `data` |
| `frontend-testing.mdc` | Vitest + `mockSdkService`; `data-testid`; `helper.withCurrentUser` for inject; prefer user-visible assertions |
| `e2e-authoring.mdc` | Capability-named `notebook_health`; page objects; `pnpm cypress run --spec …`; no full suite unless required |
| `generate-api-client` skill | After `User`/`UserDTO` change: `pnpm generateTypeScript`; never hand-edit generated client |
| `gsd-coexistence.mdc` | Local Behavior/Structure grammar and deploy gate override plain GSD defaults |

## Standard Stack

### Core

| Library / Tool | Version | Purpose | Why Standard |
|----------------|---------|---------|--------------|
| Spring Boot + JPA `User` | in-repo | Persist preference | Existing preference columns pattern [VERIFIED: User.java] |
| Flyway SQL migrations | in-repo | Schema change | Project DB versioning [VERIFIED: db-migration.mdc] |
| Jackson / springdoc OpenAPI | in-repo | Serialize `User` + `UserDTO` | Controllers return `User`; DTO for PATCH body [VERIFIED: UserController.java] |
| Vue 3 | 3.5.40 | Health panel UI | SPA stack [VERIFIED: frontend/package.json] |
| Generated `@generated/doughnut-backend-api` | regen | Typed `User` / `UserDto` / SDK | Never hand-edit [VERIFIED: generate-api-client skill] |
| Vitest | 4.1.10 | Component tests | Frontend unit gate [VERIFIED: frontend/package.json] |
| Cypress | 15.18.1 | Targeted E2E | Repo E2E runner [VERIFIED: package.json] |

### Supporting

| Library / Tool | Version | Purpose | When to Use |
|----------------|---------|---------|-------------|
| DaisyUI / Tailwind | in-repo | Action-bar button classes | Match Phase 5 UI-SPEC density |
| `apiCallWithLoading` | in-repo | Save loading + toasts | All user-initiated Save |
| Testability `updateCurrentUserSettingsWith` | in-repo | Optional E2E seed | Only if planner wants seed without UI; not required for D-15 UI path |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Column on `User` | `NotebookSettings` | Rejected — not cross-notebook [CONTEXT D-03; ARCHITECTURE anti-pattern 5] |
| New prefs microservice / JSON blob | Single boolean column | Overkill for one v1 option [CONTEXT D-01] |
| Auto-save on toggle | Explicit Save | Rejected — one-off runs must not persist [CONTEXT D-07] |
| Add field to `UserProfileForm` | Health-only Save | Rejected for v1 [CONTEXT D-08] |

**Installation:** None — no new npm/PyPI packages.

**Version verification:** N/A for new packages. Schema tip verified: highest migration is `V300000231__rename_index_content_to_readme_content.sql`; next must be **`V300000232` or higher**. [VERIFIED: `backend/src/main/resources/db/migration/`]

## Package Legitimacy Audit

> No external packages to install for this phase.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| — | — | — | — | — | — | N/A |

**Packages removed due to [SLOP] verdict:** none  
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
┌──────────────────────────────────────────────────────────────┐
│ Vue SPA                                                      │
│  DoughnutApp                                                 │
│    provide("currentUser") ← CurrentUserInfo.user (on load)   │
│         │                                                    │
│         ▼                                                    │
│  NotebookHealthPanel (Health tab, v-else-if remount)         │
│    onMounted: removeEmptyFolders = currentUser?.health…      │
│    Save as defaults → apiCallWithLoading(updateUser)         │
│         │  on success: currentUser.value = updated User      │
│         │  (no lint, no fix)                                 │
│    Run lint → bodyless POST .../health/lint (unchanged)      │
└───────────────────────────┬──────────────────────────────────┘
                            │ PATCH /api/user/{id}  UserDTO
                            │ GET  /api/user | /api/user/current-info
┌───────────────────────────▼──────────────────────────────────┐
│ UserController.updateUser / getUserProfile                   │
│ CurrentUserInfoController.currentUserInfo                    │
│   assertAuthorization(user) on PATCH                         │
│   map DTO fields → User → EntityPersister.save               │
└───────────────────────────┬──────────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────────┐
│ MySQL `user.health_remove_empty_folders_default` tinyint(1)  │
│ DEFAULT 0                                                    │
└──────────────────────────────────────────────────────────────┘
```

### Recommended Project Structure

```
backend/
  src/main/resources/db/migration/
    V300000232__add_health_remove_empty_folders_default.sql
  src/main/java/com/odde/doughnut/
    entities/User.java                    # + Boolean field
    controllers/dto/UserDTO.java          # + Boolean field
    controllers/UserController.java       # + set in updateUser
    (optional) services/UserService.java  # + setter if extending testability
frontend/src/components/notebook/
  NotebookHealthPanel.vue                 # prefill + Save as defaults
packages/generated/doughnut-backend-api/  # regen only
e2e_test/features/notebooks/
  notebook_health.feature                 # + cross-notebook scenario
```

### Pattern 1: Extend User preference columns (mirror existing)

**What:** Add `@Column` + Lombok getter/setter on `User`; matching field on `UserDTO`; one line in `updateUser`.  
**When to use:** Any per-user preference across notebooks.  
**Example:**

```java
// Source: backend/.../entities/User.java (existing pattern)
@Column(name = "daily_assimilation_count")
@Getter @Setter
private Integer dailyAssimilationCount = 15;

// Phase 6 addition (prescriptive)
@Column(name = "health_remove_empty_folders_default")
@Getter @Setter
private Boolean healthRemoveEmptyFoldersDefault = false;
```

```java
// Source: backend/.../controllers/UserController.java updateUser
user.setName(updates.getName());
user.setSpaceIntervals(updates.getSpaceIntervals());
user.setDailyAssimilationCount(updates.getDailyAssimilationCount());
user.setHealthRemoveEmptyFoldersDefault(
    updates.getHealthRemoveEmptyFoldersDefault() != null
        ? updates.getHealthRemoveEmptyFoldersDefault()
        : false);
```

### Pattern 2: Prefill from inject; Save mutates same ref

**What:** `inject<Ref<User | undefined>>("currentUser")`; seed local `ref`; after PATCH assign `currentUser.value = data`.  
**When to use:** Session-wide preference without full reload.  
**Example:**

```typescript
// Source: frontend DoughnutApp provide + UserProfileForm update pattern
const currentUser = inject<Ref<User | undefined>>("currentUser")
const removeEmptyFolders = ref(false)

onMounted(() => {
  removeEmptyFolders.value =
    currentUser?.value?.healthRemoveEmptyFoldersDefault ?? false
})

async function saveAsDefaults() {
  const user = currentUser?.value
  if (!user) return
  const { data, error } = await apiCallWithLoading(() =>
    UserController.updateUser({
      path: { user: user.id },
      body: {
        name: user.name,
        dailyAssimilationCount: user.dailyAssimilationCount,
        spaceIntervals: user.spaceIntervals,
        healthRemoveEmptyFoldersDefault: removeEmptyFolders.value,
      },
    })
  )
  if (!error && currentUser) {
    currentUser.value = data!
  }
}
```

### Pattern 3: Health panel remount as prefill trigger

**What:** `NotebookPageView` uses `v-else-if="activeTab === 'health'"` so opening Health remounts the panel — `onMounted` is sufficient for “when Health is shown.” [VERIFIED: NotebookPageView.vue]  
**When to use:** Prefer over watching tab ids elsewhere.  
**Fallback:** Only if inject lacks the field after regen failure, call `UserController.getUserProfile` once (D-04).

### Anti-Patterns to Avoid

- **NotebookSettings for user defaults** — wrong scope [ARCHITECTURE anti-pattern 5]
- **Auto-save on checkbox toggle** — silent preference overwrite [CONTEXT D-07]
- **Sending defaults on lint body / calling lint on open or save** — violates lint≠fix and success criterion 3 [CONTEXT D-05/D-10/D-12]
- **PATCH with only `{ healthRemoveEmptyFoldersDefault }`** — `name` is `@NotNull`; omitted other fields can reset to DTO Java defaults (`dailyAssimilationCount=15`, default `spaceIntervals` string) because `updateUser` always copies those fields [VERIFIED: UserDTO.java + UserController.updateUser]
- **Hand-editing generated OpenAPI client** — regen only [generate-api-client]
- **Adding Fix / purge** — Phase 7
- **Putting Health defaults in `UserProfileForm`** — deferred [CONTEXT D-08]

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| User prefs storage | New prefs table/service | `User` column + `UserDTO` | Existing cross-notebook pattern |
| Session user cache | Pinia store for prefs | `provide`/`inject` `currentUser` | Already loaded at app start |
| API client types | Manual TS interfaces | `pnpm generateTypeScript` | Drift risk |
| Loading/errors on Save | Custom modal/spinner | `apiCallWithLoading` | Thin bar + toasts |
| Boolean DB type | Custom enum/char | `tinyint(1) NOT NULL DEFAULT 0` | Matches baseline booleans |

**Key insight:** Defaults only **seed UI run options**; they must never become an implicit Fix or lint mutation path.

## Runtime State Inventory

> Migration / preference-column phase — runtime leftovers after source edits.

| Category | Items Found | Action Required |
|----------|-------------|-----------------|
| Stored data | Existing `user` rows lack new column until migration | Flyway `ADD COLUMN … NOT NULL DEFAULT 0` — no backfill script; all existing users get `false` |
| Live service config | None for this preference (no n8n/Datadog tags) | None — verified by absence of Health defaults outside `User` |
| OS-registered state | None | None — verified (preference is DB-only) |
| Secrets/env vars | None keyed by this field name | None |
| Build artifacts | `packages/generated/doughnut-backend-api` OpenAPI types/`User`/`UserDto` | Must regen after backend change; ERD `docs/database-erd.md` after migration |

## Common Pitfalls

### Pitfall 1: Defaults enable silent mutation

**What goes wrong:** Opening Health or saving defaults triggers lint/fix/delete.  
**Why it happens:** “Health” language conflates audit with cure [PITFALLS.md Pitfall 1 / Anti-pattern 3].  
**How to avoid:** Prefill and Save touch only user preference + local checkbox; assert lint spy not called; keep lint bodyless.  
**Warning signs:** E2E where folders disappear after open/save without Fix.

### Pitfall 2: Partial PATCH wipes other preferences

**What goes wrong:** Save sends only the new boolean; `spaceIntervals` / assimilation count reset.  
**Why it happens:** `updateUser` unconditionally sets all mapped DTO fields; DTO field initializers apply when JSON omits properties. [VERIFIED: UserController + UserDTO]  
**How to avoid:** Always send `name`, `dailyAssimilationCount`, `spaceIntervals`, and `healthRemoveEmptyFoldersDefault` from current profile (same as `UserProfileForm` sending full user-shaped body).  
**Warning signs:** User reports spaced-repetition intervals reset after Health Save.

### Pitfall 3: Forgetting OpenAPI regen

**What goes wrong:** Frontend cannot type/access `healthRemoveEmptyFoldersDefault`.  
**Why it happens:** Generated client lags Java controllers.  
**How to avoid:** Backend change → `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` → frontend tests.  
**Warning signs:** TS errors on `User` / `UserDto`; runtime `undefined` forever.

### Pitfall 4: Wrong Flyway version

**What goes wrong:** Collision with `V300000231` already used for readme rename.  
**Why it happens:** Placeholder comments still mention `V300000231` as “next.” [VERIFIED: migration dir]  
**How to avoid:** Use **`V300000232__add_health_remove_empty_folders_default.sql`** (or higher).  
**Warning signs:** Flyway checksum/version conflict on migrate.

### Pitfall 5: Inject not updated after Save

**What goes wrong:** Notebook B in same SPA session still shows old unchecked default until full reload.  
**Why it happens:** Prefill reads `currentUser`; Save only hits DB.  
**How to avoid:** Assign `currentUser.value = data` after successful PATCH (D-09).  
**Warning signs:** Cross-notebook E2E fails unless hard refresh.

### Pitfall 6: Auto-save or User Settings scope creep

**What goes wrong:** Toggle autosaves or Health fields appear in `UserProfileForm`.  
**Why it happens:** Feels convenient.  
**How to avoid:** Honor D-07/D-08; one explicit Save button on Health action bar only.

## Code Examples

### Flyway migration (prescriptive)

```sql
-- V300000232__add_health_remove_empty_folders_default.sql
-- Source: baseline tinyint(1) boolean style + db-migration.mdc versioning
ALTER TABLE `user`
  ADD COLUMN `health_remove_empty_folders_default` tinyint(1) NOT NULL DEFAULT 0;
```

### UserDTO field (optional Boolean, no @NotNull)

```java
// Source: existing UserDTO style for non-name fields
@Getter @Setter
private Boolean healthRemoveEmptyFoldersDefault = false;
```

### Backend controller test sketch

```java
// Source: UserControllerTest.updateUserSuccessfully pattern
@Test
void updateUserPersistsHealthRemoveEmptyFoldersDefault() throws Exception {
  UserDTO dto = new UserDTO();
  dto.setName(currentUser.getUser().getName());
  dto.setSpaceIntervals(currentUser.getUser().getSpaceIntervals());
  dto.setDailyAssimilationCount(currentUser.getUser().getDailyAssimilationCount());
  dto.setHealthRemoveEmptyFoldersDefault(true);

  User response = controller.updateUser(currentUser.getUser(), dto);
  assertThat(response.getHealthRemoveEmptyFoldersDefault(), equalTo(true));
  assertThat(controller.getUserProfile().getHealthRemoveEmptyFoldersDefault(), equalTo(true));
}

@Test
void newUserHealthRemoveEmptyFoldersDefaultIsFalse() {
  assertThat(controller.getUserProfile().getHealthRemoveEmptyFoldersDefault(), equalTo(false));
}
```

### Frontend component test sketch

```typescript
// Source: NotebookHealthPanel.spec.ts + RenderingHelper.withCurrentUser
it("prefills Remove empty folders from currentUser without calling lint", async () => {
  const wrapper = helper
    .component(NotebookHealthPanel)
    .withProps({ notebookId: 42 })
    .withCurrentUser({
      id: 1,
      name: "Learner",
      externalIdentifier: "learner",
      healthRemoveEmptyFoldersDefault: true,
    })
    .mount()
  await flushPromises()
  const checkbox = wrapper.get(
    '[data-testid="notebook-health-remove-empty-folders"] input[type="checkbox"]'
  )
  expect((checkbox.element as HTMLInputElement).checked).toBe(true)
  expect(lintSpy).not.toHaveBeenCalled()
})
```

### E2E acceptance sketch

```gherkin
@wip
Scenario: Save Remove empty folders default applies on another notebook
  Given I have a notebook "Defaults A" with a note "A1"
  And I have a notebook "Defaults B" with a note "B1"
  When I open the notebook "Defaults A" from my notebooks catalog
  And I open the notebook workspace Health tab
  And I check Remove empty folders on the notebook health panel
  And I save notebook health options as defaults
  And I open the notebook "Defaults B" from my notebooks catalog
  And I open the notebook workspace Health tab
  Then Remove empty folders on the notebook health panel is checked
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Local `ref(false)` only (Phase 5) | Prefill from `User` + explicit Save | Phase 6 | Cross-notebook convenience without silent mutation |
| Research SUMMARY “two booleans” | **One** boolean (CONTEXT D-01) | Discuss 2026-07-22 | Do not invent enable-auto-fix flag |
| Milestone research phase numbers (defaults as “Phase 5”) | Roadmap Phase 6 | Roadmap lock | Prefer ROADMAP/CONTEXT over older SUMMARY phase labels |

**Deprecated/outdated:**
- HealthUserDefaultsService from early ARCHITECTURE diagram — not required; map directly in `updateUser`.
- Saving Health defaults in global User Settings — deferred.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Navigating notebook A→B remounts `NotebookPageView` / Health panel so `onMounted` re-runs | Architecture Patterns | If instance reused while staying on Health, need `watch(notebookId)` to re-seed — low risk; E2E opens Health tab again |
| A2 | Optional testability key for seeding is not required for D-15 | Validation / E2E | Planner may still extend `testability_update_user` for convenience |

**If only A1–A2:** Verified claims dominate; confirm remount behavior only if a SPA edge case appears during execute.

## Open Questions

1. **Should testability seed the new preference?**
   - What we know: `updateCurrentUserSettingsWith` already seeds `daily_assimilation_count` / `space_intervals` via snake_case keys [VERIFIED: TestabilityRestController].
   - What's unclear: Whether E2E wants a non-UI seed path in addition to Save.
   - Recommendation: **Do not block** on it; D-15 is UI Save → other notebook. Optional follow-up if useful for isolation.

2. **UI-SPEC for Phase 6 Save button?**
   - What we know: Phase 5 UI-SPEC reserves primary for Run lint; Save is discretion (secondary/ghost/sm); label **Save as defaults**; add `data-testid` e.g. `notebook-health-save-defaults`.
   - What's unclear: Whether `/gsd-ui-phase` will produce `06-UI-SPEC.md` before plan.
   - Recommendation: Planner can proceed with DaisyUI secondary/ghost sm matching action bar; UI-SPEC can refine later without blocking Behavior.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Nix + `CURSOR_DEV=true nix develop` | All repo tooling | ✓ | nix present | Cloud VM skill if no Nix |
| Node | Frontend / Cypress | ✓ | v24.5.0 | — |
| MySQL (via `pnpm sut` / process-compose) | Flyway + backend tests | Assume running | — | Inspect `mysql/mysql.log`; start `pnpm sut` |
| Redis | App runtime | Assume via sut | — | Inspect `redis/redis.log` |
| `pnpm generateTypeScript` | Client regen | ✓ (script in root package.json) | — | — |

**Missing dependencies with no fallback:**
- None for planning; execution assumes `pnpm sut` as per project contract.

**Missing dependencies with fallback:**
- Host `mysql` CLI not on PATH outside Nix — use Nix shell / app-managed MySQL, not host CLI.

**Step 2.6 note:** Graphify disabled in this workspace (`graphify.enabled` false) — no graph queries used.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Backend: JUnit 5 + Spring Boot Test; Frontend: Vitest 4.1.10; E2E: Cypress 15.18.1 + Cucumber |
| Config file | Backend: Spring test profile; Frontend: Vitest in `frontend/`; E2E: `e2e_test/config/ci.ts` |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/notebook/NotebookHealthPanel.spec.ts` and backend focused class via full `backend:test_only` after migration |
| Full suite command | `CURSOR_DEV=true nix develop -c pnpm backend:test` (includes migrate) + `pnpm frontend:test` + targeted `pnpm cypress run --spec e2e_test/features/notebooks/notebook_health.feature` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DFLT-01 | PATCH persists boolean; Save UI | unit (controller) + component | `pnpm backend:test` / `pnpm frontend:test …NotebookHealthPanel.spec.ts` | ❌ extend `UserControllerTest` + panel spec |
| DFLT-01 | Cross-notebook save | e2e | `pnpm cypress run --spec e2e_test/features/notebooks/notebook_health.feature` | ❌ add scenario (`@wip` until green) |
| DFLT-02 | Prefill from user default | component | same frontend test file | ❌ add cases |
| DFLT-02 / SC3 | Open/save does not call lint | component | assert `lint` spy not called on mount/save | ❌ extend existing “no lint on mount” |
| Regression | Existing Health Run scenarios | e2e | same feature file | ✅ `notebook_health.feature` |

### Sampling Rate

- **Per task commit:** targeted frontend spec and/or backend tests for touched slice
- **Per wave merge:** `backend:test` (migration involved) + `frontend:test` for Health panel
- **Phase gate:** targeted `notebook_health` E2E green; `@wip` removed; no full E2E suite unless required

### Wave 0 Gaps

- [ ] Extend `UserControllerTest` for default `false` + PATCH round-trip of `healthRemoveEmptyFoldersDefault`
- [ ] Extend `NotebookHealthPanel.spec.ts` for prefill, Save PATCH body, no lint on save/open
- [ ] E2E page object methods: `saveAsDefaults()`, `expectRemoveEmptyFoldersChecked()`
- [ ] E2E steps for Save + checkbox assertion
- [ ] New scenario in `notebook_health.feature` tagged `@wip` until green
- [ ] Optional: `UserService` + testability key if seeding desired (not required for D-15)

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes (indirect) | Existing session / principal; endpoints already behind app auth |
| V3 Session Management | no change | — |
| V4 Access Control | yes | `authorizationService.assertAuthorization(user)` on `updateUser`; do not weaken |
| V5 Input Validation | yes | `@Valid UserDTO`; `name` `@NotNull`/`@Size`; boolean unconstrained beyond type |
| V6 Cryptography | no | Preference flag is not a secret |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Update another user’s prefs | Elevation / Tampering | Keep `assertAuthorization(user)`; existing foreign-user test |
| Defaults used to auto-delete folders | Tampering | Prefill UI only; no fix on open/save; Phase 7 gates Fix separately |
| Mass-assignment of privileged fields | Tampering | PATCH accepts only `UserDTO` fields (`@JsonIgnoreProperties(ignoreUnknown = true)`); do not put admin flags on DTO |
| Information disclosure via Health | Information Disclosure | Unchanged lint auth; this phase does not add Health endpoints |

## Sources

### Primary (HIGH confidence)

- `.planning/phases/06-user-level-defaults/06-CONTEXT.md` — locked D-01..D-15
- `backend/.../entities/User.java`, `UserDTO.java`, `UserController.java` — preference update pattern
- `frontend/src/DoughnutApp.vue`, `UserProfileForm.vue`, `NotebookHealthPanel.vue`, `NotebookPageView.vue` — inject + Health UI
- `backend/.../db/migration/*` — tip version `V300000231`
- `.cursor/rules/db-migration.mdc`, `frontend-api.mdc`, `e2e-authoring.mdc`
- `.cursor/skills/generate-api-client/SKILL.md`
- `.planning/research/ARCHITECTURE.md` Pattern 3; `.planning/research/PITFALLS.md` silent mutation

### Secondary (MEDIUM confidence)

- Flyway/MySQL `ADD COLUMN … tinyint(1) NOT NULL DEFAULT 0` community/docs patterns cross-checked with in-repo baseline booleans [CITED: documentation.red-gate.com/flyway MySQL; baseline.sql]
- Context7 Jackson databind property visibility (getters serialize by default unless `@JsonIgnore`) [CITED: /fasterxml/jackson-databind]

### Tertiary (LOW confidence)

- SPA remount assumption when switching notebooks while Health stays selected (A1)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all in-repo patterns verified
- Architecture: HIGH — locked CONTEXT + live code seams
- Pitfalls: HIGH — PITFALLS + verified PATCH/DTO behavior

**Research date:** 2026-07-22  
**Valid until:** 2026-08-21 (stable domain; re-check only if `User`/`updateUser` or Health panel reshaped)

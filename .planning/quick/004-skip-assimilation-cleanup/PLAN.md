# Skip-assimilation plan cleanup

**Status:** in-progress
**Type mix:** Structure only

Cleanup pass over the shipped assimilation-sequence skip plan (commits `1a9f574814`..`30da1de038`). Each phase is one Structure commit: no new behavior, existing tests stay green.

## Findings remaining

### 3. `MemoryTrackerInformation.vue` shows old `spelling` boolean (stale display)

Shows "Spelling: Yes/No" from the legacy boolean. Should show tracker `type` (`UNDERSTANDING` / `SPELLING` / `COMMISSIONED`).

## Phase index

| # | Type | One outcome | Status |
|---|---|---|---|
| 1 | Structure | Shared JDBC conversion test harness | done |
| 2 | Structure | Use exported selector constants in settings test support | done |
| 3 | Structure | Show tracker type instead of legacy spelling boolean | planned |

---

## Phase 1 — Shared JDBC conversion test harness

- **Type:** Structure
- **Status:** done

`DummySequenceSkipConversionTestBase` holds helpers, sibling seeding, default-gate no-op, and shared `@Nested WhenGateIsEnabled`. Concrete classes supply SQL path, gate, grain key, and the other-grain exclusion.

**Learning:** Nested classes must extend the base nested class so JUnit still discovers inherited tests. Shared `@BeforeEach` sibling seeding belongs on the base; `skipCount` is grain-parameterized.

**Tests:** both conversion classes (8 each) + `pnpm backend:test_only` green.

---

## Phase 2 — Use exported selector constants in settings test support

- **Type:** Structure
- **Status:** done

Settings helpers use `reviveButtonSelector` and `returnToSequenceButtonSelector`. Shared `propertyRowControl` lives in `assimilationPropertyTestSupport.ts`.

**Learning:** Five identical `querySelector` lookups collapsed onto one helper; settings named-button wrappers stay for the spec API.

**Tests:** `AssimilationSettings.spec.ts` (6) and `AssimilationPanel.property.spec.ts` (4) green.

---

## Phase 3 — Show tracker type instead of legacy spelling boolean

- **Type:** Structure
- **Status:** planned

**Change:** In `MemoryTrackerInformation.vue`, replace the "Spelling: Yes/No" row with a "Type:" row that displays `memoryTracker.type` (`UNDERSTANDING` / `SPELLING` / `COMMISSIONED`).

**Constraint:** No new behavior. The tracker page display changes from a stale boolean to the current enum. If any test pins the old "Spelling:" label, update it to "Type:".

**Tests:** `CURSOR_DEV=true nix develop -c pnpm frontend:test` (focused on MemoryTrackerPageView specs)

**Done when:** `MemoryTrackerInformation.vue` shows the tracker type, not the legacy spelling boolean.

---

## Out of scope

- Removing `showSkip` prop from `AssimilationButtons.vue` (always `true` in current usage, but it's a valid component API prop).
- Adding `blockUi` to `useRemoveFromRecall.ts` (matches the existing tracker-page pattern; deliberate consistency).
- Removing `assimilateDoesNotCreateTrackerRemovedFromRecall` test (valid regression guard for the Phase 16 flag drop; unique assertion).
- Changes to the gated migrations themselves (committed migrations are immutable).

## Stop-safe

| Stop after | User value | Waste if later phases never happen |
|---|---|---|
| 1 | Less duplicated test code | None |
| 2 | Consistent test selectors | None |
| 3 | Accurate tracker display | None |

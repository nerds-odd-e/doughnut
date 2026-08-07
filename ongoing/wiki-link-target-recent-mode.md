# Link-target Matches / Recent mode switch

## Status

Phase 1 done (committed after post-change-refactor). No further phases.

## Learned / friction

- Link-target search (`SearchForm` → `SearchForNoteAndFolder`) is the right surface; create-note (`embedSemanticToggle`) stays unchanged.
- Dead wiki links share the same dialog via `initialSearchKey` prefill.
- Post-change-refactor required splitting already-large files touched by the change (`SearchResults.vue`, `searchResultsModel.ts`, search E2E steps).

## Decision

Labeled **`[ Matches | Recent ]`** in the link-target results header. No API changes. Reuse `getRecentNotes`.

## Phase 1 — Behavior: switch to Recent while search key is non-empty

**Status:** done

**Pre:** Link-target dialog open; at least one recently updated note; search key non-empty (Matches showing).

**Trigger:** User selects **Recent**.

**Post:** Recently updated notes shown; search key unchanged; **Matches** restores search hits; Add link / dead-link flows unchanged.

### Shipped

- `listPreference` on display state (`auto` | `matches` | `recent`) via `searchDisplayState` + `SearchResultsModel`
- Matches/Recent toggle when not `embedSemanticToggle` (`SearchListModeToggle` / `useSearchListPreference`)
- Vitest: `SearchDialog.spec.ts`; E2E: `add_relationship.feature`
- Structure cleanup: extract merge/display helpers; move note-target search steps to `search.ts`

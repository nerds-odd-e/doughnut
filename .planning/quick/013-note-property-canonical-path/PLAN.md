# Note property canonical path

**Status:** planned, not started.
**Type:** ad-hoc plan (`.planning/quick/`)
**Do not execute until the developer approves.**
**Policy:** [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md) (**Property**, **Wiki link**), [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md) (`#prop:`), Proposed [ADR 0005](../../../docs/adrs/0005-web-routes.md) (`noteProperty`).

## Goal

A property has one web location: the note page with that property open
(`noteProperty`). Later expansion stays on that path (or a child). Next to
assimilate, answered question, and memory tracker use this route — not a
side channel on `noteShow`. Portable spelling is `#prop:` (ADR 0004).

## Requirement (what to replace)

Today a property is not a location. Several parallel tricks exist:

| Surface | Current | Replace with |
|---|---|---|
| Next to assimilate (property unit) | `openForNote(id, key)` + `push(noteShow)` + `pendingPropertyKey` (highlight, expand options, scroll; assimilation settings stay closed) | `push(notePropertyLocation(id, key))`; focused property from the route |
| Property value dialog | Local `valuePopupOpen` on the row | Open iff current location is that key; opening **replaces** to `noteProperty`, closing **replaces** to `noteShow` |
| Answered question / memory tracker | `NoteTitleWithLink` → always `noteShow` | When `focusedPropertyKey` is set → `noteProperty` |
| Conversation on a property | Toolbar always `noteShow` + `?conversation=` | Query on the **current** named route |
| Wiki / paste | Note tokens only; `/n{id}/p/…` is not an internal URL | `#prop:` compiles to `noteProperty`; paste of that SPA URL becomes `#prop:` wiki |

Do **not** keep `pendingPropertyKey` once the route is the source of truth.

## Design decisions

- **Name** `noteProperty`. **Table path** `/n:noteId(\\d+)/p/:propertyKey` (compact child of `/n:noteId`). Param is the authored YAML key (percent-encoded; do not slugify).
- **Same page** as `noteShow` (`NoteShowPage`), nested as a **child** of the existing `/n:noteId` sidebar parent so open/close does not remount notebook chrome.
- **Open** means: that row is selected (reuse `data-test-pending` / options expanded / scroll) **and** the value dialog is open when the key is text-capable. Missing key: note shows, property unresolved (no dialog).
- **replace** for panel open/close; **push** for inbound (assimilate, recall links, wiki click).
- Helpers `notePropertyLocation` / `notePropertyHref` next to `noteShowLocation` / `noteShowHref`. Classifier `pathnameLooksLikeInternalNoteShow` (or a sibling) treats `/n{id}/p/…` as internal.
- Legacy `/n/:noteId/p/:propertyKey` redirects to the compact child (same honesty as `/n/:id` → `/n{id}`).
- Wiki: `#prop:` marker (not bare `#`). Live property token → `noteProperty`; live note token → `noteShow`. Unresolved does not navigate.
- Conversation stays query. Closing conversation must not drop `noteProperty`.

**Out of scope:** same-note `[[#prop:key]]`; heading fragments; list-item indexes; a second `/properties/…` tree; portable insert UI beyond paste + authored `#prop:` in markdown.

## Testing

Capability-named artifacts only. E2E: Given may `push` a named location; triggers prefer UI. Compile hrefs from helpers — no second path dialect.

- Visit / open-close: `e2e_test/features/note_topology/note_property.feature` (new).
- Next assimilate: extend `e2e_test/features/recall/property_memory_tracker.feature` (pending-property scenarios).
- Tracker / answered-question link: same feature (tracker page already has “note under question”) plus a recall answered-question path if one already mounts `NoteUnderQuestion`.
- Wiki: extend `e2e_test/features/note_topology/wiki_link.feature`.
- Route table / helpers / paste: Vitest (`routes.spec.ts`, location helpers, strip-paste).

## Slices

### 1. Nested `noteProperty` route — **Structure** — planned

Add `noteProperty` as a child of the `/n:noteId` sidebar parent; helpers; classifier; `/n/:id/p/:key` redirect. Visiting it renders the same note page with `propertyKey` unused. Existing `noteShow` tests still pass.

### 2. Visiting `noteProperty` opens that property — **Behavior** — planned

**Pre:** note with a text-capable property. **Trigger:** open `noteProperty` (Given: named location). **Post:** that row is selected and the value dialog is open. Drive selection from the route, not `pendingPropertyKey`.

### 3. Opening or closing the property updates the location — **Behavior** — planned

**Pre:** on `noteShow`, property exists. **Trigger:** open the value panel. **Post:** location is `noteProperty` (`replace`). Close → `noteShow`. Conversation query is applied to the current named route (opening conversation from `noteProperty` does not jump to bare `noteShow`).

### 4. Next to assimilate a property uses `noteProperty` — **Behavior** — planned

**Pre:** next unit is a property. **Trigger:** start / continue assimilation from the menu. **Post:** location is `noteProperty` for that key; assimilation settings stay closed; selected-row UX still holds. `useGoToNextAssimilation` pushes `notePropertyLocation` when `propertyKey` is set. Remove `pendingPropertyKey` / `openForNote(…, key)` as a nav side channel. Update `useGoToNextAssimilation` unit tests and property-queue E2E.

### 5. Answered question and memory tracker link to `noteProperty` — **Behavior** — planned

**Pre:** property-keyed recalled note (`focusedPropertyKey` set). **Trigger:** follow the note link on answered question or memory tracker. **Post:** `noteProperty` with that property open. Note-level trackers still use `noteShow`. One link helper (extend `NoteTitleWithLink` or the breadcrumb additional slot) — do not fork two `:to` dialects.

### 6. Live `#prop:` wiki goes to `noteProperty` — **Behavior** — planned

**Pre:** body (or property value) has a live `[[Title#prop:key]]` / path-Markdown dual. **Trigger:** click. **Post:** `noteProperty`. HTML `href` compiled from `notePropertyHref`. Unresolved `#prop:` does not navigate. Note-only wiki unchanged.

### 7. Paste of `noteProperty` URL becomes `#prop:` wiki — **Behavior** — planned

**Pre:** paste a compiled property location into note content. **Trigger:** paste/strip. **Post:** stored wiki `[[Title#prop:key]]` (not a SPA URL, not a note-only wiki). Extend the internal-URL classifier so `/n{id}/p/…` is not left as a raw href.

## Discoveries

- `noteShow` is already a named child under `NotebookSidebarLayout` at `/n:noteId`. A **sibling** metadata row at `/n:noteId/p/:key` would create a second layout parent and remount chrome on every panel toggle — nest instead.
- `pathnameLooksLikeInternalNoteShow` anchors `/n\d+$`, so `/n123/p/…` is not treated as internal today (paste/classifier).
- Next-assimilate already distinguishes property units (settings off, pending row). Only the **destination** is the wrong language (`noteShow` + memory).
- Conversation toolbar hard-codes `noteShowLocation`; must follow ADR 0005 “query on **that** named route.”
- Dummy route records are a flat map of `routeMetadata`; nested production children still need a metadata row (or mapper change) so `notePropertyHref` compiles honestly.

```
## SLICE PLAN WRITTEN
```

# Note property canonical path

**Status:** in progress (slices 1–18 done; 19–20 remaining).
**Type:** ad-hoc plan (`.planning/quick/`)
**Policy:** [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md) (**Property**, **Property panel**, **Wiki link**), [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md) (`#prop:`), Proposed [ADR 0005](../../../docs/adrs/0005-web-routes.md) (`noteProperty`).
**Human-owned exception (2026-08-29):** ADR 0001 / ADR 0004 may depend on
Proposed ADR 0005 while this route policy is being refined. Do not change
ADR 0005 status as part of execution.

## Goal

A property has one web location: the note page with that **property panel**
open (`noteProperty`). Later expansion stays on that path (or a child). Next
to assimilate, answered question, and memory tracker use this route — the
property panel is how that location looks, so skip/assimilate are on it.
Portable spelling is `#prop:<encoded-key>` (ADR 0004).

The **property value dialog** edits a text-capable value. It is not a
location.

## Requirement (what to replace)

| Surface | Current | Replace with |
|---|---|---|
| `noteProperty` presentation | Focus plus value dialog on text-capable rows | Focus plus **property panel** on editable rows |
| Property panel | Local expander on the row | Open iff current location is that key; opening **replaces** to `noteProperty`, closing **replaces** to `noteShow` |
| Property value dialog | Opens with `noteProperty`; its open/close **replaces** the route | Local on the row; does not change the URL |
| Next to assimilate (property unit) | `push(noteProperty)`; skip/assimilate only after closing the value dialog | `push(noteProperty)`; skip/assimilate on the open **property panel**; assimilation settings stay closed |
| Answered question / memory tracker | `NoteTitleWithLink` → `noteProperty` when `focusedPropertyKey` is set | Same route; arrival shows the **property panel** |
| Wiki / paste | Live `#prop:` compiles to `noteProperty` | Paste still must resolve note id to portable identity and keep label as display only |

Conversation query, note-route family, missing-key banner, rename/delete
location follow, property-panel presentation, and live `#prop:` compile
already shipped. Remaining: note-identity rewrite of the `#prop:` suffix,
and paste.

## Design decisions

- **Name** `noteProperty`. **Route-table path**
  `/n:noteId(\\d+)/p/:propertyKey` (compact child of `/n:noteId`). The
  helper accepts the exact decoded non-empty YAML key and Vue Router encodes
  it as one path parameter; do not pre-encode, slugify, or normalize case.
  Route consumers use Vue Router's decoded param. Portable `#prop:` tokens
  separately use ADR 0004's strict UTF-8 `%HH` component codec.
- **Same page** as `noteShow` (`NoteShowPage`), with `noteShow` and
  `noteProperty` as sibling children of one `/n:noteId` sidebar parent so
  open/close does not remount notebook chrome. They share one note-route-family
  predicate / metadata contract used by routing, sticky active realm, drawer,
  and main-navigation state.
- **Property panel** is the visible presentation of `noteProperty`. `isFocused`
  from `useFocusedNoteProperty` opens it on an editable row. Read-only rows
  stay focused and scrolled. Do not reuse assimilation "pending" names or
  selectors.
- The **property value dialog** is local editing chrome. Opening it does
  not replace the route; closing it does not replace to `noteShow`.
- A readable note with a **missing property** stays on `noteProperty` and
  shows a visible `Property "<key>" not found` state. An intentional deletion
  of the currently focused property replaces to `noteShow`. A successful key
  rename replaces to `noteProperty` with the new key. Existing authored
  `#prop:` links to the old key deliberately become unresolved; automatic
  inbound property-link rewrite is out of scope.
- **replace** for property-panel open/close, focused-key rename/delete, and
  conversation query changes; **push** for inbound navigation (assimilate,
  recall links, wiki click). Preserve unrelated query values across property
  transitions.
- Helpers `notePropertyLocation` / `notePropertyHref` live next to
  `noteShowLocation` / `noteShowHref`. A route-family classifier treats
  `/n{id}/p/…` as internal. Legacy `/n/:noteId/p/:propertyKey` redirects
  to the compact child while preserving query and hash. Parse pasted SPA
  URLs through the route table so the property param is decoded once; do not
  add a second path parser/decoder.
- Wiki property targets use one codec: note target plus literal `#prop:` plus
  the ADR 0004 encoded key. Java resolution / cache / health / rewrite and
  TypeScript render / click / paste use the same examples and edge-case
  contract. A property token is live only when the note is readable and its
  exact decoded property key currently exists. The existing resolved-link
  cache remains the only cache and cannot keep a deleted/renamed property live.
- Note title, folder, and notebook rewrites transform only the note-target
  portion and preserve the encoded property suffix and display text.
- Paste never uses anchor text as note identity. Resolve the SPA note id to
  the portable note target, qualify it for a different source notebook, append
  the encoded property key, and use anchor text only as optional display.
- Conversation stays query on the current note-family route. Opening/closing
  either conversation or the property panel preserves the other state.

**Out of scope:** same-note `[[#prop:key]]`; heading fragments; list-item
indexes; a second `/properties/…` tree; portable property insert UI beyond
paste + authored `#prop:` in markdown; automatic rewrite of inbound property
links when a property key is renamed.

## Testing

Capability-named artifacts only. E2E: Given may `push` a named location; triggers prefer UI. Compile hrefs from helpers — no second path dialect.

Assert the **property panel** (or the location) as the unique claim. Do not
add tests whose unique claim is that the property value dialog is closed or
that it does not change the URL.

- Visit / open-close / read-only / missing: `e2e_test/features/note_topology/note_property.feature`.
- Next assimilate: `e2e_test/features/recall/property_memory_tracker.feature`; skip
  and assimilate on the property panel.
- Tracker / answered-question link: same feature; arrival is `noteProperty`
  with the property panel open.
- Wiki live/dead, target note rename/move, and cache freshness:
  `e2e_test/features/note_topology/property_wiki_link.feature` (property
  tokens) plus `wiki_link.feature` (note-only) and backend small tests
  through resolver/controller boundaries.
- Route table / family / helpers / paste / property-key codec: Vitest
  (`routes.spec.ts`, `noteRouteFamily.spec.ts`, mounted property components,
  authored-link helpers, strip-paste). Backend changes run the full backend
  unit suite per repo rules.
- Codec fixtures cover spaces, Unicode, `/`, `%`, `|`, `]`, `?`, `#`, and
  mixed-case keys; Java and TypeScript use the same input/output examples.

## Slices

### 1. Consolidate the existing note route family — **Structure** — done

Shared `/n:noteId` sidebar parent via `routeRecordsFromMetadata`; family
contract `isNoteRouteFamily` / `noteRouteFamilyNoteId`; classifier
`pathnameLooksLikeInternalNoteFamily`. Sticky realm, drawer, and main-nav
consume it. `noteProperty` is not registered; `/n123/p/…` is still not
internal until slice 2 adds the child.

### 2. Visiting `noteProperty` focuses an editable property — **Behavior** — done

Named `noteProperty` at `/n:noteId(\\d+)/p/:propertyKey` (sibling of
`noteShow`). Helpers `notePropertyLocation` / `notePropertyHref`. Focus via
`useFocusedNoteProperty` + `isFocusedProperty`. E2E:
`e2e_test/features/note_topology/note_property.feature`. Legacy
`/n/:noteId/p/:propertyKey` redirects with query/hash. `/n123/p/…` is now
internal. Pending-property dual-ref wiring remains until slice 9.

### 3. Visiting a read-only or specialized property keeps visible focus — **Behavior** — done

Read-only list uses `useFocusedNoteProperty`. Specialized (`wikidata_id`)
and subscribed read-only rows highlight and scroll without a value dialog.
E2E in `note_property.feature`. Focused-row Cypress assertion:
`expectRichNotePropertyRowFocused`.

### 4. A stale property location fails visibly — **Behavior** — done

Stay on `noteProperty`. Banner `Property "<decoded key>" not found` via
`RichFrontmatterPropertyNotFound`. No other dialog. E2E in
`note_property.feature`; steps in `e2e_test/step_definitions/note_property.ts`.

### 5. Property panel transitions update the location — **Behavior** — done

Open `replace`s to `noteProperty`; close `replace`s to `noteShow` via
`replaceKeepingQuery` on `RichFrontmatterScalarPropertyValue`. Unrelated
query is preserved. E2E in `note_property.feature`. Panel replace is
local to the value component. Conversation uses `currentRouteSettingConversation`.

### 6. Conversation preserves the current property location — **Behavior** — done

`currentRouteSettingConversation` is the current-route helper for toolbar,
overflow, and `handleCloseConversation`. Open/close `replace` query only.
E2E on specialized `wikidata_id` so the toolbar is reachable.

### 7. Renaming the focused property follows its new location — **Behavior** — done

Successful rename `replace`s to `noteProperty` with the new exact key via
`useFollowFocusedPropertyLocation`. Query preserved (`locationKeepingQuery`).
Property value dialog uses `closeOnRouteChange: false` so the replace does
not close the dialog. Inbound `#prop:` not rewritten.

### 8. Deleting the focused property returns to the note — **Behavior** — done

Focused-key removal `replace`s to `noteShow` via
`useFollowFocusedPropertyLocation` (`replaceWhenFocusedKeyRemoved`). Query
preserved. Deleting a different key stays on `noteProperty`. E2E in
`note_property.feature`.

### 9. Next to assimilate a property uses `noteProperty` — **Behavior** — done

`useGoToNextAssimilation` `push`es `notePropertyLocation` when `propertyKey`
is set; `dismiss()` keeps settings closed. `pendingPropertyKey`,
`usePendingAssimilationProperty`, and `data-test-pending` are gone.
E2E: `property_memory_tracker.feature`.

### 10. Answered question and memory tracker link to `noteProperty` — **Behavior** — done

`NoteTitleWithLink` compiles `noteProperty` when `focusedPropertyKey` is
set; otherwise `noteShow`. `NoteUnderQuestion` forwards the existing prop.
E2E: `property_memory_tracker.feature` (tracker + answered-question).
Answered-question MCQ under `@mockBrowserTime` flushes the mocked clock
before click so the click fires.

### 11. Java property-target codec preserves note-link behavior — **Structure** — done

`WikiLinkAuthoredTarget` parses/formats note target + optional `#prop:`
encoded key. Rewrites and `resolveAnyTargetWikiLinkToken` consume the
note-target portion. Path-shaped `:` trap is documented
(`WikiLinkTargetReferenceTest`). Encode pairs in
`WikiLinkAuthoredTargetTest` for TypeScript slice 16.

### 12. Visiting `noteProperty` opens the property panel — **Behavior** — done

Editable rows open the **property panel** from `isFocused`. Chevron
`replace`s via `useNotePropertyPanelLocation` (`noteProperty` / `noteShow`);
query preserved; already-at-this-property is a no-op. E2E:
`note_property.feature`.

### 13. Next to assimilate a property uses the property panel — **Behavior** — done

Arrival at `noteProperty` shows the **property panel**; skip, assimilate,
return-to-sequence, and remove-from-recall run from it with settings
closed. The property value dialog opens only from its own control and
does not replace. E2E: `property_memory_tracker.feature`.

### 14. Names match property panel and property value dialog — **Structure** — done

`RichFrontmatterPropertyRowOptions` → `RichFrontmatterPropertyPanel`. Gherkin,
test ids, and page objects use **property panel** vs **property value dialog**.
Unrelated popups unchanged.

### 15. Property wiki resolution requires the exact target property — **Behavior** — done

`WikiLinkPropertyMatch` requires the decoded exact YAML key. Absent,
invalid, and case-mismatched `#prop:` tokens stay unresolved in notebook
health. The one cache stores the full encoded token. Note-only links
unchanged.

### 16. TypeScript property-target codec preserves note-link behavior — **Structure** — done

`wikiLinkAuthoredTarget` matches Java encode pairs (ADR 0004). Note-only
render/click still compiles to `noteShow` via `wikiLinkResolvedLocation`.
Path-Markdown `#prop:` fragments are kept (not discarded by the
accept-check). Live `#prop:` compiles to `notePropertyHref` there.

### 17. Live `#prop:` wiki goes to `noteProperty` — **Behavior** — done

`wikiLinkResolvedLocation` compiles `#prop:` to `notePropertyHref`.
Click **push**es `noteProperty` with the **property panel** open.
Unresolved tokens do not navigate. Location follow on a property value
runs only on an actual key rename (not on wiki click/blur). E2E:
`property_wiki_link.feature`.

### 18. Removing a target property makes cached links unresolved — **Behavior** — done

`WikiTitleCacheRefresh` drops stale inbound rows; self-links rebuild with
the note. Old `#prop:` tokens are unresolved and do not navigate. Note-only
links stay live. E2E: `property_wiki_link.feature`.

### 19. Note identity changes preserve property-link suffixes — **Behavior** — planned

**Pre:** a live property token targets a note by title/path/qualification.
**Trigger:** rename the target note or move it across folder/notebook scope
using existing reference handling. **Post:** the rewritten link still targets
the same encoded property and retains authored display text. Extend existing
wiki rename/move scenarios rather than creating parallel rewrite tests.

### 20. Paste of `noteProperty` URL becomes a portable property wiki — **Behavior** — planned

**Pre:** paste a compiled property location into note content. **Trigger:**
paste/strip with the note id resolvable through storage/API. **Post:** stored
wiki uses the portable note target plus encoded key (and qualification when
cross-notebook); custom anchor text is display only. If identity cannot be
resolved, leave a normal link and surface the existing paste choice—never
invent identity from the label. Extend the internal-URL classifier so
`/n{id}/p/…` is not left as a raw SPA href after successful conversion.

## Discoveries

- Slice 1: `routeRecordsFromMetadata` groups sibling metadata under one
  sidebar parent (`noteShow` / `notebookPage` / `folderPage` paths from
  `routeMetadata`). Family tests live in `noteRouteFamily.spec.ts`.
  Classifier follows `/d/` and `/n/:id` redirects. Do not add a second
  path parser for property URLs.
- `NoteShowPage` does not contain a child `RouterView`. Register `noteProperty`
  as a sibling child of the shared `/n:noteId` parent, not a second layout.
- Vue Router named-param resolution round-trips decoded keys containing spaces,
  `/`, `%`, `|`, `]`, `?`, `#`, and Unicode through one path segment. Helpers
  must pass the decoded key exactly once; portable `#prop:` encoding remains a
  separate stricter serialization.
- Sticky realm, drawer, and main-nav consume `isNoteRouteFamily`; do not
  re-append `noteShow` to those lists when adding `noteProperty`.
- Missing-key stays on `noteProperty` with `RichFrontmatterPropertyNotFound`.
  Intentional deletion of the focused key still replaces to `noteShow`.
- `useFocusedNoteProperty` is the route-neutral focus seam. Editable rows
  present the **property panel** from `isFocused`. Replace lives in
  `useNotePropertyPanelLocation`. The property value dialog is local
  (own control; no route replace). Skip/assimilate/return/remove run on
  that panel.
- Slice 11: `WikiLinkAuthoredTarget` splits `#prop:` first so rewrites keep
  the encoded suffix. Path-shaped `:` still drops a non-`#prop:` suffix
  (`#heading`) — documented, not inherited by `#prop:`. Slice 15:
  `WikiLinkPropertyMatch` requires the decoded exact key; `link_text` is
  the full encoded token. TypeScript `wikiLinkAuthoredTarget` shares the
  Java encode pairs. `wikiLinkResolvedLocation` compiles `#prop:` to
  `notePropertyHref` / `notePropertyLocation`. Location follow on a
  property value runs only on actual key rename, not wiki click/blur.
  Slice 18: `WikiTitleCacheRefresh` drops inbound rows whose encoded
  token no longer matches; do not add a second cache.
- Current paste/strip code converts internal URLs with anchor text as the wiki
  target. A property URL contains only server note id + key, so correct portable
  conversion requires resolving the note's concept identity.
- A note title that itself contains the literal substring `#prop:` (e.g.
  `Foo#prop:bar`) cannot be the sole unqualified target of a link — the
  parser always splits on the first `#prop:` marker. Accepted as a trade-off
  in ADR 0004 rather than adding escaping.

```
## SLICE PLAN WRITTEN
```

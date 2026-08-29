# Note property canonical path

**Status:** planned, not started.
**Type:** ad-hoc plan (`.planning/quick/`)
**Do not execute until the developer approves.**
**Policy:** [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md) (**Property**, **Wiki link**), [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md) (`#prop:`), Proposed [ADR 0005](../../../docs/adrs/0005-web-routes.md) (`noteProperty`).
**Human-owned exception (2026-08-29):** ADR 0001 / ADR 0004 may depend on
Proposed ADR 0005 while this route policy is being refined. Do not change
ADR 0005 status as part of execution.

## Goal

A property has one web location: the note page with that property open
(`noteProperty`). Later expansion stays on that path (or a child). Next to
assimilate, answered question, and memory tracker use this route — not a
side channel on `noteShow`. Portable spelling is
`#prop:<encoded-key>` (ADR 0004).

## Requirement (what to replace)

Today a property is not a location. Several parallel tricks exist:

| Surface | Current | Replace with |
|---|---|---|
| Next to assimilate (property unit) | `openForNote(id, key)` + `push(noteShow)` + `pendingPropertyKey` (highlight, expand options, scroll; assimilation settings stay closed) | `push(notePropertyLocation(id, key))`; focused property from the route |
| Property value dialog | Local `valuePopupOpen` on the row | Open iff current location is that key; opening **replaces** to `noteProperty`, closing **replaces** to `noteShow` |
| Answered question / memory tracker | `NoteTitleWithLink` → always `noteShow` | When `focusedPropertyKey` is set → `noteProperty` |
| Conversation on a property | Toolbar always `noteShow` + `?conversation=` | Query on the **current** named route |
| Note-route chrome | Exact `route.name === "noteShow"` checks in sticky realm / drawer plus separate navigation-name lists | One note-route-family predicate or metadata contract covering `noteShow` and `noteProperty` |
| Read-only and missing properties | Only editable rows participate in pending-property focus; a missing key has no visible result | Route-neutral focus in editable and read-only presentation; explicit unresolved-property state |
| Wiki resolution / rewrite | Resolver and rewrite code treats the entire target as a note target | One authored-target codec separates note target and encoded property key; cache / health / rename / move use it |
| Wiki / paste | Note tokens only; `/n{id}/p/…` is not an internal URL; paste uses anchor label as target | `#prop:` compiles to `noteProperty`; paste resolves note id to portable identity and keeps label as display only |

Do **not** keep `pendingPropertyKey`, `usePendingAssimilationProperty`, or
`data-test-pending` once the route is the source of truth.

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
- **Focused property** is a route-neutral presentation state. Editable and
  read-only rows can be focused and scrolled into view. Editable text-capable
  rows also open the value dialog; specialized or read-only values remain
  visibly focused with their value available. Do not reuse assimilation
  "pending" names or selectors.
- A readable note with a **missing property** stays on `noteProperty` and
  shows a visible `Property "<key>" not found` state. An intentional deletion
  of the currently focused property replaces to `noteShow`. A successful key
  rename replaces to `noteProperty` with the new key. Existing authored
  `#prop:` links to the old key deliberately become unresolved; automatic
  inbound property-link rewrite is out of scope.
- **replace** for panel open/close, focused-key rename/delete, and conversation
  query changes; **push** for inbound navigation (assimilate, recall links,
  wiki click). Preserve unrelated query values across property transitions.
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
  either conversation or property preserves the other state.

**Out of scope:** same-note `[[#prop:key]]`; heading fragments; list-item
indexes; a second `/properties/…` tree; portable property insert UI beyond
paste + authored `#prop:` in markdown; automatic rewrite of inbound property
links when a property key is renamed.

## Testing

Capability-named artifacts only. E2E: Given may `push` a named location; triggers prefer UI. Compile hrefs from helpers — no second path dialect.

- Visit / open-close / read-only / missing: `e2e_test/features/note_topology/note_property.feature` (new).
- Next assimilate: extend `e2e_test/features/recall/property_memory_tracker.feature`; replace pending-property steps/assertions with route-focused property language.
- Tracker / answered-question link: same feature (tracker page already has “note under question”) plus a recall answered-question path if one already mounts `NoteUnderQuestion`.
- Wiki live/dead, target note rename/move, and cache freshness: extend `e2e_test/features/note_topology/wiki_link.feature` plus backend small tests through resolver/controller boundaries.
- Route table / family / helpers / paste / property-key codec: Vitest
  (`routes.spec.ts`, mounted property components, authored-link helpers,
  strip-paste). Backend changes run the full backend unit suite per repo rules.
- Codec fixtures cover spaces, Unicode, `/`, `%`, `|`, `]`, `?`, `#`, and
  mixed-case keys; Java and TypeScript use the same input/output examples.

## Slices

### 1. Consolidate the existing note route family — **Structure** — planned

Refactor current `noteShow` routing so one shared `/n:noteId` sidebar parent,
one note-route-family predicate / metadata contract, and one route-table-based
internal-URL classifier own note-page membership. Update sticky active realm,
mobile drawer, and main-navigation state to consume that contract. Do **not**
register `noteProperty` yet; existing `noteShow` URLs and behavior remain
unchanged. This structure exists only to enable slice 2.

### 2. Visiting `noteProperty` focuses an editable property — **Behavior** — planned

**Pre:** editable note with a text-capable property. **Trigger:** visit the
named `noteProperty` location. **Post:** the same note page and notebook chrome
remain mounted; the row is visibly focused and scrolled into view; its value
dialog is open. Register the route and helpers inside this slice—never expose
an inert route.

### 3. Visiting a read-only or specialized property keeps visible focus — **Behavior** — planned

**Pre:** readable note whose property is read-only or uses specialized UI.
**Trigger:** visit `noteProperty`. **Post:** its row/value is visibly focused
and scrolled into view without offering an invalid editor. Route-derived focus
is shared by editable and read-only property presentations.

### 4. A stale property location fails visibly — **Behavior** — planned

**Pre:** readable note without the routed property key. **Trigger:** visit
`noteProperty`. **Post:** note context remains visible and an explicit
property-not-found state names the decoded key; no other property dialog opens.

### 5. Property panel transitions update the location — **Behavior** — planned

**Pre:** on a note-family route with an existing property. **Trigger:** open or
close its value panel. **Post:** opening replaces to `noteProperty`; closing
replaces to `noteShow`; unrelated query values are preserved.

### 6. Conversation preserves the current property location — **Behavior** — planned

**Pre:** on `noteProperty`. **Trigger:** start or close the note conversation.
**Post:** only the conversation query changes; route name, note id, and focused
property key remain. Apply the same current-route helper to toolbar and overflow
conversation actions, and to `NoteShowPage.vue`'s `handleCloseConversation` — a
third hardcoded `noteShowLocation` call site (conversation close), not just the
toolbar's open action.

### 7. Renaming the focused property follows its new location — **Behavior** — planned

**Pre:** on `noteProperty` for an editable key. **Trigger:** successfully
rename that key through the existing memory-tracker guard/save flow. **Post:**
the route replaces to the same note with the new exact key and the property
remains focused. Authored links to the old key become unresolved by the policy
above; this slice does not rewrite them.

### 8. Deleting the focused property returns to the note — **Behavior** — planned

**Pre:** on `noteProperty` for an editable key. **Trigger:** confirm and save
that property's removal through the existing tracker guard. **Post:** the route
replaces to `noteShow`; unrelated query values remain.

### 9. Next to assimilate a property uses `noteProperty` — **Behavior** — planned

**Pre:** next unit is a property. **Trigger:** start / continue assimilation from the menu. **Post:** location is `noteProperty` for that key; assimilation settings stay closed; selected-row UX still holds. `useGoToNextAssimilation` pushes `notePropertyLocation` when `propertyKey` is set. Remove `pendingPropertyKey` / `openForNote(…, key)` as a nav side channel. Update `useGoToNextAssimilation` unit tests and property-queue E2E.

### 10. Answered question and memory tracker link to `noteProperty` — **Behavior** — planned

**Pre:** property-keyed recalled note (`focusedPropertyKey` set). **Trigger:** follow the note link on answered question or memory tracker. **Post:** `noteProperty` with that property open. Note-level trackers still use `noteShow`. One link helper (extend `NoteTitleWithLink` or the breadcrumb additional slot) — do not fork two `:to` dialects. `focusedPropertyKey` already flows from `RecalledNote.propertyKey` through `recalledNoteUnderQuestionProps` into `NoteUnderQuestion.vue`; today it only reaches a static `FocusedPropertyIndicator` text display and is not forwarded to `NoteTitleWithLink`. This slice wires that existing prop into the link — it is not adding a new field.

### 11. Java property-target codec preserves note-link behavior — **Structure** — planned

Introduce one domain-shaped Java authored-target parser/formatter: note target
plus optional encoded property key. Reuse the existing `PropertyKeyNaming` /
`NotePropertyIndex.propertyKey` domain concept for key semantics rather than
reinventing it — property keys are not a new concept, only the `#prop:`
token-splitting codec on the link-target string is. Refactor **both**
existing full-token rewrite families to consume its note-target portion:
`WikiLinkTargetReference` (rename/move: `replaceNoteTitle`,
`replaceFolderName`, `replaceNotebookName`) and `WikiLinkMarkdownRewrite`
(regex-level markdown splice). Also cover `WikiLinkResolver.resolveAnyTargetWikiLinkToken`
(the viewer-unaware path used for cross-notebook-move co-migration matching,
outside the main `resolveToken`/`resolveWikiLinkToken` path) — easy to miss.
Before layering the codec on, add a regression test for the existing bug this
codec must not inherit: `PathShapedTarget.tryParse` already returns
`Optional.empty()` for any target containing `:`, so `Qualified`
(`Notebook:Title`) rewrites already silently drop any suffix today — prove
this is understood, not accidentally relied on, once `#prop:` (which
contains `:`) is introduced. Prove every existing note-only output unchanged.
Pure contract tests establish encoded property parsing/formatting, but do not
make property tokens live or rewrite them yet. This structure exists only to
enable slice 12.

### 12. Property wiki resolution requires the exact target property — **Behavior** — planned

**Pre:** a note contains wiki and path-Markdown property tokens. **Trigger:**
save/load or lint it. **Post:** the note API resolves a token only when its note
is readable and its decoded exact property exists; absent/invalid/mismatched
keys are unresolved in notebook health. The one resolved-link cache stores the
full encoded token. Drive this through backend controller/resolver boundaries;
note-only links remain unchanged.

### 13. TypeScript property-target codec preserves note-link behavior — **Structure** — planned

Introduce the matching TypeScript authored-target codec and refactor current
note-only render/click helpers to consume the parsed note target with unchanged
outputs. Share the ADR examples and edge-case fixture table with Java tests.
`WikiLinkMarkdown.isConceptPathHref` currently strips and discards a path-Markdown
`#…` fragment purely to reject it during validation — the new codec must
intentionally keep what that check throws away, and the two code paths
(accept-check vs. extract) are coupled through the same regex, so this is an
easy spot to introduce a regression; add a targeted test. Do not change
property-link rendering yet. This structure exists only to enable slice 14.

### 14. Live `#prop:` wiki goes to `noteProperty` — **Behavior** — planned

**Pre:** the note API returns a resolved wiki or path-Markdown property token.
**Trigger:** render and click it in note body or a property value. **Post:** its
HTML `href` is compiled by `notePropertyHref` and click pushes
`noteProperty`; unresolved property tokens do not navigate. Note-only wiki
rendering remains unchanged.

### 15. Removing a target property makes cached links unresolved — **Behavior** — planned

**Pre:** a saved live property token and an existing resolved-link cache row.
**Trigger:** remove or rename the target property, then render or lint the
referring note. **Post:** the old token is dead/unresolved and does not
navigate; no second cache is introduced. Cover self-links and another-note
links so property-index/cache refresh order is honest.

### 16. Note identity changes preserve property-link suffixes — **Behavior** — planned

**Pre:** a live property token targets a note by title/path/qualification.
**Trigger:** rename the target note or move it across folder/notebook scope
using existing reference handling. **Post:** the rewritten link still targets
the same encoded property and retains authored display text. Extend existing
wiki rename/move scenarios rather than creating parallel rewrite tests.

### 17. Paste of `noteProperty` URL becomes a portable property wiki — **Behavior** — planned

**Pre:** paste a compiled property location into note content. **Trigger:**
paste/strip with the note id resolvable through storage/API. **Post:** stored
wiki uses the portable note target plus encoded key (and qualification when
cross-notebook); custom anchor text is display only. If identity cannot be
resolved, leave a normal link and surface the existing paste choice—never
invent identity from the label. Extend the internal-URL classifier so
`/n{id}/p/…` is not left as a raw SPA href after successful conversion.

## Discoveries

- `noteShow` is already a named child under `NotebookSidebarLayout` at
  `/n:noteId`. A separate top-level metadata row at `/n:noteId/p/:key`
  would create a second layout parent and remount chrome on every panel
  toggle. Build one parent with sibling `noteShow` / `noteProperty` children;
  `NoteShowPage` does not contain a child `RouterView`. Concretely,
  `routes.ts`'s `notebookSidebarNestedRouteNames` mechanism today maps
  each `routeMetadata` entry to its own implicit `{ parent, one child at
  path: "" }` pair; slice 1 must change that generation to group multiple
  metadata entries under one shared parent — that code-shape change is the
  crux of why slice 1 is Structure before slice 2 can register `noteProperty`.
- `pathnameLooksLikeInternalNoteShow` is an OR of three patterns — legacy
  `/^\/d\/n\/\d+(\/|$)/`, legacy `/^\/n\/\d+(\/|$)/`, and compact
  `/^\/n\d+$/` — none of which match `/n123/p/…`, so a property URL is not
  treated as internal today (paste/classifier). All three legacy shapes
  need equivalent treatment when this classifier is extended, not just the
  compact one.
- Vue Router named-param resolution round-trips decoded keys containing spaces,
  `/`, `%`, `|`, `]`, `?`, `#`, and Unicode through one path segment. Helpers
  must pass the decoded key exactly once; portable `#prop:` encoding remains a
  separate stricter serialization.
- `useStickyActiveNoteRealmForRoute`, `useNotebookSidebarDrawer`, and
  `useNavigationItems` all encode `noteShow` membership separately. Route
  family is a shared concept, not another appended name in three lists.
- Next-assimilate already distinguishes property units (settings off, pending
  row). The route replaces both the destination and the pending-property
  memory/selector language.
- Conversation toolbar hard-codes `noteShowLocation`; must follow ADR 0005 “query on **that** named route.”
- Dummy route records are a flat map of `routeMetadata`; nested production children still need a metadata row (or mapper change) so `notePropertyHref` compiles honestly.
- `WikiLinkTargetReference` currently resolves and rewrites the whole authored
  target as a note title. Without a property-target codec,
  `Title#prop:key` cannot resolve and title/folder/notebook rewrites can drop
  or corrupt the suffix. This is sharper than it sounds: `replaceNoteTitle`
  (qualified case) and `PathShapedTarget.tryParse` (which already returns
  `Optional.empty()` for any target containing `:`) mean a `Notebook:Title`
  rewrite can silently drop a suffix **today**, before `#prop:` exists —
  add a regression test for this pre-existing behavior before layering the
  codec on top, not as an incidental side effect of slice 11.
  `NotePropertyIndex.propertyKey` / `PropertyKeyNaming` already model
  property-key semantics and can be reused rather than reinvented.
- Current paste/strip code converts internal URLs with anchor text as the wiki
  target. A property URL contains only server note id + key, so correct portable
  conversion requires resolving the note's concept identity.
- `WikiLinkResolver.resolveAnyTargetWikiLinkToken` (used by
  `WikiLinkRewriteSupport.coMovedTargetResolvesFrom` for cross-notebook-move
  co-migration matching) is a second, viewer-unaware resolution path outside
  the main `resolveToken` / `resolveWikiLinkToken` path — easy to miss; slice
  11/12 must confirm it is covered too.
- A note title that itself contains the literal substring `#prop:` (e.g.
  `Foo#prop:bar`) cannot be the sole unqualified target of a link — the
  parser always splits on the first `#prop:` marker. Accepted as a trade-off
  in ADR 0004 rather than adding escaping.

```
## SLICE PLAN WRITTEN
```

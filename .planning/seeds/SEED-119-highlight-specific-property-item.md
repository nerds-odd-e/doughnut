---
id: SEED-119
status: active
planted: 2026-11-10
scope: story
---

# Highlight and link to a specific property item

<a id="highlight-and-link-to-a-specific-property-item"></a>

### Highlight and link to a specific property item

**Identity:** SEED-119#highlight-and-link-to-a-specific-property-item
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/087-highlight-specific-property-item/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"09c13e2ebfb30a8c53b9fc9ee4cd2aaee1dbaf3881418f0e265704ef120bf346","plan":"7132f5da2220c2e57a85db564b91c5349b89439381efc501e29fde38d7b47c52"}}
```

**Goal.** A learner can focus a particular value in a note, return to a list
item from a direct link, and remove one item together with its learning history
after an informed confirmation, preserving the other values and their order.

**Scope.** Opening a list property's chevron on the note page highlights its
first item in displayed order. Clicking empty space in another item row moves
the highlight and updates the URL. Value links and assimilation/tracker actions
keep their own interactions. Keyboard users can select an item too.

An item destination identifies the note, exact authored property key, exact
parsed item text, and its occurrence among equal values. Opening or reloading
that URL expands the property and highlights that occurrence. Distinct values
keep their destination through reordering and unrelated item removal. Changed
or missing text, or a missing occurrence, leaves the readable note and property
visible with explicit unresolved-item feedback. It does not substitute another
value. If the property itself is missing, existing unresolved-property behavior
applies. Identical occurrences have no durable identity through edits among
equal rows. Assimilation knows a value and opens its first matching occurrence.
The shared item destination and current focus are available to other surfaces.

Scalar properties use the same tint and leading edge on their single value
area. Property keys, chevrons, and scalar action panels retain their ordinary
presentation. Apply this value focus to read-only scalar/context rendering too.
Delete the whole-property-box highlight implementation and its orphaned
dependencies; route focus, expansion, scrolling and missing-property handling
remain required responsibilities. Replace affected proof with proof of value
highlighting and retained interactions, and perform a separate acceptance sweep.

Each expanded list item has a minus button at the start of its row. Delete the
list's whole-property minus and everything used only by that interface. The
expanded list offers item removal; ordinary Markdown and value editing remain
available. Scalar removal remains available. Item minus removes one displayed
occurrence. When that is
the final item, remove the entire property entry and return to the whole-note
location. Otherwise preserve sibling values and their order. Removing the
highlighted item clears item focus while leaving the property open. Removing
another row retains focus on the surviving selected occurrence, adjusting its
occurrence number when necessary.

Before deleting matching learning history, warn the learner and offer Cancel.
Cancel keeps content, learning history, focus and URL intact. Confirm removes
the selected content and this learner's matching trackers through existing
tracker-deletion semantics. For an item in a surviving property, match exact
property key and value; for a property entry being removed, use existing
whole-property history deletion. Equal values share learning history, so an
item warning must explain that history deletion affects any remaining equal
occurrence too. Sibling values' histories remain with them. If there is no
matching learning history, removal can proceed directly. Existing authorization
continues to govern content editing and each learner's tracker deletion.

**UI decision.** Use **Highlighted row** in the existing note page, with tint
and a leading edge on the selected value row only. Preserve the existing
chevron, property edit controls, value links and learning actions. Scalar
values keep their existing specialized controls in the highlighted value
area; do not render a second copy of the scalar value. The highlight is the
only added visible selection cue. Keep controls usable on narrow/touch screens.
[Interactive mockup](../notes/property-item-highlight-in-note.html) supplies the
row treatment; the decisions recorded here govern deletion and scalar focus.

**Key examples.**

- `[walk, run, jog]` → open the chevron → `walk` is highlighted. Select the
  empty part of `run`'s row → `run` is highlighted and its URL is current.
  A value link still opens its referenced note; a tracker action opens its
  tracker instead of acting as row selection.
- Open or reload the URL for `run`, or receive `run` as the next assimilation
  item → the property is open with `run` highlighted. Reorder distinct values
  → the same URL still identifies `run`. Edit/remove that text → the saved
  URL shows unresolved-item feedback with the note and property available.
- `[run, run, walk]` → address the second `run` → the second occurrence is
  highlighted. Remove the first `run` → the surviving selected row remains
  highlighted under occurrence 1. History is shared by the equal values.
- `run` has learning history → press its minus → a warning explains the loss.
  Cancel → the original item, history and focus remain. Confirm → remove `run`
  and its matching history; `walk` and `jog` retain their values, order and
  learning histories. With `run` selected, the remaining list stays open with
  no item selected.
- `[run]` → confirm final-item removal when history exists → remove the
  property entry and return to the whole note.
- Scalar `topic: movement` → open its property → highlight the value area
  containing `movement` with the shared tint/leading edge, retaining its edit
  control and learning actions. A read-only focused scalar highlights its
  value with the same treatment.

**Routing contract.** Use named `notePropertyItem` within the note route family:
`/n:noteId/p/:propertyKey/i/:occurrence/:itemText`, with positive one-based
occurrence and `itemText = "~" + exact parsed value`. The single prefix makes
empty and dot-only values valid path segments; the route helper owns it.
Callers pass decoded authored keys and values; Vue Router handles serialization.
Compile hrefs from named locations rather than constructing URL strings.
An example destination is `/n42/p/example%20of/i/1/~run`.

The existing `noteProperty` list destination opens the property without item
selection. This represents the agreed cleared focus after deletion without
inventing parallel local selection state. Chevron expansion chooses the first
item destination; an empty list opens the property. A scalar `noteProperty`
destination highlights its scalar value. Panel/row changes replace, inbound
links push, and conversation query survives within the family. Rename follows
the key while preserving a still-resolvable item selector.
[ADR 0005 — Web routes](../../docs/adrs/0005-web-routes-accepted.md)

**Scope boundaries.** This story supplies destinations and the focus contract
for [recall navigation](SEED-117-recall-note-location.md#find-the-note-and-property-after-recall)
and [focused export](SEED-120-export-highlighted-property.md#export-a-note-from-the-highlighted-property).
Their recall presentation and export implementations remain in those stories.
Portable wiki-link item syntax and persistent row IDs are deferred promises.
Assimilation eligibility does not limit which authored rows can be focused or
removed. Ordinary list editing still supports authored empty lists; final-item
minus is the action that removes the property entry.

**Exploration and challenge.** Property-only navigation is the smallest existing
solution, but cannot return to the exact reviewed/exported value. Position-only
links address duplicates cheaply but can silently select different knowledge
after a list edit. Exact text plus occurrence, selected by the developer on
2026-11-10, survives changes among distinct values and aligns with learning
identity, at the cost of explicit stale destinations after text edits. Durable
row IDs would survive more edits but introduce an authored/persisted identity
and synchronization lifecycle absent from today's list model; that larger
promise is deferred.

The selected direction borrows a quoted-passage citation: note is document,
property is section, exact text identifies the cited passage. A missing passage
stays unresolved. The analogy ends at identical passages and text edits;
neither supplies durable occurrence identity. The strongest challenge is
silent retargeting, resolved for distinct values by text matching and for
missing text by explicit unresolved feedback, with duplicate continuity bounded
as above.

**Architecture and reuse.** Extend the existing route-owned focus and shared
named-location/href owners, not a second selection store. Reuse authored list
rendering and ordinary content saving; extend the tracker guard to distinguish
item focus from whole-property removal. Reuse current tracker DELETE and
warning/cancellation UI. History deletion and content save remain separate
existing operations; this story adds no atomic publication protocol. Scalar
and list values share the presentation responsibility while retaining their
different editing and learning semantics. Remove only obsolete presentation
and list whole-property action branches, not still-used focus machinery.

[ADR 0001 — Ubiquitous language](../../docs/adrs/0001-ubiquitous-language.md)
defines authored properties and private key/value learning identity.
[ADR 0004 — OKF-compatible notebook Markdown](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
keeps Markdown authoritative and Portable references separate from SPA locations.
[ADR 0005 — Web routes](../../docs/adrs/0005-web-routes-accepted.md) governs the
named route family, authored key, history navigation and shared chrome. The
selected approach conflicts with no consulted Accepted decision. No ADR change,
exception, schema change or new North Star direction is needed.

**Investigation evidence.** Observations in this preparation workspace at
`6282f1a14646b4d3213b14947e4bab8127021193`, 2026-11-10:

- [List representation](../../frontend/src/utils/noteProperties.ts) is ordered
  strings. [Displayed rows](../../frontend/src/components/recall/PropertyAssimilationModes.vue)
  preserve occurrences; learning deduplicates exact values. Derived index IDs
  are recreated by [index refresh](../../backend/src/main/java/com/odde/donut/services/NotePropertyIndexService.java).
- The existing parser/editor command
  `./scripts/run.sh pnpm frontend:test tests/utils/noteContentFrontmatterList.spec.ts tests/components/form/RichMarkdownEditor.propertyValueDialog.spec.ts`
  passed 20 tests, reaching empty-list Markdown emission and duplicate-preserving
  parse/compose, not server persistence.
- A disposable Chromium spec appended the proposed item metadata entry to
  production metadata, used production route assembly, serialized named raw
  parameters, normalized with native `new URL`, and navigated a fresh router.
  Nine cases round-tripped exact keys/values/occurrences, note-family identity
  and query: wiki syntax, reserved characters/backslash, Unicode, blank, dot-only,
  leading tilde, second duplicate, 260-character value, whitespace/newline.
  `./scripts/run.sh pnpm frontend:test tests/routes/seed119ItemUrlProbe.spec.ts tests/routes/noteRouteFamily.spec.ts tests/composables/useGoToNextAssimilation.spec.ts`
  passed 28 tests at 10:32 Asia/Singapore. The scratch spec was deleted. This
  observes routing/encoding, not a product-page reload or server fallback.
- The affected baseline command for property location, memory tracking, list
  layout, list learning, tracker guard, complete note properties and just-review
  context passed 7 files / 41 tests at 10:33 Asia/Singapore. Its exact command
  and limits are retained in the plan. It proves current behavior only.
- At 10:45 Asia/Singapore, a disposable spec registered the item route in real
  metadata, mounted QuillEditor, and clicked an ordinary item anchor through
  the real classifier/link handler/router. Editable and read-only cases kept
  the complete href, occurrence, raw key/value, query and note family.
  `./scripts/run.sh pnpm frontend:test tests/components/form/seed119CopiedItemLinkProbe.spec.ts tests/components/form/quillHtmlToMarkdown.spec.ts tests/notes/NoteEditableContent.paste.spec.ts`
  passed 42 tests, including 3 scratch cases and verbatim item-href Markdown
  conversion. All scratch edits were reverted. This reaches link navigation
  and conversion, not item lookup on the note page or server fallback.

The developer's answers settle removal, link lifetime and scalar presentation.
The new interactions and server-persisted outcome remain execution proof, not
claimed observations from refinement.

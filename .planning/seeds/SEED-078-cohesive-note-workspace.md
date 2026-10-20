---
id: SEED-078
status: dormant
planted: 2026-10-20
planted_during: owner-approved iPhone and iPad UX review and implementation reconciliation
trigger_when: improving everyday note navigation and reading across phone, tablet, and desktop
scope: large
---

# SEED-078: Navigate and read notes through one cohesive workspace

## Why This Matters

For Donut authors and learners, crowded navigation, separate action bars, and
cramped note details should become a quiet, predictable workspace for opening,
reading, and editing knowledge on phones, iPads, and desktop windows, while
preserving authored content, learning behavior, and access rules.

The owner supplied iPhone and iPad screenshots and approved the **Single
header** direction, then its larger-window reconciliation, on 2026-10-20.
The command center is frequently used and must remain directly accessible.
Folder contents are a separate context, with their own persistent toggle.
The date in a note title is authored title text. Editing remains inline.

The read-only implementation assessment at `234f141b41` on 2026-10-20 found
existing measured toolbar overflow, active-tool pinning, global search, global
Undo history, and sidebar-dependent New note placement. It also found
different desktop and smaller-screen menu presentations and note-owned title
and dictation state. These are reuse and reconciliation inputs, not measured
usability results. No product runtime or tests were run for that assessment.
This decomposition starts on synchronized `main` at `a90233a446`.

The owner explicitly selected these stories ahead of the existing queued
work. This supports everyday use of deliberately captured knowledge without
changing the backlog's near-future direction or interrupting Taken work.

## Alternatives and Decision

- **Defer:** Keeps working commands but leaves the reported mobile clutter,
  clipped property values, and different navigation presentations unresolved.
- **Only restyle properties:** The strongest smaller alternative delivers
  useful reading value, and is retained as an independent story below. It
  does not deliver the approved command-center search and shared action bar.
- **Only merge the mobile bars:** Improves phone space, but leaves the owner
  learning a different arrangement when the same window widens. The owner
  explicitly requested cohesion across sizes.
- **Selected direction:** Keep one quiet command-center launcher and shared
  top-bar arrangement across sizes; reuse existing navigation, search, note
  actions, adaptive overflow, and editing. Improve property presentation and
  make secondary details revealable without changing their content model.

The first story tests the highest-risk product assumption: a directly
accessible, quiet command center can replace the always-visible desktop
destinations while remaining easy to find and use. Embedding search is a
separate useful outcome, not a new search capability. Broad navigation
redesign, new note operations, and a new editor are deferred.

## Approved UI Contract and Shared Conceptual Model

This is product and ownership direction for every story, not a technical
design, class model, or executable plan. Implementers refine the smallest
cohesive structure around these concepts rather than adding a model per size.

- **Donut command center — application scope:** App destinations, their active
  state and counts, Resume, account actions, and global note search. The book
  glyph is its launcher; it does not introduce another meaning for the domain
  entity **Book**, which remains an attached reading artifact.
- **Notebook context:** The current notebook/folder ancestry and folder
  contents, including notes, attachments, and child folders. One independent
  contents-toggle state determines a docked pane or a narrow-window drawer.
  New folder and sorting stay with this context. Existing New note placement
  follows the sidebar's visibility and the current creation context.
- **Active note tools:** One ordered set of existing note commands, with the
  same permissions, availability, active states, and measured overflow policy.
  The current note owns editor mode, drafts, title/body dictation intent, and
  Assimilation content even when its controls appear in the shared header.
- **Global history:** Undo addresses the latest available history operation,
  which may belong to another note; it is not reinterpreted as current-note Undo.
- **Note document and presentation:** The authored title, content, frontmatter
  properties, resolved links, and learning identities retain their existing
  meanings. Detail disclosure, wrapping, spacing, and timestamps are views of
  those concepts, not a second metadata or editing model.

The common arrangement is:

```text
Wide:   [Donut menu] [Contents] | [Notebook / folder context] [Note actions ...] | [Undo]
Narrow: [Donut menu] [Contents] | [Note actions ...] [More]                     | [Undo]
        [Notebook / folder context]
```

The book launcher stays a normal, quiet header control. Its menu opens over
the workspace and contains the actual global search surface and existing app
destinations, with counts, active destination, Resume, and account actions.
The folder toggle remains distinct. Wide windows dock the folder pane; narrow
windows use a drawer. More appears when the note tools need overflow. Width
changes context detail and the number of visible actions, not their meaning,
order, or ownership. A single shared header-height rule covers drawers and
safe-area offsets. Parent destinations remain reachable when context is compact.

Below the bar, the note keeps its full authored title and inline editing.
Properties use readable labels and values, comfortable touch controls, and
wrapping rather than a squeezed grid. A compact properties disclosure and
quieter, unambiguous creation/update details give the content visual priority.
Use consistent theme-aware contrast, roughly 17px body text with 1.5–1.6 line
spacing, comfortable phone side margins, and a readable wide-window text
measure as design starting points, not viewport-specific business rules.

Preserve note/property route-family continuity and conversation query state
under [ADR 0005](../../docs/adrs/0005-web-routes-accepted.md). Use the current
domain vocabulary from [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md):
the speech-bubble note tool is **Conversation** and the upload-shaped tool is
**Export**, not newly invented Comments or Share commands. Authored titles,
properties, and links follow [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).

When a presentation is replaced, delete it and everything only it used,
reconcile its consumers, and record a one-time product sweep as an acceptance
reading. Behavioral proof exercises the retained command, navigation, and
editing journeys; it does not assert the absence of replaced UI or names.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including focused proof,
cleanup, and delivery, following
[SEED-039](SEED-039-faster-ci-feedback.md#story-decomposition). The owner asks
for S stories here. The five hypotheses total roughly 3–4.5 hours; they are
not timing measurements or guarantees. Reassess a story likely to exceed one
hour during refinement and resplit around a useful user outcome before
execution. Do not make separate platform models or technical-only stories to
force the estimate. This seed authorizes no implementation or slice plan.

<a id="story-5"></a>

### Focus on note content while revealing details when needed

**Identity:** SEED-078#story-5
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/009-note-details-disclosure/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"fecb692c07f4ddedd71fe57d84627fbf9ec198e9d1e497cb14ef1f30ad076b31","plan":"62845445d938028e0e0f872cd6132d19ae981fadaecc5a655615044b103ed333"}}
```

- **Goal:** A reader opening a note on a phone, iPad, or desktop window
  reaches the authored title and body first and can reveal the note's
  properties and dates when they need them. Everyday reading becomes quieter
  without changing what the note records, how properties are edited, or who
  may edit them.
- **Scope:**
  - Required: in the note view, properties and dates sit behind one details
    disclosure between the title and the body. Compact, it is one quiet line:
    the property keys present, in authored order, cut to the line when long
    (or a short "no properties" wording), and the last-updated date. Each
    note opens compact. The control works by pointer, touch (existing 44px
    coarse-pointer convention), and keyboard, and announces its expanded
    state.
  - Required: revealing shows the existing property section with its content
    unchanged (rows, per-property panels, Add property, read-only display),
    followed by the created and updated dates; collapsing returns to the
    compact line. Revealing and collapsing change presentation only: no
    save, no route change, no change to authored content.
  - Required: a property-targeted arrival or action reveals the details.
    Visiting a property location reveals the section and focuses that
    property's row and open panel as today; a stale property key still shows
    its explicit not-found state without revealing anything else; adding a
    wiki link as a property from the note tools reveals the new row. Route
    replace/push behavior stays as [ADR 0005](../../docs/adrs/0005-web-routes-accepted.md)
    defines it.
  - Required: an in-progress property edit survives disclosure. An open
    draft and a pending validation message are kept across collapse and
    reveal; while either exists, the compact line carries an unfinished-edit
    cue so the feedback is discoverable, and revealing shows the message
    beside its row as today.
  - Required: dates are unambiguous across locales (month spelled out,
    four-digit year) and quiet: muted, theme-aware text. The freshness cue
    that a recently updated note's date line is more prominent than an old
    note's is retained, expressed through theme-aware contrast instead of a
    fixed colour. The created date appears only when revealed.
  - Required: the reading surface keeps the hierarchy title → compact
    details line → body, with the agreed body line spacing, phone side
    margins, and a bounded text measure on wide windows, in theme-aware
    contrast. These are design starting points; proof observes no sideways
    scroll at phone width and bounded body line length on wide windows.
  - Required: the authored title stays exactly as authored, including any
    date prefix, with inline title and body editing unchanged. The
    trashed-note warning, note image, and references keep their meaning.
    Read-only viewers get the same disclosure over the read-only property
    display. Markdown editing mode keeps the compact line and dates while
    the raw frontmatter stays visible in the text area.
  - Deferred, not rejected: remembering the disclosure across notes or
    visits (device preference or route), relative-time wording, the readme
    page editors' own disclosure (they keep today's revealed section),
    further property row and editor restyling, and any new metadata or
    settings preference. Existing property validation and access rules are
    the only rejection constraints; this story adds none.
  - The bottom created/updated block and the fixed-RGB freshness colouring
    are replaced: delete them and everything only they used, reconcile the
    end-to-end freshness observation and the property page objects to the
    new surface, and record a one-time acceptance sweep as a reading, with
    behavioral proof of the retained reading, property, and editing journeys.
- **Key examples:**
  - A writable note titled `2026-05-15 Trip notes` with properties `topic`,
    `source`, and `url` → open it in a 375px window → the title reads
    exactly `2026-05-15 Trip notes`; one quiet line shows `topic, source,
    url` and the updated date; the body follows at once; nothing scrolls
    sideways. Reveal → the three rows, their panel toggles, and Add property
    appear, then the created and updated dates. Collapse → the compact line
    returns and no save was issued.
  - Reveal, choose Add property, type only a key, choose Add → the existing
    message appears beside the draft. Collapse → the compact line shows the
    unfinished-edit cue. Reveal → draft text and message are intact.
    Complete the draft → the property is saved, and after collapsing the
    compact line lists its key.
  - Visit `/n/{id}/p/topic` directly → details are revealed, the `topic` row
    is focused with its panel open and scrolled into view. Close the panel
    → the route returns to the note and details stay revealed. Visit a key
    the note does not have → the explicit not-found notice is visible
    without revealing the details.
  - On a compact note, insert a wiki link as a property from the note tools
    → details reveal with the new row; after collapsing, its key is in the
    compact line.
  - One note updated an hour ago and one a year ago → the recent note's
    updated line is visibly more prominent in both light and dark themes;
    both dates read unambiguously (for example `15 May 2026`, never a
    numeric `5/6/2026` form).
  - The same note in a 1440px window → body lines stay within a readable
    measure and the title, compact line, and body share that column; in a
    375px window the body keeps comfortable side margins and roughly 17px
    text with 1.5–1.6 line spacing.
  - A read-only viewer of a shared note → the compact line and reveal work
    the same over the read-only rows, with no editing offered. A read-only
    note without properties → the compact line shows only the updated date,
    and revealing shows the created date.
- **UI sketch (agreement only; no component or layout choice):**

  ```text
  2026-05-15 Trip notes
  ▸ Properties: topic, source, url · Updated 15 May 2026
  Body text …

  2026-05-15 Trip notes
  ▾ Properties: topic, source, url · Updated 15 May 2026
      [existing property rows … Add property]
      Created 3 Mar 2026 · Updated 15 May 2026
  Body text …
  ```

- **UI and system trade-offs (UX/UI + Architecture):** three interaction
  choices carry system consequences; the delivery proceeds on the stated
  smallest choice, and the developer may choose otherwise before planning.
  - Disclosure lifetime: compact on every note opening | remembered per
    device | carried in the route. User: predictable content-first opening
    vs fewer taps in property-heavy sessions vs shareable state. System: no
    state vs a device-local preference like the notebooks layout vs route
    coupling that ADR 0005 reserves for resources and conversation chrome.
    Delivery: per opening; the others are deferred.
  - Freshness cue: retain the age-graded emphasis in theme-aware contrast |
    drop it for a uniformly quiet date. User: keeps the "which notes are new"
    cue the recent-update feature promises vs a quieter line. System: keeps
    the age computation and its reconciled end-to-end observation vs
    deleting that feature and its tests. Delivery: retain, because dropping
    a promised behavior is the owner's decision.
  - Details placement: under the title, where the property section already
    lives | after the body. User: context before reading vs maximal
    content-first. System: presentation only vs relocating the property
    section out of the editor that composes body and rows, a larger
    structural change. Delivery: under the title.
- **Architecture:** One consequential concern. The rich editor composes every
  body edit from the property rows it currently has mounted; with no rows it
  rewrites the note without its frontmatter. A disclosure that unmounted the
  property section would therefore delete every property on the next body
  keystroke, and would also lose drafts, snapshots, and validation. The
  collapsed section must stay mounted and hidden. Give the note reading
  surface (title plus content region) one details-disclosure owner that the
  property section consumes through the existing provide/inject pattern
  used for readme context and the focused property key: the section hides
  its rows when collapsed, requests reveal for property-targeted triggers,
  and reports an unfinished edit; hosts without that owner keep today's
  revealed section. Dates come from the note topology already on that
  surface. No new API, persistence boundary, route, or metadata model;
  [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  (authored content unchanged), [ADR 0005](../../docs/adrs/0005-web-routes-accepted.md)
  (property locations), and [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md)
  vocabulary apply without conflict. No ADR proposal.
- **Investigation (2026-10-20, `83929302ae2e470ab670ab09e5ad1a69ae89c75f`):**
  Facts read from code in this worktree; no runtime, tests, or devices were
  run. The property section lives inside the rich editor above the body and
  is always expanded (`RichMarkdownEditor.vue`, `RichFrontmatterProperties.vue`,
  `useRichFrontmatterPropertyEditing.ts`); the section is absent in Markdown
  mode. Created and updated dates render below the body in `NoteShow.vue`
  with the locale's numeric short date and a fixed-RGB age fade in
  `NoteRecentUpdateIndicator.vue`; the end-to-end freshness check reads that
  colour channel (`e2e_test/start/pageObjects/noteRecentUpdate.ts`) and a
  unit test pins exact RGB values. Body composition falls back to no rows
  when the section is unmounted, and `composeNoteContentInPlace` with no
  rows drops the frontmatter (`utils/noteContentInPlaceEdit.ts:36`), which
  settles the mounted-while-hidden requirement. Validation messages and the
  draft row render inside the section's list; the not-found notice renders
  outside it. Property locations focus and scroll the row through
  `useFocusedNoteProperty.ts`. No code parses dates out of titles. The body
  uses Tailwind prose defaults inside a `container` main column, so wide
  windows have no text measure. Twelve end-to-end features exercise
  properties through shared page objects under the note content region.
  Story 4 was still queued at that revision and completed on trunk at
  `e1b1aaf93f` before this preparation landed; its reflowed rows are the
  section this story reveals. Still hypotheses: device-level comfort of the
  compact line and whether users want the disclosure remembered; neither
  changes this delivery.
- **Value / learning:** Tests the approved content-first hierarchy without
  changing the note's data or requiring the header work to finish first.
- **Effort hypothesis:** S, 45–60 minutes, medium confidence: presentation
  and disclosure around existing properties and dates, plus reconciling the
  freshness observation and property page objects. Reassess during planning;
  resplit around a useful outcome if it exceeds one hour.
- **Depends on:** No blocking supplier story. The readable property rows
  from the completed story 4 are the section this story reveals.
- **Safe stopping point:** Compact details improve reading independently of
  the command menu, merged header, or expanded-property restyling.
  Authorship, editable drafts, validation, and access rules remain intact on
  their own.

## Ordering and Scope Reduction

Place this selected set before the existing queue under the owner's explicit
priority instruction. Within the set, recommend stories 1–5 in this order.
Command-center consistency tests the largest navigation choice first; embedded
search makes that center more useful; the shared note bar then delivers the
main mobile space saving. Complete-value property access precedes optional
detail compaction because it fixes an immediate usability problem. Existing
Taken work and the relative order of unrelated queued stories stay unchanged.

Each story retains useful value if later stories are cancelled; shared
conceptual ownership and ordinary reconciliation avoid artificial supplier
dependencies. Story 5 is first to drop for scope reduction; retain
the selected command-center/search and shared-header outcomes as the core.
If early use challenges menu discoverability or refinement invalidates the
hour-long sizing hypothesis, return to that story's boundary before planning
execution. Do not disguise an overrun as another layer or platform story.

## Open Decisions

No unresolved parent framing or priority decision remains: the owner approved
the UI direction and asked for these bounded stories at the top of the queue.
Individual stories still need refinement and an execution-approach choice.
No implementation, timing run, manual-testing session, or executable plan is
authorized by this decomposition.

## When to Surface

Now, when selecting the next queued product improvement, under the owner's
2026-10-20 instruction to prioritize the approved responsive note workspace.

## Breadcrumbs

- Owner's iPhone and iPad screenshots, corrections, approval of **Single
  header**, larger-window review, and 30–60 minute backlog instruction in this
  conversation, 2026-10-20. The UI contract above retains the design without
  requiring session-local mock files or private example-note content.
- [Current product backlog](../PRODUCT-BACKLOG.md) and
  [short-term architectural direction](../NORTH-STAR.md).
- [Related expanded property-list story](SEED-064-note-properties-fixes.md#story-7).
- [Accepted domain vocabulary](../../docs/adrs/0001-ubiquitous-language.md),
  [Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
  and [web routes](../../docs/adrs/0005-web-routes-accepted.md).

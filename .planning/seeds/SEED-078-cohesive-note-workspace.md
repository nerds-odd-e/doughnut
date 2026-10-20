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

<a id="correction-1"></a>

### Reach and operate the Donut menu from every screen state

**Identity:** SEED-078#correction-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/004-donut-menu-reach-and-keyboard/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"bd92da166b802cf4c953d3dbb0914ea1caa52c07c1df33b5fdcfaa14dc9801c3","plan":"01f127ed3964ccd03deaa408f97d1d01cddc459f8cd0ce1e1f4d835f693c73e4"}}
```

- **Goal:** Authors, learners, and visitors reach and operate the Donut menu
  wherever they had navigation before story 1, by mouse and keyboard, with
  Account expanding its grouped actions in place, and the menu's tests identify
  it unambiguously without redundant cost. Bounded retrospective correction of
  story 1 (`.planning/seeds/SEED-078-cohesive-note-workspace.md#story-1` at
  commit e1b635a22b); it adds no feature promise.
- **Scope:** Give the page bar to the screen states that lost it (Daily probe
  and its retry, book loading and load failure, notebook catalog failure and
  a missing notebook group); expand Account inside the panel; make the sign-in
  action and the action items keyboard operable and closing; give the panel
  its own test identity; move the panel layout checks to browser-mode unit
  proof; remove leftover residue and overlapping assertions. Story 1's
  promises, deferrals, and named-route navigation stay as they are.
- **Plan:** [004 Donut menu reach and keyboard](../slice-plans/004-donut-menu-reach-and-keyboard/PLAN.md)
  holds the source, findings, proof, and slices.

<a id="story-2"></a>

### Find and open notes from search inside the Donut menu

**Identity:** SEED-078#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A learner can use one familiar command center to find the
  next note without a separate persistent search control competing for space.
- **Outcome and boundary:** The actual existing global search surface lives
  inside the command menu, including results and opening a selected note.
  Preserve current search history, scope controls, access rules, keyboard
  navigation, and Ctrl/Cmd+F. Opening the menu on touch leaves the keyboard
  closed; explicitly entering search focuses it with the current touch-focus
  behavior. Note-local Wiki link or relationship search retains its own meaning.
  This changes search placement, not matching, ranking, scope policy, or APIs.
- **Evaluation:** With the menu closed, Ctrl/Cmd+F opens it directly into
  global search; a matching result opens its existing note destination. On a
  phone, opening the menu exposes app commands without covering them with the
  keyboard, while tapping Search permits typing and selecting a result.
- **Value / learning:** Delivers the approved combined command/search journey
  while preserving the search people already know.
- **Effort hypothesis:** S, 30–45 minutes, medium confidence; assumes the
  existing global form and result navigation can be reused with explicit
  focus intent instead of a second search implementation.
- **Depends on:** No blocking supplier story established. The existing menu
  and search already support this journey; reconcile their common direction
  with story 1 through ordinary shared ownership, not an invented wait.
- **Safe stopping point:** Search within the command menu is usable even if
  note tools and property presentation are unchanged. Current result access,
  touch focus, and keyboard journeys must survive this story on their own.

<a id="story-3"></a>

### Use note actions and folder context from one adaptive top bar

**Identity:** SEED-078#story-3
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A note reader or author can reach navigation and note tools
  from one predictable bar without losing reading space on a phone.
- **Outcome and boundary:** Join the current note tools to the common top bar
  at every width, beside the distinct command-center and folder-contents
  controls. Keep reachable notebook/folder context, with compact context above
  the note on narrow windows. Reuse measured overflow, the existing action
  order, permissions, and active voice/Assimilate pinning. Reserve space for
  active controls; long context must not consume their entire region. Preserve
  sidebar-dependent New note placement, global Undo semantics, title/body
  dictation targeting, editor drafts/mode, and the note's Assimilation content.
  Folder, attachment, property, Recall, and Book-reading contexts retain the
  page controls appropriate to them. This story does not add note commands.
- **Evaluation:** With a writable note open, narrow and widen the window:
  note actions stay in one header row and trailing actions remain usable in
  the labeled More menu. Active voice input stays directly reachable and
  continues targeting the intended title or body. Toggle folder contents:
  the pane/drawer follows the same control and New note remains available in
  its existing context. Undo still addresses its actual history operation.
- **Value / learning:** Delivers the main approved space saving and tests
  whether one ownership model supports both compact and wide workspaces.
- **Effort hypothesis:** S target, 45–60 minutes, low confidence. It assumes
  visual placement can change while retaining the same note-owned state and
  existing overflow/focus behavior. The current title-dictation and panel
  coupling is the main sizing risk; refine or resplit this outcome if that
  assumption fails, rather than silently expanding past the owner's hour.
- **Depends on:** No blocking supplier story established. Existing shell,
  context, and note tools suffice; common code alone is not a prerequisite.
- **Safe stopping point:** One usable note bar stands alone, with current
  search and detail UI if later work is cancelled. Preserve editing, learning,
  access, and the other pages' controls within this increment, not as repairs
  delegated to a later story.

<a id="story-4"></a>

### Read and edit complete property values comfortably in narrow windows

**Identity:** SEED-078#story-4
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A learner inspecting or editing properties can read complete
  values and reliably use their links and controls on a phone or iPad.
- **Outcome and boundary:** Present expanded properties as legible label/value
  rows that stack or wrap with available space, with comfortable touch areas.
  Reduce form-like borders outside active editing while keeping control
  affordances clear. Preserve authored keys/values/order, current scalar/list
  editors, validation, linked note/property destinations, pending/dead-link
  behavior, read-only access, and per-value learning controls. This does not
  add property operations or alter learning identities. Whole-section
  disclosure and timestamp hierarchy belong to story 5.
- **Evaluation:** A property has a long linked value and a list containing
  repeated authored values. On a phone, inspect it and follow its actual note
  or property destination; the label and values remain readable. Open the
  existing editor and commit a valid change with its ordinary validation and
  learning behavior. The same property is comfortably laid out on a wide view.
- **Value / learning:** Fixes the clipped-value problem independently of any
  navigation changes or default disclosure preference.
- **Effort hypothesis:** S, 30–60 minutes, medium confidence; assumes layout
  and control presentation reuse the current property editors and link
  rendering rather than introducing another frontmatter representation.
- **Depends on:** No blocking supplier story established. Preserve and
  ordinarily reconcile the distinct expanded-list journey in
  [SEED-064#story-7](SEED-064-note-properties-fixes.md#story-7); do not duplicate
  its per-value navigation outcome or interrupt its Taken work.
- **Safe stopping point:** Expanded properties are readable and operable even
  if all detail sections stay expanded and the current header remains.

<a id="story-5"></a>

### Focus on note content while revealing details when needed

**Identity:** SEED-078#story-5
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A reader can reach the authored content quickly and inspect
  properties or timestamps when useful, without the details dominating the note.
- **Outcome and boundary:** Use one compact properties disclosure showing
  concise existing context, with the current property section revealed on
  demand across sizes. Keep the most useful update date quiet but readable,
  with the creation date available in expanded details and unambiguous date
  formatting. Apply the agreed body spacing, side margins, text measure, and
  theme-aware hierarchy to this reading surface. Keep the exact authored
  title, including any date prefix, and existing inline editing. Disclosure
  must preserve an in-progress property edit and make validation feedback
  discoverable. Property row/editor redesign belongs to story 4.
- **Evaluation:** Open a note with several properties: its compact summary
  leaves the title and body prominent. Reveal properties and timestamps to
  inspect the same values and dates, then continue inline editing. An authored
  title such as `2026-05-15 Trip notes` remains one unchanged title. A property
  validation error remains discoverable when details are being used.
- **Value / learning:** Tests the approved content-first hierarchy without
  changing the note's data or requiring the header work to finish first.
- **Effort hypothesis:** S, 30–45 minutes, medium confidence; assumes
  presentation/disclosure around existing properties and timestamp values,
  with no alternate editor or persisted metadata model.
- **Depends on:** No blocking supplier story established. Existing property
  presentation is sufficient; improved row layout can be reconciled separately.
- **Safe stopping point:** Compact details improve reading independently of
  the command menu, merged header, or expanded-property restyling. Authorship,
  editable drafts, validation, and access rules remain intact on their own.

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
dependencies. Story 5 is first to drop for scope reduction, then story 4; retain
the selected command-center/search and shared-header outcomes as the core.
If early use challenges menu discoverability or refinement invalidates the
hour-long sizing hypothesis, return to that story's boundary before planning
execution. Do not disguise an overrun as another layer or platform story.

## Open Decisions

No unresolved parent framing or priority decision remains: the owner approved
the UI direction and asked for these bounded stories at the top of the queue.
Individual stories still need refinement and an execution-approach choice.
In particular, story 3's integration cost is an explicit sizing uncertainty,
not a measured claim that it fits an hour. No implementation, timing run,
manual-testing session, or executable plan is authorized by this decomposition.

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

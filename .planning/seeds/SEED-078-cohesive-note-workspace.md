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

<a id="story-3"></a>

### Use note actions and folder context from one adaptive top bar

**Identity:** SEED-078#story-3
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/006-adaptive-note-header/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"e4be2d3043ba6aba27b2e6cb9ced98d2b4d256ad76e5742ec39be3f8f50b78a9","plan":"9bc71d46cd39df5410ae0fa21ce05055215fa24448e76663b3baac2f835353e0"}}
```

- **Goal:** A note reader or author uses navigation, folder contents, and
  the current note's tools from one predictable top bar on phone, tablet,
  and desktop, leaving more space for the authored note. The outcome is the
  approved shared arrangement, with the existing commands and note state.
- **Scope:**
  - Required: place the active note's existing command row in the common
    top bar, after the distinct Donut menu and contents controls and before
    global Undo. Preserve command order, labels, shortcuts, availability,
    and permission rules. Conversation and Export keep their existing
    meanings; this delivery adds no command.
  - Required: use the existing measured overflow rule against the space
    actually available in the header. Trailing commands remain operable in
    the accessible More menu. Recording, starting, converting, and retrying
    voice input, and an open Assimilate control, keep their existing direct
    access and active states. Long context yields space to these controls
    and More rather than pushing them outside the window.
  - Required: wide windows show notebook/folder ancestry before the note
    commands in the bar; narrow windows show that context immediately below
    the bar, above the note. Reuse the existing ancestry links, including
    notebook, parent folders, and Circle or Bazaar when applicable. A long
    trail can scroll horizontally within its own region so parent links
    stay reachable.
  - Required: the contents control operates the same independent open
    state, as a docked pane on wide windows and a drawer on narrow ones.
    New note remains in the sidebar while it is open, in the note tools
    when it is closed on a writable note, and in its current main-column
    creation context on other writable notebook pages. Folder creation and
    sorting stay with folder contents. Drawer positioning and safe-area
    offsets use the shared bar-height rule.
  - Required: moving controls and resizing preserve the current editor
    mode, drafts, title selection/dictation intent, body insertion target,
    and note-owned Assimilation content. That content stays below the bar
    with the note. Switching notes and maximizing Conversation retain the
    current note lifecycle; note/property navigation keeps the same route
    family and conversation query.
  - Required: Undo acts on the latest global history operation, including
    another note, with its existing confirmation and destination. Folder,
    attachment, property, Recall, and Book-reading contexts retain their
    appropriate controls. Signed-in read-only notes keep their permitted
    tools; writable commands remain subject to the existing access rule.
  - Removal: replace the separate note action-bar presentation and delete
    everything used only by that presentation; reconcile product and test
    consumers. Acceptance records a one-time product sweep as a reading.
    Proof exercises the surviving commands, navigation, and editing.
  - Deferred, not rejected: command-center search relocation (story 2),
    property layout and disclosure (stories 4–5), new commands, alternate
    editor or metadata models, and changes to history or learning policy.
    No new rejection constraint is introduced.
- **Key examples:**
  - Writable note, sidebar closed, phone window → open More and choose
    Export or Edit → the existing action works from the shared header;
    widen and narrow the window → the command order and editor mode survive.
  - Title caret after “Orchard”, voice input started from the header →
    narrow the window while recording, then Stop → the highlighted control
    stays reachable and speech is inserted at that title caret and saved.
    Focus the body for the next recording → speech goes into the body.
  - Assimilate open on the current note → resize → its active control stays
    in the bar and its settings stay with the note below it.
  - Note in a deeply nested folder with long names → narrow the window →
    ancestry appears below the bar; scroll its trail and select a parent →
    that folder opens through its existing named route, with note tools
    having stayed reachable before navigation.
  - Writable note → toggle contents open, then closed → New note is usable
    in the sidebar, then in the note tools, using the same folder creation
    context. At phone width the same toggle opens a drawer; at wide width
    it opens the docked pane.
  - Note A open, latest history entry is a change to note B → press Undo
    and confirm → the change to B is undone and its existing destination
    opens; the operation is not reinterpreted as an edit to A.
  - Open a focused property with Conversation, then switch to a folder,
    attachment, Recall, or Book reading → the property/conversation route
    state and each destination's page controls behave as before. A
    read-only note offers its permitted Conversation action with the
    existing editing restriction.
- **UI:** Follow the shared wide/narrow arrangement above. The compact
  ancestry line is part of the content area below the fixed-height bar;
  it does not become another action bar. The Donut menu and contents toggle
  stay distinct, Undo stays at the trailing edge, and the note tools use
  the middle space. Keep the full authored title and inline editing.
- **Architecture:** The note owns command behavior and editing/dictation
  state even when its controls render in the layout's header. Reuse the
  existing toolbar, overflow calculation, sidebar state, breadcrumbs, and
  global history; do not copy those responsibilities into a header store.
  Preserve the vocabulary of [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md),
  authored content under [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
  and named-route/property continuity under [ADR 0005](../../docs/adrs/0005-web-routes-accepted.md).
- **Investigation (2026-10-20, `ad2a592d06` in this preparation worktree):**
  A disposable browser-mode harness mounted the real NoteShow and GlobalBar.
  A scratch relocation of only the toolbar navigation through Vue Teleport
  retained NoteShow ownership. Eleven checks passed: the ten current
  toolbar/title dictation scenarios (one extended to narrow to 320px during
  recording and widen before Stop), plus selecting Assimilate through the
  bounded phone overflow menu and observing its settings with the note.
  Disabling the relocation made the header-placement observation fail
  (Stop ended at 87px while the bar ended at 55px). The scratch edits and
  spec were reverted before recording. This establishes a viable placement
  approach, not final layout acceptance or manual device usability evidence.
- **Value / learning:** Delivers the main approved space saving and tests
  whether one ownership model supports both compact and wide workspaces.
- **Effort hypothesis:** S target, 45–60 minutes, medium confidence after
  the placement/dictation observation. The remaining work is a control
  placement seam, shared header sizing and ancestry, and affected-consumer
  proof. Reassess the story boundary if execution invalidates the hour-long
  hypothesis; the scratch result is not a timing guarantee.
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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/007-readable-property-values/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"213d9e3c79b3c024ce9ffcce10e9531f676f9efe9cee68c936af8b46a480e448","plan":"be0c560ac316333e85d5b6ff03f1d5aa4a00b91a9710c657c64f6ec514b9c357"}}
```

- **Goal:** A learner or author inspecting note properties on a phone, iPad,
  or narrow desktop window can read complete values, follow their links, and
  use the existing editors comfortably. This fixes cramped property reading
  independently of header changes or detail disclosure.
- **Scope:**
  - Required: expanded property content uses legible labels and complete
    values that wrap or stack with available space. Long prose, link labels,
    URLs, and unbroken values remain readable without horizontal panning of
    the property section. Wide windows retain a readable arrangement.
  - Required: retain the existing compact/expanded list journey. An open
    property panel shows each authored list item in order, including repeated
    values, with its existing per-value learning controls. Closing it restores
    the compact value presentation; read-only properties remain readable and
    followable under their existing access rule.
  - Required: keep key editing, inline scalar editing, and the existing
    scalar/list value dialog, their commit/cancel and validation behavior,
    presets, and specialized image, URL, relation, and Wikidata controls.
    Labels and controls stay reachable as space narrows; resizing must not
    discard entered text. Reduce form-like borders while idle and make
    editing and keyboard focus clear. Provide comfortable touch areas for
    property actions, using the existing 44px touch-target convention.
  - Required: preserve authored keys, values, ordering, wiki display labels,
    resolved note/property destinations, pending/dead-link behavior, read-only
    authorization, and learning identities. Existing rejection constraints
    come from property validation and access rules; this story adds none.
  - Deferred, not rejected: whole-section disclosure, timestamps and body
    hierarchy (story 5), new property operations, a replacement editor or
    metadata model, new learning policy, and a dialog-only reading workflow.
    Existing list-dialog text fields keep their ordinary editing behavior;
    this story does not redesign that dialog.
  - Delete replaced layout rules and their exclusive consumers outright;
    reconcile shared consumers. Record a one-time acceptance sweep as a
    reading, with behavioral proof of the retained reading/editing journeys.
- **Key examples:**
  - A writable note contains a long prose scalar and a long labeled wiki link
    → read properties in a narrow window → values wrap into visible lines;
    follow the link → its actual note or property destination opens. Inline
    edit the scalar and commit → the ordinary authored value is saved.
  - A list contains a link, plain text twice, an empty authored item, and a
    long unbroken value → expand its panel → every authored item remains in
    order and readable; the repeated item keeps the existing shared tracker
    identity and first-occurrence assimilation behavior. Follow a resolved
    item or use its learning control → the current operation still applies
    to that value. Collapse → compact links and Edit remain usable.
  - Open the existing list editor on a phone → reorder repeated values and
    save a valid change → authored order is committed. Cancel a later edit
    → the saved value remains; submit invalid input → the existing feedback
    and correction path remain reachable.
  - Begin editing a property → narrow and widen the window → the draft and
    keyboard focus remain usable. Idle rows look like readable note details;
    focusing a field makes the edit affordance visible. Property actions
    retain comfortable touch targets.
  - A read-only viewer inspects the same long properties → the full display
    and resolved links remain usable, with the existing editing restriction.
    A saved unresolved link and a newly entered unresolved link retain their
    different recovery/pending behavior.
- **UI and system trade-off:** Continue the approved inline, wrapping
  direction. Values may take more vertical space, keeping reading and actions
  together; presentation changes leave authored data, editor state, route
  resolution, and learning ownership with their existing owners. No new user
  decision is needed for this already approved direction.
- **Alternatives considered (Explore + Borrow):**
  - Existing workaround: open the value dialog or pan a field. This reuses
    editing but adds a step to reading and does not solve the expanded
    learning list's overflow.
  - Read-first view with a separate edit mode: quiet reading is plausible,
    but it introduces a new interaction/state transition and changes the
    approved inline-editing experience. Deferred.
  - Recommended within the approved scope: borrow document reflow. Authored
    properties correspond to ordered document entries; available width
    changes their line breaks, not content or identity. Let values consume
    vertical space and keep their actions reachable. The analogy stops at
    interaction: links, editable fields, and learning controls need explicit
    focus and activation proof. The browser observation below, rather than
    the analogy, supports feasibility.
- **Architecture:** Change the existing property presentation, keeping one
  authored frontmatter model and one rendering/learning path per value.
  `RichMarkdownEditor` already owns editing and validation;
  `WikiLinkToken`/the scalar field own link interaction; the expanded panel
  consumes the shared `AssimilationModes` rows. Preserve these responsibilities
  and reconcile shared note-level consumers if their presentation changes.
  Follow [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  for authored content and
  [ADR 0005](../../docs/adrs/0005-web-routes-accepted.md) for named note/property
  destinations. No new API, persistence boundary, or ADR is needed.
- **Investigation (2026-10-20, `4535beaeb57a2dd617c90f964ddf452a300fbd03`):**
  In this preparation worktree, a disposable headless Chromium harness mounted
  the real rich editor. It reproduced scalar horizontal overflow at 320px
  (1426px scroll width in a 24px field) and expanded-list panel overflow.
  Temporary reflow styles made complete values fit at 320/768/1280px, retained
  duplicate list display, emitted an inline edit, and followed a labeled link
  to its property destination; a read-only link also worked at 320px.
  Disabling reflow failed all three width checks. Five probe cases and 32
  existing link, learning, dialog, validation, row-editing and read-only cases
  passed together. Scratch files/styles were removed before recording.
  This establishes feasibility and the failure signal, not final design,
  device usability, touch-keyboard behavior, or acceptance of an implementation.
  A further two-case probe observed real coarse-pointer controls at least
  44px high and retained scalar text/focus during resizing; the restored
  editor/assimilation baseline passed 186 tests. Details are in the plan.
- **Value / learning:** Fixes clipped-value reading without waiting for
  navigation changes or a default disclosure preference.
- **Effort hypothesis:** S, 40–60 minutes, medium confidence after the local
  reflow observation. Reuse existing editors and list/learning ownership;
  reassess the story boundary if execution invalidates the one-hour hypothesis.
- **Depends on:** No blocking supplier story. SEED-064#story-7 is completed
  ([completion record](../done/SEED-064_story-7.json)); its expanded-list journey
  is present and documented in
  [Rich property editing](../../docs/note-content-saving.md#rich-property-editing).
  Preserve it through ordinary reconciliation, without duplicating navigation.
- **Safe stopping point:** Readable, operable expanded properties stand alone
  while all detail sections remain expanded and the current header remains.

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

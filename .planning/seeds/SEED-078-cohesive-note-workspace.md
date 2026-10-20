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

<a id="story-2"></a>

### Find and open notes from search inside the Donut menu

**Identity:** SEED-078#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/005-donut-menu-search/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"37e7383d53ec1e7517e5db53ff770c318173d69b5a68a3a4e136ed971e4dc5ef","plan":"ef467dc8db5bf6ae6b2c7a834fdef2ee24c89d7fcec2276dfc61c2a44b7bed16"}}
```

- **Goal:** A signed-in learner finds and opens the next note from the Donut
  menu they already use for every app destination, so the page bar no longer
  carries a second, persistent search control competing for space. The
  observable outcome is the existing global note search, with its results,
  living inside the Donut menu panel at every width; the shared note bar and
  content presentation stay with the sibling stories.
- **Scope:**
  - Required: the Donut menu panel, for a signed-in user, contains the
    existing global search surface after the destinations and Account: the
    search field (placeholder "Search"), the search history control, the
    "All notebooks" and "All My Circles" scope controls, and the results
    (Recent notes before a key is typed, Matches once one is, with the
    Matches/Recent switch). Matching, ranking, scope policy, history
    recording, access rules, and the search and recent-notes APIs are
    unchanged.
  - Required: the panel opens quietly. Opening it from the launcher, by tap
    or click at any width, lists the commands and the search field without
    moving focus into the field, so a phone keeps its keyboard closed and
    the commands uncovered. The results region appears only once the learner
    enters search: focusing the field by tap, click, or Tab, picking a
    history key, or opening with the shortcut. It then stays until the panel
    closes. Tapping the field opens the phone keyboard through the learner's
    own tap; no primer focus is needed.
  - Required: with the panel closed, Ctrl/Cmd+F while signed in opens the
    panel directly into search with the field focused and the browser's own
    find suppressed, as the shortcut does today; with the panel already
    open it focuses the field. Ctrl/Cmd+Shift+F stays the note-local Wiki
    link or relationship search of the note toolbar, and the shortcut stays
    inactive when signed out.
  - Required: a result opens its existing destination (note, folder, or
    notebook named route) and closes the panel, including when the chosen
    note is the one already open; a modified click (new tab) keeps the panel
    open, as the dialog does today. Picking a history key, toggling a scope
    control, or switching Matches/Recent keeps the panel open; the current
    closing triggers (launcher, outside tap or click, Escape, route change)
    keep working with the search surface present.
  - Required: keyboard journeys survive: Tab reaches the field after the
    panel items; ArrowDown moves from the field into the results and
    ArrowUp returns; Enter on a result opens it.
  - Required: the panel fits the window: on a phone with the keyboard open
    or on a short window the panel scrolls within the space below the bar so
    the field and results stay reachable; on a wide window it stays anchored
    below the launcher.
  - Removal: the page bar's separate "search note" control, its shortcut
    registration in the bar, its touch primer handling, and everything only
    they used (bar search tests, the E2E step path through the toolbar
    button, dialog-closed assertions) are deleted; the E2E search journeys
    continue through the Donut menu. Acceptance records the one-time sweep
    as a reading.
  - Deferred, not rejected: a Search item or count on the launcher; a route
    or query that records search being open; a dedicated phone layout for
    the search surface; search for a signed-out visitor; changes to the
    note-local search in the note toolbar, folder search, or wiki-link
    flows, which share the search components and must keep working.
  - Boundary assumption: while a modal dialog is open, the Donut menu is
    unreachable (the dialog owns the top layer and the page is inert), so
    Ctrl/Cmd+F then leaves the dialog alone instead of opening a hidden
    panel. No rejection constraint was found.
  - Boundary assumption: [correction-1](#correction-1) is executing in a
    sibling worktree on the same panel file and test support (panel identity
    by role "region" and name "Donut menu panel", Account disclosure,
    keyboard items). Execution starts from the trunk of that moment and
    reconciles ordinarily; this is shared ownership, not a dependency.
- **Key examples:**
  - Desktop, a note open, panel closed → press Ctrl+F (Cmd+F on macOS) →
    the panel opens with the field focused and Recent notes listed; type
    "Sed" → Matches lists Sedation and Sedative; press ArrowDown then Enter
    on Sedative → Sedative opens and the panel is closed.
  - Phone, a note open → tap the launcher → the panel lists the
    destinations, Account, and the search field; the keyboard stays closed
    and no results are shown; tap the field → the keyboard opens and Recent
    notes appear; type "pam" → Pam, Diazepam, Lorazepam, Clonazepam; tap Pam
    → Pam opens and the panel is closed.
  - Panel open into search with results → press the history control and
    pick "older" → the field reads "older", the panel stays open, and
    Matches for "older" appear.
  - Search entered with "Sedation" in Recent, Sedation already open → press
    Enter on the Sedation result → the route is unchanged and the panel is
    closed.
  - Panel open into search → press Escape → the panel is closed; press
    Ctrl/Cmd+F again → it reopens into search with the field focused.
  - Desktop, panel open into search, Tab from the last Account action →
    the field is focused; ArrowDown enters the results; ArrowUp returns.
  - Signed-out visitor → the panel offers sign-in only and Ctrl/Cmd+F does
    nothing.
  - Note toolbar, Ctrl/Cmd+Shift+F → the Wiki link or relationship search
    dialog opens as today, unchanged.
- **UI:** The search surface is the panel's last section, after the
  destinations and Account and before the "Donut by" footer, in the same
  order at every width: a row with the history control, the field, and the
  scope toggles, then the results region once search is entered. Width
  changes the panel's size, not the contents or order. Considered and not
  chosen: always listing Recent notes on every open (it makes every trip to
  Recall or Messages carry a five-note list and a fetch), and a Search item
  that switches the panel into a search mode (one more tap and a return
  path). The field itself is the quiet entry; its focus is the learner's
  explicit intent.
- **Architecture:** The Donut menu, an app-shell control, owns opening,
  closing, and the "open into search" focus intent, including the
  Ctrl/Cmd+F registration that moves from the bar's search button to the
  menu. Search key, scope, history, results, and result navigation stay with
  the existing search components; no second search state or presentation
  model is added. Result destinations keep named-route navigation under
  [ADR 0005](../../docs/adrs/0005-web-routes-accepted.md); search being
  open is transient chrome and gets no route or query. "All notebooks" keeps
  its [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md) meaning of
  catalog-wide search. One shared consequence: the dropdown's outside-click
  closing must treat a click inside a panel portaled from a nested dropdown
  (the history list) as inside, a rule of the shared auto-collapse
  behavior rather than a Donut-menu special case. No new decision record is
  needed.
- **Investigation (2026-10-20, read at `2c6a1ef05d`):** Facts: the global
  search is a page-bar button (signed-in only) opening a modal with the
  shared search form and no note; Ctrl/Cmd+F is the bar's `note-search`
  registration, unguarded by editable targets, and Ctrl/Cmd+Shift+F is the
  toolbar's note-local search; tapping the bar button primes a hidden input
  on coarse-pointer devices so the field's autofocus keeps the phone
  keyboard; result title links navigate by named route and emit a close,
  which modified clicks skip; the panel is a details-based dropdown that
  closes on outside click, Escape, route change, and the launcher. Settled
  by a scratch browser-mode spec mounting the search form inside such a
  dropdown (deleted after the run): the history list teleports outside the
  panel and picking a key closed the panel, and Recent notes rendered as
  soon as the form mounted. No product runtime was started; no product file
  was edited. Remaining hypothesis, settled only by use: a search field at
  the bottom of the panel is as easy to find as the bar's magnifier was.
- **Value / learning:** Delivers the approved combined command and search
  journey while keeping the search people already know, and tests whether
  one quiet panel can carry both commands and a working search on a phone.
- **Effort hypothesis:** S, 45–60 minutes, medium confidence. Refinement
  found the search form, results, history, scope controls, keyboard
  navigation, and shortcut binding reuse intact; the work is relocation,
  the entered-search disclosure, the nested-dropdown closing rule, panel
  scrolling, the bar control's removal, and retargeting the E2E search
  steps and bar tests. No smaller boundary keeps a useful outcome: a
  half-moved search would leave two controls. Reconciling with correction-1
  is the sizing risk.
- **Depends on:** No blocking supplier story established. The existing menu
  and search already support this journey; reconcile their common direction
  with story 1 and correction-1 through ordinary shared ownership, not an
  invented wait.
- **Safe stopping point:** Search within the command menu is usable even if
  note tools and property presentation are unchanged. Current result access,
  touch focus, and keyboard journeys must survive this story on their own.

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

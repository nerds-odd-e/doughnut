---
id: SEED-033
status: dormant
planted: 2026-09-18
planted_during: investigation of embedded NoteShow and owner backlog request
trigger_when: story 2 selected by owner for refinement on 2026-09-27
scope: medium
---

# SEED-033: Simplify note presentation outside the note page

## Why This Matters

A full editable note used to appear beneath spelling results and still
appears inside Message Center conversations and during Just review. An
embedded NoteShow has no notebook sidebar, while making its page controls
work there requires context-specific behavior. Removing the spelling embed
made results easier to scan, but after a wrong answer the learner now needs
another click to read the note's properties, content, and references. The
owner wants that context
back without putting a full note page inside another workflow.

Two other embedded consumers remain: Message Center's conversation subject,
and memory-tracker review through Just review. Replacing those embeds lets
maintainers simplify NoteShow and NoteShowPage around the note page's needs,
with fewer constraints on its menu and layout. The selected replacement
interactions are recorded in story 2 below.

## Alternatives and Decision

The owner chose two stories on 2026-09-18: remove the spelling-result embed
first, and queue the remaining embedded-use removal and resulting cleanup
last. Keeping all embeds would retain the page-specific complexity; removing
all three at once would have deferred the small known spelling gain. The owner
accepted the first removal as a separate outcome.

On 2026-09-27, after experiencing the extra click following a wrong spelling
answer, the owner selected a common read-only note-reading direction for all
three workflows. The reading view shows the complete properties, body, and
inbound references and links to the full note page. It carries no note-page
toolbar, editing controls, or notebook sidebar. The existing main menu and
sidebar are outside this story.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours. These are hypotheses
including delivery, not commitments.

<a id="story-2"></a>

### 2. Read note context during spelling answers, conversations, and Just review

**Identity:** SEED-033#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/012-read-note-context/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"09854d0f0283a70e00c83145aae90fb8c316cafc1e7a5e19aecdecfbabeaf141","plan":"360483de49d096efff5f56c0d6b939159d7f861e57ce6396653619467b9890c5"}}
```

**Goal:** Learners can understand a spelling result, discuss a note, and grade
a Just review while seeing the note information relevant to that task. These
workflows use a read-only note-reading view instead of embedding the full
NoteShow page, leaving NoteShow and NoteShowPage dedicated to the note page.

**Scope:**

- The shared reading view presents the note title and location, every property
  and its value, the complete body (including images and resolved links), and
  the inbound references when present. It has an **Open full note** route for
  editing and page-specific actions. It never exposes note editing controls.
- A wrong spelling result automatically reveals the reviewed note's reading
  view after the answer is submitted, without another click. A correct result
  keeps its compact summary and offers the reading view on demand. An
  accidental match distinguishes the reviewed note from the different note
  named by the answer.
- A note-subject conversation keeps its messages and composer primary, with
  the note reading view beside them on wider screens and available from the
  conversation header on narrow screens. Recall-prompt subjects retain their
  own answer context; they are not reinterpreted as note subjects.
- Just review shows the reading view before Good/Again grading, with grading
  controls kept reachable while reading a long note. A property memory tracker
  indicates its focused property without hiding the rest of the note. The
  existing grade and scheduling behavior remains.
- Remove the conversation and Just review NoteShow embeds and the NoteShow /
  NoteShowPage options and branches confirmed to exist only for embeds. Keep
  the note page's own editing, sidebar, and conversation capabilities. The
  global main menu and notebook sidebar are not redesigned or changed.

**Key examples:**

1. A learner answers a spelling prompt incorrectly → the result names the
   reviewed note and immediately displays all its properties, body, and
   inbound references; **Open full note** reaches the ordinary note page.
2. A learner answers correctly → the concise success result remains easy to
   scan; the note reading view is available without loading a full note page
   inside the result.
3. A learner selects a conversation about a note → messages, reply composer,
   and the read-only note context can be used together on desktop; on a narrow
   screen, the same complete context is reachable from the conversation.
4. A learner reaches Just review for a property tracker → the complete note is
   readable, the focused property is identifiable, and Good/Again remains
   reachable and grades the same tracker as before.
5. A conversation about a recall prompt still presents its answered-question
   subject; the note page still provides its sidebar, toolbar, editing, and
   on-page conversation behavior.

**Value / learning:** Restore immediate learning context and free NoteShow
from non-page consumers without duplicating a second editable note page.

**Effort hypothesis:** L — one shared reading capability across three
workflows, with low confidence until the slice plan sizes the rendering and
responsive layout work.

**Depends on:** Story 1's spelling embed removal, already delivered.

**Safe stopping point:** Each workflow stays usable between slices, and full
note details remain reachable throughout; the final state has no non-note-page
NoteShow caller.

## Ordering and Scope Reduction

Story 1 delivered its small spelling-result simplification independently.
Story 2 remains in its existing backlog position while the owner prepares its
expanded three-workflow outcome. Existing Taken work, unrelated queue order,
and the near-future notebook-publication direction remain unchanged.

## When to Surface

Story 2 was selected for refinement on 2026-09-27 after the owner experienced
the extra click following a wrong spelling answer.

## Breadcrumbs

- 2026-09-18 investigation: NoteShow has four production consumers: the note
  page, spelling results, conversation subject, and memory-tracker Just review.
- Owner request: queue the first removal first and refine/plan it; queue removal
  of the other two embeds and complete resulting page simplification last.
- [SEED-032](SEED-032-retry-overlapped-spelling-match.md#story-1) owns the
  separate declared-overlap inline-retry interaction.

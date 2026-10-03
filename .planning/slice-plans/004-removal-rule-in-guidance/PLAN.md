# Removals leave no trace without the owner restating the rule

**Identity:** SEED-068#removal-rule-in-guidance

## Source

[Removals leave no trace without the owner restating the rule](../../seeds/SEED-068-owner-rules-reach-every-agent.md#removal-rule-in-guidance),
refined on 2026-10-03 from DD-202 and DD-142 in
[Donut retrospective findings](../../../DonutRetrospectiveFindings.md#open-findings).
Owner decisions (2026-10-03): a "never happens" test written for a removal is
a trace; this story also deletes the one left on main.

## Goal and scope

Any agent given a removal (refiner, planner, implementer, refactor agent,
coordinator, retrospective reviewer) leaves no trace of the removed thing,
without the owner restating the rule.

- **Included:** One principle in `AGENTS.md` and `CLAUDE.md`, kept in sync,
  with the rule's three parts (delete outright and transitively; sweep the
  whole product; no absence check, "never happens" test, return guard, or
  "no longer" note) and what each role does with it. Delete the leftover test
  `never changes the title across many chunks and a later recording` and the
  fixture details only it used.
- **Excluded:** Shared Open Dough `dough-*` skills (overwritten by
  `dough-update`). Any automated trace check. A search for other old traces.
- **Boundary:** a refusal the product enforces (for example, a non-owner
  cannot edit a notebook) is not a removal; its tests stay.

## Decisive premises

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| Delegated Claude Code agents that plan or deliver receive `CLAUDE.md` | Slice 1 | Searched the last 80 subagent transcripts under `~/.claude/projects/-Users-terryyin-git-doughnut/*/subagents/` for the `CLAUDE.md` text "Portable digest" | Confirmed: every general-purpose agent (implementers, refactorers, retrospectives, CI repair) had it; only two read-only Explore agents did not, and they neither plan nor deliver |
| Codex agents receive `AGENTS.md` | Slice 1 | Codex's documented behavior; `CLAUDE.md` says to keep both files in sync | Accepted without a local Codex run |
| No script or test checks `AGENTS.md` against `CLAUDE.md` | Slice 1 | `git grep -l "AGENTS.md" -- scripts '*.mjs' '*.ts' '*.sh'` | No hits; the two files are synced by hand and differ only in lines 1 and 3 (`diff AGENTS.md CLAUDE.md`) |
| The leftover test is the only title check in the audio-tools specs, and some fixture details exist only for it | Slice 2 | `git grep -n -i title frontend/tests/notes/NoteAudioTools* frontend/tests/notes/noteAudioTools*`; `git show aa093fffeb -- frontend/tests/notes/NoteAudioTools.processing.spec.ts` | Confirmed. `aa093fffeb` added, for that test only: `.title("Author chosen title")` on `originalRealm`, `.title(note.noteTopology.title)` in the `updateNoteContent` mock, and the `flushPromises` and `stopRecording` imports (each used once more, only in that test) |

## Outside-in proof

| Key example | Signal |
| --- | --- |
| A fresh agent refines or plans a removal without a negative promise, absence check, or "no longer" note | Slice 1: a fresh general-purpose subagent, given only the SEED-066 removal goal ("stop dictation from changing note titles; remove automatic title generation from voice input"), outlines scope and slices that delete and sweep, with no "never changes the title" promise, test, or docs note |
| A role meeting a plan step that prescribes a trace skips and reports it | Slice 1: the rule text says so for implementer, refactor agent and coordinator (inspection) |
| Leftover mentions are swept | Slice 1: the rule text names the whole-product sweep including docs (inspection) |
| An enforced refusal keeps its tests | Slice 1: the rule text states the boundary (inspection) |
| After delivery, the processing spec has no title test | Slice 2: `git grep -n -i title frontend/tests/notes/NoteAudioTools.processing.spec.ts` returns nothing, and `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/notes/NoteAudioTools.processing.spec.ts` passes |

The guidance proof is a demonstration, not a test: guidance has no executable
entry point, and the story's deferred promises exclude an automated trace check.

## Slices

### 1. Removal rule in always-loaded guidance
Type: Behavior
Status: done
Proof: inspection of both files plus the fresh-subagent demonstration above;
`diff AGENTS.md CLAUDE.md` still shows only lines 1 and 3.

Behavior: an agent is given a removal → it refines, plans, implements,
refactors and reviews it as deletion plus a whole-product sweep, leaving no
trace, and skips and reports any step that asks for one.

Add principle 7 under `## Principles` in both files, matching the digest style
(short and self-contained, since no Donut-owned skill can hold the detail).
It names the three parts, the role duties, and the enforced-refusal boundary.

### 2. Delete the leftover title test
Type: Behavior
Status: done
Proof: the grep and focused spec run in Outside-in proof.

Behavior: main follows the rule → `NoteAudioTools.processing.spec.ts` has no
test that dictation leaves the title unchanged, and no fixture detail that
only served it.

Delete the test, `.title("Author chosen title")`,
`.title(note.noteTopology.title)`, and the `flushPromises` and
`stopRecording` imports.

## Current decisions

- The principle lives only in `AGENTS.md` and `CLAUDE.md`; it cites no skill,
  because the stack skills do not own removal and the `dough-*` skills are
  shared.

## Learnings

- Slice 1 proof (2026-10-03): principle 7 is identical in both files and
  `diff AGENTS.md CLAUDE.md` still shows only lines 1 and 3. A fresh
  general-purpose subagent, given only the SEED-066 goal, found the behavior
  already gone and planned only deletion plus sweep: it named the leftover
  title test as a trace to delete and wrote no negative promise, absence test,
  or "no longer" note.
- That agent also noticed possible traces outside this story's scope, which
  excludes searching for other old traces: an unused
  `frontend/src/components/notes/SuggestTitle.vue` and a "both titles survived
  unchanged" phrase in `docs/voice-input.md`. Reported to the owner; not acted on.
- Slice 2 proof (2026-10-03): the title grep on
  `NoteAudioTools.processing.spec.ts` returns nothing and the focused spec
  passes (6 tests). The `startRecording` import was also only used by the
  deleted test and went with it; the helpers stay because
  `NoteAudioTools.recording.spec.ts` uses them.

## Execution complete

Product advice: no change to this story. `frontend/src/components/notes/SuggestTitle.vue`
is an unused component left by an older Wikidata dialog rework (e729a7aad5), not by the
dictation-title removal; the owner may choose a small cleanup that deletes it and anything
only it used under principle 7. The "both titles survived unchanged" phrase in
`docs/voice-input.md` records an observed bug reproduction and stays as is.

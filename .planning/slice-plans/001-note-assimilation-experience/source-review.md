# Source review: Choose how to assimilate a note and when to move on

**Launch:** 2026-11-09, fresh general-purpose sub-agent of the Execute Plan
coordinator (Claude Code, `claude-opus-5-5`), no conversation inheritance, fork
or resume. Instructed to use only the supplied text, read no project files,
change nothing, and return numbered commitments (compound items split),
qualifications, trigger/journey examples and exclusions, without slice
ownership or acceptance. It used no tools.

**Input:** the verbatim text of
[SEED-113#assimilate-notes-with-a-clear-default-and-result](../../seeds/SEED-113-note-assimilation-experience.md#assimilate-notes-with-a-clear-default-and-result)
— heading, Goal, Why this matters, Scope (bullets and following paragraph),
Scope limits, Key examples, and the Challenge and decision (Terry's explicit
2026-11-09 amendment). No plan, investigation, architecture, product code or
proof was supplied.

## Returned qualifications

- G.1–G.3 are the outcome the other commitments serve, not separate checks.
- 1.1: the two mockups are Visible options (2.x) and Read first (3.x); one
  experience that changes with width.
- 2.1 / 3.1 / 3.5 / 3.6: widths are approximate; the breakpoint is set by the
  space available to the panel; iPad portrait is a layout check, not the
  definition.
- 2.3–2.5: Understanding is the default, one primary action, Skip secondary.
  Beyond "title first" and "alongside", no further order or styling is set.
- 2.7 / 2.8: description and distinct action apply to each optional mode;
  Verify spelling and Add to sessions read as those actions.
- 3.4: E1 says the optional modes are "one tap away from the bottom bar".
- 4.1.1–4.1.3: existing icons and success checkmark are retained, in any layout.
- 4.2.1: title or alias is an alternative, not a restriction.
- 4.2.2 + 8.3: the Spelling tracker is added only after verification succeeds.
- 4.3: human or AI tutor is an alternative; adding does not launch a session.
- 4.5 / 5.8 / 6.11: adding an optional mode does not advance; keeps the result
  and keeps a skip.
- 4.6.x: "already active" means modes added before.
- 5.1: the result is temporary; not restored after reload.
- 5.6: applies also when the completed Understanding was the last unit (E5).
- 5.7: deliberate advance applies after note-level Understanding; property
  rows keep their advance (7.3); a confirmed skip advances immediately (6.7).
- 6.1–6.4.2: the confirmation wording follows the 2026-11-09 decision.
- 6.4.x vs 6.5.x: "Its property entries and optional modes are unaffected" is
  kept both as confirmation text and as behavior.
- 6.6: conditional — only when such a property is next.
- 6.3 / 6.9 / 6.10: "until restored" is fulfilled by Return to sequence.
- 6.8 / 6.9: both remain available on revisit.
- 7.6–7.8: apply to an explicit advance (E5 shows Next item); whether a skip
  that finds no next item counts is neither stated nor excluded.
- 7.7: reuse the existing message.
- 7.9: the sequence continues past the daily target; quotas unchanged.
- Scope limits are delivery boundaries, not product refusals.

## Returned key examples

| # | Trigger | Journey | Result | Illustrates |
| --- | --- | --- | --- | --- |
| E1 | Learner opens かた と ほう on a wide screen | Visible options layout | Title, default Understanding action and both optional modes visible | G.1, G.2, 1.1, 2.1–2.8 |
| E1b | Same note at iPad portrait width | Read first layout | Note first; optional modes one tap away from the bottom bar | 3.1–3.6 |
| E2 | Learner selects Assimilate understanding | Result shows; learner verifies Spelling or adds Commissioned | Result names what was added; title visible; result kept; only Next item advances | G.3, 4.2.1, 4.3–4.5, 5.1–5.8 |
| E3 | Note has pending Understanding and a property entry | Confirm Skip from sequence…; reopen later | Only note-level Understanding excluded; property eligible; Return to sequence restores; direct Understanding available; optional additions keep the skip | 6.1–6.11 |
| E4 | Understanding succeeds; next unit is a same-note property | Next item | Property opens in sequence order; property assimilation keeps its advance | 5.5, 5.7, 7.1–7.5 |
| E5 | Understanding succeeds on the last eligible unit | Optional modes still addable; Next item | No more notes to assimilate; presentation closes; note page stays | 5.6, 7.6–7.8 |

## Returned exclusions

- X1: "The story covers the note-level panel. Property-row assimilation is a
  separate concern." (7.2, 7.3, 7.5 still preserve property ordering/advance.)
- X2: does not "redesign property-row learning".
- X3: does not "launch or change commissioned learning sessions".
- X4: does not "change scheduling or daily quotas".
- X5: does not "restore the temporary success result after a browser reload".
- X2–X5 are "delivery boundaries, not new product refusals".

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| G.1 | A learner can see which note they are assimilating | slice 5 |  |
| G.2 | distinguish the default Understanding action from optional modes | slice 5 |  |
| G.3 | decide when to advance after seeing what was added to recall | slice 5 |  |
| 1.1 | Combine the two selected mockups into one responsive note-level assimilation experience. | slice 5 |  |
| 2.1 | At wider widths, use Visible options. | slice 1 | observed: frontend/tests/layouts/NotebookWorkspaceAssimilation.spec.ts 'reads the note title first, then the default Understanding action beside the optional modes' (1280px workspace, production CSS): two columns at wide width |
| 2.2 | Put the note title first | slice 1 | observed: frontend/tests/layouts/NotebookWorkspaceAssimilation.spec.ts 'reads the note title first, then the default Understanding action beside the optional modes' (1280px workspace, production CSS): note-title bottom <= action and mode-icon tops |
| 2.3 | show default Understanding alongside Optional modes | slice 1 | observed: frontend/tests/layouts/NotebookWorkspaceAssimilation.spec.ts 'reads the note title first, then the default Understanding action beside the optional modes' (1280px workspace, production CSS): optional column beside Understanding (icon top < action bottom, icon left >= action right); frontend/tests/components/recall/AssimilationPanel.spec.ts 'offers Understanding as the one primary action and each optional mode with its icon, description and own action': Default mode then Optional modes |
| 2.4 | Use one primary Assimilate understanding action | slice 1 | observed: frontend/tests/components/recall/AssimilationPanel.spec.ts 'offers Understanding as the one primary action and each optional mode with its icon, description and own action': only .daisy-btn-primary is 'Assimilate understanding'; frontend/tests/layouts/NotebookWorkspaceAssimilation.spec.ts 'reads the note title first, then the default Understanding action beside the optional modes' (1280px workspace, production CSS): primary background differs from optional actions |
| 2.5 | with Skip from sequence… as a secondary action | slice 1 | observed: frontend/tests/components/recall/AssimilationPanel.spec.ts 'offers Understanding as the one primary action and each optional mode with its icon, description and own action': Understanding row lists ghost 'Skip from sequence…' after primary action |
| 2.6 | Show both optional modes (Spelling and Commissioned) | slice 1 | observed: frontend/tests/components/recall/AssimilationPanel.spec.ts 'offers Understanding as the one primary action and each optional mode with its icon, description and own action': SPELLING and COMMISSIONED rows rendered |
| 2.7 | each optional mode is shown with a short description | slice 1 | observed: frontend/tests/components/recall/AssimilationPanel.spec.ts 'offers Understanding as the one primary action and each optional mode with its icon, description and own action': descriptions 'Recall the title or an alias from memory.' and 'Review in future learning sessions with a human or AI tutor.' |
| 2.8 | each optional mode is shown with a distinct action | slice 1 | observed: frontend/tests/components/recall/AssimilationPanel.spec.ts 'offers Understanding as the one primary action and each optional mode with its icon, description and own action': own actions 'Verify spelling' and 'Add to sessions'; frontend/tests/layouts/NotebookWorkspaceAssimilation.spec.ts 'reads the note title first, then the default Understanding action beside the optional modes' (1280px workspace, production CSS): same outline style as each other, distinct from primary |
| 3.1 | Around iPad portrait width and below, use Read first. | slice 5 |  |
| 3.2.1 | Show the note title … before the assimilation actions | slice 5 |  |
| 3.2.2 | Show the note … properties … before the assimilation actions | slice 5 |  |
| 3.2.3 | Show the note … content before the assimilation actions | slice 5 |  |
| 3.3.1 | Keep Assimilate understanding … accessible in a bottom action bar | slice 5 |  |
| 3.3.2 | Keep … Skip from sequence… … accessible in a bottom action bar | slice 5 |  |
| 3.3.3 | Keep … Optional modes accessible in a bottom action bar | slice 5 |  |
| 3.4 | Open Spelling and Commissioned in a popover from that bar. | slice 5 |  |
| 3.5 | Choose the breakpoint by the space available to the panel | slice 5 |  |
| 3.6 | use iPad portrait as a layout check | slice 5 |  |
| 4.1.1 | Keep the spelling … icon next to the Spelling … label | slice 1 | observed: frontend/tests/components/recall/AssimilationPanel.spec.ts 'offers Understanding as the one primary action and each optional mode with its icon, description and own action': lucide-spell-check svg in SPELLING row; frontend/tests/layouts/NotebookWorkspaceAssimilation.spec.ts 'reads the note title first, then the default Understanding action beside the optional modes' (1280px workspace, production CSS): icon width > 0 and left of 'Spelling' label |
| 4.1.2 | Keep the … tutor icon next to the … Commissioned label | slice 1 | observed: frontend/tests/components/recall/AssimilationPanel.spec.ts 'offers Understanding as the one primary action and each optional mode with its icon, description and own action': lucide-graduation-cap svg in COMMISSIONED row; frontend/tests/layouts/NotebookWorkspaceAssimilation.spec.ts 'reads the note title first, then the default Understanding action beside the optional modes' (1280px workspace, production CSS): icon left of 'Commissioned' label |
| 4.1.3 | Keep the checkmark for the successful result | slice 2 |  |
| 4.2.1 | Verify spelling asks for the title or an alias from memory | slice 1 | observed: frontend/tests/components/recall/AssimilationPanel.trackers.spec.ts spelling tests (popup asks recall, posts only after verification) + e2e_test/features/assimilation/assimilate_with_remembering_spelling.feature 6 scenarios incl. alias, passed |
| 4.2.2 | with the note hidden during verification | slice 1 | observed: frontend/tests/components/recall/AssimilationPanel.trackers.spec.ts spelling popup with opaque-content-blocker overlay (z-index 9989) while verifying; cancel posts nothing |
| 4.3 | Add to sessions adds the note for future learning sessions with a human or AI tutor. | slice 1 | observed: frontend/tests/components/recall/AssimilationPanel.trackers.spec.ts commissioned assimilation posts assimilateAsCommissioned and stays on note; e2e commissioned_learning_session.feature steps through tracker creation passed |
| 4.4 | Each optional mode can be added independently | slice 1 | observed: frontend/tests/components/recall/AssimilationPanel.trackers.spec.ts each optional mode offered independently of existing trackers |
| 4.5 | each optional mode, when added, keeps the learner on the note | slice 1 | observed: frontend/tests/components/recall/AssimilationPanel.trackers.spec.ts commissioned/spelling additions keep route on the same note |
| 4.6.1 | Already active modes show their tracker | slice 1 | observed: frontend/tests/components/recall/AssimilationPanel.trackers.spec.ts 'shows its tracker link and next recall date in place of its action': View tracker link to memoryTrackerShow |
| 4.6.2 | Already active modes show their … next recall date | slice 1 | observed: frontend/tests/components/recall/AssimilationPanel.trackers.spec.ts 'shows its tracker link and next recall date in place of its action': 'Next <date>' text |
| 5.1 | After Understanding succeeds, keep the current note … visible in a temporary result state. | slice 2 |  |
| 5.2 | keep the current title visible (E2: the note title remains visible) | slice 2 |  |
| 5.3 | Show a checkmark | slice 2 |  |
| 5.4 | Show Understanding added to recall (E2: The result names what was added) | slice 2 |  |
| 5.5 | Offer Next item as the primary action. | slice 2 |  |
| 5.6 | Leave any available optional modes accessible so the learner can add them before deliberately moving on. | slice 2 |  |
| 5.7 | The sequence waits at the result until the learner chooses to advance. (E2: Only Next item advances the sequence after Understanding succeeds.) | slice 2 |  |
| 5.8 | Adding an optional mode preserves the Understanding result on that same note. | slice 2 |  |
| 6.1 | preserve note-level skip (Decision) | slice 3 |  |
| 6.2 | Skip from sequence… opens a confirmation | slice 3 |  |
| 6.3 | the confirmation explains that only this note's Understanding entry stays out of the sequence until restored | slice 3 |  |
| 6.4.1 | the confirmation explains that property learning is unaffected (Decision) | slice 3 |  |
| 6.4.2 | the confirmation explains that optional modes are unaffected (Decision) | slice 3 |  |
| 6.5.1 | the skip leaves its property entries unaffected (E3: only note-level Understanding is excluded and the property remains eligible) | slice 3 |  |
| 6.5.2 | the skip leaves its optional modes unaffected | slice 3 |  |
| 6.6 | a property on this same note may appear next | slice 3 |  |
| 6.7 | Confirming a skip advances the sequence. | slice 3 |  |
| 6.8 | Revisiting a skipped note still allows direct Understanding assimilation (E3: Direct Understanding assimilation also remains available) | slice 3 |  |
| 6.9 | Revisiting a skipped note still allows … Return to sequence. | slice 3 |  |
| 6.10 | Return to sequence, which makes its Understanding entry eligible again (E3) | slice 3 |  |
| 6.11 | optional additions preserve the skip (E3) | slice 3 |  |
| 7.1 | Preserve the existing sequence order | slice 2 |  |
| 7.2 | Preserve the existing sequence order, including property entries | slice 2 |  |
| 7.3 | Preserve property-row advancement (E4: Assimilating a property retains its existing advance behavior.) | slice 2 |  |
| 7.4 | Next item accommodates a property destination | slice 2 |  |
| 7.5 | a property destination, including one on the current note (E4: that property opens in the existing sequence order) | slice 2 |  |
| 7.6 | When an explicit advance finds no next item, keep the current note page | slice 2 |  |
| 7.7 | When an explicit advance finds no next item, show the existing No more notes to assimilate feedback | slice 2 |  |
| 7.8 | When an explicit advance finds no next item, close the assimilation presentation | slice 2 |  |
| 7.9 | A daily target being reached does not end the sequence while another item exists. | slice 2 |  |
| 8.1 | Existing authorization … rules continue to apply | slice 5 |  |
| 8.2 | Existing … tracker rules continue to apply | slice 5 |  |
| 8.3 | Spelling still requires successful verification before adding its tracker. | slice 1 | observed: frontend/tests/components/recall/AssimilationPanel.trackers.spec.ts spelling tests: no tracker POST until verification succeeds; cancel posts nothing |

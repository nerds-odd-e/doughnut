# Source review

**Launch.** Fresh general-purpose reviewer (`a6dfbb91-5eef-4333-9926-4771c6f525dc`), source-only: Goal, Scope, and plan Correction input (Provenance through Preserved promises); no plan slicing, product files, or proof.

**Input.** SEED-113#keep-the-assimilation-panel-in-place-after-staying-actions Goal and Scope from `.planning/seeds/SEED-113-note-assimilation-experience.md`, plus Correction input from `.planning/slice-plans/004-keep-assimilation-panel-in-place/PLAN.md`.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | After a learner assimilates Understanding (an action that stays on the note), they remain on the same note-level assimilation panel without an extra note reload and without the panel being rebuilt, and keyboard focus the panel placed stays where it was. Applies in both Visible options and Read first placements. | slice 1 | observed: NotebookWorkspaceUnderstandingResult.spec.ts keeps this visit's Understanding result: panel.isConnected + no extra showNote after assimilate-UNDERSTANDING |
| 2 | After a learner adds an optional mode (an action that stays on the note), they remain on the same note-level assimilation panel without an extra note reload and without the panel being rebuilt, and keyboard focus the panel placed stays where it was. Applies in both Visible options and Read first placements. Journey example: in Read first, focus returned to the Optional modes trigger stays there. | slice 1 | observed: NotebookWorkspaceCompactOptionalModes.spec.ts adding Commissioned: expectClosedAtTrigger (focus on Optional modes); UnderstandingResult: panel.isConnected + no extra showNote after assimilate-COMMISSIONED |
| 3 | After a learner skips with nothing left to assimilate (an action that stays on the note; e.g. a confirmed skip that finds nothing next), they remain on the same note-level assimilation panel without an extra note reload and without the panel being rebuilt, and keyboard focus the panel placed stays where it was. Applies in both Visible options and Read first placements. | slice 1 | observed: NotebookWorkspaceSequenceSkip.spec.ts confirming Skip with nothing next: NO_MORE_TOAST, Note activity, no extra showNote, same note route/title |
| 4 | After each of those staying actions, the open panel updates in place: no note reload and no rebuilt panel. | slice 1 | observed: UnderstandingResult + SequenceSkip in-place/no-showNote observations after Understanding, optional addition, and skip-nothing-next |
| 5 | Every promise of completed story SEED-113#assimilate-notes-with-a-clear-default-and-result remains unchanged (this correction does not add a product promise beyond that correction). | slice 1 | observed: Focused assimilation NotebookWorkspace*/AssimilationPanel* specs + E2E assimilation family stayed green |
| 5.1 | Understanding result survives optional additions | slice 1 | observed: NotebookWorkspaceUnderstandingResult.spec.ts keeps result through optional addition |
| 5.2 | Understanding result survives width changes | slice 1 | observed: Existing NotebookWorkspace UnderstandingResult / width placement specs unchanged and green |
| 5.3 | Understanding result resets when another note opens | slice 1 | observed: NotebookWorkspaceUnderstandingResult.spec.ts resets Understanding result when earlier note opens |
| 5.4 | Tracker links update | slice 1 | observed: UnderstandingResult / CompactOptionalModes expectTrackerStatus after optional addition |
| 5.5 | Next recall dates update | slice 1 | observed: Existing AssimilationPanel.trackers / workspace tracker next-recall observations stayed green |
| 5.6 | Next item behaves as now | slice 1 | observed: NotebookWorkspaceUnderstandingResult.spec.ts Next item opens the next note |
| 5.7 | Same-note property destinations behave as now | slice 1 | observed: NotebookWorkspaceSequenceSkip.spec.ts confirming skip opens property destination |
| 5.8 | Daily-goal feedback behaves as now | slice 1 | observed: NotebookWorkspaceUnderstandingResult Next item once daily goal reached stayed green |
| 5.9 | No more notes to assimilate behaves as now (presentation dismissed, note kept) | slice 1 | observed: SequenceSkip confirming Skip with nothing next: presentation dismissed (Note activity), note kept |
| 5.10 | Skip confirmation behaves as now | slice 1 | observed: NotebookWorkspaceSequenceSkip.spec.ts Skip confirmation Cancel/OK scenarios stayed green |
| 5.11 | Return to sequence behaves as now | slice 1 | observed: Existing Return to sequence coverage in assimilation specs/E2E stayed green |
| 5.12 | Direct Understanding behaves as now | slice 1 | observed: AssimilationPanel / workspace direct Understanding paths stayed green |
| 5.13 | Property rows keep their own advance | slice 1 | observed: SequenceSkip property advance after skip stayed green |
| 5.14 | Spelling verification still covers the note | slice 1 | observed: NotebookWorkspaceCompactOptionalModes Verify spelling + E2E assimilate_with_remembering_spelling stayed green |
| 6 | ADRs 0001, 0005 and 0006 apply unchanged. | slice 1 | observed: No ADR-contrary routing/API/failure-handling introduced; fail-loudly reload path removed; typechecks and suites green |

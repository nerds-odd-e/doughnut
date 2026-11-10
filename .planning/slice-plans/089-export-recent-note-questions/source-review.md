# Source review

Source: `SEED-118#export-a-note-with-recent-quiz-questions` Goal, Scope, key examples,
exclusions, refinement decisions (2026-11-10), UI, and Architecture.

Reviewer Task agent `98980b30-5f32-4afa-a928-b54e4ec85e73` was launched with a
fresh general-purpose context and supplied only that original source text. No
plan, product, proof, or project files were supplied. The completed review is
retained below without new analysis. Example conditions and journeys do not
introduce additional rejection rules.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| C1 | A learner can export a note with its recent multiple-choice questions and their own answers, so they can later share the Markdown with an AI to analyze the questions and identify ways to improve the note. | slice 4 |  |
| C2 | Add an unchecked **Include recent multiple-choice questions** checkbox to **Export Note Data**. | slice 4 |  |
| C3 | When checked, the preview and Markdown available to copy or download include up to 10 distinct multiple-choice questions belonging to the exported note, ordered by question creation time descending, then ID descending. | slice 2 | observed: NoteExportControllerAiContextMarkdownTests.RecentQuestionsAppendix#newestTenByCreationThenIdIgnoresInsertionOrderAndIncludesEqualTimestampTies |
| C3.1 | Inclusion is limited to at most 10 distinct multiple-choice questions belonging to the exported note. | slice 2 | observed: same newest-ten test; McqRepository.findTop10ByNote_IdOrderByCreatedAtDescIdDesc |
| C3.2 | Ordering is by question creation time descending, then ID descending. | slice 2 | observed: same newest-ten chronology + equal-timestamp ID order |
| C4 | Include each complete stem, its choices, and a separately labeled **Donut answer key**. | slice 2 | observed: RecentQuestionsAppendix#includeTrueRendersCanonicalAppendixShape + Donut answer key label |
| C4.1 | Include each complete stem. | slice 2 | observed: canonical shape stem assertion |
| C4.2 | Include its choices. | slice 2 | observed: canonical shape ordered choices |
| C4.3 | Include a separately labeled **Donut answer key**. | slice 2 | observed: canonical shape Donut answer key label |
| C5 | For questions the exporting learner answered, include their latest recorded selected answer (answer time descending, then answer ID descending). | slice 3 | observed: LearnerSelectionsTests repeated/time/id + threeQuestionsOneAnswered |
| C5.1 | Include the exporting learner’s latest recorded selected answer when they answered. | slice 3 | observed: threeQuestionsOneAnsweredShowsOwnSelectionSeparateFromDonutKey |
| C5.2 | Latest selection is determined by answer time descending, then answer ID descending. | slice 3 | observed: repeatedOwnAnswersKeepLatestByAnswerTime + equalAnswerTimesPreferHigherAnswerId |
| C6 | An unanswered question remains included without a learner selection. | slice 2 | observed: questionsWithoutPromptsAreIncluded (unanswered/no selection yet) |
| C7 | Questions without prompts also qualify. | slice 2 | observed: questionsWithoutPromptsAreIncluded |
| C8 | Selecting questions does not filter on contested state or an attempt's exclusion state. | slice 2 | observed: contestedQuestionsAreIncludedWithoutSchedulingFilter; Mcq-only query |
| C8.1 | Contested state is not a selection filter. | slice 2 | observed: contestedQuestionsAreIncludedWithoutSchedulingFilter |
| C8.2 | An attempt’s exclusion state is not a selection filter. | slice 2 | observed: McqRepository selection has no attempt exclusion filter |
| C9 | The answer lookup belongs to the exporting learner; note visibility does not grant access to another learner's selections. | slice 3 | observed: newerOtherLearnerAttemptDoesNotSubstitute + questionAnsweredOnlyByOtherLearner |
| C10 | Preserve existing note-read access, including ordinary export for a guest, who has no personal selections to attach. | slice 1 | observed: backend/src/test/java/com/odde/donut/controllers/NoteExportControllerAiContextMarkdownTests.java#bazaarGuestGetsOrdinaryExportViaHttp |
| C11 | With the checkbox cleared, export the usual note context. | slice 4 |  |
| C12 | Keep the existing focus/related-note-body token budget; selected questions form a complete appendix outside that body budget. | slice 4 |  |
| C12.1 | Keep the existing focus/related-note-body token budget. | slice 1 | observed: backend/src/main/java/com/odde/donut/controllers/NoteExportController.java#getAiContextMarkdown tokenLimit; frontend NoteExportForm.vue token budget |
| C12.2 | Selected questions form a complete appendix outside that body budget. | slice 2 | observed: NoteExportController appends appendix after tokenLimit focus render |
| C13 | The story ends at the Markdown export. | slice 4 |  |
| C14 | Deferred: automatic AI analysis or note changes. |  | X1 |
| C15 | Deferred: a separate recall-history report. |  | X2 |
| C16 | Deferred: historical note-version reconstruction. |  | X3 |
| C17 | Deferred: additional quiz metadata. |  | X4 |
| C18 | A note has 12 multiple-choice questions. With the checkbox selected, the preview, copied, and downloaded Markdown includes the 10 newest by question creation time, including unanswered questions and each Donut answer key. |  | G2 |
| C19 | A note has three multiple-choice questions and the learner answered one. The export includes all three questions and shows the learner's selected answer for that one question, separately from Donut's answer key. | slice 3 | observed: threeQuestionsOneAnsweredShowsOwnSelectionSeparateFromDonutKey |
| C20 | A question was created yesterday but presented today, while another was created today and never presented → export with inclusion selected → today's new question comes first. Equal creation times use descending question IDs. | slice 2 | observed: newestTenByCreationThenIdIgnoresInsertionOrderAndIncludesEqualTimestampTies |
| C21 | The learner answered one question twice; another learner answered it later → export → one question entry contains the exporting learner's latest selection. Another learner's selection never substitutes for it. A question answered only by another learner has no selection for the exporter. | slice 3 | observed: LearnerSelectionsTests compound: repeated, other-learner, other-only |
| C21.1 | Exporting learner’s two answers → one entry with their latest selection. | slice 3 | observed: repeatedOwnAnswersKeepLatestByAnswerTime |
| C21.2 | Another learner’s later answer never substitutes. | slice 3 | observed: newerOtherLearnerAttemptDoesNotSubstitute |
| C21.3 | A question answered only by another learner has no selection for the exporter. | slice 3 | observed: questionAnsweredOnlyByOtherLearnerHasNoSelectionForExporter |
| C22 | A readable note has no questions, or is read by a guest → select inclusion → its note context still exports; a guest's output contains no personal selections. No new login prerequisite is introduced. | slice 3 | observed: guestIncludeRecentQuestionsGetsKeysWithoutPersonalSelections + zero-question context from slice 2 |
| C22.1 | Zero-question readable note with inclusion selected still exports note context. | slice 2 | observed: zeroQuestionsStillExportsOrdinaryContext |
| C22.2 | Guest with inclusion selected still exports note context with no personal selections. | slice 3 | observed: guestIncludeRecentQuestionsGetsKeysWithoutPersonalSelections |
| C22.3 | No new login prerequisite is introduced. | slice 1 | observed: NoteExportControllerAiContextMarkdownTests.java#bazaarGuestGetsOrdinaryExportViaHttp currentUser=null |
| C23 | An ordinary preview is loaded → select inclusion → copy/download wait for refreshed text; the refreshed preview and both outputs match. Clear inclusion → the ordinary context returns. If refresh fails, show the existing API error feedback and make the previous text unavailable for export; Refresh retries. | slice 4 |  |
| C23.1 | Select inclusion → copy/download wait for refreshed text; refreshed preview and both outputs match. | slice 4 |  |
| C23.2 | Clear inclusion → the ordinary context returns. | slice 4 |  |
| C23.3 | If refresh fails → existing API error feedback; previous text unavailable for export; Refresh retries. | slice 4 |  |
| C24 | Refinement (2026-11-10): developer accepted newest created note questions with the learner’s latest own selection. | slice 3 | observed: NoteExportControllerLearnerSelectionsTests#threeQuestionsOneAnsweredShowsOwnSelectionSeparateFromDonutKey (selection half; newest half slice 2) |
| C25 | Refinement (2026-11-10): developer accepted Donut’s answer key alongside that selection. | slice 2 | observed: canonical shape Donut answer key |
| C26 | Refinement (2026-11-10): The initial checkbox, ten-question bound, unanswered-question inclusion, and export-only goal remain. | slice 4 |  |
| C26.1 | Initial (unchecked) checkbox remains. | slice 4 |  |
| C26.2 | Ten-question bound remains. | slice 2 | observed: newest-ten bound in repository + test |
| C26.3 | Unanswered-question inclusion remains. | slice 2 | observed: questionsWithoutPromptsAreIncluded / unanswered remain |
| C26.4 | Export-only goal remains. | slice 4 |  |
| C27 | Keep the existing Export Note Data task and its shared preview/copy/download text. | slice 1 | observed: frontend/src/components/notes/core/NoteExportForm.vue Export Note Data + JsonExportSection |
| C28 | Changing the inclusion checkbox refreshes the preview, with copy/download unavailable while that refresh is pending, so the output corresponds to the selected option. | slice 4 |  |
| C29 | Keep the existing token budget's meaning (focus and related note bodies); complete selected questions form an additional appendix. Explain that relationship beside the option. | slice 4 |  |
| C29.1 | Keep existing token budget meaning (focus and related note bodies). | slice 4 |  |
| C29.2 | Complete selected questions form an additional appendix. | slice 2 | observed: RecentNoteQuestionsMarkdownAppendix complete appendix |
| C29.3 | Explain that relationship beside the option. | slice 4 |  |
| C30 | A zero-question note can still export its ordinary context. | slice 2 | observed: zeroQuestionsStillExportsOrdinaryContext |
| C31 | A failed refresh cannot offer previous text as the current selection's output; retain the existing Refresh retry and shared API feedback. | slice 4 |  |
| C31.1 | Failed refresh cannot offer previous text as the current selection’s output. | slice 4 |  |
| C31.2 | Retain existing Refresh retry and shared API feedback. | slice 4 |  |
| C32 | No layout is prescribed. | slice 4 |  |
| C33 | Compose this user-requested export at its existing read boundary. | slice 1 | observed: NoteExportController.getAiContextMarkdown assertReadAuthorization |
| C34 | ADR 0001 distinguishes reusable MCQ content from learner-specific recall prompts and answers. | slice 3 | observed: RecallPrompt/Answer via MemoryTracker.user scoped lookup; Mcq content separate |
| C35 | ADR 0002 keeps learning history separate from authored Portable content. | slice 1 | observed: NoteExportController returns NoteAiContextMarkdown only; no Portable/Git write |
| C36 | This appendix is a transient export; it does not become authored note content or notebook Git data. | slice 1 | observed: NoteExportController transient markdown DTO; no notebook Git mutation |
| C36.1 | Appendix is a transient export. | slice 1 | observed: NoteExportController appendix ownership at export boundary as transient markdown |
| C36.2 | It does not become authored note content. |  | X5 |
| C36.3 | It does not become notebook Git data. |  | X6 |
| C37 | The shared focus-context renderer also serves other AI consumers, so the export boundary should own this optional composition. | slice 1 | observed: NoteExportController owns export composition; FocusContextMarkdownRenderer remains shared |
| C38 | Readable note access does not authorize another learner's answers. | slice 3 | observed: newerOtherLearnerAttemptDoesNotSubstitute; current-user scoped findAnsweredChoiceIndexesForUserAndMcqs |
| C39 | No conflict with these Accepted decisions was found; no new ADR or exception is proposed. | slice 1 | observed: no new ADR or exception in this change |

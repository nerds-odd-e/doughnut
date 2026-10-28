# Independent source review

Launch: fresh general-purpose reviewer `/root/source_review`, `fork_turns: none`,
on 2026-10-28. The launch supplied only the original story Goal, Scope, key
examples, exclusions, rejection constraints, safe stopping point and explicit
2026-10-28 owner decision from
`SEED-090-commissioned-learning-via-mcp.md#story-1`. It instructed the reviewer
to read no project files, change nothing, and return numbered individual
commitments without slice ownership or acceptance. The actual launch message
and returned review are retained in this session's `/root/source_review`
conversation. The following source text preserves that return, with its
qualifications alongside each commitment. X rows are exclusions, not additional
product promises. E rows retain journey setup and expected observations; they
are examples, not rejection rules limiting the product to Japanese, three
items, or this completion sequence.

The stopping boundary is one requested session taught and its completed items
recorded, including an early finish. Allocation below maps each trigger and
expected result to the slice that owns its observation. A later owner does
not accept an earlier output that already owes the promise.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| C01 | A Donut learner connected through the ChatGPT plugin can ask ChatGPT to conduct one commissioned Learning Session. Triggered by the learner’s request in the connected conversation. | slice 4 | G4 |
| C02 | Obtain a Potential learning session available to that learner. Select the first in Donut’s returned order. | slice 2 | observed: LearningSessionRequestMcpEndpointTest.selectsTheFirstNotebookInDueOrder; first tracker uses existing due order |
| C03 | Load that session’s Learning Session Request through MCP. Use the existing Request. | slice 2 | observed: LearningSessionRequestMcpEndpointTest.returnsTheNotebookInstructionsAndSessionItemsInTheExistingMarkdownProtocol; real tools/call returns Request markdown |
| C04 | Obtain the Request’s notebook-specific instructions. Part of the Request. | slice 2 | observed: LearningSessionRequestMcpEndpointTest.returnsTheNotebookInstructionsAndSessionItemsInTheExistingMarkdownProtocol; README instruction appears in returned markdown |
| C05 | Obtain the Request’s Session Items. Part of the Request. | slice 2 | observed: LearningSessionRequestMcpEndpointTest.returnsTheNotebookInstructionsAndSessionItemsInTheExistingMarkdownProtocol; title/content and Report format returned |
| C06 | Tutor interactively from that Request. Its instructions govern the tutoring. | slice 4 | G4 |
| C07 | Submit a Learning Session Report through MCP. Triggered when the session finishes or the learner asks to end it. These are alternative triggers. | slice 4 | G4 |
| C08 | Save Feedback for completed Session Items. Applies to an ordinary finish and an early finish. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A; A grade/text/history/schedule, B/C no logs and full unchanged state; tool guidance completed-only on normal or early finish (interactive tutoring owned by slice 4) |
| C09 | Include completed Session Items only in the submitted Report. Incomplete items are excluded. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A; A grade/text/history/schedule, B/C no logs and full unchanged state; tool guidance completed-only on normal or early finish (interactive tutoring owned by slice 4) |
| C10 | Use Grade 1–4. Preserve the existing commissioned learning protocol. | slice 3 | observed: LearningSessionRecordTutorFeedbackRecallLogTests.matchedGradeLeavesMappedRecallLog selected 1 AGAIN, 2 HARD, 3 GOOD, 4 EASY; MCP grade3 and OAuth grade4 also observed |
| C11 | Use descriptive Feedback. Preserve the existing commissioned learning protocol. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A; A grade/text/history/schedule, B/C no logs and full unchanged state; tool guidance completed-only on normal or early finish (interactive tutoring owned by slice 4) |
| C12 | Use title matching within the notebook. Preserve the existing matching rules. | slice 3 | observed: LearningSessionRecordMcpEndpointTest rejectedTitlesAndInvalidGradesAreReturnedWithoutChangingLearningState and matched A case; real existing parser/record service, rejections returned, no logs/state changes for rejected entries; existing HTTP record tests pass |
| C13 | Use existing Feedback recording behavior. Reuse the existing record domain behavior. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A; A grade/text/history/schedule, B/C no logs and full unchanged state; tool guidance completed-only on normal or early finish (interactive tutoring owned by slice 4) |
| C14 | Use existing scheduling behavior. Reuse the existing record domain behavior. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A; A grade/text/history/schedule, B/C no logs and full unchanged state; tool guidance completed-only on normal or early finish (interactive tutoring owned by slice 4) |
| C15 | Give incomplete items no Feedback. Applies when the learner ends early as well. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A; A grade/text/history/schedule, B/C no logs and full unchanged state; tool guidance completed-only on normal or early finish (interactive tutoring owned by slice 4) |
| C16 | Preserve incomplete items’ current learning state. The early-end example explicitly includes their existing schedules. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A; A grade/text/history/schedule, B/C no logs and full unchanged state; tool guidance completed-only on normal or early finish (interactive tutoring owned by slice 4) |
| C17 | Tell the learner that no session is available. Triggered when Donut supplies no Potential learning session. | slice 2 | observed: LearningSessionRequestMcpEndpointTest.explainsWhenTheLearnerHasNoPotentialSession; explicit no-session signal and tool instruction to explain none |
| C18 | Reuse existing Request domain behavior. No replacement protocol is commissioned. | slice 2 | observed: RequestLearningSessionTool delegates existing LearningSessionService.request and markdown builder |
| C19 | Expand hosted-assistant OAuth with a narrow grant for recording Learning Session Reports. Confirmed by the owner decision dated 2026-10-28. | slice 1 | observed: dual-scope discovery in AssistantDiscoveryTest; AssistantMcpAccessTest.reportCapableAssistantCanReadWithItsConsentedScopes exchanges and uses both grants; full backend pass |
| C20 | Expand hosted-assistant consent with that narrow recording grant. Confirmed by the same owner decision. | slice 1 | G1 |
| C21 | Keep the recording grant distinct from general note editing. Its purpose is Learning Session Report recording. | slice 1 | observed: AssistantResourcePolicy admits exact read or read+record sets; existing general API refusal observations remain green in AssistantMcpAccessTest |
| C22 | Keep the recording grant distinct from `notes:write`. Its purpose is Learning Session Report recording. | slice 1 | observed: AssistantConsentTest.authorizationRefusesUnsupportedResourceScopeAndPkce rejects notes:write and read+write requests |
| C23 | Enable the ChatGPT plugin path to save Feedback using the recording grant. Through the commissioned learning flow. | slice 4 | G4 |
| C24 | Refuse Report recording under `notes:read` alone. The read grant is insufficient. | slice 3 | observed: AssistantLearningSessionTest.readOnlyAssistantCannotRecordEvenWithItsBrowserSession: actual consent/exchange token refused with no logs or schedule change |
| C25 | Make recorded Feedback visible in Donut’s learning history. Explicit in the Japanese-session example. | slice 4 | G4 |
| C26 | Record no Feedback when the learner ends before completing any item. Explicit zero-completion case. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.endingBeforeAnyCompletionRecordsNothing: no recorded items/logs and same TrackerState |
| R01 | Preserve existing notebook authorization. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.preservesForeignNotebookAuthorization and retained Request foreign notebook refusal; same AuthorizationService as HTTP |
| R02 | Preserve existing Report matching rules. | slice 3 | observed: LearningSessionRecordMcpEndpointTest rejectedTitlesAndInvalidGradesAreReturnedWithoutChangingLearningState and matched A case; real existing parser/record service, rejections returned, no logs/state changes for rejected entries; existing HTTP record tests pass |
| R03 | Preserve existing Report recording rules. | slice 3 | observed: LearningSessionRecordMcpEndpointTest rejectedTitlesAndInvalidGradesAreReturnedWithoutChangingLearningState and matched A case; real existing parser/record service, rejections returned, no logs/state changes for rejected entries; existing HTTP record tests pass |
| R04 | Reject unmatched report entries under those existing rules. | slice 3 | observed: LearningSessionRecordMcpEndpointTest rejectedTitlesAndInvalidGradesAreReturnedWithoutChangingLearningState and matched A case; real existing parser/record service, rejections returned, no logs/state changes for rejected entries; existing HTTP record tests pass |
| R05 | Report unmatched report entries under those existing rules. | slice 3 | observed: LearningSessionRecordMcpEndpointTest rejectedTitlesAndInvalidGradesAreReturnedWithoutChangingLearningState and matched A case; real existing parser/record service, rejections returned, no logs/state changes for rejected entries; existing HTTP record tests pass |
| R06 | Reject invalid grades under those existing rules. | slice 3 | observed: LearningSessionRecordMcpEndpointTest rejectedTitlesAndInvalidGradesAreReturnedWithoutChangingLearningState and matched A case; real existing parser/record service, rejections returned, no logs/state changes for rejected entries; existing HTTP record tests pass |
| R07 | Report invalid grades under those existing rules. | slice 3 | observed: LearningSessionRecordMcpEndpointTest rejectedTitlesAndInvalidGradesAreReturnedWithoutChangingLearningState and matched A case; real existing parser/record service, rejections returned, no logs/state changes for rejected entries; existing HTTP record tests pass |
| R08 | Leave unmatched Session Items unchanged. | slice 3 | observed: LearningSessionRecordMcpEndpointTest rejectedTitlesAndInvalidGradesAreReturnedWithoutChangingLearningState and matched A case; real existing parser/record service, rejections returned, no logs/state changes for rejected entries; existing HTTP record tests pass |
| E01 | Japanese is the only available Potential learning session. | slice 4 | G4 |
| E02 | The learner asks ChatGPT to start learning from Donut. | slice 4 | G4 |
| E03 | ChatGPT retrieves the Japanese session’s Request through MCP. | slice 4 | G4 |
| E04 | ChatGPT follows its tutoring instructions. | slice 4 | G4 |
| E05 | ChatGPT teaches interactively. | slice 4 | G4 |
| E06 | ChatGPT submits Feedback for completed items. | slice 4 | G4 |
| E07 | The learner sees that Feedback in Donut’s learning history. | slice 4 | G4 |
| E08 | When Donut supplies multiple Potential learning sessions, ChatGPT uses the first in the returned order. | slice 2 | observed: LearningSessionRequestMcpEndpointTest.selectsTheFirstNotebookInDueOrder; reversed creation order selects earliest due notebook |
| E09 | ChatGPT tutors from that selected session’s Request. | slice 4 | G4 |
| E10 | In the early-end example, the Request contains item A. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: actual Request markdown contains A, B and C before partial Report |
| E11 | It also contains item B. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: actual Request markdown contains A, B and C before partial Report |
| E12 | It also contains item C. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: actual Request markdown contains A, B and C before partial Report |
| E13 | The learner completes A. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A; A grade/text/history/schedule, B/C no logs and full unchanged state; tool guidance completed-only on normal or early finish (interactive tutoring owned by slice 4) |
| E14 | The learner ends before completing B. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A; A grade/text/history/schedule, B/C no logs and full unchanged state; tool guidance completed-only on normal or early finish (interactive tutoring owned by slice 4) |
| E15 | The learner ends before starting C. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A; A grade/text/history/schedule, B/C no logs and full unchanged state; tool guidance completed-only on normal or early finish (interactive tutoring owned by slice 4) |
| E16 | ChatGPT submits Feedback only for A. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A; A grade/text/history/schedule, B/C no logs and full unchanged state; tool guidance completed-only on normal or early finish (interactive tutoring owned by slice 4) |
| E17 | B remains unscored. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A; A grade/text/history/schedule, B/C no logs and full unchanged state; tool guidance completed-only on normal or early finish (interactive tutoring owned by slice 4) |
| E18 | B retains its existing schedule. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A; A grade/text/history/schedule, B/C no logs and full unchanged state; tool guidance completed-only on normal or early finish (interactive tutoring owned by slice 4) |
| E19 | C remains unscored. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A; A grade/text/history/schedule, B/C no logs and full unchanged state; tool guidance completed-only on normal or early finish (interactive tutoring owned by slice 4) |
| E20 | C retains its existing schedule. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A; A grade/text/history/schedule, B/C no logs and full unchanged state; tool guidance completed-only on normal or early finish (interactive tutoring owned by slice 4) |
| E21 | Ending with zero completed items records no Feedback. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.endingBeforeAnyCompletionRecordsNothing: no recorded items/logs and same TrackerState |
| E22 | When Donut supplies no Potential learning session, ChatGPT explains that none is available. | slice 4 | G4 |
| X01 | Inventing a second commissioned learning protocol. | slice 3 | observed: RecordLearningSessionTool passes existing markdown into LearningSessionService.record and returns existing recorded/rejected result; no domain implementation added |
| X02 | Introducing a program-picker. | slice 2 | observed: RequestLearningSessionTool accepts timezone and selects first notebook through existing service; no picker introduced |
| X03 | Choosing among programs when several are available. | slice 2 | observed: same first-notebook selection; program choice remains deferred by source |
| X04 | Choosing among notebooks when several are available. | slice 2 | observed: same first-notebook selection; notebook choice remains deferred by source |
| X05 | Automatically starting a further session. | slice 3 | observed: one requested notebook Report is recorded; Request/Record tool guidance says do not automatically start another session; multi-session workflow deferred |
| X06 | Replacing the markdown Request protocol with MCP-only structured schemas. | slice 2 | observed: existing Request markdown returned intact as requestMarkdown, observed by MCP endpoint test |
| X07 | Replacing the markdown Report protocol with MCP-only structured schemas. | slice 3 | observed: RecordLearningSessionTool passes existing markdown into LearningSessionService.record and returns existing recorded/rejected result; no domain implementation added |
| X08 | General note editing for assistants. | slice 1 | observed: scope expansion is confined to learning_session:record; general API refusal in AssistantMcpAccessTest and read-only MCP tool implementations unchanged |
| X09 | A broad `notes:write` grant for assistants. | slice 1 | observed: unsupported notes:write authorization rejection remains in AssistantConsentTest and AssistantMcpAccessTest |
| X10 | Further selection workflows. | slice 2 | observed: tool returns a single selected notebook Request; further selection deferred by source |
| X11 | Multi-session workflows. | slice 3 | observed: one requested notebook Report is recorded; Request/Record tool guidance says do not automatically start another session; multi-session workflow deferred |

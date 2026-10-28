# Independent source review after owner amendment

Fresh reviewer `/root/amended_source_review`, `fork_turns: none`, launched
2026-10-28 with only amended Goal/Scope/examples/exclusions and explicit owner
instruction to move former slice 4 to a new top-priority story and wrap up the
current execution. It was told to read no files/change nothing and returned
numbered individual source statements without allocation or acceptance. Actual
launch/return are retained in that agent conversation. This table preserves
its returned wording. The prior review remains in
[source-review-before-owner-amendment.md](source-review-before-owner-amendment.md)
for original intent/proof provenance until normal cleanup. Examples remain
illustrative and introduce no rejection rules. Human workflow instructions
name subsequent owners; observing their invocation does not claim closure.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | Expose the existing Learning Session Request markdown protocol through MCP. | slice 2 | observed: LearningSessionRequestMcpEndpointTest.returnsTheNotebookInstructionsAndSessionItemsInTheExistingMarkdownProtocol: real MCP output has notebook identity, instructions, items and Report format |
| 2 | Expose the existing Learning Session Report markdown protocol through MCP. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A, A grade/text/schedule recorded, B/C full state/history unchanged; existing service reused |
| 3 | Select the first available Potential learning session in Donut’s returned due order. | slice 2 | observed: LearningSessionRequestMcpEndpointTest.selectsTheFirstNotebookInDueOrder: earliest due notebook wins over creation order |
| 4 | Return the selected notebook’s instructions. | slice 2 | observed: LearningSessionRequestMcpEndpointTest.returnsTheNotebookInstructionsAndSessionItemsInTheExistingMarkdownProtocol: real MCP output has notebook identity, instructions, items and Report format |
| 5 | Return the selected session’s Session Items. | slice 2 | observed: LearningSessionRequestMcpEndpointTest.returnsTheNotebookInstructionsAndSessionItemsInTheExistingMarkdownProtocol: real MCP output has notebook identity, instructions, items and Report format |
| 6 | Record only items marked completed in the Report. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A, A grade/text/schedule recorded, B/C full state/history unchanged; existing service reused |
| 7 | Use existing Feedback recording for completed items. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A, A grade/text/schedule recorded, B/C full state/history unchanged; existing service reused |
| 8 | Use existing scheduling for completed items. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A, A grade/text/schedule recorded, B/C full state/history unchanged; existing service reused |
| 9 | Support a partial Report. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A, A grade/text/schedule recorded, B/C full state/history unchanged; existing service reused |
| 10 | An empty Report records nothing. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.endingBeforeAnyCompletionRecordsNothing: empty recorded items/logs and unchanged state |
| 11 | Preserve incomplete items’ learning state. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A, A grade/text/schedule recorded, B/C full state/history unchanged; existing service reused |
| 12 | Return an explicit no-session outcome when none is available. | slice 2 | observed: LearningSessionRequestMcpEndpointTest.explainsWhenTheLearnerHasNoPotentialSession: explicit no-session output despite another user due tracker |
| 13 | Extend hosted-assistant OAuth with `learning_session:record`. | slice 1 | observed: AssistantDiscoveryTest, AssistantConsentTest exact consent scopes, AssistantMcpAccessTest real code exchange; consent browser FormData repeated fields; full backend/browser pass |
| 14 | Extend consent with that narrow recording grant. | slice 1 | observed: AssistantDiscoveryTest, AssistantConsentTest exact consent scopes, AssistantMcpAccessTest real code exchange; consent browser FormData repeated fields; full backend/browser pass |
| 15 | Support the recording grant alongside `notes:read`. | slice 1 | observed: AssistantDiscoveryTest, AssistantConsentTest exact consent scopes, AssistantMcpAccessTest real code exchange; consent browser FormData repeated fields; full backend/browser pass |
| 16 | Refuse Report recording under `notes:read` alone. | slice 3 | observed: AssistantLearningSessionTest dual-scope success and readOnlyAssistantCannotRecordEvenWithItsBrowserSession refusal before logs/schedule changes |
| 17 | The recording grant confers no general note-editing authority. | slice 1 | observed: AssistantResourcePolicy exact read/read+record sets; AssistantConsentTest notes:write refusal and retained general API refusals |
| 18 | Tutoring instructions direct submission of completed items on normal finish. | slice 3 | observed: RequestLearningSessionTool/RecordLearningSessionTool instructions name normal/early finish, completed-only Report and no automatic further session |
| 19 | Tutoring instructions direct submission of completed items on early finish. | slice 3 | observed: RequestLearningSessionTool/RecordLearningSessionTool instructions name normal/early finish, completed-only Report and no automatic further session |
| 20 | Those instructions do not automatically start another session. | slice 3 | observed: RequestLearningSessionTool/RecordLearningSessionTool instructions name normal/early finish, completed-only Report and no automatic further session |
| 21 | Preserve existing notebook authorization. | slice 3 | observed: Request/Record MCP tests preserve foreign notebook refusal using the same AuthorizationService as HTTP |
| 22 | Preserve existing Report matching rules. | slice 3 | observed: LearningSessionRecordMcpEndpointTest rejectedTitlesAndInvalidGradesAreReturnedWithoutChangingLearningState; unchanged HTTP record tests pass, all Grade 1–4 mappings selected |
| 23 | Preserve existing Report recording rules. | slice 3 | observed: LearningSessionRecordMcpEndpointTest rejectedTitlesAndInvalidGradesAreReturnedWithoutChangingLearningState; unchanged HTTP record tests pass, all Grade 1–4 mappings selected |
| 24 | Preserve existing handling of reported unmatched entries. | slice 3 | observed: LearningSessionRecordMcpEndpointTest rejectedTitlesAndInvalidGradesAreReturnedWithoutChangingLearningState; unchanged HTTP record tests pass, all Grade 1–4 mappings selected |
| 25 | Preserve existing handling of invalid grades. | slice 3 | observed: LearningSessionRecordMcpEndpointTest rejectedTitlesAndInvalidGradesAreReturnedWithoutChangingLearningState; unchanged HTTP record tests pass, all Grade 1–4 mappings selected |
| 26 | One due Japanese notebook → Request identifies that notebook. | slice 2 | observed: LearningSessionRequestMcpEndpointTest.returnsTheNotebookInstructionsAndSessionItemsInTheExistingMarkdownProtocol: real MCP output has notebook identity, instructions, items and Report format |
| 27 | One due Japanese notebook → Request supplies its instructions. | slice 2 | observed: LearningSessionRequestMcpEndpointTest.returnsTheNotebookInstructionsAndSessionItemsInTheExistingMarkdownProtocol: real MCP output has notebook identity, instructions, items and Report format |
| 28 | One due Japanese notebook → Request supplies its Session Items. | slice 2 | observed: LearningSessionRequestMcpEndpointTest.returnsTheNotebookInstructionsAndSessionItemsInTheExistingMarkdownProtocol: real MCP output has notebook identity, instructions, items and Report format |
| 29 | One due Japanese notebook → Request supplies the Report format. | slice 2 | observed: LearningSessionRequestMcpEndpointTest.returnsTheNotebookInstructionsAndSessionItemsInTheExistingMarkdownProtocol: real MCP output has notebook identity, instructions, items and Report format |
| 30 | Several due notebooks → MCP selects the first in Donut’s returned due order. | slice 2 | observed: LearningSessionRequestMcpEndpointTest.selectsTheFirstNotebookInDueOrder: earliest due notebook wins over creation order |
| 31 | Request contains A/B/C; Report completes A → record A’s Grade. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A, A grade/text/schedule recorded, B/C full state/history unchanged; existing service reused |
| 32 | Request contains A/B/C; Report completes A → record A’s descriptive Feedback. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A, A grade/text/schedule recorded, B/C full state/history unchanged; existing service reused |
| 33 | Request contains A/B/C; Report completes A → B/C retain their histories. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A, A grade/text/schedule recorded, B/C full state/history unchanged; existing service reused |
| 34 | Request contains A/B/C; Report completes A → B/C retain their schedules. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC: Request A/B/C then Report A, A grade/text/schedule recorded, B/C full state/history unchanged; existing service reused |
| 35 | Empty Report → no recording. | slice 3 | observed: LearningSessionRecordMcpEndpointTest.endingBeforeAnyCompletionRecordsNothing: empty recorded items/logs and unchanged state |
| 36 | Nothing available → explicit no-session outcome. | slice 2 | observed: LearningSessionRequestMcpEndpointTest.explainsWhenTheLearnerHasNoPotentialSession: explicit no-session output despite another user due tracker |
| 37 | Real OAuth consent/code exchange → both `notes:read` and `learning_session:record` are granted. | slice 1 | observed: AssistantDiscoveryTest, AssistantConsentTest exact consent scopes, AssistantMcpAccessTest real code exchange; consent browser FormData repeated fields; full backend/browser pass |
| 38 | Assistant attempts recording → recording requires `learning_session:record`. | slice 3 | observed: AssistantLearningSessionTest dual-scope success and readOnlyAssistantCannotRecordEvenWithItsBrowserSession refusal before logs/schedule changes |
| 39 | Real ChatGPT interoperability belongs to the separate follow-up identified as `SEED-090#story-2`. | slice 3 | G4 |
| 40 | Interactive tutoring belongs to that separate follow-up. | slice 3 | G4 |
| 41 | Visible history verification belongs to that separate follow-up. | slice 3 | G4 |
| 42 | Choosing among programs is deferred. | slice 2 | observed: Request tool chooses first existing due notebook and exposes no picker; source excludes program/notebook choice |
| 43 | Choosing among notebooks is deferred. | slice 2 | observed: Request tool chooses first existing due notebook and exposes no picker; source excludes program/notebook choice |
| 44 | Automatically starting another session is deferred. | slice 3 | observed: RequestLearningSessionTool/RecordLearningSessionTool instructions name normal/early finish, completed-only Report and no automatic further session |
| 45 | Replacing markdown with MCP-only schemas is deferred. | slice 3 | observed: Request/Record tools pass existing markdown protocol to LearningSessionService; unchanged parser/service tests pass |
| 46 | General note editing is deferred. | slice 1 | observed: AssistantResourcePolicy exact read/read+record sets; AssistantConsentTest notes:write refusal and retained general API refusals |
| 47 | `notes:write` is deferred. | slice 1 | observed: AssistantResourcePolicy exact read/read+record sets; AssistantConsentTest notes:write refusal and retained general API refusals |
| 48 | The current safe stop covers implemented and verified MCP Request/Report boundaries. | slice 3 | observed: three delivered slices and accepted 2981-test backend proof in execution-evidence.md; final MCP implementation candidate b261a1e |
| 49 | The current safe stop covers implemented and verified OAuth boundaries. | slice 1 | observed: real consent/code exchange/renewal/withdrawal observations and SDK E2E lifecycle in accepted evidence |
| 50 | Live ChatGPT verification remains separate. | slice 3 | G4 |
| 51 | Former execution slice 4 becomes a new first-priority backlog story. | slice 3 | observed: PRODUCT-BACKLOG.md first queued item is SEED-090#story-2; canonical home records former probe scope |
| 52 | The owner will perform the probe directly in ChatGPT. | slice 3 | observed: explicit owner instruction and story-2 Owner / operation name Terry as direct ChatGPT probe operator |
| 53 | Finish execution of the current MCP capability story now. | slice 3 | observed: owner-authorized current scope has three done slices with accepted proof; execution completion record follows retrospective |
| 54 | Wrap up the current MCP capability story now. | slice 3 | observed: explicit user invocation requires Story Wrap Up after execution completion/CI gates; that workflow owns subsequent closure, no premature closure claim |
| 55 | The amendment changes the current story’s proof boundary. | slice 3 | G4 |
| 56 | The amendment does not claim that live ChatGPT behavior was observed. | slice 3 | G4 |

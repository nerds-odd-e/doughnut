# Independent source review

The coordinator launched `/root/source_review` without conversation inheritance.
Its input contained the canonical Goal, Scope, four key examples and Decision
from the linked seed, with formatting flattened. Its brief required independent
observable promises, barred repository reads and edits, and supplied no plan
or implementation. The table retains its numbered return. For promises about
web or CLI first presentation, the owning client slice combines its observation
with the controller proof in slice 2 and, for late import, slice 4.

The ordinary correction example uses a stored question about an observatory that
opens at 9. The author changes the note to 10 before opening web or CLI recall,
and the question is made from the note that says 10.

The owner selected exact focus-content comparison at first presentation.
Recording the inputs allows on-demand renewal, including replacement of a sound
MCQ after a typo. This avoids asking a model to judge the typo’s effect or
requiring learners to discover and clear obsolete questions. Linked-note edits
and judging otherwise poor questions are deferred.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| C1 | Web recall reuses a waiting MCQ when the authored title, body and properties of the focus note exactly match its generation input, including an old question with known provenance. | slice 5 | |
| C2 | CLI recall reuses a waiting MCQ under the same exact-match condition, including an old question with known provenance. | slice 7 | |
| C3 | On first presentation in web recall, a difference in the authored title causes the learner to see a question generated from the current focus note. | slice 5 | |
| C4 | On first presentation in CLI recall, a difference in the authored title causes the learner to see a question generated from the current focus note. | slice 7 | |
| C5 | On first presentation in web recall, a difference in the authored body causes the learner to see a question generated from the current focus note. | slice 5 | |
| C6 | On first presentation in CLI recall, a difference in the authored body causes the learner to see a question generated from the current focus note. | slice 7 | |
| C7 | On first presentation in web recall, a difference in the authored properties causes the learner to see a question generated from the current focus note. | slice 5 | |
| C8 | On first presentation in CLI recall, a difference in the authored properties causes the learner to see a question generated from the current focus note. | slice 7 | |
| C9 | A punctuation-only edit renews the question on its next web presentation even when the observatory still opens at 9. | slice 5 | |
| C10 | A punctuation-only edit renews the question on its next CLI presentation even when the observatory still opens at 9. | slice 7 | |
| C11 | A legacy question with unknown source content is renewed when it comes up in web recall. | slice 5 | |
| C12 | A legacy question with unknown source content is renewed when it comes up in CLI recall. | slice 7 | |
| C13 | When a batch requested before the observatory edit finishes importing afterward, web recall first presents a question from the note that says 10. | slice 5 | |
| C14 | When a batch requested before the observatory edit finishes importing afterward, CLI recall first presents a question from the note that says 10. | slice 7 | |
| C15 | When web recall prefetched the observatory card behind another card and the correction to 10 is accepted before the learner reaches it, the learner first sees the current question. | slice 5 | |
| C16 | The web learner can answer the MCQ presented from the corrected note. | slice 5 | |
| C17 | The CLI learner can answer the MCQ presented from the corrected note. | slice 7 | |
| C18 | Renewal keeps the learner’s tracker. | slice 2 | observed: MemoryTrackerRecallPromptFreshnessControllerTest.failedRenewalAndRetryPreservePriorRecallState checks the reloaded prompt is on the original tracker. gradesTheIssuedPromptAgainstItsOriginalChoicesAfterRenewal checks that tracker on the issued prompt's answer. |
| C19 | Renewal keeps the learner’s earlier answers. | slice 2 | observed: MemoryTrackerRecallPromptFreshnessControllerTest.failedRenewalAndRetryPreservePriorRecallState reads the earlier answer ID, choice and original choices after renewal. |
| C20 | Renewal keeps the learner’s schedule. | slice 2 | observed: MemoryTrackerRecallPromptFreshnessControllerTest.failedRenewalAndRetryPreservePriorRecallState checks the exact due timestamp and single prior log ID after a failed load and again after successful retry. |
| C21 | Saving the corrected note requires no learner action for renewal. | slice 2 | observed: MemoryTrackerRecallPromptFreshnessControllerTest.replacesWaitingQuestionAfterAnAcceptedMarkdownEdit and replacesWaitingQuestionAfterAnAcceptedTitleEdit observe an accepted revision advance while the model call count remains one. |
| C22 | The learner waits for renewal only when recall needs a current question. | slice 2 | observed: MemoryTrackerRecallPromptFreshnessControllerTest.replacesWaitingQuestionAfterAnAcceptedMarkdownEdit and replacesWaitingQuestionAfterAnAcceptedTitleEdit observe two calls for distinct authored sources and reuse the renewed ID on an exact match. MemoryTrackerRecallPromptControllerTest.shouldRecycleMostRecentUnansweredMcqPrompt proves old matching reuse. |
| C23 | A focus-note edit while the learner is reading a displayed web question leaves its displayed choices intact. | slice 5 | |
| C24 | A focus-note edit while the learner is reading a displayed CLI question leaves its displayed choices intact. | slice 7 | |
| C25 | After a focus-note edit while the web learner is reading, submission is graded against the original key for the prompt they saw. | slice 5 | |
| C26 | After a focus-note edit while the CLI learner is reading, submission is graded against the original key for the prompt they saw. | slice 7 | |
| C27 | After renewal fails, the web learner can retry. | slice 5 | |
| C28 | After renewal fails, the web learner can choose Self-review. | slice 5 | |
| C29 | After renewal fails, the CLI offers just review. | slice 7 | |
| C30 | The tracker stays due while web renewal is loading. | slice 5 | |
| C31 | The tracker stays due while CLI renewal is loading. | slice 7 | |
| C32 | Choosing Retry on the web leaves the tracker due until the learner actually answers or grades it. | slice 5 | |
| C33 | Choosing Self-review on the web leaves the tracker due until the learner actually answers or grades it. | slice 5 | |
| C34 | Choosing review in the CLI leaves the tracker due until the learner actually answers or grades it. | slice 7 | |

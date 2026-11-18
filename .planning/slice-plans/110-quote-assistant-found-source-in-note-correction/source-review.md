# Source review

Launch: 2026-11-18, a fresh general-purpose reviewer without conversation
inheritance. Input: only the selected story's Goal, Why, Owner decisions,
Required behavior, Not promised now, Out of scope, Key examples and Effort
hypothesis from
[the seed section](../../seeds/SEED-073-learning-refinement-and-retrieval.md#assistant-quoted-source-in-corrections),
with the instruction to read no project file and change nothing. The
reviewer returned ids 1 to 7; ids 2, 3, 4, 6 and 7 were returned as
headings split into the sub-ids below, which carry their full text.

Unnumbered context returned: the goal's outcome and wider aim; Why; the
paid-try authorization; "offered as today" as the settled baseline; the
five "Not promised now" deferrals; "Out of scope: storing the source in the
note"; the effort hypothesis. Reading notes: promise 1 has no key example;
4b, 4c and 6b rest on Required behavior text alone; "one passage" is a
qualification, not a rule rejecting more sources; key example 5 spans 3
and 6a.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | "When the assistant proposes a content correction it first searches the web for a source for the corrected claim." Content corrections only; the search comes first; the author does not supply the source. (Goal; Required behavior 1; Why) | slice 2 | |
| 2a | The correction carries one passage copied word for word from the source, as its own part, when a source was found. (Required behavior 1; Owner decisions; Goal) | slice 1 | observed: backend/src/test/java/com/odde/donut/controllers/ConversationAiReplyControllerTest.java keepsTheQuotedWebSourceOfAContentCorrection row 1: getSourceQuote equals the passage given in the tool call |
| 2b | The correction carries the address of the page, as its own part, when a source was found. (Required behavior 1; Owner decisions) | slice 1 | observed: backend/src/test/java/com/odde/donut/controllers/ConversationAiReplyControllerTest.java keepsTheQuotedWebSourceOfAContentCorrection row 1: getSourceUrl equals the https address given in the tool call |
| 3a | The quote is shown with the correction, apart from the reason, to an author deciding on a content correction with a source. (Goal; Required behavior 2; Key examples 1, 3) | slice 3 | |
| 3b | A link to the source is shown with the correction, apart from the reason, and it opens the source. (Goal; Required behavior 2; Key examples 1, 3, 5) | slice 3 | |
| 4a | After the correction is accepted, the quote and link are still shown with it, also when the conversation is reopened later. (Required behavior 2; Key example 6) | slice 3 | |
| 4b | After the correction is declined, the quote and link are still shown with it. (Required behavior 2) | slice 3 | |
| 4c | A reader who cannot accept sees the quote and link with the correction. (Required behavior 2) | slice 3 | |
| 5 | "The link opens the source at the quoted passage and the browser marks it, where the browser and the page allow." (Owner decisions; Required behavior 3; Key example 2) | slice 3 | |
| 6a | For a PDF source, the link opens the PDF at its start; quote and link are shown. (Required behavior 3; Key example 5) | slice 3 | |
| 6b | For a page where the quoted words cannot be found, the link opens the page at its start. (Required behavior 3) | slice 3 | |
| 7a | When the assistant found no source, the correction is offered as today. (Owner decisions; Required behavior 4; Key example 4) | slice 1 | observed: backend/src/test/java/com/odde/donut/controllers/ConversationAiReplyControllerTest.java keepsTheQuotedWebSourceOfAContentCorrection row 2: a content tool call without a source still returns its correction, with no source; the existing correction tests in that class stay green |
| 7b | When the assistant found no source, the review says that no source was found. (Owner decisions; Required behavior 4; Key example 4) | slice 3 | |

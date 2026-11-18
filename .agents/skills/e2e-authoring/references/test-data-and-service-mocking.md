## E2E test data and service mocks

Donut's notebook Given accepts a Cucumber data table and injects the notebook
in one batch. For example:

```gherkin
Given I have a notebook "Geometry set" with notes:
  | Title  | Folder |
  | Square | Shapes |
```

The optional `Folder` column supplies a notebook-local path. Separate nested
folder segments with `/`. Testability creates the folder path and places the
note there.

For scenarios that need an OpenAI substitute, use `@usingMockedOpenAiService`
and the Responses helpers. Conversation replies can use the
`OpenAI will reply below for user messages:` Given in
[ai.ts](../../../../e2e_test/step_definitions/ai.ts), which passes the
scenario's reply table to `stubConversationAiReplyStream`. The
[Cucumber hooks](../../../../e2e_test/step_definitions/hook.ts) set up and
restore the tagged mock.

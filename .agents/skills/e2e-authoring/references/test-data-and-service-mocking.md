## Test Data And Service Mocking

- Use Given steps to set up test data.
- Mock external services, such as OpenAI, for reliable tests.
- Use tags to indicate when mocks are required.
- Keep mock responses consistent with real service behavior.
- Store mock data separately from test code.
- Clean up test data after each test.
- Use data tables for complex test data.

```gherkin
Given I have a notebook "Geometry set" with notes:
  | Title  |
  | Shape  |
  | Square |
```

Square is placed under Shape when the notebook is created in one inject batch. Do not add a `Folder` column; see testability inject ordering.

```typescript
Given("OpenAI assistant will reply below for user messages in a stream run:", (data: DataTable) => {
  mock_services
    .openAi()
    .stubCreateThread("thread-123")
    .createThreadAndStubMessages("thread-123", data.hashes())
})
```


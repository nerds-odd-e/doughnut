## Switch from Assistant to Chat Completion for Question Evaluation

### Background
Currently, the question evaluation is using OpenAI Assistant API. We want to switch to using chat completion API with JSON schema validation for better control and potentially better performance.

### Requirements
1. Use `requestAndGetJsonSchemaResult` instead of assistant API for question evaluation
2. Preserve notebook-specific AI instructions in the chat completion
3. Keep the same functionality of question evaluation
4. Maintain the same input/output interface for question evaluation

### Technical Notes
- The notebook-specific instructions are stored in `NotebookAiAssistant` entity
- We need to incorporate these instructions into the chat completion system message
- We'll use the existing JSON schema validation mechanism
- The evaluation should still return `Optional<QuestionEvaluation>`

### Impact
- Only affects the question evaluation part
- No change to the question generation part (which still uses assistant)
- No change to the API interface 

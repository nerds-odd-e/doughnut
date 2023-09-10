package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AiCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class OpenAIChatAboutNoteRequestBuilder {
  String model = "gpt-3.5-turbo-16k";
  private List<ChatMessage> messages = new ArrayList<>();
  private List<ChatFunction> functions = new ArrayList<>();
  private int maxTokens;

  public OpenAIChatAboutNoteRequestBuilder() {
    addMessage(
        ChatMessageRole.SYSTEM,
        "This is a PKM system using hierarchical notes, each with a topic and details, to capture atomic concepts.");
  }

  public OpenAIChatAboutNoteRequestBuilder contentOfNoteOfCurrentFocus(Note note) {
    String noteOfCurrentFocus =
        """
The note of current focus:
Context path: %s
Topic: %s
%s
      """
            .formatted(note.getContextPathString(), note.getTopic(), getDetails(note));
    return addMessage(ChatMessageRole.SYSTEM, noteOfCurrentFocus);
  }

  private static String getDetails(Note note) {
    if (note.isDetailsBlankHtml()) return "";
    return "Details (until the end of this message):\n%s".formatted(note.getDetails());
  }

  public OpenAIChatAboutNoteRequestBuilder userInstructionToGenerateQuestionWithFunctionCall() {
    functions.add(
        ChatFunction.builder()
            .name("ask_single_answer_multiple_choice_question")
            .description("Ask a single-answer multiple-choice question to the user")
            .executor(AIGeneratedQuestion.class, null)
            .build());

    String messageBody =
        """
  Please assume the role of a Memory Assistant, which involves helping me review, recall, and reinforce information from my notes. As a Memory Assistant, focus on creating exercises that stimulate memory and comprehension. Please adhere to the following guidelines:

  1. Generate a multiple-choice question based on the note in the current context path
  2. Only the top-level of the context path is visible to the user.
  3. Provide 2 to 4 choices with only 1 correct answer.
  4. Vary the lengths of the choice texts so that the correct answer isn't consistently the longest.
  5. If there's insufficient information in the note to create a question, leave the 'stem' field empty.

  Note: The specific note of focus and its more detailed contexts are not known. Focus on memory reinforcement and recall across various subjects.
  """;
    return addMessage(ChatMessageRole.USER, messageBody);
  }

  public OpenAIChatAboutNoteRequestBuilder instructionForCompletion(
      String contextPath, AiCompletionRequest aiCompletionRequest) {
    addMessage(
        ChatMessageRole.SYSTEM,
        "Please behave like a text completion service and keep the content concise.\nCurrent context path of the note: %s"
            .formatted(contextPath));
    addMessage(ChatMessageRole.USER, aiCompletionRequest.prompt);
    if (!Strings.isEmpty(aiCompletionRequest.incompleteContent)) {
      addMessage(ChatMessageRole.ASSISTANT, aiCompletionRequest.incompleteContent);
    }
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder maxTokens(int maxTokens) {
    this.maxTokens = maxTokens;
    return this;
  }

  public ChatCompletionRequest build() {
    ChatCompletionRequest.ChatCompletionRequestBuilder requestBuilder =
        ChatCompletionRequest.builder()
            .model(model)
            .messages(messages)
            //
            // an effort has been made to make the api call more responsive by using stream(true)
            // however, due to the library limitation, we cannot do it yet.
            // find more details here:
            //    https://github.com/TheoKanning/openai-java/issues/83
            .stream(false)
            .n(1);
    if (!functions.isEmpty()) {
      requestBuilder.functions(functions);
    }
    return requestBuilder.maxTokens(maxTokens).build();
  }

  public OpenAIChatAboutNoteRequestBuilder useGPT4() {
    model = "gpt-4";
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder validateQuestionAgain(AIGeneratedQuestion question) {
    functions.add(
        ChatFunction.builder()
            .name("evaluate_question")
            .description("answer and evaluate the feasibility of the question")
            .executor(QuestionEvaluation.class, null)
            .build());

    AIGeneratedQuestionBody clone = new AIGeneratedQuestionBody();
    clone.stem = question.stem;
    clone.choices = question.choices;

    String messageBody =
        """
Please assume the role of a learner, who has learned the note of focus as well as many other notes.
Only the top-level of the context path is visible to you.
Without the specific note of focus and its more detailed contexts revealed to you,
please critically check if the following question makes sense and is possible to you:

%s

"""
            .formatted(clone.toJsonString());
    return addMessage(ChatMessageRole.USER, messageBody);
  }

  public OpenAIChatAboutNoteRequestBuilder chatMessage(String userMessage) {
    ChatMessageRole role = ChatMessageRole.USER;
    return addMessage(role, userMessage);
  }

  private OpenAIChatAboutNoteRequestBuilder addMessage(ChatMessageRole role, String userMessage) {
    messages.add(new ChatMessage(role.value(), userMessage));
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder userInstructionToGenerateQuestionWithGPT35FineTunedModel(
) {
    this.model = "ft:gpt-3.5-turbo-0613:odd-e::7uWJuLEw";
    ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);
    String schemaString;
    try {
      JsonSchema schema = jsonSchemaGenerator.generateSchema(AIGeneratedQuestion.class);
      schemaString = objectMapper.writeValueAsString(schema);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    addMessage(
        ChatMessageRole.SYSTEM,
        "When generating a question, please use this json structure:\n" + schemaString);
    String messageBody =
        "Please assume the role of a Memory Assistant, which involves helping me review, recall, and reinforce information from my notes. Generate a multiple-choice question based on the note in the current context path\n";

    return addMessage(ChatMessageRole.USER, messageBody);
  }
}

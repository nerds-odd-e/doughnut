package com.odde.doughnut.services.ai;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.odde.doughnut.controllers.json.AiCompletionParams;
import com.odde.doughnut.controllers.json.ClarifyingQuestionAndAnswer;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.theokanning.openai.completion.chat.*;
import java.util.HashMap;
import java.util.Optional;
import lombok.AllArgsConstructor;

public class OpenAIChatAboutNoteRequestBuilder {
  public static final String askClarificationQuestion = "ask_clarification_question";
  protected final OpenAIChatRequestBuilder openAIChatRequestBuilder =
      new OpenAIChatRequestBuilder();

  public OpenAIChatAboutNoteRequestBuilder(String modelName, Note note) {
    openAIChatRequestBuilder.model(modelName);
    this.openAIChatRequestBuilder.addSystemMessage(
        "This is a PKM system using hierarchical notes, each with a topic and details, to capture atomic concepts.");
    openAIChatRequestBuilder.addSystemMessage(note.getNoteDescription());
  }

  public OpenAIChatAboutNoteRequestBuilder addTool(AiTool tool) {
    tool.addToolToChatMessages(openAIChatRequestBuilder);
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder instructionForDetailsCompletion(
      AiCompletionParams aiCompletionParams) {
    openAIChatRequestBuilder.functions.add(
        ChatFunction.builder()
            .name("complete_note_details")
            .description("Text completion for the details of the note of focus")
            .executor(NoteDetailsCompletion.class, null)
            .build());
    openAIChatRequestBuilder.functions.add(
        ChatFunction.builder()
            .name(askClarificationQuestion)
            .description("Ask question to get more context")
            .executor(ClarifyingQuestion.class, null)
            .build());

    HashMap<String, String> arguments = new HashMap<>();
    arguments.put("details_to_complete", aiCompletionParams.getDetailsToComplete());
    openAIChatRequestBuilder.addUserMessage(
        ("Please complete the concise details of the note of focus. Keep it short."
                + " Don't make assumptions about the context. Ask for clarification through tool function `%s` if my request is ambiguous."
                + " The current details in JSON format are: \n%s")
            .formatted(
                askClarificationQuestion,
                defaultObjectMapper().valueToTree(arguments).toPrettyString()));
    aiCompletionParams.getClarifyingQuestionAndAnswers().forEach(this::answeredClarifyingQuestion);

    return this;
  }

  @AllArgsConstructor
  public static class UserResponseToClarifyingQuestion {
    public String answerFromUser;
  }

  private void answeredClarifyingQuestion(ClarifyingQuestionAndAnswer qa) {

    ChatMessage functionCallMessage = new ChatMessage(ChatMessageRole.ASSISTANT.value());
    functionCallMessage.setFunctionCall(
        new ChatFunctionCall(
            askClarificationQuestion, defaultObjectMapper().valueToTree(qa.questionFromAI)));
    openAIChatRequestBuilder.messages.add(functionCallMessage);

    Optional<ChatMessage> message =
        AiToolFactory.getFunctionExecutor(qa)
            .executeAndConvertToMessageSafely(functionCallMessage.getFunctionCall());

    openAIChatRequestBuilder.messages.add(message.get());
  }

  public OpenAIChatAboutNoteRequestBuilder chatMessage(String userMessage) {
    openAIChatRequestBuilder.addUserMessage(userMessage);
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder maxTokens(int maxTokens) {
    openAIChatRequestBuilder.maxTokens(maxTokens);
    return this;
  }

  public ChatCompletionRequest build() {
    return openAIChatRequestBuilder.build();
  }
}

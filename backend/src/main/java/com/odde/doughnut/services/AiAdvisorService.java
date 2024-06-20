package com.odde.doughnut.services;

import static com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder.askClarificationQuestion;
import static com.odde.doughnut.services.ai.tools.AiToolFactory.COMPLETE_NOTE_DETAILS;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.*;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.client.OpenAiApi;
import java.io.IOException;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class AiAdvisorService {

  private final OpenAiApiHandler openAiApiHandler;

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiApiHandler = new OpenAiApiHandler(openAiApi);
  }

  public String getImage(String prompt) {
    return getOtherAiServices().getTimage(prompt);
  }

  public AiAssistantResponse initiateAiCompletion(
      AiCompletionParams aiCompletionParams, Note note, String assistantId) {
    return getContentCompletionService()
        .initiateAThread(note, assistantId, aiCompletionParams.getCompletionPrompt());
  }

  public AiAssistantResponse answerAiCompletionClarifyingQuestion(
      AiCompletionAnswerClarifyingQuestionParams answerClarifyingQuestionParams) {
    return getContentCompletionService()
        .answerAiCompletionClarifyingQuestion(answerClarifyingQuestionParams);
  }

  public String chatWithAi(Note note, String userMessage, String assistantId) {
    return getChatService().initiateAThread(note, assistantId, userMessage).getLastMessage();
  }

  public List<String> getAvailableGptModels() {
    return getOtherAiServices().getAvailableGptModels();
  }

  private @NotNull OtherAiServices getOtherAiServices() {
    return new OtherAiServices(openAiApiHandler);
  }

  public String uploadAndTriggerFineTuning(
      List<OpenAIChatGPTFineTuningExample> examples, String question) throws IOException {
    return getOtherAiServices().uploadAndTriggerFineTuning(examples, question);
  }

  public SrtDto getTranscription(String filename, byte[] bytes) throws IOException {
    return getOtherAiServices().getTranscription(filename, bytes);
  }

  public String createCompletionAssistant(String modelName) {
    return getContentCompletionService()
        .createAssistant(modelName, "Note details completion")
        .getId();
  }

  public String createChatAssistant(String modelName) {
    return getChatService().createAssistant(modelName, "Chat assistant").getId();
  }

  private AssistantService getContentCompletionService() {
    return new AssistantService(
        openAiApiHandler,
        List.of(
            AiTool.build(
                COMPLETE_NOTE_DETAILS,
                "Text completion for the details of the note of focus",
                NoteDetailsCompletion.class,
                (noteDetailsCompletion) -> {
                  AiCompletionRequiredAction result = new AiCompletionRequiredAction();
                  result.setContentToAppend(noteDetailsCompletion.completion);
                  return result;
                }),
            AiTool.build(
                askClarificationQuestion,
                "Ask question to get more context",
                ClarifyingQuestion.class,
                (clarifyingQuestion) -> {
                  AiCompletionRequiredAction result = new AiCompletionRequiredAction();
                  result.setClarifyingQuestion(clarifyingQuestion);
                  return result;
                })));
  }

  private AssistantService getChatService() {
    return new AssistantService(openAiApiHandler, List.of());
  }
}

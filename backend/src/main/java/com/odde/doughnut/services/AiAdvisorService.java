package com.odde.doughnut.services;

import static com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder.askClarificationQuestion;
import static com.odde.doughnut.services.ai.tools.AiToolFactory.COMPLETE_NOTE_DETAILS;
import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.controllers.json.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.ai.*;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.AiToolList;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.Assistant;
import com.theokanning.openai.assistants.AssistantRequest;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.messages.MessageRequest;
import com.theokanning.openai.runs.*;
import com.theokanning.openai.threads.Thread;
import com.theokanning.openai.threads.ThreadRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AiAdvisorService {

  private final OpenAiApiHandler openAiApiHandler;

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiApiHandler = new OpenAiApiHandler(openAiApi);
  }

  public String getImage(String prompt) {
    return openAiApiHandler.getOpenAiImage(prompt);
  }

  public MCQWithAnswer generateQuestion(Note note, String modelName)
      throws QuizQuestionNotPossibleException {
    return getAiQuestionGenerator(note, modelName).getAiGeneratedQuestion();
  }

  public AiCompletionResponse getAiCompletion(
      AiCompletionParams aiCompletionParams, Note note, String assistantId) {
    String threadId = createThread(aiCompletionParams, note);
    Run run = openAiApiHandler.createRun(threadId, assistantId);
    return getThreadResponse(threadId, aiCompletionParams.getDetailsToComplete(), run);
  }

  public AiCompletionResponse answerAiCompletionClarifyingQuestion(
      AiCompletionAnswerClarifyingQuestionParams answerClarifyingQuestionParams) {
    String threadId = answerClarifyingQuestionParams.getThreadId();

    Run retrievedRun = openAiApiHandler.submitToolOutputs(answerClarifyingQuestionParams);

    return getThreadResponse(
        threadId, answerClarifyingQuestionParams.getDetailsToComplete(), retrievedRun);
  }

  private String createThread(AiCompletionParams aiCompletionParams, Note note) {
    ThreadRequest threadRequest = ThreadRequest.builder().build();
    Thread thread = openAiApiHandler.createThread(threadRequest);
    MessageRequest messageRequest =
        MessageRequest.builder()
            .content(
                note.getNoteDescription()
                    + "------------\n"
                    + aiCompletionParams.getCompletionPrompt())
            .build();

    openAiApiHandler.createMessage(thread.getId(), messageRequest);
    return thread.getId();
  }

  private AiCompletionResponse getThreadResponse(
      String threadId, String detailsToComplete, Run currentRun) {
    Run run = openAiApiHandler.retrieveUntilCompletedOrRequiresAction(threadId, currentRun);

    AiCompletionResponse completionResponseForClarification;
    if (run.getStatus().equals("requires_action")) {
      RequiredAction requiredAction = run.getRequiredAction();
      ToolCall toolCall = requiredAction.getSubmitToolOutputs().getToolCalls().get(0);
      ToolCallFunction function = toolCall.getFunction();
      String arguments = function.getArguments();
      JsonNode jsonNode = null;
      try {
        jsonNode = defaultObjectMapper().readTree(arguments);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }

      if (function.getName().equals(askClarificationQuestion)) {
        ClarifyingQuestion result1;
        try {
          result1 = defaultObjectMapper().treeToValue(jsonNode, ClarifyingQuestion.class);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
        AiCompletionResponse result = new AiCompletionResponse();
        ClarifyingQuestionRequiredAction cqra = new ClarifyingQuestionRequiredAction();
        cqra.clarifyingQuestion = result1;
        cqra.toolCallId = toolCall.getId();

        result.setClarifyingQuestionRequiredAction(cqra);
        completionResponseForClarification = result;
      } else if (function.getName().equals(COMPLETE_NOTE_DETAILS)) {
        completionResponseForClarification = getAiCompletionResponse(jsonNode, detailsToComplete);
      } else {
        throw new RuntimeException("Unknown function name: " + function.getName());
      }
    } else {
      throw new RuntimeException("not implemented");
    }
    completionResponseForClarification.setThreadId(threadId);
    completionResponseForClarification.setRunId(currentRun.getId());
    return completionResponseForClarification;
  }

  public Assistant createNoteCompletionAssistant(String modelName) {
    AiToolList tool = AiToolFactory.getNoteContentCompletionTools(null);
    return createAssistant(modelName, tool);
  }

  private Assistant createAssistant(String modelName, AiToolList tool) {
    AssistantRequest assistantRequest =
        AssistantRequest.builder()
            .model(modelName)
            .name("Note details completion")
            .instructions(OpenAIChatRequestBuilder.systemInstruction)
            .tools(tool.getTools())
            .build();
    return openAiApiHandler.createAssistant(assistantRequest);
  }

  private static AiCompletionResponse getAiCompletionResponse(
      JsonNode jsonNode, String detailsToComplete) {
    String result1;
    try {
      NoteDetailsCompletion noteDetailsCompletion =
          defaultObjectMapper().treeToValue(jsonNode, NoteDetailsCompletion.class);
      result1 = detailsToComplete + noteDetailsCompletion.completion;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    AiCompletionResponse result = new AiCompletionResponse();
    String content = result1;
    result.setMoreCompleteContent(content);
    return result;
  }

  public String chatWithAi(Note note, String userMessage, String modelName) {
    ChatCompletionRequest chatCompletionRequest =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder(modelName, note)
            .addUserMessage(userMessage)
            .maxTokens(150)
            .build();

    Optional<ChatCompletionChoice> response =
        openAiApiHandler.chatCompletion(chatCompletionRequest);
    if (response.isPresent()) {
      return response.get().getMessage().getContent();
    }
    return "";
  }

  private AiQuestionGenerator getAiQuestionGenerator(Note note, String modelName) {
    return new AiQuestionGenerator(note, openAiApiHandler, modelName);
  }

  public List<String> getAvailableGptModels() {
    List<String> modelVersionOptions = new ArrayList<>();

    openAiApiHandler
        .getModels()
        .forEach(
            (e) -> {
              if (e.id.startsWith("ft:") || e.id.startsWith("gpt")) {
                modelVersionOptions.add(e.id);
              }
            });

    return modelVersionOptions;
  }

  public QuizQuestionContestResult contestQuestion(
      QuizQuestionEntity quizQuestionEntity, String modelName) {
    return getAiQuestionGenerator(quizQuestionEntity.getThing().getNote(), modelName)
        .evaluateQuestion(quizQuestionEntity.getMcqWithAnswer())
        .map(e -> e.getQuizQuestionContestResult(quizQuestionEntity.getCorrectAnswerIndex()))
        .orElse(null);
  }

  public String uploadAndTriggerFineTuning(
      List<OpenAIChatGPTFineTuningExample> examples, String question) throws IOException {
    String fileId = openAiApiHandler.uploadFineTuningExamples(examples, question);
    return openAiApiHandler.triggerFineTuning(fileId).getFineTunedModel();
  }
}

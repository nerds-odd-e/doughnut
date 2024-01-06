package com.odde.doughnut.services;

import static com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder.askClarificationQuestion;
import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.controllers.json.AiCompletionParams;
import com.odde.doughnut.controllers.json.AiCompletionResponse;
import com.odde.doughnut.controllers.json.ClarifyingQuestionRequiredAction;
import com.odde.doughnut.controllers.json.QuizQuestionContestResult;
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
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.messages.MessageRequest;
import com.theokanning.openai.runs.RequiredAction;
import com.theokanning.openai.runs.Run;
import com.theokanning.openai.runs.ToolCall;
import com.theokanning.openai.runs.ToolCallFunction;
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
      AiCompletionParams aiCompletionParams, Note note, String modelName, String assistantId) {
    String threadId = ensureThread(aiCompletionParams, note);
    return getAiCompletionResponse(threadId, assistantId, aiCompletionParams, note, modelName);
  }

  public AiCompletionResponse answerAiCompletionClarifyingQuestion(
      AiCompletionParams aiCompletionParams, Note note, String modelName, String assistantId) {
    String threadId = ensureThread(aiCompletionParams, note);
    return getAiCompletionResponse(threadId, assistantId, aiCompletionParams, note, modelName);
  }

  private String ensureThread(AiCompletionParams aiCompletionParams, Note note) {
    String threadId = aiCompletionParams.getThreadId();
    if (threadId == null) {
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
    return threadId;
  }

  private AiCompletionResponse getAiCompletionResponse(
      String threadId,
      String assistantId,
      AiCompletionParams aiCompletionParams,
      Note note,
      String modelName) {
    Run run = openAiApiHandler.blockGetRun(threadId, assistantId);

    AiToolList tool =
        AiToolFactory.getNoteContentCompletionTools(aiCompletionParams.getCompletionPrompt());

    ChatCompletionRequest chatCompletionRequest =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder(modelName, note)
            .addTool(tool)
            .addMessages(aiCompletionParams.getQAMessages())
            .maxTokens(150)
            .build();

    boolean isClarifyingQuestion = run.getStatus().equals("requires_action");
    AiCompletionResponse completionResponseForClarification;
    if (isClarifyingQuestion) {
      RequiredAction requiredAction = run.getRequiredAction();
      ToolCall toolCall = requiredAction.getSubmitToolOutputs().getToolCalls().get(0);
      ToolCallFunction function = toolCall.getFunction();
      if (function.getName().equals(askClarificationQuestion)) {
        String arguments = function.getArguments();
        JsonNode jsonNode = null;
        try {
          jsonNode = defaultObjectMapper().readTree(arguments);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
        ClarifyingQuestion result1;
        try {
          result1 = defaultObjectMapper().treeToValue(jsonNode, ClarifyingQuestion.class);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
        AiCompletionResponse result = new AiCompletionResponse();
        result.setFinishReason("question");
        ClarifyingQuestionRequiredAction cqra = new ClarifyingQuestionRequiredAction();
        cqra.clarifyingQuestion = result1;
        cqra.toolCallId = toolCall.getId();

        result.setClarifyingQuestionRequiredAction(cqra);
        completionResponseForClarification = result;
      } else {
        throw new RuntimeException("Unknown function name: " + function.getName());
      }
    } else {
      ChatFunctionCall chatFunctionCall =
          openAiApiHandler.getFunctionCall(chatCompletionRequest).orElseThrow();
      completionResponseForClarification =
          getAiCompletionResponse(aiCompletionParams, chatFunctionCall.getArguments());
    }
    completionResponseForClarification.setThreadId(threadId);
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
      AiCompletionParams aiCompletionParams, JsonNode jsonNode) {
    String result1;
    try {
      NoteDetailsCompletion noteDetailsCompletion =
          defaultObjectMapper().treeToValue(jsonNode, NoteDetailsCompletion.class);
      aiCompletionParams.setDetailsToComplete(
          aiCompletionParams.getDetailsToComplete() + noteDetailsCompletion.completion);
      result1 = aiCompletionParams.getDetailsToComplete();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    AiCompletionResponse result = new AiCompletionResponse();
    String content = result1;
    result.setMoreCompleteContent(content);
    result.setFinishReason("stop");
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

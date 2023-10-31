package com.odde.doughnut.services;

import com.odde.doughnut.controllers.json.AiCompletion;
import com.odde.doughnut.controllers.json.AiCompletionParams;
import com.odde.doughnut.controllers.json.AiTrainingFile;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.OpenAIChatAboutNoteRequestBuilder;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
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

  public MCQWithAnswer generateQuestion(Note note) throws QuizQuestionNotPossibleException {
    return getAiQuestionGenerator(note).getAiGeneratedQuestion();
  }

  public QuestionEvaluation contestMCQ(QuizQuestionEntity quizQuestionEntity) {
    return getAiQuestionGenerator(quizQuestionEntity.getThing().getNote())
        .evaluateQuestion(quizQuestionEntity.getMcqWithAnswer())
        .orElse(null);
  }

  public AiCompletion getAiCompletion(AiCompletionParams aiCompletionParams, Note note) {
    ChatCompletionRequest chatCompletionRequest =
        new OpenAIChatAboutNoteRequestBuilder()
            .systemBrief()
            .contentOfNoteOfCurrentFocus(note)
            .instructionForCompletion(aiCompletionParams)
            .maxTokens(100)
            .build();
    return openAiApiHandler.getAiCompletion(aiCompletionParams, chatCompletionRequest).orElse(null);
  }

  public String chatToAi(Note note, String userMessage) {
    ChatCompletionRequest chatCompletionRequest =
        new OpenAIChatAboutNoteRequestBuilder()
            .systemBrief()
            .contentOfNoteOfCurrentFocus(note)
            .chatMessage(userMessage)
            .maxTokens(150)
            .build();

    Optional<ChatCompletionChoice> response =
        openAiApiHandler.chatCompletion(chatCompletionRequest);
    if (response.isPresent()) {
      return response.get().getMessage().getContent();
    }
    return "";
  }

  private AiQuestionGenerator getAiQuestionGenerator(Note note) {
    return new AiQuestionGenerator(note, openAiApiHandler);
  }

  public List<AiTrainingFile> listTrainingFiles() {
    return openAiApiHandler.getTrainingFileList();
  }
}

package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AiCompletion;
import com.odde.doughnut.entities.json.AiCompletionRequest;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.ai.AIGeneratedQuestion;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.OpenAIChatAboutNoteRequestBuilder;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import java.util.Optional;

public class AiAdvisorService {
  private final OpenAiApiHandler openAiApiHandler;

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiApiHandler = new OpenAiApiHandler(openAiApi);
  }

  public String getImage(String prompt) {
    return openAiApiHandler.getOpenAiImage(prompt);
  }

  public AIGeneratedQuestion generateQuestion(Note note) throws QuizQuestionNotPossibleException {
    AiQuestionGenerator aiQuestionGenerator = new AiQuestionGenerator(note, openAiApiHandler);
    return aiQuestionGenerator.getAiGeneratedQuestion();
  }

  public AiCompletion getAiCompletion(AiCompletionRequest aiCompletionRequest, String notePath) {
    ChatCompletionRequest chatCompletionRequest =
        new OpenAIChatAboutNoteRequestBuilder(notePath)
            .instructionForCompletion(aiCompletionRequest)
            .maxTokens(100)
            .build();
    return openAiApiHandler
        .getAiCompletion(aiCompletionRequest, chatCompletionRequest)
        .orElse(null);
  }

  public String chatToAi(Note note, String userMessage) {
    ChatCompletionRequest chatCompletionRequest =
        new OpenAIChatAboutNoteRequestBuilder(note.getPath())
            .detailsOfNoteOfCurrentFocus(note)
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
}

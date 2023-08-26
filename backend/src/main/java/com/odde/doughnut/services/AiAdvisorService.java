package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AiCompletion;
import com.odde.doughnut.entities.json.AiCompletionRequest;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.ai.AIGeneratedQuestion;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.openAiApis.OpenAIChatAboutNoteRequestBuilder;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;

public class AiAdvisorService {
  private final OpenAiApiHandler openAiApiHandler;

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiApiHandler = new OpenAiApiHandler(openAiApi);
  }

  private static ChatCompletionRequest generateChatCompletionRequest(ChatMessages messages) {
    return ChatCompletionRequest.builder().model("gpt-4").messages(messages.getMessages()).stream(
            false)
        .n(1)
        .maxTokens(100)
        .build();
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

  public String chatToAi(String question) {
    ChatMessages chatMessages =
        new ChatMessages(
            List.of(
                new ChatMessage(ChatMessageRole.USER.value(), ""),
                new ChatMessage(ChatMessageRole.ASSISTANT.value(), question)));
    ChatCompletionRequest request = generateChatCompletionRequest(chatMessages);

    Optional<ChatCompletionChoice> response = openAiApiHandler.chatCompletion(request);
    if (response.isPresent()) {
      return response.get().getMessage().getContent();
    }
    return "";
  }

  @AllArgsConstructor
  @Data
  private static class ChatMessages {
    private List<ChatMessage> messages;
  }
}

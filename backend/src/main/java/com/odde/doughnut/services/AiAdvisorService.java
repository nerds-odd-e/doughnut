package com.odde.doughnut.services;

import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.entities.json.AiSuggestionRequest;
import com.odde.doughnut.services.openAiApis.OpenAiAPIChatCompletion;
import com.odde.doughnut.services.openAiApis.OpenAiAPIImage;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class AiAdvisorService {
  private final OpenAiAPIChatCompletion openAiAPIChatCompletion;
  private final OpenAiAPIImage openAiAPIImage;

  static String contextTemplate =
      "This is a personal knowledge management system, consists of notes with a title and a description, which should represent atomic concepts.\n"
          + "context: ";

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiAPIChatCompletion = new OpenAiAPIChatCompletion(openAiApi);
    openAiAPIImage = new OpenAiAPIImage(openAiApi);
  }

  public AiSuggestion getAiSuggestion(String context, AiSuggestionRequest aiSuggestionRequest) {
    String prompt = aiSuggestionRequest.prompt;
    String incompleteAssistantMessage = aiSuggestionRequest.incompleteAssistantMessage;
    List<ChatMessage> messages = getChatMessages(context, prompt, incompleteAssistantMessage);
    return openAiAPIChatCompletion
        .getOpenAiCompletion(messages, 100)
        .prependPreviousIncompleteMessage(incompleteAssistantMessage);
  }

  private static List<ChatMessage> getChatMessages(
      String context, String prompt, String incompleteAssistantMessage) {
    List<ChatMessage> messages = new ArrayList<>();
    String content = contextTemplate + context;
    messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), content));
    messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));
    if (!Strings.isEmpty(incompleteAssistantMessage)) {
      messages.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), incompleteAssistantMessage));
    }
    return messages;
  }

  public AiSuggestion generateQuestion(String context, String prompt) {
    List<ChatMessage> messages = getChatMessages(context, prompt, null);
    return openAiAPIChatCompletion.getOpenAiCompletion(messages, 1100);
  }

  public AiEngagingStory getEngagingStory(String prompt) {
    return new AiEngagingStory(openAiAPIImage.getOpenAiImage(prompt));
  }
}

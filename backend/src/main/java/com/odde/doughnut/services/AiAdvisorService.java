package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.NoteModel;
import com.odde.doughnut.services.openAiApis.OpenAiAPIChatCompletion;
import com.odde.doughnut.services.openAiApis.OpenAiAPIImage;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;

public class AiAdvisorService {
  private final OpenAiAPIChatCompletion openAiAPIChatCompletion;
  private final OpenAiAPIImage openAiAPIImage;

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiAPIChatCompletion = new OpenAiAPIChatCompletion(openAiApi);
    openAiAPIImage = new OpenAiAPIImage(openAiApi);
  }

  public AiSuggestion getAiSuggestion(
      List<ChatMessage> messages, String incompleteAssistantMessage) {
    return openAiAPIChatCompletion
        .getOpenAiCompletion(messages, 100)
        .prependPreviousIncompleteMessage(incompleteAssistantMessage);
  }

  public AiEngagingStory getEngagingStory(String prompt) {
    return new AiEngagingStory(openAiAPIImage.getOpenAiImage(prompt));
  }

  public String generateQuestionJsonString(Note note, ModelFactoryService modelFactoryService) {
    NoteModel noteModel = modelFactoryService.toNoteModel(note);
    List<ChatMessage> messages = noteModel.getChatMessagesForGenerateQuestion();
    AiSuggestion openAiCompletion = openAiAPIChatCompletion.getOpenAiCompletion1(messages, 1100);
    return openAiCompletion.getSuggestion();
  }
}

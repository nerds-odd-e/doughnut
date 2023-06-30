package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AIGeneratedQuestion;
import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.models.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.openAiApis.OpenAIChatAboutNoteMessageBuilder;
import com.odde.doughnut.services.openAiApis.OpenAiAPIChatCompletion;
import com.odde.doughnut.services.openAiApis.OpenAiAPIImage;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class AiAdvisorService {
  private final OpenAiAPIChatCompletion openAiAPIChatCompletion;
  private final OpenAiAPIImage openAiAPIImage;

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiAPIChatCompletion = new OpenAiAPIChatCompletion(openAiApi);
    openAiAPIImage = new OpenAiAPIImage(openAiApi);
  }

  public AiSuggestion getCompletion(
      List<ChatMessage> messages, String incompleteAssistantMessage) {
    return openAiAPIChatCompletion
        .getOpenAiCompletion(messages)
        .prependPreviousIncompleteMessage(incompleteAssistantMessage);
  }

  public AiEngagingStory getEngagingStory(String prompt) {
    return new AiEngagingStory(openAiAPIImage.getOpenAiImage(prompt));
  }

  public String generateQuestionJsonString(Note note) throws QuizQuestionNotPossibleException {
    List<ChatMessage> messages =
        new OpenAIChatAboutNoteMessageBuilder(note)
            .detailsOfNoteOfCurrentFocus()
            .userInstructionToGenerateQuestion()
            .build();
    AIGeneratedQuestion openAiGenerateQuestion =
        openAiAPIChatCompletion.getOpenAiGenerateQuestion(messages);
    if (openAiGenerateQuestion == null || Strings.isBlank(openAiGenerateQuestion.question)) {
      throw new QuizQuestionNotPossibleException();
    }
    return new ObjectMapper().valueToTree(openAiGenerateQuestion).toString();
  }
}

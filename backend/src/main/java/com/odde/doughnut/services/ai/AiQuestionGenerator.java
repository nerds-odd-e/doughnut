package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.NoteQuestionGenerationService;
import com.odde.doughnut.services.NotebookAssistantForNoteServiceFactory;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.client.OpenAiApi;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record AiQuestionGenerator(
    OpenAiApi openAiApi, GlobalSettingsService globalSettingsService) {

  public MCQWithAnswer getAiGeneratedQuestion(Note note) {
    NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory =
        new NotebookAssistantForNoteServiceFactory(openAiApi, globalSettingsService);
    NoteQuestionGenerationService service =
        notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(note);
    try {
      MCQWithAnswer original = service.generateQuestion();
      if (original != null && Boolean.FALSE.equals(original.isStrictChoiceOrder())) {
        return shuffleChoices(original);
      }
      return original;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private MCQWithAnswer shuffleChoices(MCQWithAnswer original) {
    List<String> choices = new ArrayList<>(original.getMultipleChoicesQuestion().getChoices());
    String correctChoice = choices.get(original.getCorrectChoiceIndex());
    Collections.shuffle(choices);
    int newCorrectIndex = choices.indexOf(correctChoice);

    MultipleChoicesQuestion shuffledQuestion =
        new MultipleChoicesQuestion(original.getMultipleChoicesQuestion().getStem(), choices);

    return new MCQWithAnswer(shuffledQuestion, newCorrectIndex, false);
  }

  public MCQWithAnswer getAiGeneratedRefineQuestion(Note note, MCQWithAnswer mcqWithAnswer) {
    return forNote(note, globalSettingsService.globalSettingQuestionGeneration().getValue())
        .refineQuestion(mcqWithAnswer)
        .orElse(null);
  }

  public QuestionContestResult getQuestionContestResult(PredefinedQuestion predefinedQuestion) {
    return forNote(
            predefinedQuestion.getNote(),
            globalSettingsService.globalSettingEvaluation().getValue())
        .evaluateQuestion(predefinedQuestion.getMcqWithAnswer())
        .map(e -> e.getQuestionContestResult(predefinedQuestion.getCorrectAnswerIndex()))
        .orElse(null);
  }

  // Temporary method until we migrate all functionality
  private AiQuestionGeneratorForNote forNote(Note note, String modelName1) {
    OpenAIChatRequestBuilder chatAboutNoteRequestBuilder =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder(modelName1, note);
    return new AiQuestionGeneratorForNote(
        new OpenAiApiHandler(openAiApi), chatAboutNoteRequestBuilder);
  }
}

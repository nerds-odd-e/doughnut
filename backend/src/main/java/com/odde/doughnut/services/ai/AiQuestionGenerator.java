package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.NoteQuestionGenerationService;
import com.odde.doughnut.services.NotebookAssistantForNoteServiceFactory;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.client.OpenAiApi;
import java.util.ArrayList;
import java.util.List;

public record AiQuestionGenerator(
    OpenAiApi openAiApi,
    GlobalSettingsService globalSettingsService,
    com.odde.doughnut.models.Randomizer randomizer) {

  public MCQWithAnswer getAiGeneratedQuestion(Note note, MessageRequest additionalMessage) {
    NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory =
        new NotebookAssistantForNoteServiceFactory(openAiApi, globalSettingsService);
    NoteQuestionGenerationService service =
        notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(note);
    try {
      MCQWithAnswer original = service.generateQuestion(additionalMessage);
      if (original != null && !original.isStrictChoiceOrder()) {
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
    choices = randomizer.shuffle(choices);
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

  public QuestionEvaluation getQuestionContestResult(Note note, MCQWithAnswer mcqWithAnswer) {
    NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory =
        new NotebookAssistantForNoteServiceFactory(openAiApi, globalSettingsService);
    NoteQuestionGenerationService service =
        notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(note);
    try {
      return service.evaluateQuestion(mcqWithAnswer).orElse(null);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  // Temporary method until we migrate all functionality
  private AiQuestionGeneratorForNote forNote(Note note, String modelName1) {
    OpenAIChatRequestBuilder chatAboutNoteRequestBuilder =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder(modelName1, note);
    return new AiQuestionGeneratorForNote(
        new OpenAiApiHandler(openAiApi), chatAboutNoteRequestBuilder);
  }

  public MCQWithAnswer regenerateQuestion(
      QuestionContestResult contestResult, Note note, MCQWithAnswer mcqWithAnswer) {
    MessageRequest additionalMessage =
        AiToolFactory.buildRegenerateQuestionMessage(contestResult, mcqWithAnswer);
    return getAiGeneratedQuestion(note, additionalMessage);
  }
}

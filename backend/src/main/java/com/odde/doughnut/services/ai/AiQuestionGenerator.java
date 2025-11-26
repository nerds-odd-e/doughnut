package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.NoteQuestionGenerationService;
import com.odde.doughnut.services.NotebookAssistantForNoteServiceFactory;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.utils.Randomizer;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiQuestionGenerator {
  private final NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory;
  private final GlobalSettingsService globalSettingsService;
  private final Randomizer randomizer;
  private final ObjectMapper objectMapper;
  private final OpenAiApiHandler openAiApiHandler;
  private final TestabilitySettings testabilitySettings;

  @Autowired
  public AiQuestionGenerator(
      NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory,
      GlobalSettingsService globalSettingsService,
      TestabilitySettings testabilitySettings,
      ObjectMapper objectMapper,
      OpenAiApiHandler openAiApiHandler) {
    this.notebookAssistantForNoteServiceFactory = notebookAssistantForNoteServiceFactory;
    this.globalSettingsService = globalSettingsService;
    this.randomizer = testabilitySettings.getRandomizer();
    this.objectMapper = objectMapper;
    this.openAiApiHandler = openAiApiHandler;
    this.testabilitySettings = testabilitySettings;
  }

  // Test-only constructor for injecting Randomizer directly
  public AiQuestionGenerator(
      NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory,
      GlobalSettingsService globalSettingsService,
      Randomizer randomizer,
      ObjectMapper objectMapper,
      OpenAiApiHandler openAiApiHandler,
      TestabilitySettings testabilitySettings) {
    this.notebookAssistantForNoteServiceFactory = notebookAssistantForNoteServiceFactory;
    this.globalSettingsService = globalSettingsService;
    this.randomizer = randomizer;
    this.objectMapper = objectMapper;
    this.openAiApiHandler = openAiApiHandler;
    this.testabilitySettings = testabilitySettings;
  }

  public MCQWithAnswer getAiGeneratedQuestion(Note note, String additionalMessage) {
    if (testabilitySettings.isOpenAiDisabled()) {
      return null;
    }
    NoteQuestionGenerationService service =
        notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(note);
    try {
      MCQWithAnswer original = service.generateQuestion(additionalMessage);
      if (original != null && !original.isF2__strictChoiceOrder()) {
        return shuffleChoices(original);
      }
      return original;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private MCQWithAnswer shuffleChoices(MCQWithAnswer original) {
    List<String> choices =
        new ArrayList<>(original.getF0__multipleChoicesQuestion().getF1__choices());
    String correctChoice = choices.get(original.getF1__correctChoiceIndex());
    choices = randomizer.shuffle(choices);
    int newCorrectIndex = choices.indexOf(correctChoice);

    MultipleChoicesQuestion shuffledQuestion =
        new MultipleChoicesQuestion(
            original.getF0__multipleChoicesQuestion().getF0__stem(), choices);

    return new MCQWithAnswer(shuffledQuestion, newCorrectIndex, false);
  }

  public MCQWithAnswer getAiGeneratedRefineQuestion(Note note, MCQWithAnswer mcqWithAnswer) {
    if (testabilitySettings.isOpenAiDisabled()) {
      return null;
    }
    return forNote(note, globalSettingsService.globalSettingQuestionGeneration().getValue())
        .refineQuestion(mcqWithAnswer)
        .orElse(null);
  }

  public QuestionEvaluation getQuestionContestResult(Note note, MCQWithAnswer mcqWithAnswer) {
    if (testabilitySettings.isOpenAiDisabled()) {
      return null;
    }
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
    return new AiQuestionGeneratorForNote(openAiApiHandler, chatAboutNoteRequestBuilder);
  }

  public MCQWithAnswer regenerateQuestion(
      QuestionContestResult contestResult, Note note, MCQWithAnswer mcqWithAnswer) {
    String additionalMessage =
        AiToolFactory.buildRegenerateQuestionMessage(contestResult, mcqWithAnswer);
    return getAiGeneratedQuestion(note, additionalMessage);
  }
}

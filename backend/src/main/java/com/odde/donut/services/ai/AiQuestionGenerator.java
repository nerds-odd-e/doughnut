package com.odde.donut.services.ai;

import com.odde.donut.controllers.dto.QuestionContestResult;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.PropertyFocus;
import com.odde.donut.services.NoteQuestionGenerationService;
import com.odde.donut.services.NoteQuestionGenerationService.GeneratedQuestion;
import com.odde.donut.services.ai.tools.AiToolFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiQuestionGenerator {
  private final NoteQuestionGenerationService noteQuestionGenerationService;
  private final GeneratedQuestionPostProcessor generatedQuestionPostProcessor;

  @Autowired
  public AiQuestionGenerator(
      NoteQuestionGenerationService noteQuestionGenerationService,
      GeneratedQuestionPostProcessor generatedQuestionPostProcessor) {
    this.noteQuestionGenerationService = noteQuestionGenerationService;
    this.generatedQuestionPostProcessor = generatedQuestionPostProcessor;
  }

  public Mcq getAiGeneratedQuestion(
      Note note, String additionalMessage, Long contextSeed, PropertyFocus propertyFocus) {
    GeneratedQuestion generated =
        noteQuestionGenerationService.generateQuestionWithFocus(
            note, additionalMessage, contextSeed, propertyFocus);
    Mcq mcq = generatedQuestionPostProcessor.assembleMcq(generated.question(), contextSeed);
    if (mcq != null) {
      mcq.setFocusNoteFingerprint(generated.focusNoteFingerprint());
    }
    return mcq;
  }

  public SupportedChoices supportedChoices(Note note, Mcq mcq) {
    return noteQuestionGenerationService.supportedChoices(note, mcq).orElse(null);
  }

  public Mcq regenerateQuestion(
      QuestionContestResult contestResult,
      Note note,
      Mcq mcq,
      Long contextSeed,
      PropertyFocus propertyFocus) {
    String additionalMessage = AiToolFactory.buildRegenerateQuestionMessage(contestResult, mcq);
    return getAiGeneratedQuestion(note, additionalMessage, contextSeed, propertyFocus);
  }
}

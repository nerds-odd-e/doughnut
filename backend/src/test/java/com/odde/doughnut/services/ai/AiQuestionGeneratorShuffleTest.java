package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.NoteQuestionGenerationService;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiQuestionGeneratorShuffleTest {

  @Test
  void shouldReturnPostProcessedGeneratedQuestion() throws JsonProcessingException {
    NoteQuestionGenerationService noteQuestionGenerationService =
        mock(NoteQuestionGenerationService.class);
    GeneratedQuestionPostProcessor generatedQuestionPostProcessor =
        mock(GeneratedQuestionPostProcessor.class);

    AiQuestionGenerator generator =
        new AiQuestionGenerator(noteQuestionGenerationService, generatedQuestionPostProcessor);

    Note note = mock(Note.class);
    MCQWithAnswer originalQuestion =
        new MCQWithAnswer(
            new MultipleChoicesQuestion("What is 2+2?", List.of("4", "3", "5", "6")),
            0,
            true,
            null,
            null);
    MCQWithAnswer postProcessedQuestion =
        new MCQWithAnswer(
            new MultipleChoicesQuestion("What is 2+2?", List.of("6", "4", "5", "3")),
            1,
            true,
            null,
            null);
    when(noteQuestionGenerationService.generateQuestion(eq(note), eq(null), eq(null), eq(null)))
        .thenReturn(originalQuestion);
    when(generatedQuestionPostProcessor.postProcess(originalQuestion))
        .thenReturn(postProcessedQuestion);

    MCQWithAnswer result = generator.getAiGeneratedQuestion(note, null);

    verify(generatedQuestionPostProcessor).postProcess(originalQuestion);
    assertThat(result, equalTo(postProcessedQuestion));
    assertThat(result.getSolutionChoiceIndex(), equalTo(1));
  }
}

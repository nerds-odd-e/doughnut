package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.NoteQuestionGenerationService;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.utils.Randomizer;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiQuestionGeneratorShuffleTest {

  @Test
  void shouldShuffleChoicesInSpecificOrder() throws JsonProcessingException {
    Randomizer mockedRandomizer = mock(Randomizer.class);
    NoteQuestionGenerationService noteQuestionGenerationService =
        mock(NoteQuestionGenerationService.class);
    GlobalSettingsService globalSettingsService = mock(GlobalSettingsService.class);
    OpenAiApiHandler openAiApiHandler = mock(OpenAiApiHandler.class);
    TestabilitySettings testabilitySettings = mock(TestabilitySettings.class);

    AiQuestionGenerator generator =
        new AiQuestionGenerator(
            noteQuestionGenerationService,
            globalSettingsService,
            mockedRandomizer,
            openAiApiHandler,
            testabilitySettings);

    Note note = mock(Note.class);
    MCQWithAnswer originalQuestion =
        new MCQWithAnswer(
            new MultipleChoicesQuestion("What is 2+2?", List.of("4", "3", "5", "6")),
            0,
            true,
            null,
            null);

    List<String> shuffledChoices = Arrays.asList("6", "4", "5", "3");
    doReturn(shuffledChoices).when(mockedRandomizer).shuffle(any());
    when(noteQuestionGenerationService.generateQuestion(eq(note), eq(null), eq(null), eq(null)))
        .thenReturn(originalQuestion);

    MCQWithAnswer result = generator.getAiGeneratedQuestion(note, null);

    assertThat(result.getQuestion().getResponseChoices(), equalTo(shuffledChoices));
    assertThat(result.getSolutionChoiceIndex(), equalTo(1));
  }
}

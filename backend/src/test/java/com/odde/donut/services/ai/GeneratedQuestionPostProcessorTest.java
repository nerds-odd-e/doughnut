package com.odde.donut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.controllers.dto.Randomization;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.Note;
import com.odde.donut.testability.TestabilitySettings;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeneratedQuestionPostProcessorTest {

  @Test
  void assemblesSemanticAnswerAndMetadata() {
    TestabilitySettings testabilitySettings = new TestabilitySettings();
    testabilitySettings.setRandomization(new Randomization(Randomization.RandomStrategy.first, 0));
    GeneratedQuestionPostProcessor postProcessor =
        new GeneratedQuestionPostProcessor(testabilitySettings);
    GeneratedMcq originalQuestion =
        new GeneratedMcq(
            "Which ordered choice is correct?",
            "correct choice",
            List.of("first distractor", "second distractor", "third distractor"),
            "focus",
            "rationale");

    Note note = new Note();
    Mcq result = postProcessor.assembleMcq(originalQuestion, note, 37L);

    assertThat(result.getNote(), equalTo(note));
    assertThat(result.getQuestionStem(), equalTo(originalQuestion.getQuestionStem()));
    assertThat(
        result.getResponseChoices(),
        equalTo(
            List.of(
                "correct choice", "first distractor", "second distractor", "third distractor")));
    assertThat(result.getCorrectAnswerIndex(), equalTo(0));
    assertThat(result.getContextSeed(), equalTo(37L));
    assertThat(result.getTestedFocus(), equalTo("focus"));
    assertThat(result.getValidationRationale(), equalTo("rationale"));
  }
}

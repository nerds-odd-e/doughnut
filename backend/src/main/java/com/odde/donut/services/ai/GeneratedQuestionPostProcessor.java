package com.odde.donut.services.ai;

import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.Note;
import com.odde.donut.testability.TestabilitySettings;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class GeneratedQuestionPostProcessor {
  private final TestabilitySettings testabilitySettings;

  public GeneratedQuestionPostProcessor(TestabilitySettings testabilitySettings) {
    this.testabilitySettings = testabilitySettings;
  }

  public Mcq assembleMcq(GeneratedMcq original, Note note, Long contextSeed) {
    if (original == null) {
      return null;
    }
    if (!original.isValid()) {
      throw new IllegalArgumentException("generated question must be valid before post-processing");
    }

    List<TaggedChoice> choices =
        testabilitySettings.getRandomizer().shuffle(taggedChoices(original));

    Mcq mcq = new Mcq();
    mcq.setNote(note);
    mcq.setQuestionStem(original.getQuestionStem());
    mcq.setResponseChoices(choices.stream().map(TaggedChoice::text).toList());
    mcq.setCorrectAnswerIndex(correctAnswerIndex(choices));
    mcq.setContextSeed(contextSeed);
    mcq.setTestedFocus(original.getTestedFocus());
    mcq.setValidationRationale(original.getValidationRationale());
    return mcq;
  }

  private List<TaggedChoice> taggedChoices(GeneratedMcq generatedMcq) {
    return Stream.concat(
            Stream.of(new TaggedChoice(true, generatedMcq.getCorrectAnswer())),
            generatedMcq.getDistractors().stream().map(text -> new TaggedChoice(false, text)))
        .toList();
  }

  private int correctAnswerIndex(List<TaggedChoice> choices) {
    for (int index = 0; index < choices.size(); index++) {
      if (choices.get(index).correct()) {
        return index;
      }
    }
    throw new IllegalArgumentException("correct answer missing after shuffle");
  }

  private record TaggedChoice(boolean correct, String text) {}
}

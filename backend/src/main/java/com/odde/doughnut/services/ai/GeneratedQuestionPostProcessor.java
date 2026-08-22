package com.odde.doughnut.services.ai;

import com.odde.doughnut.entities.Mcq;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.ArrayList;
import java.util.List;
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

    List<IndexedChoice> choices = indexedChoices(original.getResponseChoices());
    if (original.isChoicesMayBeShuffled()) {
      choices = testabilitySettings.getRandomizer().shuffle(choices);
    }

    Mcq mcq = new Mcq();
    mcq.setNote(note);
    mcq.setQuestionStem(original.getQuestionStem());
    mcq.setResponseChoices(choices.stream().map(IndexedChoice::choice).toList());
    mcq.setCorrectAnswerIndex(newCorrectIndex(choices, original.getCorrectAnswerIndex()));
    mcq.setContextSeed(contextSeed);
    mcq.setTestedFocus(original.getTestedFocus());
    mcq.setValidationRationale(original.getValidationRationale());
    return mcq;
  }

  private List<IndexedChoice> indexedChoices(List<String> choices) {
    List<IndexedChoice> indexedChoices = new ArrayList<>();
    for (int index = 0; index < choices.size(); index++) {
      indexedChoices.add(new IndexedChoice(index, choices.get(index)));
    }
    return indexedChoices;
  }

  private int newCorrectIndex(List<IndexedChoice> choices, int originalCorrectIndex) {
    for (int index = 0; index < choices.size(); index++) {
      if (choices.get(index).originalIndex() == originalCorrectIndex) {
        return index;
      }
    }
    throw new IllegalArgumentException("correct choice index missing after shuffle");
  }

  private record IndexedChoice(int originalIndex, String choice) {}
}

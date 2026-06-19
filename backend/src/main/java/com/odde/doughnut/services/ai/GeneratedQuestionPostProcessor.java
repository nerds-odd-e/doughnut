package com.odde.doughnut.services.ai;

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

  public MCQWithAnswer postProcess(MCQWithAnswer original) {
    if (original == null || !original.isChoicesMayBeShuffled()) {
      return original;
    }

    List<IndexedChoice> indexedChoices =
        indexedChoices(original.getQuestion().getResponseChoices());
    List<IndexedChoice> shuffledChoices =
        testabilitySettings.getRandomizer().shuffle(indexedChoices);
    int newCorrectIndex = newCorrectIndex(shuffledChoices, original.getSolutionChoiceIndex());

    MultipleChoicesQuestion shuffledQuestion =
        new MultipleChoicesQuestion(
            original.getQuestion().getQuestionStem(),
            shuffledChoices.stream().map(IndexedChoice::choice).toList());

    return new MCQWithAnswer(
        shuffledQuestion,
        newCorrectIndex,
        true,
        original.getTestedFocus(),
        original.getValidationRationale());
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

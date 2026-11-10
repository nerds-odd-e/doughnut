package com.odde.donut.controllers;

import com.odde.donut.entities.Mcq;
import java.util.List;
import java.util.Map;

final class RecentNoteQuestionsMarkdownAppendix {
  private RecentNoteQuestionsMarkdownAppendix() {}

  static String render(List<Mcq> questions, Map<Integer, Integer> latestChoiceIndexByMcqId) {
    if (questions.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("\n\n## Recent multiple-choice questions\n");
    int number = 1;
    for (Mcq mcq : questions) {
      sb.append('\n').append(number++).append(". ").append(mcq.getQuestionStem()).append('\n');
      for (String choice : mcq.getResponseChoices()) {
        sb.append("   - ").append(choice).append('\n');
      }
      sb.append("   Donut answer key: ")
          .append(mcq.getResponseChoices().get(mcq.getCorrectAnswerIndex()))
          .append('\n');
      Integer choiceIndex = latestChoiceIndexByMcqId.get(mcq.getId());
      if (choiceIndex != null) {
        sb.append("   Learner's selected answer: ")
            .append(mcq.getResponseChoices().get(choiceIndex))
            .append('\n');
      }
    }
    return sb.toString();
  }
}

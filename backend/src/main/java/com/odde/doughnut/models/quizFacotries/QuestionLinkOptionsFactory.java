package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.models.Randomizer;
import java.util.List;
import java.util.stream.Collectors;

public interface QuestionLinkOptionsFactory {
  Link generateAnswer();

  List<Link> generateFillingOptions();

  default List<Link> generateOptions1() {
    Link answerNote = generateAnswer();
    if (answerNote == null) return null;
    List<Link> fillingOptions = generateFillingOptions();
    if (fillingOptions.isEmpty()) {
      return null;
    }
    fillingOptions.add(answerNote);
    return fillingOptions;
  }

  default String generateOptions(Randomizer randomizer) {
    List<Link> options = generateOptions1();
    if (options == null) return null;
    return randomizer.shuffle(options).stream()
        .map(Link::getId)
        .map(Object::toString)
        .collect(Collectors.joining(","));
  }
}

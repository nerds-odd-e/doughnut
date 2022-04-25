package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.models.Randomizer;
import java.util.List;
import java.util.stream.Collectors;

public interface QuestionOptionsFactory {
  Note generateAnswer();

  List<Note> generateFillingOptions();

  default String generateOptions(Randomizer randomizer) {
    List<Note> options = null;
    Note answerNote = generateAnswer();
    if (answerNote != null) {
      List<Note> fillingOptions = generateFillingOptions();
      if (!fillingOptions.isEmpty()) {
        fillingOptions.add(answerNote);
        options = fillingOptions;
      }
    }
    if (options == null) return null;
    return randomizer.shuffle(options).stream()
        .map(Note::getId)
        .map(Object::toString)
        .collect(Collectors.joining(","));
  }
}

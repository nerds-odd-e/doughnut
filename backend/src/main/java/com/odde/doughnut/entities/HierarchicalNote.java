package com.odde.doughnut.entities;

import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.services.QuestionType;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "hierarchical_note")
@PrimaryKeyJoinColumn(name = "note_id")
public class HierarchicalNote extends Note {
  private HierarchicalNote() {}

  public static Note createNote(
      User user, Note parentNote, Timestamp currentUTCTimestamp, String topicConstructor) {
    final Note note = new HierarchicalNote();
    note.initialize(user, parentNote, currentUTCTimestamp, topicConstructor);
    return note;
  }

  @Override
  public List<QuizQuestionFactory> getQuizQuestionFactories() {
    List<QuestionType> result =
        List.of(
            QuestionType.SPELLING,
            QuestionType.CLOZE_SELECTION,
            QuestionType.PICTURE_TITLE,
            QuestionType.PICTURE_SELECTION);
    return result.stream().map(t -> t.getQuizQuestionFactory(this)).toList();
  }
}

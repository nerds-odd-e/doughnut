package com.odde.doughnut.entities;

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
  public List<QuizQuestionEntity.QuestionType> getAvailableQuestionTypes(
      Boolean aiQuestionTypeOnlyForReview) {
    if (aiQuestionTypeOnlyForReview) {
      return List.of(QuizQuestionEntity.QuestionType.AI_QUESTION);
    }
    return List.of(
        QuizQuestionEntity.QuestionType.SPELLING,
        QuizQuestionEntity.QuestionType.CLOZE_SELECTION,
        QuizQuestionEntity.QuestionType.PICTURE_TITLE,
        QuizQuestionEntity.QuestionType.PICTURE_SELECTION);
  }
}

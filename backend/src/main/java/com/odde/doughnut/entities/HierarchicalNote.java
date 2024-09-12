package com.odde.doughnut.entities;

import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionServant;
import com.odde.doughnut.factoryServices.quizFacotries.factories.*;
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
  public List<PredefinedQuestionFactory> getPredefinedQuestionFactories(
      PredefinedQuestionServant servant) {
    return List.of(
        new SpellingPredefinedFactory(this),
        new ClozeTitleSelectionPredefinedFactory(this, servant),
        new ImageTitleSelectionPredefinedFactory(this, servant));
  }
}

package com.odde.doughnut.testability.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Setter;

@Data
public class PredefinedQuestionsTestData {
  private String notebookName;
  private List<PredefinedQuestionTestData> predefinedQuestionTestData;

  @Setter
  static class PredefinedQuestionTestData {
    @JsonProperty("Note Title")
    private String noteTitle;

    @JsonProperty("Question")
    private String question;

    @JsonProperty("Answer")
    private String answer;

    @JsonProperty("One Wrong Choice")
    private String oneWrongChoice;

    public PredefinedQuestion buildPredefinedQuestion(Note note) {
      MultipleChoicesQuestion mcq = new MultipleChoicesQuestion();
      mcq.setQuestionStem(question);
      mcq.setResponseChoices(List.of(answer, oneWrongChoice));
      PredefinedQuestion predefinedQuestion = new PredefinedQuestion();
      predefinedQuestion.setNote(note);
      predefinedQuestion.setCorrectAnswerIndex(0);
      predefinedQuestion.setMultipleChoicesQuestion(mcq);
      return predefinedQuestion;
    }
  }

  public List<PredefinedQuestion> buildPredefinedQuestions(NoteRepository noteRepository) {
    return predefinedQuestionTestData.stream()
        .map(
            row -> {
              Note note = noteRepository.findFirstInNotebookByName(notebookName, row.noteTitle);
              if (note == null) {
                throw new IllegalArgumentException(
                    "No note with title `"
                        + row.noteTitle
                        + "` in notebook named `"
                        + notebookName
                        + "`");
              }
              return row.buildPredefinedQuestion(note);
            })
        .collect(Collectors.toList());
  }
}

package com.odde.doughnut.testability.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.entities.Mcq;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Setter;

@Data
public class McqsTestData {
  private String notebookName;
  private List<McqTestData> mcqTestData;

  @Setter
  static class McqTestData {
    @JsonProperty("Note Title")
    private String noteTitle;

    @JsonProperty("Question")
    private String question;

    @JsonProperty("Answer")
    private String answer;

    @JsonProperty("One Wrong Choice")
    private String oneWrongChoice;

    public Mcq buildMcq(Note note) {
      MultipleChoicesQuestion multipleChoicesQuestion = new MultipleChoicesQuestion();
      multipleChoicesQuestion.setQuestionStem(question);
      multipleChoicesQuestion.setResponseChoices(List.of(answer, oneWrongChoice));
      Mcq mcq = new Mcq();
      mcq.setNote(note);
      mcq.setCorrectAnswerIndex(0);
      mcq.setMultipleChoicesQuestion(multipleChoicesQuestion);
      return mcq;
    }
  }

  public List<Mcq> buildMcqs(NoteRepository noteRepository) {
    return mcqTestData.stream()
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
              return row.buildMcq(note);
            })
        .collect(Collectors.toList());
  }
}

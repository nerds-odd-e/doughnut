package com.odde.donut.testability.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.NoteRepository;
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
      Mcq mcq = new Mcq();
      mcq.setNote(note);
      mcq.setQuestionStem(question);
      mcq.setResponseChoices(List.of(answer, oneWrongChoice));
      mcq.setCorrectAnswerIndex(0);
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

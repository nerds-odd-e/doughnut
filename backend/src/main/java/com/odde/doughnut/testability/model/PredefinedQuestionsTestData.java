package com.odde.doughnut.testability.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Setter;

@Data
public class PredefinedQuestionsTestData {
  private String notebookTitle;
  private Boolean notebookCertifiable;
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

    @JsonProperty("Approved")
    private boolean approved;

    public PredefinedQuestion buildPredefinedQuestion(Note note) {
      MultipleChoicesQuestion mcq = new MultipleChoicesQuestion();
      mcq.setStem(question);
      mcq.setChoices(List.of(answer, oneWrongChoice));
      PredefinedQuestion question = new PredefinedQuestion();
      question.setNote(note);
      question.setCorrectAnswerIndex(0);
      question.setMultipleChoicesQuestion(mcq);
      question.setApproved(approved);
      return question;
    }
  }

  public List<PredefinedQuestion> buildPredefinedQuestions(ModelFactoryService factoryService) {
    return predefinedQuestionTestData.stream()
        .map(
            question ->
                question.buildPredefinedQuestion(
                    factoryService.noteRepository.findFirstInNotebookByTitle(
                        notebookTitle, question.noteTitle)))
        .collect(Collectors.toList());
  }
}

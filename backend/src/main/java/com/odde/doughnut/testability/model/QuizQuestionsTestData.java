package com.odde.doughnut.testability.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionAndAnswer;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Setter;

@Data
public class QuizQuestionsTestData {
  private String notebookTitle;
  private Boolean notebookCertifiable;
  private List<QuizQuestionTestData> quizQuestionTestData;

  @Setter
  static class QuizQuestionTestData {
    @JsonProperty("Note Topic")
    private String noteTopic;

    @JsonProperty("Question")
    private String question;

    @JsonProperty("Answer")
    private String answer;

    @JsonProperty("One Wrong Choice")
    private String oneWrongChoice;

    @JsonProperty("Approved")
    private boolean approved;

    public QuizQuestionAndAnswer buildQuizQuestion(Note note) {
      MultipleChoicesQuestion mcq = new MultipleChoicesQuestion();
      mcq.setStem(question);
      mcq.setChoices(List.of(answer, oneWrongChoice));
      QuizQuestionAndAnswer question = new QuizQuestionAndAnswer();
      question.setNote(note);
      question.setCorrectAnswerIndex(0);
      question.setMultipleChoicesQuestion(mcq);
      question.setApproved(approved);
      question.getQuizQuestion().setQuizQuestionAndAnswer(question);
      return question;
    }
  }

  public List<QuizQuestionAndAnswer> buildQuizQuestions(ModelFactoryService factoryService) {
    return quizQuestionTestData.stream()
        .map(
            question ->
                question.buildQuizQuestion(
                    factoryService.noteRepository.findFirstInNotebookByTopicConstructor(
                        notebookTitle, question.noteTopic)))
        .collect(Collectors.toList());
  }
}

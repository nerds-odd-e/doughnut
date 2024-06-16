package com.odde.doughnut.testability.model;

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
  private List<QuizQuestionTestData> quizQuestionTestData;

  @Setter
  static class QuizQuestionTestData {
    private String noteTopic;
    private String question;
    private String answer;
    private String oneWrongChoice;
    private boolean approved;

    public QuizQuestionAndAnswer buildQuizQuestion(Note note) {
      MultipleChoicesQuestion mcq = new MultipleChoicesQuestion();
      mcq.setStem(question);
      mcq.setChoices(List.of(answer, oneWrongChoice));
      QuizQuestionAndAnswer question = new QuizQuestionAndAnswer();
      question.setNote(note);
      question.setCorrectAnswerIndex(0);
      question.getQuizQuestion().setMultipleChoicesQuestion(mcq);
      question.setApproved(approved);
      return question;
    }
  }

  public List<QuizQuestionAndAnswer> buildQuizQuestions(ModelFactoryService factoryService) {
    return quizQuestionTestData.stream()
        .map(
            question ->
                question.buildQuizQuestion(
                    factoryService.noteRepository.findFirstByTopicConstructor(question.noteTopic)))
        .collect(Collectors.toList());
  }
}

package com.odde.doughnut.testability.model;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
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

    public QuizQuestion buildQuizQuestion(Note note) {
      MultipleChoicesQuestion mcq = new MultipleChoicesQuestion();
      mcq.setStem(question);
      mcq.setChoices(List.of(answer, oneWrongChoice));
      QuizQuestion question = new QuizQuestion();
      question.setNote(note);
      question.setCorrectAnswerIndex(0);
      question.setMultipleChoicesQuestion(mcq);
      return question;
    }
  }

  public List<QuizQuestion> buildQuizQuestions(ModelFactoryService factoryService) {
    return quizQuestionTestData.stream()
        .map(
            question ->
                question.buildQuizQuestion(
                    factoryService.noteRepository.findFirstByTopicConstructor(question.noteTopic)))
        .collect(Collectors.toList());
  }
}

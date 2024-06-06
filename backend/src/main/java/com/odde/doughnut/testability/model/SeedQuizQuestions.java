package com.odde.doughnut.testability.model;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Setter;

@Data
public class SeedQuizQuestions {
  // initialize a variable of noteTitleNotesMap<notebook title, notes>
  // private function to fetch the data from database and build the noteTitleNotesMap
  // tbc, to check if there are at least 5 questions from different 5 notes
  //
  private List<SeedQuizQuestion> seedQuizQuestions;
  private String externalIdentifier;
  private String circleName;

  @Setter
  static class SeedQuizQuestion {
    private String topicConstructor;
    private String question;
    private String answer;
    private String option;

    public QuizQuestion buildQuizQuestion(Note note) {
      MultipleChoicesQuestion mcq = new MultipleChoicesQuestion();
      mcq.setStem(question);
      mcq.setChoices(List.of(answer, option));
      QuizQuestion question = new QuizQuestion();
      question.setNote(note);
      question.setCorrectAnswerIndex(0);
      question.setMultipleChoicesQuestion(mcq);
      return question;
    }
  }

  public Map<String, QuizQuestion> buildQuizQuestions(ModelFactoryService factoryService) {
    return seedQuizQuestions.stream()
        .map(question -> question.buildQuizQuestion(factoryService.noteRepository.findFirstByTopicConstructor(question.topicConstructor)))
        .collect(Collectors.toMap(question -> question.getMultipleChoicesQuestion().stem, n -> n));
  }
}

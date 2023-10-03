package com.odde.doughnut.controllers.json;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionSuggestionForFineTuningRepository;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class QuestionSuggestion {
  public String comment;
  public MCQWithAnswer mcqWithAnswer;

  public SuggestedQuestionForFineTuning createQuestionSuggestionForFineTuning(
      QuizQuestionEntity quizQuestionEntity,
      User user,
      Timestamp currentUTCTimestamp,
      QuestionSuggestionForFineTuningRepository questionSuggestionForFineTuningRepository) {
    SuggestedQuestionForFineTuning suggestedQuestionForFineTuning =
        new SuggestedQuestionForFineTuning();
    suggestedQuestionForFineTuning.setUser(user);
    suggestedQuestionForFineTuning.setQuizQuestion(quizQuestionEntity);
    suggestedQuestionForFineTuning.setComment(comment);
    suggestedQuestionForFineTuning.setCreatedAt(currentUTCTimestamp);
    suggestedQuestionForFineTuning.setPreservedQuestion(mcqWithAnswer.toJsonString());

    return questionSuggestionForFineTuningRepository.save(suggestedQuestionForFineTuning);
  }
}

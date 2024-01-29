package com.odde.doughnut.controllers.json;

import com.odde.doughnut.entities.PictureWithMask;
import com.odde.doughnut.entities.QuizQuestionEntity;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.springframework.lang.Nullable;

@AllArgsConstructor
public class QuizQuestion {
  @Getter public Integer quizQuestionId;

  @Getter public QuizQuestionEntity.QuestionType questionType;

  @Getter public String stem;

  @Getter public String mainTopic;

  @Getter @Nullable public NotebookViewedByUser notebook;

  @Getter public List<Choice> choices;

  @Getter public Optional<PictureWithMask> pictureWithMask;

  @Data
  public static class Choice {
    private boolean isPicture = false;
    private String display;
    @Nullable private PictureWithMask pictureWithMask;
  }
}

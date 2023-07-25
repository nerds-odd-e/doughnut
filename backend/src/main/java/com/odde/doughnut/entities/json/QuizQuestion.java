package com.odde.doughnut.entities.json;

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

  @Getter public String description;

  @Getter public String mainTopic;

  @Getter public LinksOfANote hintLinks;

  @Getter @Nullable public NotePositionViewedByUser notebookPosition;

  @Getter public List<Option> options;

  @Getter public Optional<PictureWithMask> pictureWithMask;

  @Data
  public static class Option {
    private boolean isPicture = false;
    private String display;
    @Nullable private PictureWithMask pictureWithMask;
  }
}

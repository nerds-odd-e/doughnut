package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.PictureWithMask;
import com.odde.doughnut.entities.QuizQuestionEntity;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;

@AllArgsConstructor
public class QuizQuestion {
  @Getter public Integer quizQuestionId;

  @Getter public QuizQuestionEntity.QuestionType questionType;

  @Getter public String stem;

  @Getter public String mainTopic;

  @Getter @Nullable public NotePositionViewedByUser notebookPosition;

  @Getter public List<Choice> choices;

  @Getter public Optional<PictureWithMask> pictureWithMask;

  @Data
  public static class Choice {
    private boolean isPicture = false;
    private String display;
    @Nullable private PictureWithMask pictureWithMask;
    private String reason;

    @NotNull
    public Choice getChoice(Pair<String, String> pair) {
      setDisplay(pair.getFirst());
      setReason(pair.getSecond());
      return this;
    }
  }
}

package com.odde.doughnut.controllers.json;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PictureWithMask;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@AllArgsConstructor
public class QuizQuestion {
  @Getter public Integer id;

  @Getter public String stem;

  @Getter public String mainTopic;

  @Getter public Note headNote;

  @Getter public List<Choice> choices;

  @Getter public Optional<PictureWithMask> pictureWithMask;

  @Data
  public static class Choice {
    private boolean isPicture = false;
    private String display;
    private PictureWithMask pictureWithMask;
  }
}

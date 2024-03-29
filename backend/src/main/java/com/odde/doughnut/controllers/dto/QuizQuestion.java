package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PictureWithMask;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@AllArgsConstructor
public class QuizQuestion {
  @NotNull @Getter public Integer id;

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

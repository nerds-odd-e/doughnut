package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.ImageWithMask;
import com.odde.doughnut.entities.Note;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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

  @Getter public ImageWithMask imageWithMask;

  @Data
  public static class Choice {
    private boolean isImage = false;
    private String display;
    private ImageWithMask imageWithMask;
  }
}

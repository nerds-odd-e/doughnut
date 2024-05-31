package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.ImageWithMask;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class QuizQuestion {
  @NotNull @Getter public Integer id;

  @Getter public String stem;

  @NotNull @Getter public Note headNote;

  @Getter public List<QuizQuestionEntity.Choice> choices;

  @Getter public ImageWithMask imageWithMask;
}

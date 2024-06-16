package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.ImageWithMask;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuizQuestionInNotebook {
  @NotNull private Integer id;
  @NotNull private MultipleChoicesQuestion multipleChoicesQuestion;
  private ImageWithMask imageWithMask;
  @NotNull private Notebook notebook;
}

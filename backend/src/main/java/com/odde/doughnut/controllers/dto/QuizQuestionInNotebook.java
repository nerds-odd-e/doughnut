package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.QuizQuestion1;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuizQuestionInNotebook {
  @NotNull private Notebook notebook;
  @NotNull private QuizQuestion1 quizQuestion;
}

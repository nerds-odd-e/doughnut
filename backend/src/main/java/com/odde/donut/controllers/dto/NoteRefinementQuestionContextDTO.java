package com.odde.donut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteRefinementQuestionContextDTO {
  @Schema(description = "MCQ question stem (markdown allowed)")
  private String stem;

  @Schema(description = "MCQ response choices (markdown allowed)")
  private List<String> choices;

  @Schema(description = "Zero-based index of the correct choice, when known")
  private Integer correctAnswerIndex;

  @Schema(description = "Optional description of what the question was testing")
  private String testedFocus;
}

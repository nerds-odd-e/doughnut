package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.entities.converters.MCQToJsonConverter;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Embeddable
@Data
@JsonPropertyOrder({"checkSpell", "imageWithMask", "multipleChoicesQuestion"})
public class QuizQuestion1 {
  @Column(name = "raw_json_question")
  @Convert(converter = MCQToJsonConverter.class)
  @NotNull
  private MultipleChoicesQuestion multipleChoicesQuestion;

  @Column(name = "check_spell")
  private Boolean checkSpell;

  @Embedded ImageWithMask imageWithMask;
}

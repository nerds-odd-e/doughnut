package com.odde.doughnut.entities;

import com.odde.doughnut.entities.converters.MCQToJsonConverter;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Embeddable
@Data
public class BareQuestion {
  @Column(name = "raw_json_question")
  @Convert(converter = MCQToJsonConverter.class)
  @NotNull
  private MultipleChoicesQuestion multipleChoicesQuestion;
}

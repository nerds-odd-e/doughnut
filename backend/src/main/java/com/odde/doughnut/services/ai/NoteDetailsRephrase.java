package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@JsonClassDescription(
    "Rephrase the note details to remove the specified point while preserving the overall meaning and flow of the content.")
@Data
public class NoteDetailsRephrase {
  @NotNull
  @JsonPropertyDescription("The rephrased note details with the specified point removed.")
  @JsonProperty(required = true)
  public String details;
}

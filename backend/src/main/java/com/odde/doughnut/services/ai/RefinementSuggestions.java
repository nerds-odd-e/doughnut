package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@JsonClassDescription(
    "Generate refinement suggestions for the note content: decompose the note into key points that could be removed, extracted to a new note, or otherwise refined to make the note more succinct and well-structured.")
@Data
public class RefinementSuggestions {
  @NotNull
  @JsonPropertyDescription(
      "A list of refinement suggestions, each being a complete sentence that captures a distinct aspect of the note content worth refining.")
  @JsonProperty(required = true)
  public List<String> suggestions;
}

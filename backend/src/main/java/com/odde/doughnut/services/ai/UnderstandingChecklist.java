package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@JsonClassDescription(
    "Generate an understanding checklist of the note details broken down into key points. Each point should be a complete sentence that captures an important aspect of the note content. The checklist should help the user check whether they truly understood the important points in the note.")
@Data
public class UnderstandingChecklist {
  @NotNull
  @JsonPropertyDescription(
      "A list of understanding points, each point being a complete sentence that captures a key aspect of the note content.")
  @JsonProperty(required = true)
  public List<String> points;
}

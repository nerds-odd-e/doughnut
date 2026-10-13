package com.odde.donut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@JsonClassDescription(
    "Propose to the learner that the answered attempt under discussion be excluded from evaluation because the wording of the original question does not support a unique answer. Use it only when the learner's selected answer is also defensible under a reasonable reading of the question as written. Do not use it when the question or its source fixes one meaning and the selected choice means something else. Calling this changes nothing: the learner reviews the proposal and confirms or declines it.")
@Data
public class AmbiguousQuestionExclusionProposal {
  @NotNull
  @JsonPropertyDescription(
      "Why the wording does not support a unique answer: the defensible interpretation of the learner's selected answer, based on the original question, the selected answer, and the available evidence. Written for the learner to read.")
  @JsonProperty(required = true)
  public String reason;
}

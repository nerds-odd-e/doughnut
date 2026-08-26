package com.odde.donut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonClassDescription("Ask a single-answer multiple-choice question to the user")
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedMcq {

  @JsonPropertyDescription(
      "The question stem — the full, self-contained text of the prompt. Markdown allowed. Must not reference external context.")
  @JsonProperty(required = true)
  private String questionStem;

  @JsonPropertyDescription("The one correct answer. Markdown allowed.")
  @JsonProperty(required = true)
  private String correctAnswer;

  @JsonPropertyDescription(
      "Exactly three plausible but clearly incorrect answers. Each must be independent and safe to reorder. Markdown allowed.")
  @JsonProperty(required = true)
  private List<String> distractors;

  @JsonPropertyDescription(
      "Internal summary of the specific focus-note knowledge point tested. Up to a few sentences, at most 50 words. Not shown to the learner.")
  private String testedFocus;

  @JsonPropertyDescription(
      "Internal explanation of why the solution choice is uniquely correct and the other choices are incorrect. Note ambiguity if any. Up to a few sentences, at most 100 words. Not shown to the learner.")
  private String validationRationale;

  @JsonIgnore
  public boolean isValid() {
    if (questionStem == null || questionStem.isBlank()) return false;
    if (correctAnswer == null || correctAnswer.isBlank()) return false;
    if (distractors == null || distractors.size() != 3) return false;

    Set<String> choiceIdentities = new HashSet<>();
    choiceIdentities.add(correctAnswer.strip());
    for (String distractor : distractors) {
      if (distractor == null || distractor.isBlank()) return false;
      if (!choiceIdentities.add(distractor.strip())) return false;
    }
    return true;
  }
}

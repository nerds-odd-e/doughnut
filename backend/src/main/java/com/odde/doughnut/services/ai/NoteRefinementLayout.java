package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonClassDescription(
    "A single current-content layout of the Focus Note, with at most top-level items and one level of child items. Items must cover Focus Note content only, not Retrieved Notes. Do not return alternative layouts or suggestions.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteRefinementLayout {
  @Valid
  @NotNull
  @JsonProperty(required = true)
  @JsonPropertyDescription(
      "The one layout for the Focus Note content only. Items may have children, but children must not have children. Return an empty array for blank or unavailable content.")
  public List<NoteRefinementLayoutItem> items = List.of();

  public static NoteRefinementLayout empty() {
    return new NoteRefinementLayout(List.of());
  }
}

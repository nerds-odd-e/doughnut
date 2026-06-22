package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteRefinementLayoutItem {
  @NotBlank
  @JsonProperty(required = true)
  @JsonPropertyDescription(
      "Stable unique id for this layout item. Must be unique across the entire layout.")
  public String id;

  @NotBlank
  @JsonProperty(required = true)
  @JsonPropertyDescription(
      "The Focus Note content represented by this layout item. Do not represent content that appears only in Retrieved Notes.")
  public String text;

  @JsonProperty(required = true)
  @JsonPropertyDescription(
      "True only when this item represents a simple standalone wiki-link-only line that has already been extracted, such as [[Target note]] or [[Target note|Label]]. These items should be shown as Already extracted but remain selectable.")
  public boolean alreadyExtracted;

  @Valid
  @NotNull
  @JsonProperty(required = true)
  @JsonPropertyDescription(
      "Child layout items. Only top-level items may have children; child items must have an empty children array. Grandchildren are prohibited.")
  public List<NoteRefinementLayoutItem> children = List.of();
}

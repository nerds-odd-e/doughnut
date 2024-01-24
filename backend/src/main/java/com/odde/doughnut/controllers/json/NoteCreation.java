package com.odde.doughnut.controllers.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.Link.LinkType;
import com.odde.doughnut.entities.TextContent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

public class NoteCreation {
  @Getter @Setter @NotNull public LinkType linkTypeToParent;
  @Getter @Setter @Valid @NotNull private TextContent textContent = new TextContent();

  @JsonIgnore
  public String getTopicConstructor() {
    return textContent.getTopic();
  }

  @Getter
  @Setter
  @Nullable
  @Pattern(regexp = "^$|Q\\d+", message = "The wikidata Id should be Q<numbers>")
  public String wikidataId;
}

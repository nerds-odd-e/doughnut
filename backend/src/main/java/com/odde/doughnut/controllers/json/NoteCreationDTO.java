package com.odde.doughnut.controllers.json;

import com.odde.doughnut.entities.Link.LinkType;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

public class NoteCreationDTO extends NoteUpdateTopicDTO {
  @Getter @Setter @Nullable public LinkType linkTypeToParent;

  @Getter
  @Setter
  @Nullable
  @Pattern(regexp = "^$|Q\\d+", message = "The wikidata Id should be Q<numbers>")
  public String wikidataId;
}

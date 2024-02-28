package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.LinkType;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

public class NoteCreationDTO extends NoteUpdateTopicDTO {
  @Getter @Setter public LinkType linkTypeToParent;

  @Getter
  @Setter
  @Pattern(regexp = "^$|Q\\d+", message = "The wikidata Id should be Q<numbers>")
  public String wikidataId;
}

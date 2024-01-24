package com.odde.doughnut.controllers.json;

import com.odde.doughnut.entities.Link.LinkType;
import com.odde.doughnut.entities.Note;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

public class NoteCreation {
  @Getter @Setter @Nullable public LinkType linkTypeToParent;

  @Size(min = 1, max = Note.MAX_TITLE_LENGTH)
  @Getter
  @Setter
  private String topicConstructor = "";

  @Getter
  @Setter
  @Nullable
  @Pattern(regexp = "^$|Q\\d+", message = "The wikidata Id should be Q<numbers>")
  public String wikidataId;
}

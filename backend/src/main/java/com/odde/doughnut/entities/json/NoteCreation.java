package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Link.LinkType;
import com.odde.doughnut.entities.TextContent;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

public class NoteCreation {
  @Getter @Setter @NotNull public LinkType linkTypeToParent;
  @Getter @Setter @Valid @NotNull public TextContent textContent = new TextContent();
  @Getter @Setter @Nullable public String wikidataId;
}

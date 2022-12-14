package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Link;
import jakarta.validation.constraints.NotNull;

public class LinkCreation {
  @NotNull public Link.LinkType linkType;
  public Boolean fromTargetPerspective = false;
  public Boolean moveUnder;
  public Boolean asFirstChild = false;
}

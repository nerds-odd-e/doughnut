package com.odde.doughnut.controllers.json;

import com.odde.doughnut.entities.Link;
import javax.validation.constraints.NotNull;

public class LinkCreation {
  @NotNull public Link.LinkType linkType;
  public Boolean fromTargetPerspective = false;
  public Boolean moveUnder;
  public Boolean asFirstChild = false;
}

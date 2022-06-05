package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Link;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public class LinksOfANote {
  @Getter @Setter private Map<Link.LinkType, LinkViewed> links;
}

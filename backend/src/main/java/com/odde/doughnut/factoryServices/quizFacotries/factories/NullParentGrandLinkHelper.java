package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Thing;
import java.util.List;

public class NullParentGrandLinkHelper implements ParentGrandLinkHelper {

  @Override
  public Thing getParentGrandLink() {
    return null;
  }

  @Override
  public List<Link> getCousinLinksAvoidingSiblings() {
    return List.of();
  }
}

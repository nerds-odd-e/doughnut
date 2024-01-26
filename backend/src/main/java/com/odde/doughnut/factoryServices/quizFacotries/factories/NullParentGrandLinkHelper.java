package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Link;
import java.util.stream.Stream;

public class NullParentGrandLinkHelper implements ParentGrandLinkHelper {

  @Override
  public Link getParentGrandLink() {
    return null;
  }

  @Override
  public Stream<Link> getCousinLinksAvoidingSiblings() {
    return Stream.empty();
  }
}

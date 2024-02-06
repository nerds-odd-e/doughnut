package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.LinkingNote;
import java.util.List;

public class NullParentGrandLinkHelper implements ParentGrandLinkHelper {

  @Override
  public LinkingNote getParentGrandLink() {
    return null;
  }

  @Override
  public List<LinkingNote> getCousinLinksAvoidingSiblings() {
    return List.of();
  }
}

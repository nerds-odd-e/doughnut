package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.Note;
import java.util.List;

public class NullParentGrandLinkHelper implements ParentGrandLinkHelper {

  @Override
  public Note getParentGrandLink() {
    return null;
  }

  @Override
  public List<LinkingNote> getCousinLinksAvoidingSiblings() {
    return List.of();
  }
}

package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.LinkingNote;
import java.util.List;

public interface ParentGrandLinkHelper {

  LinkingNote getParentGrandLink();

  List<LinkingNote> getCousinLinksAvoidingSiblings();
}

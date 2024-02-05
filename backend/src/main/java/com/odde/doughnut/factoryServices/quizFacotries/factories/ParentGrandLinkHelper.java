package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.Note;
import java.util.List;

public interface ParentGrandLinkHelper {

  Note getParentGrandLink();

  List<LinkingNote> getCousinLinksAvoidingSiblings();
}

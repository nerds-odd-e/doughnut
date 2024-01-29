package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Thing;
import java.util.List;

public interface ParentGrandLinkHelper {

  Thing getParentGrandLink();

  List<Link> getCousinLinksAvoidingSiblings();
}

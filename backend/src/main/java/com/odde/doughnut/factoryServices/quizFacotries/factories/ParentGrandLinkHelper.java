package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Link;
import java.util.List;

public interface ParentGrandLinkHelper {

  Link getParentGrandLink();

  List<Link> getCousinLinksAvoidingSiblings();
}

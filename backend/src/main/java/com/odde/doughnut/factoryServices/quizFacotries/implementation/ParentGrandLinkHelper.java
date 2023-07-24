package com.odde.doughnut.factoryServices.quizFacotries.implementation;

import com.odde.doughnut.entities.Link;
import java.util.List;

public interface ParentGrandLinkHelper {

  Link getParentGrandLink();

  List<Link> getCousinLinksAvoidingSiblings();
}

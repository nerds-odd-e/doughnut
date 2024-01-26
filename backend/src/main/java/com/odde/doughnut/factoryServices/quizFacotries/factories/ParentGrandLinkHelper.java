package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Link;
import java.util.stream.Stream;

public interface ParentGrandLinkHelper {

  Link getParentGrandLink();

  Stream<Link> getCousinLinksAvoidingSiblings();
}

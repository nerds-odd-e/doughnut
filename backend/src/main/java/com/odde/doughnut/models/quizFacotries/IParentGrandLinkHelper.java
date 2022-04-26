package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import java.util.List;

public interface IParentGrandLinkHelper {

  Link getParentGrandLink();

  List<Link> getCousinLinksAvoidingSiblings();
}

package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.ReviewPoint;
import java.util.List;

public interface SecondaryReviewPointsFactory {
  List<ReviewPoint> getViceReviewPoints();

  Link getCategoryLink();
}

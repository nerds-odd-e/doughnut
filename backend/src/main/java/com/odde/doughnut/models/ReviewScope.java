package com.odde.doughnut.models;

import com.odde.doughnut.entities.Thing;
import java.util.stream.Stream;

public interface ReviewScope {
  int getThingsHaveNotBeenReviewedAtAllCount();

  Stream<Thing> getThingHaveNotBeenReviewedAtAll();
}

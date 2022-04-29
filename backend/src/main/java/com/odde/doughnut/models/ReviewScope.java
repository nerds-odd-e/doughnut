package com.odde.doughnut.models;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Thing;
import java.util.stream.Stream;

public interface ReviewScope {
  int getNotesHaveNotBeenReviewedAtAllCount();

  int getLinksHaveNotBeenReviewedAtAllCount();

  Stream<Thing> getThingHaveNotBeenReviewedAtAll();
}

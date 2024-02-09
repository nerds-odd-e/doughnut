package com.odde.doughnut.models.randomizers;

import com.odde.doughnut.models.Randomizer;
import java.util.*;

public class RealRandomizer implements Randomizer {
  @Override
  public <T> List<T> shuffle(List<T> list) {
    List<T> newList = new ArrayList<>(list);
    Collections.shuffle(newList);
    return newList;
  }

  @Override
  public <T> Optional<T> chooseOneRandomly(List<T> list) {
    if (list.isEmpty()) {
      return Optional.empty();
    }
    Random rand = new Random();
    return Optional.of(list.get(rand.nextInt(list.size())));
  }
}

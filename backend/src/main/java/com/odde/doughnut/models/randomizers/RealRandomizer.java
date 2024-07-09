package com.odde.doughnut.models.randomizers;

import com.odde.doughnut.models.Randomizer;
import java.util.*;

public class RealRandomizer implements Randomizer {
  Random rand = new Random();

  @Override
  public <T> List<T> shuffle(List<T> list) {
    List<T> newList = new ArrayList<>(list);
    Collections.shuffle(newList, rand);
    return newList;
  }

  @Override
  public <T> Optional<T> chooseOneRandomly(List<T> list) {
    if (list.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(list.get(rand.nextInt(list.size())));
  }
}

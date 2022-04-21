package com.odde.doughnut.models.randomizers;

import com.odde.doughnut.models.Randomizer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class RealRandomizer implements Randomizer {
  @Override
  public <T> List<T> shuffle(List<T> list) {
    Collections.shuffle(list);
    return list;
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

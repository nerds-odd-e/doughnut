package com.odde.doughnut.models.randomizers;

import com.odde.doughnut.models.Randomizer;
import java.util.*;

public class RealRandomizer implements Randomizer {
  Random rand = new Random();

  public RealRandomizer() {}

  public RealRandomizer(Integer seed) {
    rand = new Random(seed);
  }

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

  @Override
  public int randomInteger(int min, int max) {
    return rand.nextInt(max - min + 1) + min;
  }
}

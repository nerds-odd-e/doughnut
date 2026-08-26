package com.odde.donut.utils.randomizers;

import com.odde.donut.controllers.dto.Randomization;
import com.odde.donut.utils.Randomizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.Setter;

public class NonRandomizer implements Randomizer {
  @Setter public Randomization.RandomStrategy alwaysChoose = Randomization.RandomStrategy.first;

  @Override
  public <T> List<T> shuffle(List<T> list) {
    if (alwaysChoose.equals(Randomization.RandomStrategy.last)) {
      List<T> newList = new ArrayList<>(list);
      Collections.reverse(newList);
      return newList;
    }
    return list;
  }

  @Override
  public <T> Optional<T> chooseOneRandomly(List<T> list) {
    if (list.isEmpty()) {
      return Optional.empty();
    }
    if (alwaysChoose.equals(Randomization.RandomStrategy.last)) {
      return Optional.of(list.get(list.size() - 1));
    }
    return Optional.of(list.get(0));
  }

  @Override
  public int randomInteger(int min, int max) {
    if (alwaysChoose.equals(Randomization.RandomStrategy.last)) {
      return max;
    }
    return min;
  }
}

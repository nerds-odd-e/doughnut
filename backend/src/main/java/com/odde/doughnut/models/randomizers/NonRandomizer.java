package com.odde.doughnut.models.randomizers;

import com.odde.doughnut.models.Randomizer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.Setter;

public class NonRandomizer implements Randomizer {
  @Setter public String alwaysChoose = "first";

  @Override
  public <T> List<T> shuffle(List<T> list) {
    if (alwaysChoose.equals("last")) {
      Collections.reverse(list);
    }
    return list;
  }

  @Override
  public <T> Optional<T> chooseOneRandomly(List<T> list) {
    if (list.isEmpty()) {
      return Optional.empty();
    }
    if (alwaysChoose.equals("last")) {
      return Optional.of(list.get(list.size() - 1));
    }
    return Optional.of(list.get(0));
  }
}

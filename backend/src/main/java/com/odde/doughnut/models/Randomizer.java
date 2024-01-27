package com.odde.doughnut.models;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface Randomizer {
  <T> List<T> shuffle(List<T> list);

  <T> Optional<T> chooseOneRandomly(List<T> list);

  default <T> Stream<T> randomlyChoose(int maxSize, List<T> list) {
    return shuffle(list).stream().limit(maxSize);
  }
}

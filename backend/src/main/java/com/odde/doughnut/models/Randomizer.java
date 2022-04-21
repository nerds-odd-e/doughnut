package com.odde.doughnut.models;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface Randomizer {
  <T> List<T> shuffle(List<T> list);

  <T> T chooseOneRandomly(List<T> list);

  default <T> Optional<T> chooseOneRandomly1(List<T> list) {
    return Optional.ofNullable(chooseOneRandomly(list));
  }

  default <T> List<T> randomlyChoose(int maxSize, List<T> list) {
    shuffle(list);
    return list.stream().limit(maxSize).collect(Collectors.toList());
  }
}

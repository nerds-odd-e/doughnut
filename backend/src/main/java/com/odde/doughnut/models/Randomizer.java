package com.odde.doughnut.models;

import java.util.List;
import java.util.Optional;

public interface Randomizer {
  <T> List<T> shuffle(List<T> list);

  <T> Optional<T> chooseOneRandomly(List<T> list);

  int randomInteger(int min, int max);
}

package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.util.Strings;

public class SpacedRepetitionAlgorithm {
  public static final List<Integer> DEFAULT_SPACES =
      Arrays.asList(
          0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765,
          10946, 17711, 28657, 46368, 75025);
  private final List<Integer> spaces;

  public SpacedRepetitionAlgorithm(String spaceIntervals) {
    if (!Strings.isEmpty(spaceIntervals)) {
      spaces =
          Arrays.stream(spaceIntervals.split(",\\s*"))
              .map(Integer::valueOf)
              .collect(Collectors.toList());
    } else {
      spaces = new ArrayList<>();
    }
  }

  public Integer getRepeatInHoursF(float index) {
    if (index < 0) {
      return 0;
    }
    final Integer floor = getSpacing((int) index);
    final Integer ceiling = getSpacing((int) index + 1);
    return (int) (floor * 24 + (ceiling - floor) * 24 * (index - (int) index));
  }

  private Integer getSpacing(int index) {
    if (index + 1 > spaces.size()) {
      return DEFAULT_SPACES.get(index);
    }
    return spaces.get(index);
  }
}

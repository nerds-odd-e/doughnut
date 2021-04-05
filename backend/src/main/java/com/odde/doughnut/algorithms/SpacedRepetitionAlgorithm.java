package com.odde.doughnut.algorithms;

import lombok.Getter;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SpacedRepetitionAlgorithm {
    public static final Integer DEFAULT_FORGETTING_CURVE_INDEX = 100;
    public static final Integer DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT = 10;
    public static final List<Integer> DEFAULT_SPACES = Arrays.asList(0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765, 10946, 17711, 28657, 46368, 75025);
    private final List<Integer> spaces;
    public SpacedRepetitionAlgorithm(String spaceIntervals) {
        if (!Strings.isEmpty(spaceIntervals)) {
            spaces = Arrays.stream(spaceIntervals.split(",\\s*")).map(Integer::valueOf).collect(Collectors.toList());
        }
        else {
            spaces = new ArrayList<>();
        }
    }

    public static class MemoryStateChange {
        @Getter
        private final int nextForgettingCurveIndex;
        @Getter
        private final int nextRepeatInDays;

        public MemoryStateChange(int nextForgettingCurveIndex, int nextRepeatInDays) {
            this.nextForgettingCurveIndex = nextForgettingCurveIndex;
            this.nextRepeatInDays = nextRepeatInDays;
        }
    }

    //
    // adjustment:
    //   -1: reduce the increase of index by 1/2
    //   +1: add to the increase of index by 1/2
    //
    public MemoryStateChange getMemoryStateChange(Integer oldForgettingCurveIndex, int adjustment) {
        final int nextForgettingCurveIndex = oldForgettingCurveIndex + DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT * (2 + adjustment) / 2;
        return new MemoryStateChange(nextForgettingCurveIndex, getRepeatInDays(nextForgettingCurveIndex));
    }

    private Integer getRepeatInDays(Integer forgettingCurveIndex) {
        int index = (forgettingCurveIndex - DEFAULT_FORGETTING_CURVE_INDEX) / DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT - 1;
        if (index < 0) {
            return 0;
        }
        final int remainder = forgettingCurveIndex % DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;
        final Integer floor = getSpacing(index);
        final Integer ceiling = getSpacing(index + 1);
        return floor + (ceiling - floor) * remainder / DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;
    }

    private Integer getSpacing(int index) {
        if (index + 1 > spaces.size()) {
           return DEFAULT_SPACES.get(index);
        }
        return spaces.get(index);
    }

}

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
    public static final List<Integer> DEFAULT_SPACES = Arrays.asList(0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765, 10946);
    private final List<Integer> spaces;
    public SpacedRepetitionAlgorithm(String spaceIntervals) {
        if (!Strings.isEmpty(spaceIntervals)) {
            spaces = Arrays.stream(spaceIntervals.split(",\\s*")).map(Integer::valueOf).collect(Collectors.toList());
        }
        else {
            spaces = new ArrayList<>();
        }
    }

    public class MemoryStateChange {
        @Getter
        private final int nextForgettingCurveIndex;
        @Getter
        private final int nextRepeatInDays;

        public MemoryStateChange(int nextForgettingCurveIndex, int nextRepeatInDays) {

            this.nextForgettingCurveIndex = nextForgettingCurveIndex;
            this.nextRepeatInDays = nextRepeatInDays;
        }
    }

    public MemoryStateChange getMemoryStateChange(Integer oldForgettingCurveIndex) {
        final int nextForgettingCurveIndex = oldForgettingCurveIndex + DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;
        return new MemoryStateChange(nextForgettingCurveIndex, getRepeatInDays(nextForgettingCurveIndex));
    }

    private Integer getRepeatInDays(Integer forgettingCurveIndex) {
        int index = (forgettingCurveIndex - DEFAULT_FORGETTING_CURVE_INDEX) / DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT - 1;
        if (index < 0) {
            return 0;
        }
        if (index + 1 > spaces.size()) {
           return DEFAULT_SPACES.get(index);
        }
        return spaces.get(index);
    }

}

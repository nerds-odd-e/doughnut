package com.odde.doughnut.models;

import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SpacedRepetition {
    public static final Integer DEFAULT_FORGETTING_CURVE_INDEX = 100;
    public static final Integer DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT = 10;
    public static final List<Integer> DEFAULT_SPACES = Arrays.asList(1, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765, 10946);
    private final List<Integer> spaces;
    public SpacedRepetition(String spaceIntervals) {
        if (!Strings.isEmpty(spaceIntervals)) {
            spaces = Arrays.stream(spaceIntervals.split(",\\s*")).map(Integer::valueOf).collect(Collectors.toList());
        }
        else {
            spaces = new ArrayList<>();
        }
    }

    public static int getNextForgettingCurveIndex(Integer oldForgettingCurveIndex) {
        return oldForgettingCurveIndex + DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;
    }

    public Integer getNextRepeatInDays(Integer forgettingCurveIndex) {
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

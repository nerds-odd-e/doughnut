package com.odde.doughnut.algorithms;

import org.junit.jupiter.api.Test;

import static com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm.DEFAULT_FORGETTING_CURVE_INDEX;
import static org.hamcrest.MatcherAssert.assertThat;

public class SpacedRepetitionEarlyRewardsAndLatePenaltyTest {
    SpacedRepetitionAlgorithm spacedRepetitionAlgorithm = new SpacedRepetitionAlgorithm("3, 6, 9, 12, 15");

    @Test
    void fallOnTheSecondRepeatLevel() {
        int index = getNextForgettingCurveIndex(DEFAULT_FORGETTING_CURVE_INDEX);
    }

    private int getNextForgettingCurveIndex(Integer index) {
        return spacedRepetitionAlgorithm.getNextForgettingCurveIndex(index, 0);
    }

}

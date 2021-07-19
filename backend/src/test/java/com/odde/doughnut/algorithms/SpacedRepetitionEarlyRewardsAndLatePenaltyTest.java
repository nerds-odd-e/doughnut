package com.odde.doughnut.algorithms;

import org.junit.jupiter.api.Test;

import static com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm.DEFAULT_FORGETTING_CURVE_INDEX;
import static com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm.DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class SpacedRepetitionEarlyRewardsAndLatePenaltyTest {
    SpacedRepetitionAlgorithm spacedRepetitionAlgorithm = new SpacedRepetitionAlgorithm("3, 6, 9, 12, 15");
    final int currentForgettingCurveIndex = DEFAULT_FORGETTING_CURVE_INDEX + DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT * 2;
    final int baselineForgettingCurveIndex = DEFAULT_FORGETTING_CURVE_INDEX + DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT * 3;

    @Test
    void repeatOnTime() {
        int index = getNextForgettingCurveIndexWithDelay(0);
        assertThat(index, equalTo(baselineForgettingCurveIndex));
    }

    @Test
    void repeatEarly_immediatelyWhichIsImpossible() {
        int index = getNextForgettingCurveIndexWithDelay(-6 * 24);
        assertThat(index, equalTo(currentForgettingCurveIndex));
    }

    @Test
    void repeatEarly_inOneHour() {
        int index = getNextForgettingCurveIndexWithDelay(-6 * 24 + 1);
        assertThat(index, greaterThanOrEqualTo(currentForgettingCurveIndex));
        assertThat(index, lessThan(baselineForgettingCurveIndex));
    }

    @Test
    void repeatEarly_OneHourEarlier() {
        int index = getNextForgettingCurveIndexWithDelay(-1);
        assertThat(index, greaterThan(currentForgettingCurveIndex));
        assertThat(index, lessThanOrEqualTo(baselineForgettingCurveIndex));
    }

    private int getNextForgettingCurveIndexWithDelay(int delayInHours) {
        return spacedRepetitionAlgorithm.getNextForgettingCurveIndex(currentForgettingCurveIndex, 0, delayInHours);
    }

}

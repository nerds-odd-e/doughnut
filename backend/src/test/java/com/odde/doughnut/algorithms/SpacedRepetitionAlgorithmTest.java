package com.odde.doughnut.algorithms;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm.DEFAULT_FORGETTING_CURVE_INDEX;
import static com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm.DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class SpacedRepetitionAlgorithmTest {

    @Test
    void defaultSetting() {
        SpacedRepetitionAlgorithm spacedRepetitionAlgorithm = new SpacedRepetitionAlgorithm(null);

        final int nextForgettingCurveIndex = spacedRepetitionAlgorithm.getNextForgettingCurveIndex(0, 0, 0);
        assertThat(spacedRepetitionAlgorithm.getRepeatInHours(nextForgettingCurveIndex), equalTo(0));
    }

    @Nested
    class ASettingOf379 {
        SpacedRepetitionAlgorithm spacedRepetitionAlgorithm = new SpacedRepetitionAlgorithm("3, 6, 9");

        @Test
        void fallBeforeTheFirstRepeatLevel() {
            int index = DEFAULT_FORGETTING_CURVE_INDEX - 1;
            assertThat(getNextRepeatInHours(index), equalTo(0));
        }

        @Test
        void fallOnTheFirstRepeatLevel() {
            int index = DEFAULT_FORGETTING_CURVE_INDEX;
            assertThat(getNextRepeatInHours(index), equalTo(3 * 24));
        }

        @Test
        void betweenTheFirstAndSecond() {
            int index = DEFAULT_FORGETTING_CURVE_INDEX + DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT / 2;
            assertThat(getNextRepeatInHours(index), greaterThan(3 * 24));
            assertThat(getNextRepeatInHours(index), lessThan(6 * 24));
        }

        @Test
        void fallOnTheSecondRepeatLevel() {
            int index = getNextForgettingCurveIndex(DEFAULT_FORGETTING_CURVE_INDEX);
            assertThat(getNextRepeatInHours(index), equalTo(6 * 24));
        }

        @Test
        void beyondSettingShouldGetFromDefault() {
            int index = getNextForgettingCurveIndex(DEFAULT_FORGETTING_CURVE_INDEX);
            index = getNextForgettingCurveIndex(index);
            index = getNextForgettingCurveIndex(index);
            index = getNextForgettingCurveIndex(index);
            index = getNextForgettingCurveIndex(index);
            index = getNextForgettingCurveIndex(index);
            assertThat(getNextRepeatInHours(index), equalTo(8 * 24));
        }

        private Integer getNextRepeatInHours(int index) {
            return spacedRepetitionAlgorithm.getRepeatInHours(getNextForgettingCurveIndex(index));

        }

        private int getNextForgettingCurveIndex(Integer index) {
            return spacedRepetitionAlgorithm.getNextForgettingCurveIndex(index, 0, 0);
        }

    }
}

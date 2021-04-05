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

        assertThat(spacedRepetitionAlgorithm.getMemoryStateChange(0, 0).getNextRepeatInDays(), equalTo(0));
    }

    @Nested
    class ASettingOf379 {
        SpacedRepetitionAlgorithm spacedRepetitionAlgorithm = new SpacedRepetitionAlgorithm("3, 6, 9");

        @Test
        void fallBeforeTheFirstRepeatLevel() {
            int index = DEFAULT_FORGETTING_CURVE_INDEX - 1;
            assertThat(getNextRepeatInDays(index), equalTo(0));
        }

        @Test
        void fallOnTheFirstRepeatLevel() {
            int index = DEFAULT_FORGETTING_CURVE_INDEX;
            assertThat(getNextRepeatInDays(index), equalTo(3));
        }

        @Test
        void betweenTheFirstAndSecond() {
            int index = DEFAULT_FORGETTING_CURVE_INDEX + DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT / 2;
            assertThat(getNextRepeatInDays(index), greaterThan(3));
            assertThat(getNextRepeatInDays(index), lessThan(6));
        }

        @Test
        void fallOnTheSecondRepeatLevel() {
            int index = getNextForgettingCurveIndex(DEFAULT_FORGETTING_CURVE_INDEX);
            assertThat(getNextRepeatInDays(index), equalTo(6));
        }

        @Test
        void beyondSettingShouldGetFromDefault() {
            int index = getNextForgettingCurveIndex(DEFAULT_FORGETTING_CURVE_INDEX);
            index = getNextForgettingCurveIndex(index);
            index = getNextForgettingCurveIndex(index);
            index = getNextForgettingCurveIndex(index);
            index = getNextForgettingCurveIndex(index);
            index = getNextForgettingCurveIndex(index);
            assertThat(getNextRepeatInDays(index), equalTo(8));
        }

        private Integer getNextRepeatInDays(int index) {
            return spacedRepetitionAlgorithm.getMemoryStateChange(index, 0).getNextRepeatInDays();

        }

        private int getNextForgettingCurveIndex(Integer index) {
            return spacedRepetitionAlgorithm.getMemoryStateChange(index, 0).getNextForgettingCurveIndex();
        }

    }
}

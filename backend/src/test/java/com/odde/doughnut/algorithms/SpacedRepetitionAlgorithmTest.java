package com.odde.doughnut.algorithms;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class SpacedRepetitionAlgorithmTest {

    @Test
    void defaultSetting() {
        SpacedRepetitionAlgorithm spacedRepetitionAlgorithm = new SpacedRepetitionAlgorithm(null);

        assertThat(spacedRepetitionAlgorithm.getMemoryStateChange(0, 0).getNextRepeatInDays(), equalTo(0));
    }

    @Nested
    class ASettingOf379 {
        SpacedRepetitionAlgorithm spacedRepetitionAlgorithm = new SpacedRepetitionAlgorithm("3, 4, 5");

        @Test
        void fallBeforeTheFirstRepeatLevel() {
            int index = SpacedRepetitionAlgorithm.DEFAULT_FORGETTING_CURVE_INDEX - 1;
            assertThat(getNextRepeatInDays(index), equalTo(0));
        }

        @Test
        void fallOnTheFirstRepeatLevel() {
            int index = SpacedRepetitionAlgorithm.DEFAULT_FORGETTING_CURVE_INDEX;
            assertThat(getNextRepeatInDays(index), equalTo(3));
        }

        @Test
        void fallOnTheSecondRepeatLevel() {
            int index = getNextForgettingCurveIndex(SpacedRepetitionAlgorithm.DEFAULT_FORGETTING_CURVE_INDEX);
            assertThat(getNextRepeatInDays(index), equalTo(4));
        }

        @Test
        void beyondSettingShouldGetFromDefault() {
            int index = getNextForgettingCurveIndex(SpacedRepetitionAlgorithm.DEFAULT_FORGETTING_CURVE_INDEX);
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

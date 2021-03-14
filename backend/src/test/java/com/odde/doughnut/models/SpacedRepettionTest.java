package com.odde.doughnut.models;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class SpacedRepettionTest {

    @Test
    void defaultSetting() {
        SpacedRepetition spacedRepetition = new SpacedRepetition(null);
        assertThat(spacedRepetition.getNextRepeatInDays(0), equalTo(0));
    }

    @Nested
    class ASettingOf379 {
        SpacedRepetition spacedRepetition = new SpacedRepetition("3, 4, 5");

        @Test
        void fallBeforeTheFirstRepeatLevel() {
            int index = SpacedRepetition.DEFAULT_FORGETTING_CURVE_INDEX;
            assertThat(spacedRepetition.getNextRepeatInDays(index), equalTo(0));
        }

        @Test
        void fallOnTheFirstRepeatLevel() {
            int index = spacedRepetition.getNextForgettingCurveIndex(SpacedRepetition.DEFAULT_FORGETTING_CURVE_INDEX);
            assertThat(spacedRepetition.getNextRepeatInDays(index), equalTo(3));
        }

        @Test
        void fallOnTheSecondRepeatLevel() {
            int index = spacedRepetition.getNextForgettingCurveIndex(SpacedRepetition.DEFAULT_FORGETTING_CURVE_INDEX);
            index = spacedRepetition.getNextForgettingCurveIndex(index);
            assertThat(spacedRepetition.getNextRepeatInDays(index), equalTo(4));
        }

        @Test
        void beyondSettingShouldGetFromDefault() {
            int index = spacedRepetition.getNextForgettingCurveIndex(SpacedRepetition.DEFAULT_FORGETTING_CURVE_INDEX);
            index = spacedRepetition.getNextForgettingCurveIndex(index);
            index = spacedRepetition.getNextForgettingCurveIndex(index);
            index = spacedRepetition.getNextForgettingCurveIndex(index);
            index = spacedRepetition.getNextForgettingCurveIndex(index);
            index = spacedRepetition.getNextForgettingCurveIndex(index);
            index = spacedRepetition.getNextForgettingCurveIndex(index);
            assertThat(spacedRepetition.getNextRepeatInDays(index), equalTo(8));
        }
    }
}

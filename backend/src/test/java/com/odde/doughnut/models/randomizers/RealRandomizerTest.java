package com.odde.doughnut.models.randomizers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;

import com.odde.doughnut.models.Randomizer;

import org.junit.jupiter.api.Test;

class RealRandomizerTest {
    @Test
    void shouldReturnNullWhenListIsEmpty() {
        Randomizer randomizer = new RealRandomizer();
        assertThat(randomizer.chooseOneRandomly(new ArrayList<String>()), is(nullValue()));
    }
}
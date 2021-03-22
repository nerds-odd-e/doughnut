package com.odde.doughnut.models.randomizers;

import com.odde.doughnut.models.Randomizer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class RealRandomizerTest {
    @Test
    void shouldReturnNullWhenListIsEmpty() {
        Randomizer randomizer = new RealRandomizer();
        assertThat(randomizer.chooseOneRandomly(new ArrayList<String>()), is(nullValue()));
    }
}
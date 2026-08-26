package com.odde.donut.utils.randomizers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.odde.donut.utils.Randomizer;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RealRandomizerTest {
  @Test
  void shouldReturnNullWhenListIsEmpty() {
    Randomizer randomizer = new RealRandomizer();
    assertThat(randomizer.chooseOneRandomly(new ArrayList<String>()), is(Optional.empty()));
  }
}

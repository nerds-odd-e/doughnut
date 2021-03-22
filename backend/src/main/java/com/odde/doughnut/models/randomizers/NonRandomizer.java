package com.odde.doughnut.models.randomizers;

import com.odde.doughnut.models.Randomizer;

import java.util.List;

public class NonRandomizer implements Randomizer {
    @Override
    public <T> void shuffle(List<T> list) {
    }

    @Override
    public <T> T chooseOneRandomly(List<T> list) {
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }
}

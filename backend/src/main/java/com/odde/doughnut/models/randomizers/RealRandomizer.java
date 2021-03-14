package com.odde.doughnut.models.randomizers;

import com.odde.doughnut.models.Randomizer;

import java.util.List;
import java.util.Random;

public class RealRandomizer implements Randomizer {
    @Override
    public <T> T chooseOneRandomly(List<T> list) {
        Random rand = new Random();
        return list.get(rand.nextInt(list.size()));
    }
}

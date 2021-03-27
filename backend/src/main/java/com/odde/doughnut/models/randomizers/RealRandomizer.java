package com.odde.doughnut.models.randomizers;

import com.odde.doughnut.models.Randomizer;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RealRandomizer implements Randomizer {
    @Override
    public <T> void shuffle(List<T> list) {
        Collections.shuffle(list);
    }

    @Override
    public <T> T chooseOneRandomly(List<T> list) {
        if(list.isEmpty()) {
            return null;
        }
        Random rand = new Random();
        return list.get(rand.nextInt(list.size()));
    }

}

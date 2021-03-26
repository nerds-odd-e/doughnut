package com.odde.doughnut.models.randomizers;

import com.odde.doughnut.models.Randomizer;
import lombok.Setter;

import java.util.List;

public class NonRandomizer implements Randomizer {
    @Setter
    public String alwaysChoose = "first";

    @Override
    public <T> void shuffle(List<T> list) {
    }

    @Override
    public <T> T chooseOneRandomly(List<T> list) {
        if (list.isEmpty()) {
            return null;
        }
        if (alwaysChoose.equals("last")) {
            return list.get(list.size() - 1);
        }
        return list.get(0);
    }

}

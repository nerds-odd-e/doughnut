package com.odde.doughnut.models;

import java.util.List;

public interface Randomizer {
    <T> T chooseOneRandomly(List<T> list);
}

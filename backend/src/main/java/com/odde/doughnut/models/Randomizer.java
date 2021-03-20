package com.odde.doughnut.models;

import java.util.List;

public interface Randomizer {
    <T> void shuffle(List<T> list);
    <T> T chooseOneRandomly(List<T> list);
}

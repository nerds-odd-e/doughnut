package com.odde.doughnut.models;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Randomizer {
    <T> void shuffle(List<T> list);
    <T> T chooseOneRandomly(List<T> list);

    default <T> List<T> randomlyChoose(int maxSize, List<T> list) {
        shuffle(list);
        return list.stream().limit(maxSize).collect(Collectors.toList());
    }
}

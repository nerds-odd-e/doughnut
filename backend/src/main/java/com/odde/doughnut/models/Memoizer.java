package com.odde.doughnut.models;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class Memoizer {
    private final Map<String, Integer> cache = new ConcurrentHashMap<>();
    public int call(String toRepeatCount, Supplier<Integer> getToRepeatCount) {
        return cache.computeIfAbsent(toRepeatCount, (a)->{return getToRepeatCount.get();});
    }
}

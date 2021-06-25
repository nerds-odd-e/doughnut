package com.odde.doughnut.models;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class Memoizer {
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    public <T> T call(String name, Supplier<T> method) {
        return (T) cache.computeIfAbsent(name, (a)-> method.get());
    }
}

package com.odde.doughnut.testability.builders;

import java.util.function.Function;

public class TestObjectCounter {
    private final Function<Integer, String> naming;
    private Integer count = 0;
    public TestObjectCounter(Function<Integer, String> naming) {
        this.naming = naming;
    }

    public String generate() {
        count += 1;
        return naming.apply(count);
    }
}

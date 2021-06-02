package com.odde.doughnut.algorithms;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NoteTitle {

    private final String title;

    public NoteTitle(String title) {
        this.title = title;
    }

    public Stream<String> getTitles() {
        return Arrays.stream(title.split("(?<!/)/(?!/)"))
                .map(String::trim);
    }
}
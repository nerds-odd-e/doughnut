package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NoteTitle {

    private final String title;

    public NoteTitle(String title) {
        this.title = title;
    }

    public boolean matches(String answer) {
        if(title.trim().equalsIgnoreCase(answer)) {
            return true;
        }
        return getTitles().anyMatch(t-> t.matches(answer));
    }

    String replaceTitleFragments(String pronunciationMasked, ClozeReplacement clozeReplacement) {
        String titleMasked = getTitles()
                .reduce(pronunciationMasked, (d, t) -> t.clozeIt(d), (x, y) -> y);
        return TitleFragment.replaceMasks(titleMasked, clozeReplacement);
    }

    private Stream<TitleFragment> getTitles() {
        List<TitleFragment> result = new ArrayList<>();
        String[] split = title.split("\\(", 2);
        IntStream.range(0, split.length).forEach(idx->
                getFragments(split[idx], idx != 0).forEach(result::add));
        return result.stream();
    }

    private Stream<TitleFragment> getFragments(String subString, boolean subtitle) {
        if(subtitle) {
            subString = subString.substring(0, subString.length() - 1);
        }
        return Arrays.stream(subString.split("(?<!/)/(?!/)"))
                .map(s->new TitleFragment(s, subtitle));
    }

}
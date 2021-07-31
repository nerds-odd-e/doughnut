package com.odde.doughnut.algorithms;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
        Pattern pattern = Pattern.compile("(?U)(.*?)(\\p{Ps}(.*)\\p{Pe})?$");
        Matcher matcher = pattern.matcher(title);
        if(matcher.find()) {
            getFragments(matcher.group(1), false).forEach(result::add);
            getFragments(matcher.group(3), true).forEach(result::add);
        }
        result.sort(Comparator.comparing(TitleFragment::length));
        Collections.reverse(result);
        return result.stream();
    }

    private Stream<TitleFragment> getFragments(String subString, boolean subtitle) {
        return Arrays.stream(subString !=null ? subString.split("(?<!/)/(?!/)") : new String[]{})
                .map(s->new TitleFragment(s, subtitle));
    }

}
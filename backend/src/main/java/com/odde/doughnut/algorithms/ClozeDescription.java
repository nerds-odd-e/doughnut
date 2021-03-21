package com.odde.doughnut.algorithms;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ClozeDescription {
    public ClozeDescription() {
    }

    public String getClozeDescription(String title, String description) {
        return Arrays.stream(title.split("/"))
                .map(String::trim)
                .reduce(description, this::clozeString);
    }

    public String clozeString(String description, String wordToHide) {
        return replacePronunciation(
                replaceSimilar(wordToHide,
                replaceLiteralWords(wordToHide, description)));
    }

    private String replacePronunciation(String description) {
        Pattern pattern = Pattern.compile("\\/[^\\/]+\\/", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(description).replaceAll("/.../");
    }

    private String replaceLiteralWords(String wordToHide, String description) {
        String ptn = String.join("([\\s-]+)((and\\s+)|(the\\s+)|(a\\s+)|(an\\s+))?",
                Arrays.stream(wordToHide.split("[\\s-]+"))
                        .filter(x -> !Arrays.asList("the", "a", "an").contains(x))
                        .map(Pattern::quote).collect(Collectors.toUnmodifiableList()));
        Pattern pattern = Pattern.compile(ptn, Pattern.CASE_INSENSITIVE);
        return pattern.matcher(description).replaceAll("[...]");
    }

    private String replaceSimilar(String wordToHide, String literal) {
        String substring = wordToHide.substring(0, wordToHide.length() * 3 / 4);
        Pattern pattern = Pattern.compile(Pattern.quote(substring), Pattern.CASE_INSENSITIVE);
        return pattern.matcher(literal).replaceAll("[..~]");
    }
}
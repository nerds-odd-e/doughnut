package com.odde.doughnut.algorithms;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ClozeDescription {
    private final String partialMatchReplacement;
    private final String fullMatchReplacement;
    private final String pronunciationReplacement;
    private final String internalPartialMatchReplacement = "__p_a_r_t_i_a_l__";
    private final String internalFullMatchReplacement = "__f_u_l_l__";
    private final String internalPronunciationReplacement = "__p_r_o_n_u_n_c__";

    public ClozeDescription(String partialMatchReplacement, String fullMatchReplacement, String pronunciationReplacement) {
        this.partialMatchReplacement = partialMatchReplacement;
        this.fullMatchReplacement = fullMatchReplacement;
        this.pronunciationReplacement = pronunciationReplacement;
    }

    static public ClozeDescription htmlClosedDescription() {
        return new ClozeDescription(
                "<mark title='Hidden text that is partially matching the answer'>[..~]</mark>",
                "<mark title='Hidden text that is matching the answer'>[...]</mark>",
                "<mark title='Hidden pronunciation'>/.../</mark>"
        );
    }

    public String getClozeDescription(NoteTitle noteTitle, String description) {
        return noteTitle
                .getTitles()
                .reduce(description, this::clozeString)
                .replace(internalFullMatchReplacement, fullMatchReplacement)
                .replace(internalPartialMatchReplacement, partialMatchReplacement)
                .replace(internalPronunciationReplacement, pronunciationReplacement);
    }

    public String clozeString(String description, String wordToHide) {
        return replacePronunciation(
                replaceSimilar(wordToHide,
                replaceLiteralWords(wordToHide, description)));
    }

    private String replacePronunciation(String description) {
        Pattern pattern = Pattern.compile("\\/[^\\s^\\/][^\\/\\n]*\\/(?!\\w)", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(description).replaceAll(internalPronunciationReplacement);
    }

    private String replaceLiteralWords(String wordToHide, String description) {
        Pattern pattern = Pattern.compile(getPatternStringForLiteralMatch(wordToHide), Pattern.CASE_INSENSITIVE);
        return pattern.matcher(description).replaceAll(internalFullMatchReplacement);
    }

    private String getPatternStringForLiteralMatch(String wordToHide) {
        if (wordToHide.length() >= 4) {
            return String.join("([\\s-]+)((and\\s+)|(the\\s+)|(a\\s+)|(an\\s+))?",
                    Arrays.stream(wordToHide.split("[\\s-]+"))
                            .filter(x -> !Arrays.asList("the", "a", "an").contains(x))
                            .map(Pattern::quote).collect(Collectors.toUnmodifiableList()));
        }
        if (wordToHide.matches("^\\d+$")) {
            return "(?<!\\d)" + Pattern.quote(wordToHide) + "(?!\\d)";
        }
        return "(?<!\\w)" + Pattern.quote(wordToHide) + "(?!\\w)";
    }

    private String replaceSimilar(String wordToHide, String literal) {
        if (wordToHide.length() < 4) {
            return literal;
        }
        String substring = wordToHide.substring(0, (wordToHide.length() + 1) * 3 / 4);
        Pattern pattern = Pattern.compile(Pattern.quote(substring), Pattern.CASE_INSENSITIVE);
        return pattern.matcher(literal).replaceAll(internalPartialMatchReplacement);
    }
}
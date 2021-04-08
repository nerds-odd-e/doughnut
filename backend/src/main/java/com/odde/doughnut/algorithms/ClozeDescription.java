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

    public String getClozeDescription(String title, String description) {
        return Arrays.stream(title.split("(?<!/)/(?!/)"))
                .map(String::trim)
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
        Pattern pattern = Pattern.compile("\\/[^\\s^\\/][^\\/]*\\/(?!\\w)", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(description).replaceAll(internalPronunciationReplacement);
    }

    private String replaceLiteralWords(String wordToHide, String description) {
        Pattern pattern = getPatternForLiteralMatch(wordToHide);
        return pattern.matcher(description).replaceAll(internalFullMatchReplacement);
    }

    private Pattern getPatternForLiteralMatch(String wordToHide) {
        String ptn;
        if (wordToHide.length() < 4) {
            ptn = "(?<!\\w)" + Pattern.quote(wordToHide) + "(?!\\w)";
            if (wordToHide.matches("^\\d+$")) {
                ptn = "(?<!\\d)" + Pattern.quote(wordToHide) + "(?!\\d)";
            }
        }
        else {
            ptn = String.join("([\\s-]+)((and\\s+)|(the\\s+)|(a\\s+)|(an\\s+))?",
                    Arrays.stream(wordToHide.split("[\\s-]+"))
                            .filter(x -> !Arrays.asList("the", "a", "an").contains(x))
                            .map(Pattern::quote).collect(Collectors.toUnmodifiableList()));
        }
        return Pattern.compile(ptn, Pattern.CASE_INSENSITIVE);
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
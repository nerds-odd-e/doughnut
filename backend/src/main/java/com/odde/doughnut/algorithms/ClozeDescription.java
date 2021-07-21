package com.odde.doughnut.algorithms;

import java.util.function.Function;
import java.util.regex.Pattern;

public class ClozeDescription {
    private final String partialMatchReplacement;
    private final String fullMatchReplacement;
    private final String pronunciationReplacement;

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
        return replacePronunciations(description, d-> replaceTitleFragments(noteTitle, d));
    }

    private String replacePronunciations(String description, Function<String, String> innerReplacer) {
        final String internalPronunciationReplacement = "__p_r_o_n_u_n_c__";
        final Pattern pattern = Pattern.compile("\\/[^\\s^\\/][^\\/\\n]*\\/(?!\\w)", Pattern.CASE_INSENSITIVE);
        return innerReplacer
                .apply(pattern.matcher(description).replaceAll(internalPronunciationReplacement))
                    .replace(internalPronunciationReplacement, pronunciationReplacement);
    }

    private String replaceTitleFragments(NoteTitle noteTitle, String pronunciationMasked) {
        String titleMasked = noteTitle
                .getTitles()
                .reduce(pronunciationMasked, (d, t) -> t.clozeIt(d), (x, y) -> y);
        return TitleFragment.replaceMasks(titleMasked, this.fullMatchReplacement, this.partialMatchReplacement);
    }

}
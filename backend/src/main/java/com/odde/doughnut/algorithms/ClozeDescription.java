package com.odde.doughnut.algorithms;

import java.util.regex.Pattern;

public class ClozeDescription {
    private final String partialMatchReplacement;
    private final String fullMatchReplacement;
    private final String pronunciationReplacement;
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
                    .reduce(replacePronunciation(description), this::clozeString, (x, y)->y)
                    .replace(TitleFragment.internalFullMatchReplacement, fullMatchReplacement)
                    .replace(TitleFragment.internalPartialMatchReplacement, partialMatchReplacement)
                    .replace(internalPronunciationReplacement, pronunciationReplacement);
    }

    public String clozeString(String description, TitleFragment titleFragment) {
        return titleFragment.clozeIt(description);
    }

    private String replacePronunciation(String description) {
        Pattern pattern = Pattern.compile("\\/[^\\s^\\/][^\\/\\n]*\\/(?!\\w)", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(description).replaceAll(internalPronunciationReplacement);
    }

}
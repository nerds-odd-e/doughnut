package com.odde.doughnut.algorithms;

import java.util.function.Function;
import java.util.regex.Pattern;

public class ClozeDescription {
    private final String pronunciationReplacement;
    private final ClozeReplacement clozeReplacement = new ClozeReplacement();

    public ClozeDescription(String partialMatchReplacement, String fullMatchReplacement, String pronunciationReplacement, String partialMatchSubtitleReplacement, String fullMatchSubtitleReplacement) {
        clozeReplacement.partialMatchReplacement = partialMatchReplacement;
        clozeReplacement.fullMatchReplacement = fullMatchReplacement;
        clozeReplacement.partialMatchSubtitleReplacement = partialMatchSubtitleReplacement;
        clozeReplacement.fullMatchSubtitleReplacement = fullMatchSubtitleReplacement;
        this.pronunciationReplacement = pronunciationReplacement;
    }

    static public ClozeDescription htmlClosedDescription() {
        return new ClozeDescription(
                "<mark title='Hidden text that is partially matching the answer'>[..~]</mark>",
                "<mark title='Hidden text that is matching the answer'>[...]</mark>",
                "<mark title='Hidden pronunciation'>/.../</mark>",
                "<mark title='Hidden subtitle that is partially matching the answer'>(..~)</mark>",
                "<mark title='Hidden subtitle that is matching the answer'>(...)</mark>"
                );
    }

    public String getClozeDescription(NoteTitle noteTitle, String description) {
        return replacePronunciations(description, d-> noteTitle.replaceTitleFragments(d, this.clozeReplacement));
    }

    private String replacePronunciations(String description, Function<String, String> innerReplacer) {
        final String internalPronunciationReplacement = "__p_r_o_n_u_n_c__";
        final Pattern pattern = Pattern.compile("\\/[^\\s^\\/][^\\/\\n]*\\/(?!\\w)", Pattern.CASE_INSENSITIVE);
        return innerReplacer
                .apply(pattern.matcher(description).replaceAll(internalPronunciationReplacement))
                    .replace(internalPronunciationReplacement, pronunciationReplacement);
    }

}
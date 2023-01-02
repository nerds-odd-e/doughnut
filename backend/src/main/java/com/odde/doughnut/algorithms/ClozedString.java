package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.apache.logging.log4j.util.Strings;

public class ClozedString {
  private ClozeReplacement clozeReplacement;
  private String originalContent;
  private List<NoteTitle> noteTitles = new ArrayList<>();

  public ClozedString(ClozeReplacement clozeReplacement, String originalContent) {
    this.clozeReplacement = clozeReplacement;

    this.originalContent = originalContent;
  }

  public static ClozedString htmlClosedString(String content) {
    ClozeReplacement clozeReplacement =
        new ClozeReplacement(
            "<mark title='Hidden text that is partially matching the answer'>[..~]</mark>",
            "<mark title='Hidden text that is matching the answer'>[...]</mark>",
            "<mark title='Hidden pronunciation'>/.../</mark>",
            "<mark title='Hidden subtitle that is partially matching the answer'>(..~)</mark>",
            "<mark title='Hidden subtitle that is matching the answer'>(...)</mark>");
    return new ClozedString(clozeReplacement, content);
  }

  @Override
  public String toString() {
    throw new RuntimeException("Not implemented, use `cloze` instead.");
  }

  public String cloze() {
    if (Strings.isEmpty(originalContent)) return originalContent;

    return maskPronunciations(this::maskTitles);
  }

  private String maskPronunciations(Function<String, String> interimStringProcessor) {
    final String internalPronunciationReplacement = "__p_r_o_n_u_n_c__";
    final Pattern pattern =
        Pattern.compile("\\/[^\\s^\\/][^\\/\\n]*\\/(?!\\w)", Pattern.CASE_INSENSITIVE);
    String pronunciationsReplaced =
        pattern.matcher(originalContent).replaceAll(internalPronunciationReplacement);
    return interimStringProcessor
        .apply(pronunciationsReplaced)
        .replace(internalPronunciationReplacement, clozeReplacement.pronunciationReplacement);
  }

  private String maskTitles(String d) {
    return noteTitles.stream()
        .reduce(
            d,
            (s, noteTitle) -> clozeReplacement.replaceTitleFragments(s, noteTitle),
            (s, s2) -> s);
  }

  public boolean isPresent() {
    return Strings.isNotEmpty(originalContent);
  }

  public ClozedString hide(NoteTitle noteTitle) {
    this.noteTitles.add(noteTitle);
    return this;
  }
}

package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.logging.log4j.util.Strings;

public class ClozedString {
  private ClozeReplacement clozeReplacement;
  private String originalContent;
  private List<NoteTitle> noteTitles = new ArrayList<>();

  static final String internalPronunciationReplacement = "__p_r_o_n_u_n_c__";

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
    final Pattern pattern =
        Pattern.compile("\\/[^\\s^\\/][^\\/\\n]*\\/(?!\\w)", Pattern.CASE_INSENSITIVE);
    String d = pattern.matcher(originalContent).replaceAll(internalPronunciationReplacement);
    return noteTitles.stream()
        .reduce(
            d, (s, noteTitle) -> clozeReplacement.replaceTitleFragments(s, noteTitle), (s, s2) -> s)
        .replace(internalPronunciationReplacement, clozeReplacement.pronunciationReplacement);
  }

  public boolean isPresent() {
    return Strings.isNotBlank(originalContent);
  }

  public ClozedString hide(NoteTitle noteTitle) {
    this.noteTitles.add(noteTitle);
    return this;
  }
}

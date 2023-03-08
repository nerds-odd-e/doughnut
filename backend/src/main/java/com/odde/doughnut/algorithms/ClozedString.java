package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
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
            "<mark title='Hidden subtitle that is matching the answer'>(...)</mark>");
    return new ClozedString(clozeReplacement, content);
  }

  @Override
  public String toString() {
    throw new RuntimeException("Not implemented, use `cloze` instead.");
  }

  public class HtmlUtils {
    public static String replaceText(String html, Function<String, String> callback) {
      Pattern pattern = Pattern.compile("(?s)(?<=^|>)[^><]+?(?=<|$)");
      Matcher matcher = pattern.matcher(html);
      return matcher.replaceAll(
          matchResult -> {
            String text = matchResult.group();
            return callback.apply(text);
          });
    }
  }

  public String cloze() {
    if (Strings.isEmpty(originalContent)) return originalContent;

    return HtmlUtils.replaceText(
        originalContent, text -> clozeReplacement.maskPronunciationsAndTitles(text, noteTitles));
  }

  public boolean isPresent() {
    return Strings.isNotEmpty(originalContent);
  }

  public ClozedString hide(NoteTitle noteTitle) {
    this.noteTitles.add(noteTitle);
    return this;
  }
}

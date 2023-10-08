package com.odde.doughnut.algorithms;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class ClozedString {
  private ClozeReplacement clozeReplacement;
  private String originalContent;
  private List<NoteTitle> noteTitles = new ArrayList<>();

  public ClozedString(ClozeReplacement clozeReplacement, String originalContent) {
    this.clozeReplacement = clozeReplacement;

    this.originalContent = originalContent;
  }

  public static ClozedString htmlClozedString(String content) {
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

  public String clozeDetails() {
    return cloze(htmlContent());
  }

  public String clozeTitle() {
    return cloze(originalContent);
  }

  public boolean isPresent() {
    return Strings.isNotEmpty(originalContent);
  }

  public ClozedString hide(NoteTitle noteTitle) {
    this.noteTitles.add(noteTitle);
    return this;
  }

  private String cloze(String content) {
    return new HtmlOrMarkdown(content)
        .replaceText(text -> clozeReplacement.maskPronunciationsAndTitles(text, noteTitles));
  }

  private String htmlContent() {
    Parser parser = Parser.builder().build();
    HtmlRenderer renderer = HtmlRenderer.builder().build();
    return renderer.render(parser.parse(originalContent));
  }
}

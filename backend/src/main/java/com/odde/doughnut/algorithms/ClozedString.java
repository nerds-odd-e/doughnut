package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.util.Strings;

/**
 * Produces markdown with masked (clozed) portions. Doughnut markdown supports inline {@code <mark>}
 * for masking in recall question stems.
 */
public class ClozedString {
  private static final Pattern MARKDOWN_LINK_URL = Pattern.compile("\\]\\((https?://[^)]+)\\)");
  private ClozeReplacement clozeReplacement;
  private String originalContent;
  private List<NoteTitle> noteTitles = new ArrayList<>();

  public ClozedString(ClozeReplacement clozeReplacement, String originalContent) {
    this.clozeReplacement = clozeReplacement;
    this.originalContent = originalContent;
  }

  /** Creates a ClozedString that outputs markdown with {@code <mark>} tags for cloze masks. */
  public static ClozedString forMarkdownWithMarkMasks(String markdownContent) {
    ClozeReplacement clozeReplacement =
        new ClozeReplacement(
            "<mark title='Hidden text that is partially matching the answer'>[..~]</mark>",
            "<mark title='Hidden text that is matching the answer'>[...]</mark>",
            "<mark title='Hidden pronunciation'>/.../</mark>",
            "<mark title='Hidden subtitle that is matching the answer'>(...)</mark>");
    return new ClozedString(clozeReplacement, markdownContent);
  }

  @Override
  public String toString() {
    throw new RuntimeException("Not implemented, use `cloze` instead.");
  }

  /** Returns markdown with masked portions. Frontend converts to HTML when rendering. */
  public String maskedDetailsAsMarkdown() {
    List<String> urls = new ArrayList<>();
    String protectedContent = protectMarkdownLinkUrls(originalContent, urls);
    return unprotectMarkdownLinkUrls(cloze(protectedContent), urls);
  }

  private String protectMarkdownLinkUrls(String content, List<String> urls) {
    Matcher matcher = MARKDOWN_LINK_URL.matcher(content);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      urls.add(matcher.group(1));
      matcher.appendReplacement(
          sb, Matcher.quoteReplacement("](__URL_" + (urls.size() - 1) + "__)"));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private String unprotectMarkdownLinkUrls(String content, List<String> urls) {
    for (int i = urls.size() - 1; i >= 0; i--) {
      content = content.replace("__URL_" + i + "__", urls.get(i));
    }
    return content;
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
        .replaceTextWithContext(
            (text, followsNonWhitespace) ->
                clozeReplacement.maskPronunciationsAndTitles(
                    text, noteTitles, followsNonWhitespace));
  }
}

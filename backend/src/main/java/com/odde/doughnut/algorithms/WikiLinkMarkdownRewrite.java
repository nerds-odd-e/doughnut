package com.odde.doughnut.algorithms;

import com.odde.doughnut.validators.DisplayNamePathSeparators;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;

/** Rewrites stored wiki-link inner text and matching {@code [[…]]} spans in markdown. */
public final class WikiLinkMarkdownRewrite {

  private WikiLinkMarkdownRewrite() {}

  public static String newInnerForUpdateVisibleText(String storedLinkInner, String newNoteTitle) {
    return newInnerWithHandling(storedLinkInner, newNoteTitle, false);
  }

  public static String newInnerForKeepVisibleText(String storedLinkInner, String newNoteTitle) {
    return newInnerWithHandling(storedLinkInner, newNoteTitle, true);
  }

  /**
   * Rewrites the notebook prefix of a wiki link while preserving the note title and visible display
   * text. Used when a note is moved to a different notebook.
   *
   * <ul>
   *   <li>{@code [[X]]} → {@code [[NewNb:X|X]]}
   *   <li>{@code [[OldNb:X]]} → {@code [[NewNb:X|OldNb:X]]}
   *   <li>{@code [[OldNb:X|custom]]} → {@code [[NewNb:X|custom]]}
   * </ul>
   */
  public static String newInnerForKeepNotebookMove(String storedLinkInner, String newNotebookName) {
    if (newNotebookName == null) {
      throw new IllegalArgumentException("newNotebookName");
    }
    if (storedLinkInner == null || storedLinkInner.isEmpty()) {
      return storedLinkInner;
    }
    return keepVisibleInner(
        storedLinkInner,
        rawTargetToken ->
            WikiLinkTargetReference.replaceNotebookName(rawTargetToken, newNotebookName));
  }

  /**
   * Qualifies an unqualified outgoing wiki-link inner with the source notebook while preserving the
   * text currently visible to readers. Already-qualified inners are left untouched.
   */
  public static String newInnerForQualifyUnqualifiedOutgoingLink(
      String storedLinkInner, String sourceNotebookName) {
    if (sourceNotebookName == null) {
      throw new IllegalArgumentException("sourceNotebookName");
    }
    if (storedLinkInner == null || storedLinkInner.isEmpty()) {
      return storedLinkInner;
    }
    int pipeIdx = storedLinkInner.indexOf('|');
    String rawTargetPart = pipeIdx == -1 ? storedLinkInner : storedLinkInner.substring(0, pipeIdx);
    String targetToken = rawTargetPart.trim();
    if (targetToken.isEmpty() || WikiLinkTargetReference.isQualifiedToken(targetToken)) {
      return storedLinkInner;
    }
    return keepVisibleInner(
        storedLinkInner, rawTargetToken -> sourceNotebookName + ":" + rawTargetToken);
  }

  private static String newInnerWithHandling(
      String storedLinkInner, String newNoteTitle, boolean keepVisibleText) {
    if (newNoteTitle == null) {
      throw new IllegalArgumentException("newNoteTitle");
    }
    if (storedLinkInner == null || storedLinkInner.isEmpty()) {
      return newNoteTitle;
    }
    int pipeIdx = storedLinkInner.indexOf('|');
    String rawTargetPart = pipeIdx == -1 ? storedLinkInner : storedLinkInner.substring(0, pipeIdx);
    String newTargetToken =
        WikiLinkTargetReference.replaceNoteTitle(rawTargetPart.trim(), newNoteTitle.trim());
    if (pipeIdx == -1) {
      return keepVisibleText ? newTargetToken + "|" + storedLinkInner.trim() : newTargetToken;
    }
    String rawDisplay = storedLinkInner.substring(pipeIdx + 1);
    if (rawDisplay.trim().isEmpty()) {
      return keepVisibleText ? newTargetToken + "|" + rawTargetPart.trim() : newTargetToken;
    }
    return newTargetToken + "|" + rawDisplay;
  }

  /**
   * Shared keep-visible-text branching: replaces the target token using the given transform and
   * always preserves whatever text readers currently see.
   */
  private static String keepVisibleInner(
      String storedLinkInner, UnaryOperator<String> targetTokenTransform) {
    int pipeIdx = storedLinkInner.indexOf('|');
    String rawTargetPart = pipeIdx == -1 ? storedLinkInner : storedLinkInner.substring(0, pipeIdx);
    String newTargetToken = targetTokenTransform.apply(rawTargetPart.trim());
    if (pipeIdx == -1) {
      return newTargetToken + "|" + storedLinkInner.trim();
    }
    String rawDisplay = storedLinkInner.substring(pipeIdx + 1);
    if (rawDisplay.trim().isEmpty()) {
      return newTargetToken + "|" + rawTargetPart.trim();
    }
    return newTargetToken + "|" + rawDisplay;
  }

  public static String sanitizePathSeparatorsInWikiLinks(String markdown) {
    if (markdown == null || markdown.isEmpty()) {
      return markdown;
    }
    Matcher matcher = WikiLinkMarkdown.INNER_LINK_PATTERN.matcher(markdown);
    StringBuilder out = new StringBuilder();
    int last = 0;
    while (matcher.find()) {
      out.append(markdown, last, matcher.start());
      String rawInner = matcher.group(1);
      int pipeIdx = rawInner.indexOf('|');
      String rawTarget = pipeIdx == -1 ? rawInner : rawInner.substring(0, pipeIdx);
      String sanitizedTarget =
          DisplayNamePathSeparators.toFullwidthPathSeparatorsInWikiLinkTarget(rawTarget.trim());
      if (pipeIdx == -1) {
        out.append("[[").append(sanitizedTarget).append("]]");
      } else {
        out.append("[[")
            .append(sanitizedTarget)
            .append("|")
            .append(rawInner.substring(pipeIdx + 1))
            .append("]]");
      }
      last = matcher.end();
    }
    out.append(markdown.substring(last));
    return out.toString();
  }

  public static String replaceWikiLinksMatchingTrimmedInner(
      String markdown, String oldInnerTrimmed, String newInner) {
    if (markdown == null || markdown.isEmpty()) {
      return markdown;
    }
    Matcher matcher = WikiLinkMarkdown.INNER_LINK_PATTERN.matcher(markdown);
    StringBuilder out = new StringBuilder();
    int last = 0;
    while (matcher.find()) {
      out.append(markdown, last, matcher.start());
      String innerTrimmed = matcher.group(1).trim();
      if (innerTrimmed.equals(oldInnerTrimmed)) {
        out.append("[[").append(newInner).append("]]");
      } else {
        out.append(matcher.group(0));
      }
      last = matcher.end();
    }
    out.append(markdown.substring(last));
    return out.toString();
  }
}

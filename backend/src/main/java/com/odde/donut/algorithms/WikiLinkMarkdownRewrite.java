package com.odde.donut.algorithms;

import com.odde.donut.validators.DisplayNamePathSeparators;
import java.util.LinkedHashSet;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;

/**
 * Rewrites stored inter-note tokens (wiki inner or path Markdown) and matching spans in markdown.
 */
public final class WikiLinkMarkdownRewrite {

  private WikiLinkMarkdownRewrite() {}

  public static String newInnerForUpdateVisibleText(String storedLinkInner, String newNoteTitle) {
    return newInnerWithHandling(storedLinkInner, newNoteTitle, false);
  }

  public static String newInnerForKeepVisibleText(String storedLinkInner, String newNoteTitle) {
    return newInnerWithHandling(storedLinkInner, newNoteTitle, true);
  }

  /**
   * Rewrites one matching folder-name segment in a path-shaped wiki inner or path Markdown href.
   * Unqualified titles and the note-title segment are left unchanged. Spelling is preserved.
   */
  public static String newInnerForFolderRename(
      String storedLinkInner, String oldFolderName, String newFolderName) {
    if (oldFolderName == null) {
      throw new IllegalArgumentException("oldFolderName");
    }
    if (newFolderName == null) {
      throw new IllegalArgumentException("newFolderName");
    }
    if (storedLinkInner == null || storedLinkInner.isEmpty()) {
      return storedLinkInner;
    }
    UnaryOperator<String> transform =
        token -> WikiLinkTargetReference.replaceFolderName(token, oldFolderName, newFolderName);
    return rewriteAuthoredTarget(
        storedLinkInner, transform, () -> rewriteWikiInnerTarget(storedLinkInner, transform));
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
    return rewriteWikiInnerLeavingPathMarkdown(
        storedLinkInner,
        () ->
            keepVisibleInner(
                storedLinkInner,
                rawTargetToken ->
                    WikiLinkTargetReference.replaceNotebookName(rawTargetToken, newNotebookName)));
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
    return rewriteWikiInnerLeavingPathMarkdown(
        storedLinkInner, () -> qualifyUnqualifiedWikiInner(storedLinkInner, sourceNotebookName));
  }

  private static String qualifyUnqualifiedWikiInner(
      String storedLinkInner, String sourceNotebookName) {
    int pipeIdx = storedLinkInner.indexOf('|');
    String rawTargetPart = pipeIdx == -1 ? storedLinkInner : storedLinkInner.substring(0, pipeIdx);
    String targetToken = rawTargetPart.trim();
    if (targetToken.isEmpty() || WikiLinkTargetReference.isQualifiedToken(targetToken)) {
      return storedLinkInner;
    }
    return keepVisibleInner(
        storedLinkInner,
        rawTargetToken ->
            WikiLinkTargetReference.replaceNotebookName(rawTargetToken, sourceNotebookName));
  }

  private static String newInnerWithHandling(
      String storedLinkInner, String newNoteTitle, boolean keepVisibleText) {
    if (newNoteTitle == null) {
      throw new IllegalArgumentException("newNoteTitle");
    }
    if (storedLinkInner == null || storedLinkInner.isEmpty()) {
      return newNoteTitle;
    }
    return rewriteAuthoredTarget(
        storedLinkInner,
        token -> WikiLinkTargetReference.replaceNoteTitle(token, newNoteTitle.trim()),
        () -> rewriteWikiInnerNoteTitle(storedLinkInner, newNoteTitle, keepVisibleText));
  }

  private static String rewriteAuthoredTarget(
      String storedLinkInner,
      UnaryOperator<String> targetTransform,
      Supplier<String> wikiFallback) {
    return WikiLinkMarkdown.tryParsePathMarkdownToken(storedLinkInner)
        .map(token -> token.withHref(targetTransform.apply(token.href())))
        .orElseGet(wikiFallback);
  }

  private static String rewriteWikiInnerLeavingPathMarkdown(
      String storedLinkInner, Supplier<String> wikiRewrite) {
    return WikiLinkMarkdown.tryParsePathMarkdownToken(storedLinkInner)
        .map(_ -> storedLinkInner)
        .orElseGet(wikiRewrite);
  }

  private static String rewriteWikiInnerTarget(
      String storedLinkInner, UnaryOperator<String> targetTransform) {
    int pipeIdx = storedLinkInner.indexOf('|');
    String rawTargetPart = pipeIdx == -1 ? storedLinkInner : storedLinkInner.substring(0, pipeIdx);
    String newTargetToken = targetTransform.apply(rawTargetPart.trim());
    if (pipeIdx == -1) {
      return newTargetToken;
    }
    return newTargetToken + "|" + storedLinkInner.substring(pipeIdx + 1);
  }

  private static String rewriteWikiInnerNoteTitle(
      String storedLinkInner, String newNoteTitle, boolean keepVisibleText) {
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

  /** Converts OS-invalid characters in one wiki inner or path-Markdown token. */
  public static String replaceOsInvalidCharsInStoredLinkInner(String storedLinkInner) {
    if (storedLinkInner == null || storedLinkInner.isEmpty()) {
      return storedLinkInner;
    }
    UnaryOperator<String> convert =
        DisplayNamePathSeparators::replaceOsInvalidCharsInWikiLinkTarget;
    return rewriteAuthoredTarget(
        storedLinkInner, convert, () -> rewriteWikiInnerTarget(storedLinkInner, convert));
  }

  /** Converts OS-invalid characters in wiki and path-Markdown tokens. */
  public static String replaceOsInvalidCharsInAuthoredTokens(String markdown) {
    if (markdown == null || markdown.isEmpty()) {
      return markdown;
    }
    String content = markdown;
    for (String token :
        new LinkedHashSet<>(WikiLinkMarkdown.authoredTokensInOccurrenceOrder(markdown))) {
      String converted = replaceOsInvalidCharsInStoredLinkInner(token);
      if (!converted.equals(token)) {
        content = replaceWikiLinksMatchingTrimmedInner(content, token, converted);
      }
    }
    return content;
  }

  public static String replaceWikiLinksMatchingTrimmedInner(
      String markdown, String oldInnerTrimmed, String newInner) {
    if (markdown == null || markdown.isEmpty()) {
      return markdown;
    }
    if (WikiLinkMarkdown.tryParsePathMarkdownToken(oldInnerTrimmed).isPresent()
        && WikiLinkMarkdown.tryParsePathMarkdownToken(newInner).isPresent()) {
      return replacePathMarkdownMatching(markdown, oldInnerTrimmed, newInner);
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

  private static String replacePathMarkdownMatching(
      String markdown, String oldTokenTrimmed, String newToken) {
    StringBuilder out = new StringBuilder();
    int last = 0;
    for (WikiLinkMarkdown.PathMarkdownOccurrence occurrence :
        WikiLinkMarkdown.pathMarkdownOccurrences(markdown)) {
      out.append(markdown, last, occurrence.start());
      out.append(occurrence.token().equals(oldTokenTrimmed) ? newToken : occurrence.token());
      last = occurrence.end();
    }
    out.append(markdown.substring(last));
    return out.toString();
  }
}

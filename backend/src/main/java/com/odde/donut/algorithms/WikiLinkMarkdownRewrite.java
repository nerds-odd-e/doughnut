package com.odde.donut.algorithms;

import com.odde.donut.validators.DisplayNamePathSeparators;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/** Rewrites one stored inter-note token (wiki inner or path Markdown). */
public final class WikiLinkMarkdownRewrite {

  private WikiLinkMarkdownRewrite() {}

  public static String newInnerForAuthoredPortablePath(
      String storedLinkInner, String authoredPortablePath, boolean keepVisibleText) {
    if (authoredPortablePath == null) {
      throw new IllegalArgumentException("authoredPortablePath");
    }
    PortablePath authored = PortablePath.parse(authoredPortablePath);
    if (!authored.hasNotebookQualifier()
        && PathShapedTarget.tryParse(authored.notePortion()).isEmpty()) {
      return newInnerWithHandling(storedLinkInner, authored.notePortion(), keepVisibleText);
    }
    return WikiLinkMarkdown.tryParsePathMarkdownToken(storedLinkInner)
        .map(
            token ->
                token.withHref(
                    pathMarkdownHref(
                        authoredPortablePath,
                        PortablePath.parse(token.href()).notePortion().endsWith(".md"))))
        .orElseGet(
            () ->
                keepVisibleText
                    ? keepVisibleInner(storedLinkInner, _ -> authoredPortablePath)
                    : rewriteWikiInnerTarget(storedLinkInner, _ -> authoredPortablePath));
  }

  private static String pathMarkdownHref(String authoredPortablePath, boolean keepMarkdownSuffix) {
    PortablePath path = PortablePath.parse(authoredPortablePath);
    String notePortion = path.notePortion();
    String href = notePortion.startsWith("/") ? notePortion : "/" + notePortion;
    if (keepMarkdownSuffix && !href.endsWith(".md")) {
      href += ".md";
    }
    return new PortablePath(Optional.empty(), href, path.encodedPropertyKey()).format();
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
        token -> PortablePath.replaceFolderName(token, oldFolderName, newFolderName);
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
                    PortablePath.replaceNotebookName(rawTargetToken, newNotebookName)));
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
    String authoredToken = rawTargetPart.trim();
    if (authoredToken.isEmpty() || PortablePath.isQualifiedToken(authoredToken)) {
      return storedLinkInner;
    }
    return keepVisibleInner(
        storedLinkInner,
        rawTargetToken -> PortablePath.replaceNotebookName(rawTargetToken, sourceNotebookName));
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
        token -> PortablePath.replaceNoteTitle(token, newNoteTitle.trim()),
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
        PortablePath.replaceNoteTitle(rawTargetPart.trim(), newNoteTitle.trim());
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
  static String replaceOsInvalidCharsInStoredLinkInner(String storedLinkInner) {
    if (storedLinkInner == null || storedLinkInner.isEmpty()) {
      return storedLinkInner;
    }
    UnaryOperator<String> convert =
        DisplayNamePathSeparators::replaceOsInvalidCharsInWikiLinkTarget;
    return rewriteAuthoredTarget(
        storedLinkInner, convert, () -> rewriteWikiInnerTarget(storedLinkInner, convert));
  }
}

package com.odde.donut.algorithms;

import com.odde.donut.validators.DisplayNamePathSeparators;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Rewrites one stored wiki Portable-path inner token. Note-ID URL Markdown links are never
 * rewritten here.
 */
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
    return keepVisibleText
        ? keepVisibleInner(storedLinkInner, _ -> authoredPortablePath)
        : rewriteWikiInnerTarget(storedLinkInner, _ -> authoredPortablePath);
  }

  /**
   * Rewrites one matching folder-name segment in a path-shaped wiki inner. Unqualified titles and
   * the note-title segment are left unchanged. Spelling is preserved.
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
    return rewriteWikiInnerTarget(
        storedLinkInner,
        token -> PortablePath.replaceFolderName(token, oldFolderName, newFolderName));
  }

  /**
   * Rewrites a path-shaped wiki inner to a new folder trail (empty trail → exact-root {@code
   * /Title}). Unqualified shorthand is unchanged. Display text, property selector, and optional
   * {@code .md} are preserved.
   */
  public static String newInnerForLocationChange(
      String storedLinkInner, List<String> newFolderNames) {
    if (storedLinkInner == null || storedLinkInner.isEmpty()) {
      return storedLinkInner;
    }
    return rewriteWikiInnerTarget(
        storedLinkInner, token -> PortablePath.replaceFolderTrail(token, newFolderNames));
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
        rawTargetToken -> PortablePath.replaceNotebookName(rawTargetToken, newNotebookName));
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
    return qualifyUnqualifiedWikiInner(storedLinkInner, sourceNotebookName);
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
    return rewriteWikiInnerNoteTitle(storedLinkInner, newNoteTitle, keepVisibleText);
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

  /** Converts OS-invalid characters in one wiki inner token. */
  static String replaceOsInvalidCharsInStoredLinkInner(String storedLinkInner) {
    if (storedLinkInner == null || storedLinkInner.isEmpty()) {
      return storedLinkInner;
    }
    UnaryOperator<String> convert =
        DisplayNamePathSeparators::replaceOsInvalidCharsInWikiLinkTarget;
    return rewriteWikiInnerTarget(storedLinkInner, convert);
  }
}

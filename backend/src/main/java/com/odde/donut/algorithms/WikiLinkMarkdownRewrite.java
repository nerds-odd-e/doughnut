package com.odde.donut.algorithms;

import com.odde.donut.validators.DisplayNamePathSeparators;
import java.util.List;

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
        ? WikiLinkMarkdown.splitInner(storedLinkInner)
            .rewriteTargetKeepingVisible(_ -> authoredPortablePath)
        : WikiLinkMarkdown.splitInner(storedLinkInner).rewriteTarget(_ -> authoredPortablePath);
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
    return WikiLinkMarkdown.splitInner(storedLinkInner)
        .rewriteTarget(
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
    return WikiLinkMarkdown.splitInner(storedLinkInner)
        .rewriteTarget(token -> PortablePath.replaceFolderTrail(token, newFolderNames));
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
    return WikiLinkMarkdown.splitInner(storedLinkInner)
        .rewriteTargetKeepingVisible(
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
    WikiLinkMarkdown.WikiInnerSplit token = WikiLinkMarkdown.splitInner(storedLinkInner);
    String authoredToken = token.rawTarget().trim();
    if (authoredToken.isEmpty() || PortablePath.isQualifiedToken(authoredToken)) {
      return storedLinkInner;
    }
    return token.rewriteTargetKeepingVisible(
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
    return WikiLinkMarkdown.splitInner(storedLinkInner)
        .rewriteNoteTitle(newNoteTitle, keepVisibleText);
  }

  /** Converts OS-invalid characters in one wiki inner token. */
  static String replaceOsInvalidCharsInStoredLinkInner(String storedLinkInner) {
    if (storedLinkInner == null || storedLinkInner.isEmpty()) {
      return storedLinkInner;
    }
    return WikiLinkMarkdown.splitInner(storedLinkInner)
        .rewriteTarget(DisplayNamePathSeparators::replaceOsInvalidCharsInWikiLinkTarget);
  }
}

package com.odde.donut.algorithms;

/**
 * Authored semantic note reference in note content (ADR 0001 Wiki link). Distinguishes wiki
 * Portable-path spelling from a Markdown note-ID URL target without treating ordinary Markdown
 * hrefs as Portable paths.
 */
public sealed interface AuthoredNoteReference
    permits AuthoredNoteReference.WikiPortablePathTarget, AuthoredNoteReference.NoteIdUrlTarget {

  /**
   * Spelling stored in the resolved-link index {@code authored_link} column (wiki inner text, or
   * the full Markdown link text for a note-ID URL).
   */
  String authoredLink();

  String displayText();

  /**
   * Identity key for first-occurrence de-duplication within one source note's authored references.
   * Wiki targets fold on {@link WikiLinkMarkdown#authoredTokenDedupeKey(String)}; note-ID URL
   * targets fold on the authored note ID.
   */
  default String sourceLocalKey() {
    return switch (this) {
      case WikiPortablePathTarget wiki ->
          WikiLinkMarkdown.authoredTokenDedupeKey(wiki.authoredLink());
      case NoteIdUrlTarget url -> "noteIdUrl:" + url.noteId();
    };
  }

  /**
   * Wiki {@code [[portable-path]]} / {@code [[portable-path|display]]} target. {@link
   * #authoredLink()} is the inner text between brackets (no {@code [[]]}).
   */
  record WikiPortablePathTarget(String authoredLink, PortablePath portablePath, String displayText)
      implements AuthoredNoteReference {

    public static WikiPortablePathTarget fromAuthoredInner(String authoredInner) {
      WikiLinkMarkdown.WikiInnerSplit parts = WikiLinkMarkdown.splitInner(authoredInner);
      return new WikiPortablePathTarget(authoredInner, parts.portablePath(), parts.displayText());
    }
  }

  /**
   * Markdown {@code [display](/nID)} or absolute Donut-origin note URL. The href's note ID is
   * authoritative; display text is label only.
   */
  record NoteIdUrlTarget(String authoredLink, int noteId, String href, String displayText)
      implements AuthoredNoteReference {}
}

package com.odde.donut.algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extracts {@link AuthoredNoteReference} values from note content. Currently emits only {@link
 * AuthoredNoteReference.WikiPortablePathTarget}; note-ID URL recognition is not wired yet.
 */
public final class AuthoredNoteReferences {

  private AuthoredNoteReferences() {}

  /**
   * Authored note references in document order: frontmatter scalar and list-item strings first,
   * then the body. Today only wiki Portable-path targets are emitted.
   */
  public static List<AuthoredNoteReference> inOccurrenceOrder(String content) {
    if (content == null || content.isEmpty()) {
      return List.of();
    }
    return NoteContentMarkdown.splitLeadingFrontmatter(content)
        .map(
            lf -> {
              List<AuthoredNoteReference> refs = new ArrayList<>();
              for (String value : lf.frontmatter().supportedValueStringsInInsertionOrder()) {
                refs.addAll(fromMarkdownFragment(value));
              }
              refs.addAll(fromMarkdownFragment(lf.body()));
              return List.copyOf(refs);
            })
        .orElseGet(() -> fromMarkdownFragment(content));
  }

  /**
   * First-occurrence unique references. Wiki targets use {@link
   * WikiLinkMarkdown#authoredTokenDedupeKey(String)} for note-target folding.
   */
  public static List<AuthoredNoteReference> uniquePreserveOrder(List<AuthoredNoteReference> refs) {
    List<AuthoredNoteReference> out = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (AuthoredNoteReference ref : refs) {
      if (seen.add(dedupeKey(ref))) {
        out.add(ref);
      }
    }
    return List.copyOf(out);
  }

  /** Wiki Portable-path authored inners only, in first-occurrence unique order. */
  public static List<AuthoredNoteReference.WikiPortablePathTarget> uniqueWikiPortablePathTargets(
      String content) {
    List<AuthoredNoteReference.WikiPortablePathTarget> out = new ArrayList<>();
    for (AuthoredNoteReference ref : uniquePreserveOrder(inOccurrenceOrder(content))) {
      if (ref instanceof AuthoredNoteReference.WikiPortablePathTarget wiki) {
        out.add(wiki);
      }
    }
    return List.copyOf(out);
  }

  static List<AuthoredNoteReference> fromMarkdownFragment(String markdown) {
    List<AuthoredNoteReference> refs = new ArrayList<>();
    for (String inner : WikiLinkMarkdown.authoredTokensInOccurrenceOrder(markdown)) {
      refs.add(AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner(inner));
    }
    return refs;
  }

  private static String dedupeKey(AuthoredNoteReference ref) {
    return switch (ref) {
      case AuthoredNoteReference.WikiPortablePathTarget wiki ->
          WikiLinkMarkdown.authoredTokenDedupeKey(wiki.authoredLink());
      case AuthoredNoteReference.NoteIdUrlTarget url -> "noteIdUrl:" + url.noteId();
    };
  }
}

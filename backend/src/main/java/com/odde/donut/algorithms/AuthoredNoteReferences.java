package com.odde.donut.algorithms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Extracts {@link AuthoredNoteReference} values from note content: wiki Portable-path targets and
 * recognized note-ID URLs (root-relative or absolute on the configured canonical origin).
 */
public final class AuthoredNoteReferences {

  private AuthoredNoteReferences() {}

  /**
   * Authored note references in document order: frontmatter scalar and list-item strings first,
   * then the body.
   */
  public static List<AuthoredNoteReference> inOccurrenceOrder(
      String content, CanonicalDonutOrigin canonicalOrigin) {
    if (content == null || content.isEmpty()) {
      return List.of();
    }
    return NoteContentMarkdown.splitLeadingFrontmatter(content)
        .map(
            lf -> {
              List<AuthoredNoteReference> refs = new ArrayList<>();
              for (String value : lf.frontmatter().supportedValueStringsInInsertionOrder()) {
                refs.addAll(fromMarkdownFragment(value, canonicalOrigin));
              }
              refs.addAll(fromMarkdownFragment(lf.body(), canonicalOrigin));
              return List.copyOf(refs);
            })
        .orElseGet(() -> fromMarkdownFragment(content, canonicalOrigin));
  }

  /**
   * First-occurrence unique references, keyed by {@link AuthoredNoteReference#sourceLocalKey()}.
   */
  public static List<AuthoredNoteReference> uniquePreserveOrder(List<AuthoredNoteReference> refs) {
    List<AuthoredNoteReference> out = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (AuthoredNoteReference ref : refs) {
      if (seen.add(ref.sourceLocalKey())) {
        out.add(ref);
      }
    }
    return List.copyOf(out);
  }

  /** Wiki Portable-path authored inners only, in first-occurrence unique order. */
  public static List<AuthoredNoteReference.WikiPortablePathTarget> uniqueWikiPortablePathTargets(
      String content) {
    List<AuthoredNoteReference.WikiPortablePathTarget> out = new ArrayList<>();
    for (AuthoredNoteReference ref :
        uniquePreserveOrder(inOccurrenceOrder(content, CanonicalDonutOrigin.production()))) {
      if (ref instanceof AuthoredNoteReference.WikiPortablePathTarget wiki) {
        out.add(wiki);
      }
    }
    return List.copyOf(out);
  }

  static List<AuthoredNoteReference> fromMarkdownFragment(
      String markdown, CanonicalDonutOrigin canonicalOrigin) {
    if (markdown == null || markdown.isEmpty()) {
      return List.of();
    }
    record Hit(int start, AuthoredNoteReference ref) {}
    List<Hit> hits = new ArrayList<>();
    Matcher wiki = WikiLinkMarkdown.INNER_LINK_PATTERN.matcher(markdown);
    while (wiki.find()) {
      String inner = wiki.group(1).trim();
      if (!inner.isEmpty()) {
        hits.add(
            new Hit(
                wiki.start(),
                AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner(inner)));
      }
    }
    Matcher md = NoteIdUrl.MARKDOWN_LINK.matcher(markdown);
    while (md.find()) {
      NoteIdUrl.fromMarkdownLinkMatch(md.group(), md.group(1), md.group(2), canonicalOrigin)
          .ifPresent(url -> hits.add(new Hit(md.start(), url)));
    }
    hits.sort(Comparator.comparingInt(Hit::start));
    List<AuthoredNoteReference> refs = new ArrayList<>(hits.size());
    for (Hit hit : hits) {
      refs.add(hit.ref());
    }
    return refs;
  }
}

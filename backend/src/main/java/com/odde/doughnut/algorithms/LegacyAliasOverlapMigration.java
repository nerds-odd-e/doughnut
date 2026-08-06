package com.odde.doughnut.algorithms;

import java.util.List;
import java.util.Optional;

/**
 * Moves legacy wiki-link overlap declarations from {@code aliases} into {@code overlaps} on content
 * save so {@code aliases} can stay plain-only.
 */
public final class LegacyAliasOverlapMigration {

  private LegacyAliasOverlapMigration() {}

  /**
   * Moves well-formed wiki-link items from {@code aliases} into {@code overlaps} (merge/dedupe),
   * leaving only plain alias strings under {@code aliases}. No-op when there are no such items or
   * no leading frontmatter.
   */
  public static String migrate(String content) {
    if (content == null) {
      return "";
    }
    Optional<NoteContentMarkdown.LeadingFrontmatter> split =
        NoteContentMarkdown.splitLeadingFrontmatter(content);
    if (split.isEmpty()) {
      return content;
    }
    NoteContentMarkdown.LeadingFrontmatter lf = split.get();
    Frontmatter fm = lf.frontmatter();
    List<String> legacyWiki = FrontmatterAliases.overlapWikiLinkTokensFromFrontmatter(fm);
    if (legacyWiki.isEmpty()) {
      return content;
    }
    List<String> mergedOverlaps =
        FrontmatterOverlaps.mergeDedupePreserveOrder(
            FrontmatterOverlaps.overlapWikiLinkTokensFromFrontmatter(fm), legacyWiki);
    List<String> plainAliases = FrontmatterAliases.fromFrontmatter(fm);
    Frontmatter updated =
        fm.setSequenceItems("overlaps", mergedOverlaps).setSequenceItems("aliases", plainAliases);
    return updated.isEmpty() ? lf.body() : updated.fenced(lf.body());
  }
}

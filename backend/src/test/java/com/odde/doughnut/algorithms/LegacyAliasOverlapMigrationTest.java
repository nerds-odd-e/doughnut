package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;

class LegacyAliasOverlapMigrationTest {

  @Test
  void migrate_moves_wiki_items_and_keeps_plain_aliases() {
    String content =
        """
        ---
        aliases:
          - color
          - "[[Partner]]"
        ---
        Colour means a hue
        """;

    String migrated = LegacyAliasOverlapMigration.migrate(content);

    assertThat(FrontmatterAliases.fromNoteContent(migrated), equalTo(List.of("color")));
    assertThat(
        FrontmatterOverlaps.overlapWikiLinkTokensFromNoteContent(migrated),
        equalTo(List.of("[[Partner]]")));
    assertThat(
        FrontmatterAliases.overlapWikiLinkTokensFromNoteContent(migrated), equalTo(List.of()));
  }

  @Test
  void migrate_is_noop_when_no_wiki_in_aliases() {
    String content =
        """
        ---
        aliases:
          - color
        overlaps:
          - "[[Partner]]"
        ---
        body
        """;

    assertThat(LegacyAliasOverlapMigration.migrate(content), equalTo(content));
  }

  @Test
  void migrate_merges_into_existing_overlaps() {
    String content =
        """
        ---
        aliases:
          - "[[Partner]]"
        overlaps:
          - "[[Other]]"
        ---
        body
        """;

    String migrated = LegacyAliasOverlapMigration.migrate(content);

    assertThat(FrontmatterAliases.fromNoteContent(migrated), equalTo(List.of()));
    assertThat(
        FrontmatterOverlaps.overlapWikiLinkTokensFromNoteContent(migrated),
        equalTo(List.of("[[Other]]", "[[Partner]]")));
  }
}

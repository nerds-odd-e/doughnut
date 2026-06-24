package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class TitleAliasMigrationTransformTest {

  @Test
  void preview_migrates_plain_title_aliases_to_frontmatter() {
    var preview = TitleAliasMigrationTransform.preview("colour／color", "## body\n");

    assertThat(preview.status(), equalTo(TitleAliasMigrationPreviewStatus.MIGRATE));
    assertThat(preview.plannedTitle(), equalTo("colour"));
    assertThat(preview.plannedAliases(), contains("color"));
    assertThat(preview.plannedContent(), containsString("aliases:"));
    assertThat(preview.plannedContent(), containsString("- color"));
    assertThat(preview.plannedContent(), containsString("## body"));
  }

  @Test
  void preview_retains_qualifier_and_tilde_suffix_fragments_in_title() {
    var preview = TitleAliasMigrationTransform.preview("word／~logical／alias", "body");

    assertThat(preview.status(), equalTo(TitleAliasMigrationPreviewStatus.MIGRATE));
    assertThat(preview.plannedTitle(), equalTo("word／~logical"));
    assertThat(preview.plannedAliases(), contains("alias"));
  }

  @Test
  void preview_merges_title_aliases_after_existing_frontmatter_aliases() {
    String content = "---\naliases:\n  - hue\n---\n\nbody";
    var preview = TitleAliasMigrationTransform.preview("colour／color", content);

    assertThat(preview.plannedAliases(), contains("hue", "color"));
    assertThat(preview.plannedContent(), containsString("- hue"));
    assertThat(preview.plannedContent(), containsString("- color"));
  }

  @Test
  void preview_reports_no_changes_for_primary_only_title() {
    var preview = TitleAliasMigrationTransform.preview("colour", "body");

    assertThat(preview.status(), equalTo(TitleAliasMigrationPreviewStatus.NO_CHANGES));
    assertThat(preview.plannedTitle(), equalTo("colour"));
    assertThat(preview.plannedAliases(), is(empty()));
    assertThat(preview.plannedContent(), equalTo("body"));
  }

  @Test
  void preview_reports_no_changes_for_tilde_only_segments() {
    var preview = TitleAliasMigrationTransform.preview("word／~suffix", "body");

    assertThat(preview.status(), equalTo(TitleAliasMigrationPreviewStatus.NO_CHANGES));
    assertThat(preview.plannedTitle(), equalTo("word／~suffix"));
  }
}

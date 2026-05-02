package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.Set;
import org.junit.jupiter.api.Test;

class NoteFrontmatterWikiLinkTokensTest {

  @Test
  void extracts_parent_field_wikilinks_only() {
    String details = "---\nparent: \"[[Alpha]]\"\n---\n\nSee [[Beta]].";
    Set<String> keys =
        NoteFrontmatterWikiLinkTokens.normalizedWikiLinkTokensFromYamlField(details, "parent");
    assertThat(keys, containsInAnyOrder("Alpha"));
  }

  @Test
  void extracts_source_field_wikilinks_only() {
    String details =
        "---\n"
            + "type: relationship\n"
            + "relation: specializes\n"
            + "source: \"[[Subject title]]\"\n"
            + "target: \"[[Target title]]\"\n"
            + "---\n\n"
            + "[[Subject title]] specializes [[Target title]].";
    Set<String> keys =
        NoteFrontmatterWikiLinkTokens.normalizedWikiLinkTokensFromYamlField(details, "source");
    assertThat(keys, containsInAnyOrder("Subject title"));
  }
}

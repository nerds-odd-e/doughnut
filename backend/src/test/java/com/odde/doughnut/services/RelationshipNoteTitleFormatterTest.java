package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RelationType;
import org.junit.jupiter.api.Test;

class RelationshipNoteTitleFormatterTest {

  @Test
  void formats_short_source_relation_and_target() {
    String title =
        RelationshipNoteTitleFormatter.format(
            "Physics", RelationType.RELATED_TO.label, "Chemistry");
    assertThat(title, equalTo("Physics related to Chemistry"));
    assertThat(title.length() <= Note.MAX_TITLE_LENGTH, is(true));
  }

  @Test
  void truncates_to_max_title_length() {
    String source = "a".repeat(80);
    String target = "b".repeat(80);
    String composed = source + " " + RelationType.RELATED_TO.label + " " + target;
    String title =
        RelationshipNoteTitleFormatter.format(source, RelationType.RELATED_TO.label, target);
    assertThat(title.length(), equalTo(Note.MAX_TITLE_LENGTH));
    assertThat(title, equalTo(composed.substring(0, Note.MAX_TITLE_LENGTH)));
  }

  @Test
  void blank_source_and_target_use_untitled_placeholders() {
    String title = RelationshipNoteTitleFormatter.format("", RelationType.PART.label, "");
    assertThat(title, equalTo("Untitled a part of Untitled"));
  }

  @Test
  void blank_relation_label_defaults_to_related_to() {
    String title = RelationshipNoteTitleFormatter.format("A", "  ", "B");
    assertThat(title, equalTo("A related to B"));
  }
}

package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.algorithms.NotePropertyIndexPlanner.ValueKey;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NotePropertyIndexPlannerTest {

  @Test
  void plannedRows_leaves_no_index_row_for_note_level() {
    List<String> keys =
        NotePropertyIndexPlanner.plannedRows(Frontmatter.parse("note_level: 2\ntopic: physics\n"))
            .stream()
            .map(NotePropertyIndexPlanner.PlannedRow::propertyKey)
            .toList();

    assertThat(keys, containsInAnyOrder("topic"));
  }

  @Test
  void plannedRows_keeps_every_list_item_with_its_value() {
    List<NotePropertyIndexPlanner.PlannedRow> rows =
        NotePropertyIndexPlanner.plannedRows(
            Frontmatter.parse("example of:\n  - alpha\n  - \"[[B]]\"\n"));

    assertThat(
        rows.stream().map(NotePropertyIndexPlanner.PlannedRow::propertyValue).toList(),
        contains("alpha", "[[B]]"));
    assertThat(rows.get(0).sourceLocalKey(), nullValue());
    assertThat(rows.get(1).sourceLocalKey(), notNullValue());
  }

  @Test
  void plannedRows_gives_a_scalar_an_empty_value() {
    List<NotePropertyIndexPlanner.PlannedRow> rows =
        NotePropertyIndexPlanner.plannedRows(Frontmatter.parse("topic: physics\n"));

    assertThat(
        rows.stream().map(NotePropertyIndexPlanner.PlannedRow::propertyValue).toList(),
        contains(""));
  }

  @Test
  void plannedRows_skips_list_items_longer_than_the_value_column() {
    String longItem = "x".repeat(256);
    List<NotePropertyIndexPlanner.PlannedRow> rows =
        NotePropertyIndexPlanner.plannedRows(
            Frontmatter.parse("topic:\n  - " + longItem + "\n  - short\n"));

    assertThat(
        rows.stream().map(NotePropertyIndexPlanner.PlannedRow::propertyValue).toList(),
        contains("short"));
  }

  @Test
  void plannedRows_keeps_only_the_first_of_a_repeated_list_value() {
    List<NotePropertyIndexPlanner.PlannedRow> rows =
        NotePropertyIndexPlanner.plannedRows(
            Frontmatter.parse("example of:\n  - \"[[A]]\"\n  - \"[[B]]\"\n  - \"[[A]]\"\n"));

    assertThat(
        rows.stream().map(NotePropertyIndexPlanner.PlannedRow::itemIndex).toList(), contains(0, 1));
  }

  private static Set<ValueKey> changed(String before, String after) {
    return NotePropertyIndexPlanner.changedValues(
        Frontmatter.parse(before), Frontmatter.parse(after));
  }

  private static ValueKey item(String key, String text) {
    return new ValueKey(key, text);
  }

  private static ValueKey scalar(String key) {
    return new ValueKey(key, "");
  }

  @Test
  void changedValues_reports_every_value_of_a_frontmatter_that_had_none() {
    assertThat(
        NotePropertyIndexPlanner.changedValues(
            Frontmatter.empty(), Frontmatter.parse("topic: physics\ncolors:\n  - x\n")),
        containsInAnyOrder(scalar("topic"), item("colors", "x")));
  }

  @Test
  void changedValues_reports_added_items_and_new_or_edited_scalars_only() {
    assertThat(
        changed(
            "topic: physics\nstatus: draft\ncolors:\n  - x\n",
            "topic: chemistry\nstatus: draft\ncolors:\n  - x\n  - y\norigin: book\n"),
        containsInAnyOrder(scalar("topic"), item("colors", "y"), scalar("origin")));
  }

  @Test
  void changedValues_reports_an_item_whose_text_changed_as_the_new_item() {
    assertThat(
        changed("colors:\n  - alpha\n  - beta\n", "colors:\n  - alpha\n  - betta\n"),
        contains(item("colors", "betta")));
  }

  @Test
  void changedValues_ignores_reordering_and_reformatting_of_the_same_parsed_values() {
    assertThat(
        changed(
            "topic: physics\ncolors:\n  - x\n  - \"[[B]]\"\n",
            "colors: ['[[B]]', x]\ntopic: \"physics\"\n"),
        empty());
  }

  @Test
  void changedValues_ignores_removed_values() {
    assertThat(changed("topic: physics\ncolors:\n  - x\n  - y\n", "colors:\n  - y\n"), empty());
  }

  @Test
  void changedValues_ignores_values_the_index_does_not_hold() {
    String longItem = "x".repeat(256);
    assertThat(
        changed(
            "note_level: 1\n",
            "note_level: 2\ntags: [x]\ncolors:\n  - \"  \"\n  - " + longItem + "\n"),
        empty());
  }
}

package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
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
}

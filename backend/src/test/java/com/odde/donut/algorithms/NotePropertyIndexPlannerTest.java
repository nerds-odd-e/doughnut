package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

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
}

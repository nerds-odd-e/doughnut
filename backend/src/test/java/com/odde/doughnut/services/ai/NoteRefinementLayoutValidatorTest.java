package com.odde.doughnut.services.ai;

import static com.odde.doughnut.services.ai.NoteRefinementLayoutItems.leaf;
import static com.odde.doughnut.services.ai.NoteRefinementLayoutItems.parent;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class NoteRefinementLayoutValidatorTest {
  @Test
  void acceptsAUniqueTwoLevelLayout() {
    NoteRefinementLayout layout =
        new NoteRefinementLayout(
            List.of(
                parent(
                    "parent", "Parent point", List.of(leaf("child", "Child point", true, false)))));

    assertThat(NoteRefinementLayoutValidator.isValid(layout)).isTrue();
  }

  @Test
  void rejectsGrandchildren() {
    NoteRefinementLayout layout =
        new NoteRefinementLayout(
            List.of(
                parent(
                    "parent",
                    "Parent point",
                    List.of(
                        new NoteRefinementLayoutItem(
                            "child",
                            "Child point",
                            false,
                            false,
                            List.of(leaf("grandchild", "Grandchild point")))))));

    assertThat(NoteRefinementLayoutValidator.isValid(layout)).isFalse();
  }

  @Test
  void rejectsDuplicateIds() {
    NoteRefinementLayout layout =
        new NoteRefinementLayout(List.of(leaf("same", "Point 1"), leaf("same", "Point 2")));

    assertThat(NoteRefinementLayoutValidator.isValid(layout)).isFalse();
  }

  @Test
  void rejectsBlankText() {
    NoteRefinementLayout layout = new NoteRefinementLayout(List.of(leaf("p1", " ")));

    assertThat(NoteRefinementLayoutValidator.isValid(layout)).isFalse();
  }
}

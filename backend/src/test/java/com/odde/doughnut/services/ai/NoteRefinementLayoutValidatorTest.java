package com.odde.doughnut.services.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class NoteRefinementLayoutValidatorTest {
  @Test
  void acceptsAUniqueTwoLevelLayout() {
    NoteRefinementLayout layout =
        new NoteRefinementLayout(
            List.of(
                new NoteRefinementLayoutItem(
                    "parent",
                    "Parent point",
                    false,
                    false,
                    List.of(
                        new NoteRefinementLayoutItem(
                            "child", "Child point", true, false, List.of())))));

    assertThat(NoteRefinementLayoutValidator.isValid(layout)).isTrue();
  }

  @Test
  void rejectsGrandchildren() {
    NoteRefinementLayout layout =
        new NoteRefinementLayout(
            List.of(
                new NoteRefinementLayoutItem(
                    "parent",
                    "Parent point",
                    false,
                    false,
                    List.of(
                        new NoteRefinementLayoutItem(
                            "child",
                            "Child point",
                            false,
                            false,
                            List.of(
                                new NoteRefinementLayoutItem(
                                    "grandchild",
                                    "Grandchild point",
                                    false,
                                    false,
                                    List.of())))))));

    assertThat(NoteRefinementLayoutValidator.isValid(layout)).isFalse();
  }

  @Test
  void rejectsDuplicateIds() {
    NoteRefinementLayout layout =
        new NoteRefinementLayout(
            List.of(
                new NoteRefinementLayoutItem("same", "Point 1", false, false, List.of()),
                new NoteRefinementLayoutItem("same", "Point 2", false, false, List.of())));

    assertThat(NoteRefinementLayoutValidator.isValid(layout)).isFalse();
  }

  @Test
  void rejectsBlankText() {
    NoteRefinementLayout layout =
        new NoteRefinementLayout(
            List.of(new NoteRefinementLayoutItem("p1", " ", false, false, List.of())));

    assertThat(NoteRefinementLayoutValidator.isValid(layout)).isFalse();
  }
}

package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AssimilationServicePropertyWikiLinkGateTest extends AssimilationServiceTestBase {
  @Autowired NotePropertyIndexService notePropertyIndexService;

  private Note carrierWithExampleOf(Note sibling, String content) {
    return carrierOnNotebook(sibling.getNotebook(), content);
  }

  private Note carrierWithExampleOf(Notebook notebook, String content) {
    return carrierOnNotebook(notebook, content);
  }

  private Note carrierOnNotebook(Notebook notebook, String content) {
    Note carrier = makeMe.aNote().notebook(notebook).content(content).please();
    notePropertyIndexService.refreshForNote(carrier);
    makeMe.aMemoryTrackerFor(carrier).assimilatedAt(day1).please();
    return carrier;
  }

  @Test
  void gates_list_property_until_all_resolved_targets_are_assimilated() {
    Note targetA = makeMe.aNote().title("A").notebookOwnedBy(user).please();
    Note targetB = makeMe.aNote().title("B").underSameNotebookAs(targetA).please();
    Note carrier =
        carrierWithExampleOf(
            targetA,
            "---\n" + "example of:\n" + "  - \"[[A]]\"\n" + "  - \"[[B]]\"\n" + "---\n\nbody");

    assertThat(assimilationService.getCounts().getTotalUnassimilatedCount(), equalTo(2));
    AssimilationUnit next = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(next.note(), equalTo(targetA));
    assertThat(next.propertyKey(), nullValue());

    makeMe.aMemoryTrackerFor(targetA).assimilatedAt(day1).please();

    AssimilationUnit stillGated = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(stillGated.note(), equalTo(targetB));

    makeMe.aMemoryTrackerFor(targetB).assimilatedAt(day1).please();

    AssimilationUnit property = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(property.note(), equalTo(carrier));
    assertThat(property.propertyKey(), equalTo("example of"));
  }

  @Test
  void gates_property_while_target_note_is_still_pending() {
    Note target = makeMe.aNote().title("Word").notebookOwnedBy(user).please();
    Note carrier = carrierWithExampleOf(target, "---\nexample of: \"[[Word]]\"\n---\n\nbody");

    AssimilationUnit next = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(next.note(), equalTo(target));
    assertThat(next.propertyKey(), nullValue());

    makeMe.aMemoryTrackerFor(target).assimilatedAt(day1).please();

    AssimilationUnit property = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(property.note(), equalTo(carrier));
    assertThat(property.propertyKey(), equalTo("example of"));
  }

  @Test
  void gates_property_when_target_is_in_a_different_notebook() {
    Notebook otherNotebook =
        makeMe.aNotebook().creatorAndOwner(user).name("Other Notebook").please();
    Note target = makeMe.aNote().title("Word").notebook(otherNotebook).please();
    Notebook ownNotebook = makeMe.aNotebook().creatorAndOwner(user).name("My Notebook").please();
    carrierWithExampleOf(ownNotebook, "---\nexample of: \"[[Other Notebook:Word]]\"\n---\n\nbody");

    AssimilationUnit next = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(next.note(), equalTo(target));
    assertThat(next.propertyKey(), nullValue());
  }

  @Test
  void gates_property_when_target_has_only_spelling_tracker() {
    Note target = makeMe.aNote().title("Word").notebookOwnedBy(user).please();
    makeMe.aMemoryTrackerFor(target).spelling().please();
    Note carrier = carrierWithExampleOf(target, "---\nexample of: \"[[Word]]\"\n---\n\nbody");

    AssimilationUnit next = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(next.note(), equalTo(target));
    assertThat(next.propertyKey(), nullValue());
  }

  @Test
  void offers_property_when_target_has_skipped_note_level_tracker() {
    Note target = makeMe.aNote().title("Word").notebookOwnedBy(user).please();
    makeMe.aMemoryTrackerFor(target).removedFromTracking().please();
    Note carrier = carrierWithExampleOf(target, "---\nexample of: \"[[Word]]\"\n---\n\nbody");

    AssimilationUnit next = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(next.note(), equalTo(carrier));
    assertThat(next.propertyKey(), equalTo("example of"));
  }

  @Test
  void offers_property_when_target_note_is_deleted() {
    Note target = makeMe.aNote().title("Word").notebookOwnedBy(user).please();
    Note carrier =
        makeMe
            .aNote()
            .underSameNotebookAs(target)
            .content("---\nexample of: \"[[Word]]\"\n---\n\nbody")
            .please();
    notePropertyIndexService.refreshForNote(carrier);
    target.setDeletedAt(makeMe.aTimestamp().please());
    makeMe.entityPersister.merge(target);
    makeMe.aMemoryTrackerFor(carrier).assimilatedAt(day1).please();

    AssimilationUnit next = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(next.note(), equalTo(carrier));
    assertThat(next.propertyKey(), equalTo("example of"));
  }
}

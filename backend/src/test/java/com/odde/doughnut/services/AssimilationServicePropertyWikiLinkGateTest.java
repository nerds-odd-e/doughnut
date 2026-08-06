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

  private Note carrierWithExampleOf(Notebook notebook, String content) {
    Note carrier = makeMe.aNote().notebook(notebook).content(content).please();
    notePropertyIndexService.refreshForNote(carrier);
    makeMe.aMemoryTrackerFor(carrier).assimilatedAt(day1).please();
    return carrier;
  }

  @Test
  void gates_list_property_until_all_resolved_targets_are_assimilated() {
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    Note targetA = makeMe.aNote().title("A").notebook(notebook).please();
    Note targetB = makeMe.aNote().title("B").notebook(notebook).please();
    Note carrier =
        carrierWithExampleOf(
            notebook,
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
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    Note target = makeMe.aNote().title("Word").notebook(notebook).please();
    Note carrier = carrierWithExampleOf(notebook, "---\nexample of: \"[[Word]]\"\n---\n\nbody");

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
  void offers_property_when_target_skips_memory_tracking() {
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    makeMe.aNote().title("Word").notebook(notebook).skipMemoryTracking().please();
    Note carrier = carrierWithExampleOf(notebook, "---\nexample of: \"[[Word]]\"\n---\n\nbody");

    AssimilationUnit next = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(next.note(), equalTo(carrier));
    assertThat(next.propertyKey(), equalTo("example of"));
  }

  @Test
  void offers_property_when_target_note_is_deleted() {
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    Note target = makeMe.aNote().title("Word").notebook(notebook).please();
    Note carrier =
        makeMe
            .aNote()
            .notebook(notebook)
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

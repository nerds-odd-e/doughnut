package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AssimilationServicePropertyWikiLinkGateTest {
  @Autowired MakeMe makeMe;
  @Autowired AssimilationServiceFactory assimilationServiceFactory;
  @Autowired NotePropertyIndexService notePropertyIndexService;

  User user;
  Timestamp day1;
  AssimilationService assimilationService;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
    assimilationService = assimilationServiceFactory.create(user, day1, ZoneId.of("Asia/Shanghai"));
  }

  @Test
  void gates_list_property_until_all_resolved_targets_are_assimilated() {
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    Note targetA = makeMe.aNote().title("A").notebook(notebook).please();
    Note targetB = makeMe.aNote().title("B").notebook(notebook).please();
    Note carrier =
        makeMe
            .aNote()
            .notebook(notebook)
            .content(
                "---\n" + "example of:\n" + "  - \"[[A]]\"\n" + "  - \"[[B]]\"\n" + "---\n\nbody")
            .please();
    notePropertyIndexService.refreshForNote(carrier);
    makeMe.aMemoryTrackerFor(carrier).by(user).assimilatedAt(day1).please();

    assertThat(assimilationService.getCounts().getTotalUnassimilatedCount(), equalTo(2));
    AssimilationUnit next = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(next.note(), equalTo(targetA));
    assertThat(next.propertyKey(), nullValue());

    makeMe.aMemoryTrackerFor(targetA).by(user).assimilatedAt(day1).please();

    assertThat(assimilationService.getCounts().getTotalUnassimilatedCount(), equalTo(1));
    AssimilationUnit stillGated = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(stillGated.note(), equalTo(targetB));
    assertThat(stillGated.propertyKey(), nullValue());

    makeMe.aMemoryTrackerFor(targetB).by(user).assimilatedAt(day1).please();

    assertThat(assimilationService.getCounts().getTotalUnassimilatedCount(), equalTo(1));
    AssimilationUnit property = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(property.note(), equalTo(carrier));
    assertThat(property.propertyKey(), equalTo("example of"));
  }

  @Test
  void gates_property_while_target_note_is_still_pending() {
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    Note target = makeMe.aNote().title("Word").notebook(notebook).please();
    Note carrier =
        makeMe
            .aNote()
            .notebook(notebook)
            .content("---\nexample of: \"[[Word]]\"\n---\n\nbody")
            .please();
    notePropertyIndexService.refreshForNote(carrier);
    makeMe.aMemoryTrackerFor(carrier).by(user).assimilatedAt(day1).please();

    assertThat(assimilationService.getCounts().getTotalUnassimilatedCount(), equalTo(1));
    AssimilationUnit next = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(next.note(), equalTo(target));
    assertThat(next.propertyKey(), nullValue());

    makeMe.aMemoryTrackerFor(target).by(user).assimilatedAt(day1).please();

    assertThat(assimilationService.getCounts().getTotalUnassimilatedCount(), equalTo(1));
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
    Note carrier =
        makeMe
            .aNote()
            .notebook(ownNotebook)
            .content("---\nexample of: \"[[Other Notebook:Word]]\"\n---\n\nbody")
            .please();
    notePropertyIndexService.refreshForNote(carrier);
    makeMe.aMemoryTrackerFor(carrier).by(user).assimilatedAt(day1).please();

    assertThat(assimilationService.getCounts().getTotalUnassimilatedCount(), equalTo(1));
    AssimilationUnit next = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(next.note(), equalTo(target));
    assertThat(next.propertyKey(), nullValue());
  }

  @Test
  void offers_property_when_target_skips_memory_tracking() {
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    Note target = makeMe.aNote().title("Word").notebook(notebook).skipMemoryTracking().please();
    Note carrier =
        makeMe
            .aNote()
            .notebook(notebook)
            .content("---\nexample of: \"[[Word]]\"\n---\n\nbody")
            .please();
    notePropertyIndexService.refreshForNote(carrier);
    makeMe.aMemoryTrackerFor(carrier).by(user).assimilatedAt(day1).please();

    assertThat(assimilationService.getCounts().getTotalUnassimilatedCount(), equalTo(1));
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
    makeMe.aMemoryTrackerFor(carrier).by(user).assimilatedAt(day1).please();

    assertThat(assimilationService.getCounts().getTotalUnassimilatedCount(), equalTo(1));
    AssimilationUnit next = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(next.note(), equalTo(carrier));
    assertThat(next.propertyKey(), equalTo("example of"));
  }
}

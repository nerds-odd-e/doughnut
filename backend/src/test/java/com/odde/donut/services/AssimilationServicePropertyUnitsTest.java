package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AssimilationServicePropertyUnitsTest extends AssimilationServiceTestBase {

  private Note noteWithExampleOf() {
    Note note =
        makeMe
            .aNote()
            .notebookOwnedBy(user)
            .content("---\nexample of: \"[[Word]]\"\n---\n\nbody")
            .please();
    notePropertyIndexService.refreshForNote(note);
    return note;
  }

  @Test
  void counts_unassimilated_example_of_when_note_is_already_assimilated() {
    Note note = noteWithExampleOf();
    makeMe.aMemoryTrackerFor(note).assimilatedAt(day1).please();

    assertThat(assimilationService.getCounts().getTotalUnassimilatedCount(), equalTo(1));
    assertThat(assimilationService.getCounts().getDueCount(), equalTo(1));
  }

  @Test
  void does_not_count_property_with_skipped_tracker() {
    Note note =
        makeMe.aNote().notebookOwnedBy(user).content("---\ntopic: physics\n---\n\nbody").please();
    notePropertyIndexService.refreshForNote(note);
    makeMe.aMemoryTrackerFor(note).assimilatedAt(day1).please();
    makeMe.aMemoryTrackerFor(note).propertyKey("topic").removedFromTracking().please();

    assertThat(assimilationService.getCounts().getTotalUnassimilatedCount(), equalTo(0));
  }

  @Test
  void returns_example_of_property_as_next_unit_when_note_is_assimilated() {
    Note note = noteWithExampleOf();
    makeMe.aMemoryTrackerFor(note).assimilatedAt(day1).please();

    AssimilationUnit next = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(next.note(), equalTo(note));
    assertThat(next.propertyKey(), equalTo("example of"));
  }

  @Test
  void subscription_daily_budget_caps_note_and_property_units_combined() {
    User notebookOwner = makeMe.aUser().please();
    Notebook subscribedNotebook = makeMe.aNotebook().creatorAndOwner(notebookOwner).please();
    Note note =
        makeMe
            .aNote()
            .notebook(subscribedNotebook)
            .content("---\nexample of: \"[[Word]]\"\n---\n\nbody")
            .please();
    notePropertyIndexService.refreshForNote(note);
    makeMe.aSubscription().forNotebook(subscribedNotebook).forUser(user).daily(1).please();
    makeMe.refresh(user);

    assertThat(assimilationService.getCounts().getTotalUnassimilatedCount(), equalTo(2));

    AssimilationUnit first = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(first.note(), equalTo(note));
    assertThat(first.propertyKey(), nullValue());

    makeMe.aMemoryTrackerFor(note).by(user).assimilatedAt(day1).please();
    assertThat(assimilationService.getNextAssimilationUnit().isEmpty(), is(true));
    assertThat(assimilationService.getCounts().getTotalUnassimilatedCount(), equalTo(1));

    Timestamp day2 = makeMe.aTimestamp().of(2, 8).fromShanghai().please();
    AssimilationUnit nextDay =
        assimilationServiceFor(user, day2).getNextAssimilationUnit().orElseThrow();
    assertThat(nextDay.note(), equalTo(note));
    assertThat(nextDay.propertyKey(), equalTo("example of"));
  }

  @Test
  void note_level_unit_before_property_on_same_note() {
    Note note = noteWithExampleOf();

    AssimilationUnit next = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(next.note(), equalTo(note));
    assertThat(next.propertyKey(), nullValue());
  }

  @Nested
  class OrderedByChangeDate {
    Timestamp oldNoteCreated = makeMe.aTimestamp().of(0, 8).fromShanghai().please();
    Timestamp newNoteCreated = makeMe.aTimestamp().of(0, 9).fromShanghai().please();
    Note oldTrackedNote;
    Note newUntrackedNote;

    @BeforeEach
    void setup() {
      oldTrackedNote =
          makeMe
              .aNote("old")
              .notebookOwnedBy(user)
              .createdAt(oldNoteCreated)
              .content("---\ntopic: physics\n---\n\nbody")
              .please();
      notePropertyIndexService.refreshForNote(oldTrackedNote);
      makeMe.aMemoryTrackerFor(oldTrackedNote).assimilatedAt(day1).please();
      newUntrackedNote =
          makeMe.aNote("new").notebookOwnedBy(user).createdAt(newNoteCreated).please();
    }

    @Test
    void property_dated_after_a_newer_notes_creation_is_offered_after_that_note() {
      dateScalarProperty(
          oldTrackedNote, "topic", makeMe.aTimestamp().of(0, 10).fromShanghai().please());

      assertThat(getNextNoteToAssimilate(assimilationService), equalTo(newUntrackedNote));
      makeMe.aMemoryTrackerFor(newUntrackedNote).assimilatedAt(day1).please();
      AssimilationUnit next = assimilationService.getNextAssimilationUnit().orElseThrow();
      assertThat(next.note(), equalTo(oldTrackedNote));
      assertThat(next.propertyKey(), equalTo("topic"));
    }

    @Test
    void undated_property_sorts_at_its_notes_creation() {
      AssimilationUnit next = assimilationService.getNextAssimilationUnit().orElseThrow();
      assertThat(next.note(), equalTo(oldTrackedNote));
      assertThat(next.propertyKey(), equalTo("topic"));
    }

    @Test
    void property_dated_at_a_newer_notes_creation_breaks_the_tie_by_note() {
      dateScalarProperty(oldTrackedNote, "topic", newNoteCreated);

      AssimilationUnit next = assimilationService.getNextAssimilationUnit().orElseThrow();
      assertThat(next.note(), equalTo(oldTrackedNote));
      assertThat(next.propertyKey(), equalTo("topic"));
    }
  }

  @Test
  void properties_dated_at_their_notes_creation_follow_the_note_unit_in_key_order() {
    Timestamp created = makeMe.aTimestamp().of(0, 8).fromShanghai().please();
    Note note =
        makeMe
            .aNote()
            .notebookOwnedBy(user)
            .createdAt(created)
            .content("---\ntopic: physics\ndefinition: foo\n---\n\nbody")
            .please();
    notePropertyIndexService.refreshForNote(note);
    dateScalarProperty(note, "topic", created);
    dateScalarProperty(note, "definition", created);

    AssimilationUnit first = assimilationService.getNextAssimilationUnit().orElseThrow();
    assertThat(first.propertyKey(), nullValue());
    makeMe.aMemoryTrackerFor(note).assimilatedAt(day1).please();
    for (String expectedKey : List.of("definition", "topic")) {
      AssimilationUnit next = assimilationService.getNextAssimilationUnit().orElseThrow();
      assertThat(next.propertyKey(), equalTo(expectedKey));
      makeMe.aMemoryTrackerFor(note).propertyKey(expectedKey).assimilatedAt(day1).please();
    }
  }

  @Test
  void multiple_untracked_properties_ordered_by_property_key() {
    Note note =
        makeMe
            .aNote()
            .notebookOwnedBy(user)
            .content("---\ntopic: physics\nexample of: \"[[Word]]\"\ndefinition: foo\n---\n\nbody")
            .please();
    notePropertyIndexService.refreshForNote(note);
    makeMe.aMemoryTrackerFor(note).assimilatedAt(day1).please();

    for (String expectedKey : List.of("definition", "example of", "topic")) {
      AssimilationUnit next = assimilationService.getNextAssimilationUnit().orElseThrow();
      assertThat(next.propertyKey(), equalTo(expectedKey));
      makeMe.aMemoryTrackerFor(note).propertyKey(expectedKey).assimilatedAt(day1).please();
    }
  }

  @Test
  void daily_cap_limits_property_units_in_due_count() {
    makeMe.theUser(user).dailyAssimilationCount(1).please();
    Timestamp previousDay = makeMe.aTimestamp().of(0, 8).fromShanghai().please();
    Note note1 =
        makeMe
            .aNote("n1")
            .notebookOwnedBy(user)
            .content("---\nexample of: \"[[A]]\"\n---\n\nbody")
            .please();
    Note note2 =
        makeMe
            .aNote("n2")
            .notebookOwnedBy(user)
            .content("---\nexample of: \"[[B]]\"\n---\n\nbody")
            .please();
    notePropertyIndexService.refreshForNote(note1);
    notePropertyIndexService.refreshForNote(note2);
    makeMe.aMemoryTrackerFor(note1).assimilatedAt(previousDay).please();
    makeMe.aMemoryTrackerFor(note2).assimilatedAt(previousDay).please();

    assertThat(assimilationService.getCounts().getTotalUnassimilatedCount(), equalTo(2));
    assertThat(assimilationService.getCounts().getDueCount(), equalTo(1));
  }
}

package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.Subscription;
import com.odde.donut.entities.User;
import com.odde.donut.testability.SpringTestBase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UnassimilatedPropertyServiceTest extends SpringTestBase {
  private static final String TWO_VALUES =
      "---\nexample of:\n  - \"[[run]]\"\n  - \"[[past tense]]\"\n---\n\nbody";

  @Autowired NotePropertyIndexService notePropertyIndexService;
  @Autowired UnassimilatedPropertyService unassimilatedPropertyService;

  private Note noteWithContent(User user, String content) {
    Note note = makeMe.aNote().notebookOwnedBy(user).content(content).please();
    notePropertyIndexService.refreshForNote(note);
    return note;
  }

  private void reindex(Note note, String content) {
    note.setContent(content);
    notePropertyIndexService.refreshForNote(note);
  }

  private List<String> pendingValuesForUser(User user) {
    return pendingPropertiesForUser(user).stream()
        .map(unit -> unit.propertyKey() + "=" + unit.propertyValue())
        .toList();
  }

  private List<AssimilationUnit> pendingPropertiesForUser(User user) {
    return unassimilatedPropertyService
        .streamUnassimilatedPropertiesForUser(user, unit -> true)
        .toList();
  }

  @Test
  void mixed_exact_keys_remain_separate_property_units() {
    User user = makeMe.aUser().please();
    noteWithContent(
        user,
        "---\n"
            + "example of:\n"
            + "  - alpha\n"
            + "  - beta\n"
            + "example of 2: gamma\n"
            + "---\n\nbody");

    assertThat(
        pendingValuesForUser(user),
        containsInAnyOrder("example of=alpha", "example of=beta", "example of 2="));
  }

  @Test
  void list_property_emits_one_unit_per_value() {
    User user = makeMe.aUser().please();
    noteWithContent(user, TWO_VALUES);

    assertThat(unassimilatedPropertyService.countUnassimilatedPropertiesForUser(user), equalTo(2));
    assertThat(
        pendingValuesForUser(user), contains("example of=[[run]]", "example of=[[past tense]]"));
  }

  @Test
  void value_tracker_suppresses_only_that_value() {
    User user = makeMe.aUser().please();
    Note note = noteWithContent(user, TWO_VALUES);
    makeMe.aMemoryTrackerFor(note).propertyKey("example of").propertyValue("[[run]]").please();

    assertThat(unassimilatedPropertyService.countUnassimilatedPropertiesForUser(user), equalTo(1));
    assertThat(pendingValuesForUser(user), contains("example of=[[past tense]]"));
  }

  @Test
  void value_tracker_still_matches_after_reordering_or_removing_other_value() {
    User user = makeMe.aUser().please();
    Note note = noteWithContent(user, TWO_VALUES);
    makeMe.aMemoryTrackerFor(note).propertyKey("example of").propertyValue("[[run]]").please();

    reindex(note, "---\nexample of:\n  - \"[[past tense]]\"\n  - \"[[run]]\"\n---\n\nbody");
    assertThat(pendingValuesForUser(user), contains("example of=[[past tense]]"));

    reindex(note, "---\nexample of:\n  - \"[[run]]\"\n---\n\nbody");
    assertThat(pendingValuesForUser(user), empty());
  }

  @Test
  void scalar_property_emits_one_unit_with_empty_value() {
    User user = makeMe.aUser().please();
    noteWithContent(user, "---\ntopic: physics\n---\n\nbody");

    assertThat(pendingValuesForUser(user), contains("topic="));
  }

  @Test
  void key_skip_suppresses_every_value() {
    User user = makeMe.aUser().please();
    Note note = noteWithContent(user, TWO_VALUES);
    makeMe.anAssimilationSequenceSkipFor(note).propertyKey("example of").please();

    assertThat(unassimilatedPropertyService.countUnassimilatedPropertiesForUser(user), equalTo(0));
  }

  @Test
  void counts_indexed_example_of_when_no_property_tracker() {
    User user = makeMe.aUser().please();
    Note note = noteWithContent(user, "---\nexample of: \"[[Word]]\"\n---\n\nbody");

    AssimilationUnit pending = pendingPropertiesForUser(user).getFirst();
    assertThat(pending.propertyKey(), equalTo("example of"));
    assertThat(pending.note(), equalTo(note));
  }

  @Test
  void does_not_count_when_property_tracker_exists() {
    User user = makeMe.aUser().please();
    Note note = noteWithContent(user, "---\ntopic: physics\n---\n\nbody");
    makeMe.aMemoryTrackerFor(note).propertyKey("topic").please();

    assertThat(unassimilatedPropertyService.countUnassimilatedPropertiesForUser(user), equalTo(0));
  }

  @Test
  void does_not_count_when_property_tracker_is_skipped() {
    User user = makeMe.aUser().please();
    Note note = noteWithContent(user, "---\ntopic: physics\n---\n\nbody");
    makeMe.aMemoryTrackerFor(note).propertyKey("topic").removedFromTracking().please();

    assertThat(unassimilatedPropertyService.countUnassimilatedPropertiesForUser(user), equalTo(0));
  }

  @Test
  void does_not_count_when_property_has_sequence_skip() {
    User user = makeMe.aUser().please();
    Note note = noteWithContent(user, "---\ntopic: physics\n---\n\nbody");
    makeMe.anAssimilationSequenceSkipFor(note).propertyKey("topic").please();

    assertThat(unassimilatedPropertyService.countUnassimilatedPropertiesForUser(user), equalTo(0));
  }

  @Test
  void does_not_count_reserved_keys_not_in_index() {
    User user = makeMe.aUser().please();
    noteWithContent(user, "---\nimage: /x\nurl: https://example.com\n---\n\nbody");

    assertThat(unassimilatedPropertyService.countUnassimilatedPropertiesForUser(user), equalTo(0));
  }

  @Test
  void counts_unassimilated_properties_in_subscribed_notebook() {
    User owner = makeMe.aUser().please();
    User subscriber = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    makeMe.aSubscription().forNotebook(notebook).forUser(subscriber).please();
    Note note =
        makeMe
            .aNote()
            .notebook(notebook)
            .content("---\nexample of: \"[[Word]]\"\n---\n\nbody")
            .please();
    notePropertyIndexService.refreshForNote(note);
    makeMe.refresh(subscriber);

    Subscription subscription = subscriber.getSubscriptions().stream().findFirst().orElseThrow();
    List<AssimilationUnit> pending =
        unassimilatedPropertyService
            .streamUnassimilatedPropertiesForSubscription(subscription, unit -> true)
            .toList();
    assertThat(pending, hasSize(1));
    assertThat(pending.get(0).propertyKey(), equalTo("example of"));
    assertThat(
        unassimilatedPropertyService.countUnassimilatedPropertiesForSubscription(subscription),
        equalTo(1));
  }
}

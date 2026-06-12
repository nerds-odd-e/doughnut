package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UnassimilatedPropertyServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired NotePropertyIndexService notePropertyIndexService;
  @Autowired UnassimilatedPropertyService unassimilatedPropertyService;

  @Test
  void counts_indexed_example_of_when_no_property_tracker() {
    User user = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    Note note =
        makeMe
            .aNote()
            .notebook(notebook)
            .content("---\nexample of: \"[[Word]]\"\n---\n\nbody")
            .please();
    notePropertyIndexService.refreshForNote(note);

    assertThat(unassimilatedPropertyService.countUnassimilatedPropertiesForUser(user), equalTo(1));
    assertThat(
        unassimilatedPropertyService.streamUnassimilatedPropertiesForUser(user).toList(),
        hasSize(1));
    AssimilationUnit pending =
        unassimilatedPropertyService.streamUnassimilatedPropertiesForUser(user).findFirst().get();
    assertThat(pending.propertyKey(), equalTo("example of"));
    assertThat(pending.note(), equalTo(note));
  }

  @Test
  void does_not_count_when_property_tracker_exists() {
    User user = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    Note note =
        makeMe.aNote().notebook(notebook).content("---\ntopic: physics\n---\n\nbody").please();
    notePropertyIndexService.refreshForNote(note);
    makeMe.aMemoryTrackerFor(note).by(user).propertyKey("topic").please();

    assertThat(unassimilatedPropertyService.countUnassimilatedPropertiesForUser(user), equalTo(0));
  }

  @Test
  void does_not_count_when_property_tracker_is_skipped() {
    User user = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    Note note =
        makeMe.aNote().notebook(notebook).content("---\ntopic: physics\n---\n\nbody").please();
    notePropertyIndexService.refreshForNote(note);
    makeMe.aMemoryTrackerFor(note).by(user).propertyKey("topic").removedFromTracking().please();

    assertThat(unassimilatedPropertyService.countUnassimilatedPropertiesForUser(user), equalTo(0));
  }

  @Test
  void does_not_count_reserved_keys_not_in_index() {
    User user = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    Note note =
        makeMe
            .aNote()
            .notebook(notebook)
            .content("---\nimage: /x\nurl: https://example.com\n---\n\nbody")
            .please();
    notePropertyIndexService.refreshForNote(note);

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
    assertThat(
        unassimilatedPropertyService.countUnassimilatedPropertiesForSubscription(subscription),
        equalTo(1));
    List<AssimilationUnit> pending =
        unassimilatedPropertyService
            .streamUnassimilatedPropertiesForSubscription(subscription)
            .toList();
    assertThat(pending, hasSize(1));
    assertThat(pending.get(0).propertyKey(), equalTo("example of"));
  }
}

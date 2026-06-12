package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
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
class AssimilationServicePropertyUnitsTest {
  @Autowired MakeMe makeMe;
  @Autowired SubscriptionService subscriptionService;
  @Autowired UserService userService;
  @Autowired UnassimilatedPropertyService unassimilatedPropertyService;
  @Autowired NotePropertyIndexService notePropertyIndexService;

  User user;
  Timestamp day1;
  AssimilationService assimilationService;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
    assimilationService = assimilationServiceFor(user, day1);
  }

  private AssimilationService assimilationServiceFor(User forUser, Timestamp at) {
    return new AssimilationService(
        forUser,
        userService,
        subscriptionService,
        unassimilatedPropertyService,
        at,
        ZoneId.of("Asia/Shanghai"));
  }

  @Test
  void counts_unassimilated_example_of_when_note_is_already_assimilated() {
    Note note =
        makeMe
            .aNote()
            .notebookOwnedBy(user)
            .content("---\nexample of: \"[[Word]]\"\n---\n\nbody")
            .please();
    notePropertyIndexService.refreshForNote(note);
    makeMe.aMemoryTrackerFor(note).by(user).assimilatedAt(day1).please();

    assertThat(assimilationService.getCounts().getTotalUnassimilatedCount(), equalTo(1));
    assertThat(assimilationService.getCounts().getDueCount(), equalTo(1));
  }

  @Test
  void does_not_count_property_with_skipped_tracker() {
    Note note =
        makeMe.aNote().notebookOwnedBy(user).content("---\ntopic: physics\n---\n\nbody").please();
    notePropertyIndexService.refreshForNote(note);
    makeMe.aMemoryTrackerFor(note).by(user).assimilatedAt(day1).please();
    makeMe.aMemoryTrackerFor(note).by(user).propertyKey("topic").removedFromTracking().please();

    assertThat(assimilationService.getCounts().getTotalUnassimilatedCount(), equalTo(0));
  }

  @Test
  void returns_example_of_property_as_next_unit_when_note_is_assimilated() {
    Note note =
        makeMe
            .aNote()
            .notebookOwnedBy(user)
            .content("---\nexample of: \"[[Word]]\"\n---\n\nbody")
            .please();
    notePropertyIndexService.refreshForNote(note);
    makeMe.aMemoryTrackerFor(note).by(user).assimilatedAt(day1).please();

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
    AssimilationService day2Service = assimilationServiceFor(user, day2);
    AssimilationUnit nextDay = day2Service.getNextAssimilationUnit().orElseThrow();
    assertThat(nextDay.note(), equalTo(note));
    assertThat(nextDay.propertyKey(), equalTo("example of"));
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
    makeMe.aMemoryTrackerFor(note1).by(user).assimilatedAt(previousDay).please();
    makeMe.aMemoryTrackerFor(note2).by(user).assimilatedAt(previousDay).please();

    assertThat(assimilationService.getCounts().getTotalUnassimilatedCount(), equalTo(2));
    assertThat(assimilationService.getCounts().getDueCount(), equalTo(1));
  }
}

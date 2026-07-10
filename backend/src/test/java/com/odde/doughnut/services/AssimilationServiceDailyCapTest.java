package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AssimilationServiceDailyCapTest extends AssimilationServiceTestBase {

  @Nested
  class WhenTheUserSetToRecall1NewNoteOnlyPerDay {
    Note note1;
    Note note2;

    @BeforeEach
    void setup() {
      note1 = makeMe.aNote("note1").notebookOwnedBy(user).please();
      note2 = makeMe.aNote("note2").notebookOwnedBy(user).please();
      userService.setDailyAssimilationCount(user, 1);
    }

    @Test
    void shouldReturnOneIfUsersDailySettingIsOne() {
      assertThat(getNextNoteToAssimilate(assimilationService), equalTo(note1));
    }

    @Test
    void shouldNotCountSkippedMemoryTracker() {
      makeMe.aMemoryTrackerFor(note1).by(user).assimilatedAt(day1).removedFromTracking().please();
      assertThat(getNextNoteToAssimilate(assimilationService), is(note2));
    }

    @Test
    void shouldIncludeNotesThatAreRecalledByOtherPeople() {
      makeMe.aMemoryTrackerFor(note1).by(anotherUser).assimilatedAt(day1).please();
      assertThat(getNextNoteToAssimilate(assimilationService), equalTo(note1));
    }

    @Test
    void theDailyCountShouldNotBeResetOnSameDayDifferentHour() {
      makeMe.aMemoryTrackerFor(note1).by(user).assimilatedAt(day1).please();
      Timestamp day1_23 = makeMe.aTimestamp().of(1, 23).fromShanghai().please();
      AssimilationService recallService = assimilationServiceFor(user, day1_23);
      assertThat(getNextNoteToAssimilate(recallService), equalTo(note2));
      assertThat(recallService.getCounts().getDueCount(), equalTo(0));
    }

    @Test
    void theDailyCountShouldBeResetOnNextDay() {
      makeMe.aMemoryTrackerFor(note1).by(user).assimilatedAt(day1).please();
      Timestamp day2 = makeMe.aTimestamp().of(2, 1).fromShanghai().please();
      AssimilationService recallService = assimilationServiceFor(user, day2);
      assertThat(getNextNoteToAssimilate(recallService), equalTo(note2));
    }
  }

  @Nested
  class WhenRecalledMoreThanDailyLimitLastNight {
    Note note1;
    Note note2;
    Note note3;
    AssimilationService earlyMorningService;
    Timestamp earlyMorning;
    Timestamp lateMorning;

    @BeforeEach
    void setup() {
      makeMe.theUser(user).dailyAssimilationCount(2).please();
      User anotherUser = makeMe.aUser().please();
      Notebook topNb = makeMe.aNotebook().creatorAndOwner(anotherUser).please();
      note1 = makeMe.aNote().notebook(topNb).please();
      note2 = makeMe.aNote().notebook(topNb).please();
      note3 = makeMe.aNote().notebook(topNb).please();
      makeMe.aNote().notebook(topNb).please();

      makeMe.aSubscription().forNotebook(topNb).forUser(user).daily(1).please();

      Notebook userNb = makeMe.aNotebook().creatorAndOwner(user).please();
      makeMe.aNote().notebook(userNb).please();

      makeMe.refresh(user);

      earlyMorning = makeMe.aTimestamp().of(1, 6).fromShanghai().please();
      lateMorning = makeMe.aTimestamp().of(1, 10).fromShanghai().please();

      makeMe.aMemoryTrackerFor(note1).by(user).assimilatedAt(earlyMorning).please();
      makeMe.aMemoryTrackerFor(note2).by(user).assimilatedAt(earlyMorning).please();
      makeMe.aMemoryTrackerFor(note3).by(user).assimilatedAt(earlyMorning).please();

      earlyMorningService = assimilationServiceFor(user, lateMorning);
    }

    @Test
    void returnsNextNoteWithZeroDueCountWhenUserDailyPlanComplete() {
      assertThat(earlyMorningService.getNextNoteToAssimilate().isPresent(), is(true));
      assertThat(earlyMorningService.getCounts().getDueCount(), equalTo(0));
    }
  }
}

package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.dto.NoteDeleteDTO;
import com.odde.doughnut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.WikiTitleCacheService;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerDeleteReduceToSourceTests extends ControllerTestBase {
  @Autowired MemoryTrackerRepository memoryTrackerRepository;
  @Autowired NoteController controller;
  @Autowired WikiTitleCacheService wikiTitleCacheService;
  @MockitoBean HttpClientAdapter httpClientAdapter;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private NoteDeleteDTO reduceToSourcePropertyDeleteRequest(String sourcePropertyKey) {
    NoteDeleteDTO dto = new NoteDeleteDTO();
    dto.setReferenceHandling(NoteDeleteReferenceHandling.REDUCE_TO_SOURCE_PROPERTY);
    dto.setSourcePropertyKey(sourcePropertyKey);
    return dto;
  }

  private Note aRelation(Note source, Note target, String relationLabel) {
    Note relation =
        makeMe
            .aNote()
            .underSameNotebookAs(source)
            .asRelationship(relationLabel, source, target)
            .please();
    wikiTitleCacheService.refreshForNote(relation, currentUser.getUser());
    return relation;
  }

  @Test
  void shouldAddRelationLabelPropertyToSourceAndSoftDeleteRelationNote()
      throws UnexpectedNoAccessRightException {
    Note moon = makeMe.aNote("Moon").notebookOwnedBy(currentUser.getUser()).please();
    Note earth = makeMe.aNote("Earth").underSameNotebookAs(moon).please();
    Note relation = aRelation(moon, earth, "a part of");

    controller.deleteNote(relation, reduceToSourcePropertyDeleteRequest("a part of"));

    assertThat(relation.getDeletedAt(), is(not(nullValue())));
    assertThat(moon.getContent(), containsString("a part of"));
    assertThat(moon.getContent(), containsString("[[Earth]]"));
    assertThat(earth.getContent(), not(containsString("a part of")));
  }

  @Test
  void shouldRehomeRelationNoteLevelTrackerAsPropertyTrackerOnSource()
      throws UnexpectedNoAccessRightException {
    Note moon = makeMe.aNote("Moon").notebookOwnedBy(currentUser.getUser()).please();
    Note earth = makeMe.aNote("Earth").underSameNotebookAs(moon).please();
    Note relation = aRelation(moon, earth, "a part of");
    MemoryTracker relationTracker =
        makeMe
            .aMemoryTrackerFor(relation)
            .afterNthStrictRecall(3)
            .forgettingCurveAndNextRecallAt(5.5f)
            .please();
    int trackerId = relationTracker.getId();
    int recallCountBefore = relationTracker.getRecallCount();
    float forgettingCurveBefore = relationTracker.getForgettingCurveIndex();
    Timestamp nextRecallBefore = relationTracker.getNextRecallAt();

    controller.deleteNote(relation, reduceToSourcePropertyDeleteRequest("a part of"));

    MemoryTracker reloaded = memoryTrackerRepository.findById(trackerId).orElseThrow();
    assertThat(reloaded.getDeletedAt(), is(nullValue()));
    assertThat(reloaded.getNote().getId(), equalTo(moon.getId()));
    assertThat(reloaded.getPropertyKey(), equalTo("a part of"));
    assertThat(reloaded.getSpelling(), equalTo(false));
    assertThat(reloaded.getRecallCount(), equalTo(recallCountBefore));
    assertThat(reloaded.getForgettingCurveIndex(), equalTo(forgettingCurveBefore));
    assertThat(reloaded.getNextRecallAt(), equalTo(nextRecallBefore));
    assertThat(
        memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), relation.getId()),
        empty());
  }

  @Test
  void shouldUseSuffixedPropertyKeyWhenSourceAlreadyHasPropertyKey()
      throws UnexpectedNoAccessRightException {
    Note moon =
        makeMe
            .aNote("Moon")
            .notebookOwnedBy(currentUser.getUser())
            .content("---\na part of: \"[[Mars]]\"\n---\n")
            .please();
    Note earth = makeMe.aNote("Earth").underSameNotebookAs(moon).please();
    Note relation = aRelation(moon, earth, "a part of");

    controller.deleteNote(relation, reduceToSourcePropertyDeleteRequest("a part of"));

    assertThat(moon.getContent(), containsString("a part of 2"));
    assertThat(moon.getContent(), containsString("[[Earth]]"));
  }

  @Test
  void shouldRehomeTrackerWithSuffixedPropertyKeyWhenSourceAlreadyHasPropertyKey()
      throws UnexpectedNoAccessRightException {
    Note moon =
        makeMe
            .aNote("Moon")
            .notebookOwnedBy(currentUser.getUser())
            .content("---\na part of: \"[[Mars]]\"\n---\n")
            .please();
    Note earth = makeMe.aNote("Earth").underSameNotebookAs(moon).please();
    Note relation = aRelation(moon, earth, "a part of");
    int trackerId = makeMe.aMemoryTrackerFor(relation).please().getId();

    controller.deleteNote(relation, reduceToSourcePropertyDeleteRequest("a part of"));

    MemoryTracker reloaded = memoryTrackerRepository.findById(trackerId).orElseThrow();
    assertThat(reloaded.getNote().getId(), equalTo(moon.getId()));
    assertThat(reloaded.getPropertyKey(), equalTo("a part of 2"));
  }

  @Test
  void shouldUseExampleOfPropertyKeyWhenReducingExampleOfRelation()
      throws UnexpectedNoAccessRightException {
    Note word = makeMe.aNote("Word").notebookOwnedBy(currentUser.getUser()).please();
    Note earth = makeMe.aNote("Earth").underSameNotebookAs(word).please();
    Note relation = aRelation(word, earth, "an example of");
    int trackerId = makeMe.aMemoryTrackerFor(relation).please().getId();

    controller.deleteNote(relation, reduceToSourcePropertyDeleteRequest("an example of"));

    assertThat(word.getContent(), containsString("example of:"));
    assertThat(word.getContent(), containsString("[[Earth]]"));
    MemoryTracker reloaded = memoryTrackerRepository.findById(trackerId).orElseThrow();
    assertThat(reloaded.getPropertyKey(), equalTo("example of"));
  }
}

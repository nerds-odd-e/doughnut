package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.dto.NoteDeleteDTO;
import com.odde.doughnut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.WikiTitleCacheService;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import java.sql.Timestamp;
import java.util.List;
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

  private static String relationshipNoteContent(String sourceTitle, String targetTitle) {
    return relationshipNoteContent("a-part-of", sourceTitle, targetTitle);
  }

  private static String relationshipNoteContent(
      String relationKebab, String sourceTitle, String targetTitle) {
    return "---\n"
        + "type: relationship\n"
        + "relation: "
        + relationKebab
        + "\n"
        + "source: \"[["
        + sourceTitle
        + "]]\"\n"
        + "target: \"[["
        + targetTitle
        + "]]\"\n"
        + "---\n\n";
  }

  @Test
  void shouldAddRelationLabelPropertyToSourceAndSoftDeleteRelationNote()
      throws UnexpectedNoAccessRightException {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    Note moon = makeMe.aNote("Moon").notebook(nb).please();
    Note earth = makeMe.aNote("Earth").notebook(nb).please();
    Note relation =
        makeMe.aNote().notebook(nb).content(relationshipNoteContent("Moon", "Earth")).please();
    wikiTitleCacheService.refreshForNote(relation, currentUser.getUser());

    controller.deleteNote(relation, reduceToSourcePropertyDeleteRequest("a part of"));

    assertThat(relation.getDeletedAt(), is(not(nullValue())));
    assertThat(moon.getContent(), containsString("a part of"));
    assertThat(moon.getContent(), containsString("[[Earth]]"));
    assertThat(earth.getContent(), not(containsString("a part of")));
  }

  @Test
  void shouldRehomeRelationNoteLevelTrackerAsPropertyTrackerOnSource()
      throws UnexpectedNoAccessRightException {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    Note moon = makeMe.aNote("Moon").notebook(nb).please();
    makeMe.aNote("Earth").notebook(nb).please();
    Note relation =
        makeMe.aNote().notebook(nb).content(relationshipNoteContent("Moon", "Earth")).please();
    wikiTitleCacheService.refreshForNote(relation, currentUser.getUser());
    MemoryTracker relationTracker =
        makeMe
            .aMemoryTrackerFor(relation)
            .by(currentUser.getUser())
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
    List<MemoryTracker> sourceTrackers =
        memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), moon.getId());
    assertThat(sourceTrackers, hasSize(1));
    assertThat(sourceTrackers.getFirst().getId(), equalTo(trackerId));
  }

  @Test
  void shouldUseSuffixedPropertyKeyWhenSourceAlreadyHasPropertyKey()
      throws UnexpectedNoAccessRightException {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    Note moon =
        makeMe.aNote("Moon").notebook(nb).content("---\na part of: \"[[Mars]]\"\n---\n").please();
    makeMe.aNote("Earth").notebook(nb).please();
    Note relation =
        makeMe.aNote().notebook(nb).content(relationshipNoteContent("Moon", "Earth")).please();

    controller.deleteNote(relation, reduceToSourcePropertyDeleteRequest("a part of"));

    assertThat(relation.getDeletedAt(), is(not(nullValue())));
    assertThat(moon.getContent(), containsString("a part of 2"));
    assertThat(moon.getContent(), containsString("[[Earth]]"));
  }

  @Test
  void shouldRehomeTrackerWithSuffixedPropertyKeyWhenSourceAlreadyHasPropertyKey()
      throws UnexpectedNoAccessRightException {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    Note moon =
        makeMe.aNote("Moon").notebook(nb).content("---\na part of: \"[[Mars]]\"\n---\n").please();
    makeMe.aNote("Earth").notebook(nb).please();
    Note relation =
        makeMe.aNote().notebook(nb).content(relationshipNoteContent("Moon", "Earth")).please();
    MemoryTracker relationTracker =
        makeMe.aMemoryTrackerFor(relation).by(currentUser.getUser()).please();
    int trackerId = relationTracker.getId();

    controller.deleteNote(relation, reduceToSourcePropertyDeleteRequest("a part of"));

    MemoryTracker reloaded = memoryTrackerRepository.findById(trackerId).orElseThrow();
    assertThat(reloaded.getDeletedAt(), is(nullValue()));
    assertThat(reloaded.getNote().getId(), equalTo(moon.getId()));
    assertThat(reloaded.getPropertyKey(), equalTo("a part of 2"));
    assertThat(
        memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), moon.getId()),
        hasSize(1));
  }

  @Test
  void shouldUseExampleOfPropertyKeyWhenReducingExampleOfRelation()
      throws UnexpectedNoAccessRightException {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    Note word = makeMe.aNote("Word").notebook(nb).please();
    makeMe.aNote("Earth").notebook(nb).please();
    Note relation =
        makeMe
            .aNote()
            .notebook(nb)
            .content(relationshipNoteContent("an-example-of", "Word", "Earth"))
            .please();
    wikiTitleCacheService.refreshForNote(relation, currentUser.getUser());
    MemoryTracker relationTracker =
        makeMe.aMemoryTrackerFor(relation).by(currentUser.getUser()).please();
    int trackerId = relationTracker.getId();

    controller.deleteNote(relation, reduceToSourcePropertyDeleteRequest("an example of"));

    assertThat(relation.getDeletedAt(), is(not(nullValue())));
    assertThat(word.getContent(), containsString("example of:"));
    assertThat(word.getContent(), containsString("[[Earth]]"));
    MemoryTracker reloaded = memoryTrackerRepository.findById(trackerId).orElseThrow();
    assertThat(reloaded.getNote().getId(), equalTo(word.getId()));
    assertThat(reloaded.getPropertyKey(), equalTo("example of"));
  }
}

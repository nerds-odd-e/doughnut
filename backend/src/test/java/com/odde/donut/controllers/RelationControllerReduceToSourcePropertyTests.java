package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.NoteReferenceService;
import com.odde.donut.testability.RelationshipNoteMarkdown;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RelationControllerReduceToSourcePropertyTests extends ControllerTestBase {
  @Autowired RelationController controller;
  @Autowired NoteRepository noteRepository;
  @Autowired NoteReferenceService noteReferenceService;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void reducesTheRelationshipIntoTheSourcePropertyAndPermanentlyDeletesTheRelationshipNote()
      throws UnexpectedNoAccessRightException {
    Note source = makeMe.aNote("Moon").notebookOwnedBy(currentUser.getUser()).please();
    Note target = makeMe.aNote("Earth").underSameNotebookAs(source).please();
    Note relation =
        makeMe
            .aNote()
            .underSameNotebookAs(source)
            .asRelationship("a part of", source, target)
            .please();
    noteReferenceService.refreshDerivedIndexesForNote(relation);
    Integer relationId = relation.getId();

    NoteRealm result = controller.reduceToSourceProperty(relation);

    assertThat(result.getNote().getId(), equalTo(source.getId()));
    assertThat(source.getContent(), containsString("a part of"));
    assertThat(source.getContent(), containsString("[[Earth]]"));
    assertThat(noteRepository.findById(relationId).isEmpty(), equalTo(true));
  }

  @Test
  void collidingKeySuffixesTheNewPropertyWhenTheSourceAlreadyHoldsIt()
      throws UnexpectedNoAccessRightException {
    Note source =
        makeMe
            .aNote("Moon")
            .notebookOwnedBy(currentUser.getUser())
            .content("---\na part of: \"[[Somewhere]]\"\n---\n")
            .please();
    Note target = makeMe.aNote("Earth").underSameNotebookAs(source).please();
    Note relation =
        makeMe
            .aNote()
            .underSameNotebookAs(source)
            .asRelationship("a part of", source, target)
            .please();
    noteReferenceService.refreshDerivedIndexesForNote(relation);

    controller.reduceToSourceProperty(relation);

    assertThat(source.getContent(), containsString("a part of 2"));
    assertThat(source.getContent(), containsString("[[Earth]]"));
  }

  @Test
  void movesEveryLearnersUnderstandingTrackerAndDropsTheSpellingTracker()
      throws UnexpectedNoAccessRightException {
    Note source = makeMe.aNote("Moon").notebookOwnedBy(currentUser.getUser()).please();
    Note target = makeMe.aNote("Earth").underSameNotebookAs(source).please();
    Note relation =
        makeMe
            .aNote()
            .underSameNotebookAs(source)
            .asRelationship("a part of", source, target)
            .please();
    noteReferenceService.refreshDerivedIndexesForNote(relation);
    User otherLearner = makeMe.aUser().please();
    MemoryTracker activeTracker =
        makeMe.aMemoryTrackerFor(relation).by(currentUser.getUser()).recallCount(1).please();
    MemoryTracker removedTracker =
        makeMe
            .aMemoryTrackerFor(relation)
            .by(otherLearner)
            .recallCount(1)
            .removedFromTracking()
            .please();
    makeMe.aMemoryTrackerFor(relation).by(otherLearner).spelling().please();
    float activeStability = activeTracker.getStability();
    float removedStability = removedTracker.getStability();
    var activeNextRecallAt = activeTracker.getNextRecallAt();
    var removedNextRecallAt = removedTracker.getNextRecallAt();

    controller.reduceToSourceProperty(relation);

    assertThat(activeTracker.getNote().getId(), equalTo(source.getId()));
    assertThat(activeTracker.getPropertyKey(), equalTo("a part of"));
    assertThat(activeTracker.getStability(), equalTo(activeStability));
    assertThat(activeTracker.getNextRecallAt(), equalTo(activeNextRecallAt));
    assertThat(activeTracker.getRecallCount(), equalTo(1));
    assertThat(activeTracker.getRemovedFromTracking(), equalTo(false));

    assertThat(removedTracker.getNote().getId(), equalTo(source.getId()));
    assertThat(removedTracker.getPropertyKey(), equalTo("a part of"));
    assertThat(removedTracker.getStability(), equalTo(removedStability));
    assertThat(removedTracker.getNextRecallAt(), equalTo(removedNextRecallAt));
    assertThat(removedTracker.getRecallCount(), equalTo(1));
    assertThat(removedTracker.getRemovedFromTracking(), equalTo(true));

    assertThat(memoryTrackerRepository.findByNote_IdIn(List.of(relation.getId())), empty());
  }

  @Test
  void refusesToReduceARelationshipNoteThatHasBodyText() throws UnexpectedNoAccessRightException {
    Note source = makeMe.aNote("Moon").notebookOwnedBy(currentUser.getUser()).please();
    Note target = makeMe.aNote("Earth").underSameNotebookAs(source).please();
    String sourceContentBefore = source.getContent();
    Note relation =
        makeMe
            .aNote()
            .underSameNotebookAs(source)
            .content(
                RelationshipNoteMarkdown.forEndpoints(
                    null, "a part of", source, target, "Observations from orbit."))
            .please();
    noteReferenceService.refreshDerivedIndexesForNote(relation);
    Integer relationId = relation.getId();
    String relationContentBefore = relation.getContent();

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> controller.reduceToSourceProperty(relation));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    assertThat(noteRepository.findById(relationId).isPresent(), equalTo(true));
    assertThat(relation.getContent(), equalTo(relationContentBefore));
    assertThat(source.getContent(), equalTo(sourceContentBefore));
  }

  @Test
  void refusesToReduceARelationshipNoteWhoseSourceCannotBeResolved()
      throws UnexpectedNoAccessRightException {
    Note source = makeMe.aNote("Moon").notebookOwnedBy(currentUser.getUser()).please();
    Note target = makeMe.aNote("Earth").underSameNotebookAs(source).please();
    String sourceContentBefore = source.getContent();
    String unresolvedContent =
        "---\n"
            + "type: Relationship\n"
            + "relation: a-part-of\n"
            + "source: \"[[Nonexistent Note]]\"\n"
            + "target: \"[[Earth]]\"\n"
            + "---\n";
    Note relation = makeMe.aNote().underSameNotebookAs(source).content(unresolvedContent).please();
    noteReferenceService.refreshDerivedIndexesForNote(relation);
    Integer relationId = relation.getId();
    String relationContentBefore = relation.getContent();

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> controller.reduceToSourceProperty(relation));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    assertThat(noteRepository.findById(relationId).isPresent(), equalTo(true));
    assertThat(relation.getContent(), equalTo(relationContentBefore));
    assertThat(source.getContent(), equalTo(sourceContentBefore));
  }
}

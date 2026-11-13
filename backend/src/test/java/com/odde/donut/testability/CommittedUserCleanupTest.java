package com.odde.donut.testability;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.odde.donut.entities.Circle;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CommittedUserCleanupTest extends SpringTestBase {
  @Autowired EntityManager entityManager;

  @Test
  void cleansLearningOwnedByTheTargetOrItsNotesAndKeepsUnrelatedLearning() {
    User target = makeMe.aUser().please();
    User survivor = makeMe.aUser().please();
    Circle circle = makeMe.aCircle().hasMember(survivor).please();
    Notebook externalNotebook = makeMe.aNotebook().creatorAndOwner(survivor).owner(circle).please();
    Note external = makeMe.aNote().notebook(externalNotebook).please();
    Note owned = makeMe.aNote().notebookOwnedBy(target).please();
    Note unrelated = makeMe.aNote().notebookOwnedBy(survivor).please();
    MemoryTracker targetTracker = makeMe.aMemoryTrackerFor(external).by(target).please();
    RecallPrompt targetPrompt = makeMe.aRecallPrompt().forMemoryTracker(targetTracker).please();
    MemoryTracker otherTracker = makeMe.aMemoryTrackerFor(owned).by(survivor).please();
    RecallPrompt otherPrompt = makeMe.aRecallPrompt().forMemoryTracker(otherTracker).please();
    RecallPrompt unrelatedPrompt =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(makeMe.aMemoryTrackerFor(unrelated).please())
            .please();
    Integer targetQuestionId = targetPrompt.getMcq().getId();
    Integer otherQuestionId = otherPrompt.getMcq().getId();
    Integer unrelatedQuestionId = unrelatedPrompt.getMcq().getId();
    Integer targetTrackerId = targetTracker.getId();
    Integer otherTrackerId = otherTracker.getId();
    Integer targetPromptId = targetPrompt.getId();
    Integer otherPromptId = otherPrompt.getId();
    Integer unrelatedPromptId = unrelatedPrompt.getId();
    Integer ownedNoteId = owned.getId();
    Integer externalNoteId = external.getId();
    Integer unrelatedNoteId = unrelated.getId();
    Integer targetUserId = target.getId();
    Integer survivorUserId = survivor.getId();
    String targetIdentifier = target.getExternalIdentifier();
    makeMe.entityPersister.flushAndClear();

    CommittedUserCleanup.deleteByUserExternalIdentifierLike(
        entityManager, mcqRepository, targetIdentifier);
    makeMe.entityPersister.flushAndClear();

    assertAll(
        () -> assertThat(entityManager.find(Mcq.class, targetQuestionId), nullValue()),
        () -> assertThat(entityManager.find(Mcq.class, otherQuestionId), nullValue()),
        () -> assertThat(entityManager.find(MemoryTracker.class, targetTrackerId), nullValue()),
        () -> assertThat(entityManager.find(MemoryTracker.class, otherTrackerId), nullValue()),
        () -> assertThat(entityManager.find(RecallPrompt.class, targetPromptId), nullValue()),
        () -> assertThat(entityManager.find(RecallPrompt.class, otherPromptId), nullValue()),
        () -> assertThat(entityManager.find(Note.class, ownedNoteId), nullValue()),
        () -> assertThat(entityManager.find(User.class, targetUserId), nullValue()),
        () -> assertThat(entityManager.find(Mcq.class, unrelatedQuestionId), notNullValue()),
        () ->
            assertThat(
                entityManager.find(RecallPrompt.class, unrelatedPromptId).getMcq().getId(),
                equalTo(unrelatedQuestionId)),
        () -> assertThat(entityManager.find(Note.class, externalNoteId), notNullValue()),
        () -> assertThat(entityManager.find(Note.class, unrelatedNoteId), notNullValue()),
        () -> assertThat(entityManager.find(User.class, survivorUserId), notNullValue()));
  }
}

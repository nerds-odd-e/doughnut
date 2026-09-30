package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.RecallLogRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.NoteReferenceService;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NoteReifyPropertyTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @Autowired NoteRepository noteRepository;
  @Autowired NoteReferenceService noteReferenceService;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;
  @Autowired RecallLogRepository recallLogRepository;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class WikiLinkProperty {
    Folder folder;
    Note source;
    NoteRealm result;
    Note relationshipNote;

    @BeforeEach
    void reify() throws UnexpectedNoAccessRightException {
      folder = makeMe.aFolder().notebookOwnedBy(currentUser.getUser()).name("F").please();
      source =
          makeMe
              .aNote("Src")
              .folder(folder)
              .content("---\nrelated: \"[[Other]]\"\nkind: example\n---\nbody")
              .please();
      makeMe.aNote("Other").folder(folder).please();
      noteReferenceService.refreshDerivedIndexesForNote(source);

      result = controller.reifyProperty(source, "related");
      relationshipNote = noteRepository.findById(result.getNote().getId()).orElseThrow();
    }

    @Test
    void createsTheRelationshipNoteInTheSourceNotebookAndFolder() {
      assertThat(relationshipNote.getNotebook().getId(), equalTo(source.getNotebook().getId()));
      assertThat(relationshipNote.getFolder().getId(), equalTo(folder.getId()));
    }

    @Test
    void titlesTheRelationshipNoteSourceRelationTarget() {
      assertThat(relationshipNote.getTitle(), equalTo("Src related Other"));
    }

    @Test
    void writesTheRelationshipFrontmatter() {
      assertThat(
          relationshipNote.getContent(),
          containsString(
              "type: Relationship\n"
                  + "relation: related\n"
                  + "source: \"[[Src]]\"\n"
                  + "target: \"[[Other]]\"\n"));
    }

    @Test
    void removesThePropertyFromTheSource() {
      assertThat(source.getContent(), not(containsString("related")));
    }
  }

  @Nested
  class TrackedProperty {
    Note source;
    User learnerB;
    MemoryTracker trackerA;
    MemoryTracker trackerB;
    MemoryTracker otherPropertyTracker;
    MemoryTracker listValueTrackerOfSameKey;
    MemoryTracker sourceNoteLevelTracker;
    float stabilityBefore;
    Timestamp nextRecallAtBefore;
    Note relationshipNote;

    @BeforeEach
    void reify() throws UnexpectedNoAccessRightException {
      source =
          makeMe
              .aNote("Src")
              .notebookOwnedBy(currentUser.getUser())
              .content("---\nrelated: \"[[Other]]\"\nkind: example\n---\nbody")
              .please();
      makeMe.aNote("Other").underSameNotebookAs(source).please();
      noteReferenceService.refreshDerivedIndexesForNote(source);
      learnerB = makeMe.aUser().please();
      trackerA =
          makeMe
              .aMemoryTrackerFor(source)
              .by(currentUser.getUser())
              .propertyKey("related")
              .recallCount(1)
              .difficulty(4.5f)
              .lastRecalledAt(Timestamp.valueOf("2026-09-01 10:00:00"))
              .please();
      trackerB = makeMe.aMemoryTrackerFor(source).by(learnerB).propertyKey("related").please();
      otherPropertyTracker =
          makeMe.aMemoryTrackerFor(source).by(currentUser.getUser()).propertyKey("kind").please();
      listValueTrackerOfSameKey =
          makeMe
              .aMemoryTrackerFor(source)
              .by(currentUser.getUser())
              .propertyKey("related")
              .propertyValue("[[Former]]")
              .please();
      sourceNoteLevelTracker = makeMe.aMemoryTrackerFor(source).by(currentUser.getUser()).please();
      stabilityBefore = trackerA.getStability();
      nextRecallAtBefore = trackerA.getNextRecallAt();

      NoteRealm result = controller.reifyProperty(source, "related");
      relationshipNote = noteRepository.findById(result.getNote().getId()).orElseThrow();
    }

    @Test
    void everyLearnersTrackerBecomesANoteLevelTrackerOfTheRelationshipNote() {
      assertThat(
          memoryTrackerRepository.findByNote_IdIn(List.of(relationshipNote.getId())),
          containsInAnyOrder(trackerA, trackerB));
      assertThat(trackerA.getPropertyKey(), equalTo(""));
      assertThat(trackerB.getPropertyKey(), equalTo(""));
    }

    @Test
    void keepsTheLearningStateAndRecallHistory() {
      makeMe.refresh(trackerA);

      assertThat(trackerA.getUser().getId(), equalTo(currentUser.getUser().getId()));
      assertThat(trackerA.getStability(), equalTo(stabilityBefore));
      assertThat(trackerA.getDifficulty(), equalTo(4.5f));
      assertThat(trackerA.getLastRecalledAt(), equalTo(Timestamp.valueOf("2026-09-01 10:00:00")));
      assertThat(trackerA.getNextRecallAt(), equalTo(nextRecallAtBefore));
      assertThat(
          recallLogRepository.findAllByMemoryTracker_IdOrderByRecordedAtDescIdDesc(
              trackerA.getId()),
          hasSize(1));
    }

    @Test
    void leavesTheSourcesOtherTrackersOnTheSource() {
      assertThat(
          memoryTrackerRepository.findByNote_IdIn(List.of(source.getId())),
          containsInAnyOrder(
              otherPropertyTracker, listValueTrackerOfSameKey, sourceNoteLevelTracker));
    }
  }

  @Test
  void writesTheRelationInKebabForm() throws UnexpectedNoAccessRightException {
    Note source =
        makeMe
            .aNote("Moon")
            .notebookOwnedBy(currentUser.getUser())
            .content("---\na part of: \"[[Earth]]\"\n---\n")
            .please();
    makeMe.aNote("Earth").underSameNotebookAs(source).please();
    noteReferenceService.refreshDerivedIndexesForNote(source);

    NoteRealm result = controller.reifyProperty(source, "a part of");

    assertThat(result.getNote().getTitle(), equalTo("Moon a part of Earth"));
    assertThat(
        noteRepository.findById(result.getNote().getId()).orElseThrow().getContent(),
        containsString("relation: a-part-of\n"));
  }

  @Nested
  class ValueThatCannotBeReified {
    Note source;
    String originalContent;
    long originalNoteCount;

    @BeforeEach
    void setup() {
      source =
          makeMe
              .aNote("Src")
              .notebookOwnedBy(currentUser.getUser())
              .content(
                  "---\n"
                      + "plain: training\n"
                      + "related:\n"
                      + "  - \"[[Other]]\"\n"
                      + "around: see [[Other]]\n"
                      + "nowhere: \"[[Nowhere]]\"\n"
                      + "---\nbody")
              .please();
      makeMe.aNote("Other").underSameNotebookAs(source).please();
      noteReferenceService.refreshDerivedIndexesForNote(source);
      originalContent = source.getContent();
      originalNoteCount = noteRepository.count();
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
          "missing | The note has no property missing.",
          "plain   | Only a property whose value is a link to a note can be reified.",
          "related | Only a property whose value is a link to a note can be reified.",
          "around  | Only a property whose value is a link to a note can be reified.",
          "nowhere | The link [[Nowhere]] does not name an existing note.",
        })
    void isRefusedWithTheReasonAndChangesNothing(String key, String message) {
      ResponseStatusException exception =
          assertThrows(ResponseStatusException.class, () -> controller.reifyProperty(source, key));

      assertThat(exception.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
      assertThat(exception.getReason(), equalTo(message));
      makeMe.refresh(source);
      assertThat(source.getContent(), equalTo(originalContent));
      assertThat(noteRepository.count(), equalTo(originalNoteCount));
    }
  }
}

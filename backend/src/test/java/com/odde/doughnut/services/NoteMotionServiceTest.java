package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.MovementNotPossibleException;
import com.odde.doughnut.testability.MakeMe;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NoteMotionServiceTest {
  @Autowired NoteMotionService noteMotionService;
  @Autowired NoteChildContainerFolderService noteChildContainerFolderService;
  @Autowired JdbcTemplate jdbcTemplate;

  @Autowired MakeMe makeMe;
  Note topNote;
  Note firstChild;
  Note secondChild;

  @BeforeEach
  void setup() {
    topNote = makeMe.aHeadNote("topNote").please();
    firstChild = makeMe.aNote("firstChild").under(topNote).please();
    secondChild = makeMe.aNote("secondChild").under(topNote).please();
  }

  void move(Note subject, Note relativeNote, boolean asFirstChildOfNote)
      throws CyclicLinkDetectedException, MovementNotPossibleException {
    noteMotionService.validate(subject, relativeNote, asFirstChildOfNote);
    noteMotionService.execute(subject, relativeNote, asFirstChildOfNote);
  }

  private void alignFoldersForTestSubtree(Note root) {
    alignFolderForTest(root);
    root.getAllDescendants().forEach(this::alignFolderForTest);
  }

  private void alignFolderForTest(Note note) {
    Note parent = note.getParent();
    if (parent == null) {
      note.setFolder(null);
    } else {
      note.setFolder(noteChildContainerFolderService.resolveForParent(parent));
    }
  }

  @Test
  void moveBehind() throws CyclicLinkDetectedException, MovementNotPossibleException {
    move(firstChild, secondChild, false);
    assertOrder(secondChild, firstChild);
  }

  @Test
  void moveBehindTop() {
    assertThrows(MovementNotPossibleException.class, () -> move(firstChild, topNote, false));
  }

  private void assertOrder(Note note1, Note note2) {
    Note parentNote = note1.getParent();
    makeMe.refresh(parentNote);
    assertThat(parentNote.getChildren(), containsInRelativeOrder(note1, note2));
  }

  @Test
  void moveSecondBehindFirstPreservesSlugWhenFolderUnchanged()
      throws CyclicLinkDetectedException, MovementNotPossibleException {
    topNote = makeMe.aHeadNote("top").please();
    Note section = makeMe.aNote("Section").under(topNote).please();
    firstChild = makeMe.aNote("firstChild").under(section).please();
    secondChild = makeMe.aNote("secondChild").under(section).please();
    makeMe.entityPersister.flush();
    alignFoldersForTestSubtree(section);
    makeMe.entityPersister.flush();
    makeMe.wikiSlugPathService.assignSlugForNewNote(firstChild);
    makeMe.wikiSlugPathService.assignSlugForNewNote(secondChild);
    makeMe.entityPersister.flush();
    makeMe.refresh(firstChild);
    makeMe.refresh(secondChild);
    assertThat(firstChild.getFolder().getId(), equalTo(secondChild.getFolder().getId()));

    String slugBefore = secondChild.getSlug();
    int folderIdBefore = secondChild.getFolder().getId();
    move(secondChild, firstChild, false);
    makeMe.refresh(secondChild);
    assertThat(secondChild.getFolder().getId(), equalTo(folderIdBefore));
    assertThat(secondChild.getSlug(), equalTo(slugBefore));
    assertOrder(firstChild, secondChild);
  }

  @Test
  void moveSecondToBeTheFirstSibling()
      throws CyclicLinkDetectedException, MovementNotPossibleException {
    move(secondChild, topNote, true);
    assertOrder(secondChild, firstChild);
  }

  @Test
  void moveUnder() throws CyclicLinkDetectedException, MovementNotPossibleException {
    move(firstChild, secondChild, true);
    assertThat(firstChild.getParent(), equalTo(secondChild));
    makeMe.refresh(firstChild);
    assertThat(firstChild.getFolder(), notNullValue());
    assertThat(firstChild.getFolder().getName(), equalTo(secondChild.getTitle()));
    assertThat(firstChild.getSlug(), startsWith(firstChild.getFolder().getSlug() + "/"));
  }

  @Test
  void moveIntoParentWhereBasenamesConflictGetsNextDisambiguatedSlug()
      throws CyclicLinkDetectedException, MovementNotPossibleException {
    Note head = makeMe.aHeadNote("top").please();
    Note section = makeMe.aNote("Section").under(head).please();
    makeMe.aNote("dup").under(section).please();
    Note second = makeMe.aNote("dup").under(section).please();
    Note otherHead = makeMe.aHeadNote("otherNb").please();
    Note mover = makeMe.aNote("dup").under(otherHead).please();
    makeMe.entityPersister.flush();
    alignFoldersForTestSubtree(head);
    alignFoldersForTestSubtree(otherHead);
    makeMe.entityPersister.flush();
    Stream.concat(Stream.of(head), head.getAllDescendants())
        .forEach(makeMe.wikiSlugPathService::assignSlugForNewNote);
    Stream.concat(Stream.of(otherHead), otherHead.getAllDescendants())
        .forEach(makeMe.wikiSlugPathService::assignSlugForNewNote);
    makeMe.entityPersister.flush();

    move(mover, second, false);

    makeMe.refresh(mover);
    assertThat(mover.getParent(), equalTo(section));
    assertThat(WikiSlugPathAssignment.basenameOf(mover.getSlug()), equalTo("dup-3"));
  }

  @Test
  void moveBothToTheEndInSequence()
      throws CyclicLinkDetectedException, MovementNotPossibleException {
    move(firstChild, secondChild, false);
    move(secondChild, firstChild, false);
    assertOrder(firstChild, secondChild);
  }

  @Test
  void moveToTopLevelClearsHeadFolderAndAlignsDescendantFolders() {
    User user = makeMe.aUser().please();
    topNote = makeMe.aHeadNote("topNote").creatorAndOwner(user).please();
    firstChild = makeMe.aNote("middle").under(topNote).please();
    Note grandChild = makeMe.aNote("leaf").under(firstChild).please();

    noteMotionService.moveToTopLevel(firstChild, user);

    makeMe.refresh(firstChild);
    makeMe.refresh(grandChild);
    assertThat(firstChild.getParent(), nullValue());
    assertThat(firstChild.getFolder(), nullValue());
    assertThat(firstChild.getSlug(), equalTo("middle"));
    assertThat(grandChild.getFolder(), notNullValue());
    assertThat(grandChild.getFolder().getName(), equalTo(firstChild.getTitle()));
    assertThat(grandChild.getSlug(), startsWith(grandChild.getFolder().getSlug() + "/"));
  }

  @Test
  void moveToTopLevel_slugsStayUniqueWithinNotebook() {
    User user = makeMe.aUser().please();
    topNote = makeMe.aHeadNote("topNote").creatorAndOwner(user).please();
    firstChild = makeMe.aNote("middle").under(topNote).please();
    Note grandChild = makeMe.aNote("leaf").under(firstChild).please();
    makeMe.entityPersister.flush();

    noteMotionService.moveToTopLevel(firstChild, user);
    makeMe.entityPersister.flush();
    makeMe.refresh(firstChild);
    makeMe.refresh(grandChild);

    Integer notebookId = firstChild.getNotebook().getId();
    Long total =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM note WHERE notebook_id = ? AND deleted_at IS NULL",
            Long.class,
            notebookId);
    Long distinctSlugs =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT slug) FROM note WHERE notebook_id = ? AND deleted_at IS NULL",
            Long.class,
            notebookId);
    assertThat(total, equalTo(distinctSlugs));
    assertThat(grandChild.getSlug(), startsWith(firstChild.getSlug() + "/"));
  }

  @Nested
  class WhenThereIsAThirdLevel {
    Note thirdLevel;
    Note forthLevel;

    @BeforeEach
    void setup() {
      thirdLevel = makeMe.aNote("thirdLevel").under(firstChild).please();
      forthLevel = makeMe.aNote("forthLevel").under(thirdLevel).please();
    }

    @Test
    void moveAfterNoteOfDifferentLevel()
        throws CyclicLinkDetectedException, MovementNotPossibleException {
      move(secondChild, thirdLevel, false);
      assertThat(secondChild.getParent(), equalTo(firstChild));
    }

    @Test
    void moveToOwnDescendentIsNotAllowed() {
      assertThrows(CyclicLinkDetectedException.class, () -> move(topNote, thirdLevel, false));
    }

    @Test
    void moveWithOwnChild() throws CyclicLinkDetectedException, MovementNotPossibleException {
      move(firstChild, secondChild, true);
      assertThat(firstChild.getAncestors(), contains(topNote, secondChild));
      assertThat(thirdLevel.getAncestors(), contains(topNote, secondChild, firstChild));
      assertThat(forthLevel.getAncestors(), contains(topNote, secondChild, firstChild, thirdLevel));
    }
  }

  @Nested
  class WhenThereIsAThirdChild {
    Note thirdChild;

    @BeforeEach
    void setup() {
      thirdChild = makeMe.aNote().under(topNote).please();
    }

    @Test
    void moveBetweenSecondAndThird()
        throws CyclicLinkDetectedException, MovementNotPossibleException {
      move(firstChild, secondChild, false);
      assertOrder(secondChild, firstChild);
      assertOrder(firstChild, thirdChild);
    }

    @Nested
    class WhenThereIsARelationshipNote {
      Note relationNote;

      @BeforeEach
      void setup() {
        relationNote = makeMe.aRelation().between(topNote, secondChild).please();
        makeMe.theNote(relationNote).after(firstChild).please();
        makeMe.refresh(topNote);
      }

      @Test
      void moveSecondToAfterFirstAndBeforeLinkNote()
          throws CyclicLinkDetectedException, MovementNotPossibleException {
        assertThat(relationNote.getSiblingOrder(), lessThan(secondChild.getSiblingOrder()));
        move(secondChild, firstChild, false);
        assertOrder(firstChild, secondChild);
        assertThat(relationNote.getSiblingOrder(), greaterThan(secondChild.getSiblingOrder()));
      }
    }
  }

  @Nested
  class WhenMovingBetweenNotebooks {
    Note otherNotebook;
    Note firstChild;
    Note secondChild;
    Note thirdLevel;

    @BeforeEach
    void setup() {
      topNote = makeMe.aHeadNote("topForNotebookMove").please();
      otherNotebook = makeMe.aHeadNote("otherNotebook").please();
      firstChild = makeMe.aNote("firstChild").under(topNote).please();
      secondChild = makeMe.aNote("secondChild").under(firstChild).please();
      thirdLevel = makeMe.aNote("thirdLevel").under(secondChild).please();
    }

    @Test
    void shouldUpdateNotebookForAllDescendants()
        throws CyclicLinkDetectedException, MovementNotPossibleException {
      String firstSlugBefore = firstChild.getSlug();
      move(firstChild, otherNotebook, true);

      makeMe.refresh(firstChild);
      makeMe.refresh(secondChild);
      makeMe.refresh(thirdLevel);

      assertThat(firstChild.getNotebook().getId(), equalTo(otherNotebook.getNotebook().getId()));
      assertThat(secondChild.getNotebook().getId(), equalTo(otherNotebook.getNotebook().getId()));
      assertThat(thirdLevel.getNotebook().getId(), equalTo(otherNotebook.getNotebook().getId()));

      assertThat(firstChild.getFolder(), notNullValue());
      assertThat(firstChild.getFolder().getName(), equalTo(otherNotebook.getTitle()));
      assertThat(
          firstChild.getFolder().getNotebook().getId(),
          equalTo(otherNotebook.getNotebook().getId()));
      assertThat(secondChild.getFolder(), notNullValue());
      assertThat(secondChild.getFolder().getName(), equalTo(firstChild.getTitle()));
      assertThat(thirdLevel.getFolder(), notNullValue());
      assertThat(thirdLevel.getFolder().getName(), equalTo(secondChild.getTitle()));

      assertThat(firstChild.getSlug(), not(equalTo(firstSlugBefore)));
      assertThat(firstChild.getSlug(), startsWith(firstChild.getFolder().getSlug() + "/"));
      assertThat(secondChild.getSlug(), startsWith(secondChild.getFolder().getSlug() + "/"));
      assertThat(thirdLevel.getSlug(), startsWith(thirdLevel.getFolder().getSlug() + "/"));
    }

    @Test
    void shouldUpdateNotebookForAllDescendantsIncludingRelationships()
        throws CyclicLinkDetectedException, MovementNotPossibleException {
      // Create a relationship note under secondChild
      Note targetNote = makeMe.aNote("targetNote").please();
      Note relationNote = makeMe.aRelation().between(secondChild, targetNote).please();

      move(secondChild, otherNotebook, true);

      makeMe.refresh(secondChild);
      makeMe.refresh(relationNote);

      assertThat(secondChild.getNotebook().getId(), equalTo(otherNotebook.getNotebook().getId()));
      assertThat(relationNote.getNotebook().getId(), equalTo(otherNotebook.getNotebook().getId()));

      assertThat(relationNote.getFolder(), notNullValue());
      assertThat(relationNote.getFolder().getName(), equalTo(secondChild.getTitle()));
    }
  }
}

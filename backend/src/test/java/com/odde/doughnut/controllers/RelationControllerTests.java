package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.RelationshipCreation;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.RelationshipNoteMarkdownFormatter;
import com.odde.doughnut.services.RelationshipNoteTitleFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindException;

class RelationControllerTests extends ControllerTestBase {
  @Autowired NoteRepository noteRepository;
  @Autowired RelationController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class MoveNoteToFolderTest {
    User anotherUser;
    Note ownNote;
    Folder targetFolder;

    @BeforeEach
    void setup() {
      anotherUser = makeMe.aUser().please();
      ownNote = makeMe.aNote("flower").creatorAndOwner(currentUser.getUser()).please();
      Note anchor = makeMe.aRootNote("nbroot").creatorAndOwner(currentUser.getUser()).please();
      targetFolder = makeMe.aFolder().notebook(anchor.getNotebook()).name("TargetF").please();
    }

    @Test
    void moveNoteToFolderSuccessfully() throws UnexpectedNoAccessRightException {
      Note mover =
          makeMe.aNote("mover").creatorAndOwner(currentUser.getUser()).under(ownNote).please();
      var result = controller.moveNoteToFolder(mover, targetFolder);
      assertThat(result, hasSize(1));
      mover = noteRepository.findById(mover.getId()).orElseThrow();
      assertThat(mover.getFolder().getId(), equalTo(targetFolder.getId()));
      assertThat(mover.getParent(), nullValue());
    }

    @Test
    void shouldNotAllowMoveOtherPeoplesNoteToFolder() {
      Note mover = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveNoteToFolder(mover, targetFolder));
    }

    @Test
    void shouldNotAllowMoveToUnauthorizedFolderNotebook() {
      Note mover = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      Note otherAnchor = makeMe.aRootNote("other").creatorAndOwner(anotherUser).please();
      Folder otherFolder =
          makeMe.aFolder().notebook(otherAnchor.getNotebook()).name("ForeignF").please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveNoteToFolder(mover, otherFolder));
    }
  }

  @Nested
  class CreateRelationshipTest {
    User anotherUser;
    Note note1;
    Note note2;
    RelationshipCreation relationshipCreation = new RelationshipCreation();

    @BeforeEach
    void setup() {
      anotherUser = makeMe.aUser().please();
      note1 = makeMe.aNote().creatorAndOwner(anotherUser).please();
      note2 = makeMe.aNote("flower").creatorAndOwner(currentUser.getUser()).please();
      relationshipCreation.relationType = RelationType.APPLICATION;
    }

    @Test
    void createdSuccessfully()
        throws CyclicLinkDetectedException, BindException, UnexpectedNoAccessRightException {
      Note note3 = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      long beforeThingCount = noteRepository.count();
      controller.addRelationshipFinalize(
          note3, note2, relationshipCreation, makeMe.successfulBindingResult());
      long afterThingCount = noteRepository.count();
      assertThat(afterThingCount, equalTo(beforeThingCount + 1));
    }

    @Test
    void relationshipNoteHasDerivedTitle()
        throws CyclicLinkDetectedException, BindException, UnexpectedNoAccessRightException {
      Note source = makeMe.aNote("Tool").creatorAndOwner(currentUser.getUser()).please();
      Note target = makeMe.aNote("Task").under(source).please();
      relationshipCreation.relationType = RelationType.APPLICATION;
      var result =
          controller.addRelationshipFinalize(
              source, target, relationshipCreation, makeMe.successfulBindingResult());
      assertThat(result, hasSize(2));
      String expected =
          RelationshipNoteTitleFormatter.format(
              source.getTitle(), RelationType.APPLICATION.label, target.getTitle());
      assertThat(result.getFirst().getNote().getTitle(), equalTo(expected));
      String expectedDetails =
          RelationshipNoteMarkdownFormatter.format(
              RelationType.APPLICATION, source.getTitle(), target.getTitle(), null);
      assertThat(result.getFirst().getNote().getDetails(), equalTo(expectedDetails));
    }
  }
}

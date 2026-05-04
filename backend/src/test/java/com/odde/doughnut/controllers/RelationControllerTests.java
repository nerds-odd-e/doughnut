package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.RelationshipCreation;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.entities.RelationshipNotePlacement;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.RelationshipNoteMarkdownFormatter;
import com.odde.doughnut.services.RelationshipNoteTitleFormatter;
import java.util.List;
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
          makeMe
              .aNote("mover")
              .creatorAndOwner(currentUser.getUser())
              .underSameNotebookAs(ownNote)
              .please();
      var result = controller.moveNoteToFolder(mover, targetFolder);
      assertThat(result, hasSize(1));
      mover = noteRepository.findById(mover.getId()).orElseThrow();
      assertThat(mover.getFolder().getId(), equalTo(targetFolder.getId()));
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

    @Test
    void moveNoteIntoFolder_collectsPeersInFolder() throws Throwable {
      User u = currentUser.getUser();
      Note anchor = makeMe.aRootNote("top").creatorAndOwner(u).please();
      Folder folder = makeMe.aFolder().notebook(anchor.getNotebook()).name("F").please();
      Note peerA = makeMe.aNote("A").creatorAndOwner(u).underSameNotebookAs(anchor).please();
      Note peerB = makeMe.aNote("B").creatorAndOwner(u).underSameNotebookAs(anchor).please();
      Note mover = makeMe.aNote("M").creatorAndOwner(u).underSameNotebookAs(anchor).please();
      makeMe.entityPersister.flush();
      controller.moveNoteToFolder(peerA, folder);
      controller.moveNoteToFolder(peerB, folder);
      makeMe.entityPersister.flush();

      controller.moveNoteToFolder(mover, folder);
      makeMe.refresh(mover);
      assertThat(mover.getFolder().getId(), equalTo(folder.getId()));
      List<Note> ordered = noteRepository.findNotesInFolderOrderByIdAsc(folder.getId());
      assertThat(
          ordered.stream().map(Note::getId).toList(),
          containsInAnyOrder(peerA.getId(), peerB.getId(), mover.getId()));
    }

    @Test
    void moveNoteToSameFolder_isIdempotent() throws Throwable {
      User u = currentUser.getUser();
      Note anchor = makeMe.aRootNote("top2").creatorAndOwner(u).please();
      Folder folder = makeMe.aFolder().notebook(anchor.getNotebook()).name("F2").please();
      Note peerA = makeMe.aNote("A2").creatorAndOwner(u).underSameNotebookAs(anchor).please();
      Note peerB = makeMe.aNote("B2").creatorAndOwner(u).underSameNotebookAs(anchor).please();
      Note mover = makeMe.aNote("M2").creatorAndOwner(u).underSameNotebookAs(anchor).please();
      makeMe.entityPersister.flush();
      controller.moveNoteToFolder(peerA, folder);
      controller.moveNoteToFolder(peerB, folder);
      controller.moveNoteToFolder(mover, folder);
      makeMe.entityPersister.flush();

      controller.moveNoteToFolder(mover, folder);
      List<Note> ordered = noteRepository.findNotesInFolderOrderByIdAsc(folder.getId());
      assertThat(
          ordered.stream().map(Note::getId).toList(),
          containsInAnyOrder(peerA.getId(), peerB.getId(), mover.getId()));
    }
  }

  @Nested
  class MoveNoteToNotebookRootTest {
    @Test
    void moveNoteToNotebookRoot_checksAccessOnNote() {
      Note foreign = makeMe.aNote().please();
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.moveNoteToNotebookRoot(foreign));
    }

    @Test
    void moveNoteToNotebookRoot_clearsFolder() throws UnexpectedNoAccessRightException {
      User u = currentUser.getUser();
      Note root = makeMe.aRootNote("top").creatorAndOwner(u).please();
      Folder folder = makeMe.aFolder().notebook(root.getNotebook()).name("F").please();
      Note mover = makeMe.aNote("M").creatorAndOwner(u).underSameNotebookAs(root).please();
      makeMe.entityPersister.flush();
      controller.moveNoteToFolder(mover, folder);
      makeMe.entityPersister.flush();

      controller.moveNoteToNotebookRoot(mover);
      makeMe.refresh(mover);
      assertThat(mover.getFolder(), nullValue());
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
      relationshipCreation.relationshipNotePlacement = null;
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
      Note target = makeMe.aNote("Task").underSameNotebookAs(source).please();
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

    @Test
    void relationshipNote_defaultPlacement_usesRelationsSubfolderUnderSourceFolder()
        throws CyclicLinkDetectedException, BindException, UnexpectedNoAccessRightException {
      User u = currentUser.getUser();
      Note anchor = makeMe.aRootNote("nb").creatorAndOwner(u).please();
      Folder parentFolder = makeMe.aFolder().notebook(anchor.getNotebook()).name("Work").please();
      Note source =
          makeMe
              .aNote("MySource")
              .creatorAndOwner(u)
              .underSameNotebookAs(anchor)
              .folder(parentFolder)
              .please();
      Note target = makeMe.aNote("Tgt").creatorAndOwner(u).underSameNotebookAs(anchor).please();
      makeMe.entityPersister.flush();
      var result =
          controller.addRelationshipFinalize(
              source, target, relationshipCreation, makeMe.successfulBindingResult());
      Note relation = noteRepository.findById(result.getFirst().getNote().getId()).orElseThrow();
      makeMe.refresh(relation);
      assertThat(relation.getFolder().getName(), equalTo("relations"));
      assertThat(relation.getFolder().getParentFolder().getId(), equalTo(parentFolder.getId()));
    }

    @Test
    void relationshipNote_sameLevelAsSource_usesSourceFolder()
        throws CyclicLinkDetectedException, BindException, UnexpectedNoAccessRightException {
      User u = currentUser.getUser();
      Note anchor = makeMe.aRootNote("nb2").creatorAndOwner(u).please();
      Folder parentFolder = makeMe.aFolder().notebook(anchor.getNotebook()).name("Scope").please();
      Note source =
          makeMe
              .aNote("Src")
              .creatorAndOwner(u)
              .underSameNotebookAs(anchor)
              .folder(parentFolder)
              .please();
      Note target = makeMe.aNote("T2").creatorAndOwner(u).underSameNotebookAs(anchor).please();
      relationshipCreation.relationshipNotePlacement =
          RelationshipNotePlacement.SAME_LEVEL_AS_SOURCE;
      makeMe.entityPersister.flush();
      var result =
          controller.addRelationshipFinalize(
              source, target, relationshipCreation, makeMe.successfulBindingResult());
      Note relation = noteRepository.findById(result.getFirst().getNote().getId()).orElseThrow();
      makeMe.refresh(relation);
      assertThat(relation.getFolder().getId(), equalTo(parentFolder.getId()));
    }

    @Test
    void relationshipNote_namedAfterSource_usesTitleSubfolder()
        throws CyclicLinkDetectedException, BindException, UnexpectedNoAccessRightException {
      User u = currentUser.getUser();
      Note anchor = makeMe.aRootNote("nb3").creatorAndOwner(u).please();
      Folder parentFolder = makeMe.aFolder().notebook(anchor.getNotebook()).name("Area").please();
      Note source =
          makeMe
              .aNote("Alpha")
              .creatorAndOwner(u)
              .underSameNotebookAs(anchor)
              .folder(parentFolder)
              .please();
      Note target = makeMe.aNote("T3").creatorAndOwner(u).underSameNotebookAs(anchor).please();
      relationshipCreation.relationshipNotePlacement =
          RelationshipNotePlacement.NAMED_AFTER_SOURCE_NOTE;
      makeMe.entityPersister.flush();
      var result =
          controller.addRelationshipFinalize(
              source, target, relationshipCreation, makeMe.successfulBindingResult());
      Note relation = noteRepository.findById(result.getFirst().getNote().getId()).orElseThrow();
      makeMe.refresh(relation);
      assertThat(relation.getFolder().getName(), equalTo("Alpha"));
      assertThat(relation.getFolder().getParentFolder().getId(), equalTo(parentFolder.getId()));
    }
  }
}

package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.NoteReorderDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.NoteMotionService;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerMotionTests extends ControllerTestBase {

  @MockitoBean HttpClientAdapter httpClientAdapter;
  @Autowired NoteController controller;
  @Autowired NoteMotionService noteMotionService;
  @Autowired NoteRepository noteRepository;

  private static NoteReorderDTO reorderDto(Integer folderId) {
    NoteReorderDTO dto = new NoteReorderDTO();
    dto.folderId = folderId;
    return dto;
  }

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void reorder_checksAccessOnNote() {
    Note foreign = makeMe.aNote().please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.reorder(foreign, reorderDto(null), makeMe.successfulBindingResult()));
  }

  @Test
  void reorder_movesNoteIntoFolderAtEnd() throws Throwable {
    User u = currentUser.getUser();
    Note root = makeMe.aRootNote("top").creatorAndOwner(u).please();
    Folder folder = makeMe.aFolder().notebook(root.getNotebook()).name("F").please();
    Note peerA = makeMe.aNote("A").creatorAndOwner(u).please();
    Note peerB = makeMe.aNote("B").creatorAndOwner(u).please();
    Note mover = makeMe.aNote("M").creatorAndOwner(u).please();
    makeMe.entityPersister.flush();
    noteMotionService.executeMoveIntoFolder(peerA, folder);
    noteMotionService.executeMoveIntoFolder(peerB, folder);
    makeMe.entityPersister.flush();

    controller.reorder(mover, reorderDto(folder.getId()), makeMe.successfulBindingResult());
    makeMe.refresh(mover);
    assertThat(mover.getFolder().getId(), equalTo(folder.getId()));
    List<Note> ordered = noteRepository.findNotesInFolderOrderBySiblingOrder(folder.getId());
    assertThat(
        ordered.stream().map(Note::getId).toList(),
        contains(peerA.getId(), peerB.getId(), mover.getId()));
  }

  @Test
  void reorder_sameFolder_keepsNoteAtEndAmongPeers() throws Throwable {
    User u = currentUser.getUser();
    Note root = makeMe.aRootNote("top").creatorAndOwner(u).please();
    Folder folder = makeMe.aFolder().notebook(root.getNotebook()).name("F").please();
    Note peerA = makeMe.aNote("A").creatorAndOwner(u).please();
    Note peerB = makeMe.aNote("B").creatorAndOwner(u).please();
    Note mover = makeMe.aNote("M").creatorAndOwner(u).please();
    makeMe.entityPersister.flush();
    noteMotionService.executeMoveIntoFolder(peerA, folder);
    noteMotionService.executeMoveIntoFolder(peerB, folder);
    noteMotionService.executeMoveIntoFolder(mover, folder);
    makeMe.entityPersister.flush();

    controller.reorder(mover, reorderDto(folder.getId()), makeMe.successfulBindingResult());
    List<Note> ordered = noteRepository.findNotesInFolderOrderBySiblingOrder(folder.getId());
    assertThat(
        ordered.stream().map(Note::getId).toList(),
        contains(peerA.getId(), peerB.getId(), mover.getId()));
  }
}

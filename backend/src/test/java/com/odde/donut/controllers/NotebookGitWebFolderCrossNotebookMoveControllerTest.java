package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitWebFolderCrossNotebookMoveControllerTest
    extends NotebookGitWebContentControllerTestBase {

  @Autowired FolderRepository folderRepository;

  @Test
  void folderContainingAFileCannotMoveToAnotherNotebook() throws Exception {
    Notebook source = createGitBackedNotebook("Source");
    Folder refs = makeMe.aFolder().notebook(source).name("refs").please();
    Folder papers = makeMe.aFolder().parentFolder(refs).name("papers").please();
    NotebookAttachment paper =
        storeFolderAttachmentAndSnapshot(source, papers, "paper.pdf", new byte[] {1, 2, 3});
    ObjectId sourceHead = ObjectId.fromString(binding(source).getAcceptedGitObjectId());

    Notebook destination = createGitBackedNotebook("Destination");
    Folder library = makeMe.aFolder().notebook(destination).name("library").please();
    NotebookAttachment existing =
        storeFolderAttachmentAndSnapshot(
            destination, library, "existing.pdf", new byte[] {4, 5, 6});
    ObjectId destinationHead = ObjectId.fromString(binding(destination).getAcceptedGitObjectId());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> folderController.moveFolder(source, refs, folderMoveTo(destination, false)));

    assertThat(
        exception.getReason(),
        equalTo("Folders containing files cannot be moved to another notebook yet."));
    assertThat(
        folderRepository.findById(refs.getId()).orElseThrow().getNotebook().getId(),
        equalTo(source.getId()));
    assertThat(
        folderRepository.findById(refs.getId()).orElseThrow().getParentFolder(), nullValue());
    assertThat(
        folderRepository.findById(papers.getId()).orElseThrow().getParentFolder().getId(),
        equalTo(refs.getId()));
    assertThat(
        notebookAttachmentRepository.findById(paper.getId()).orElseThrow().getFolder().getId(),
        equalTo(papers.getId()));
    assertThat(
        folderRepository.findById(library.getId()).orElseThrow().getNotebook().getId(),
        equalTo(destination.getId()));
    assertThat(
        notebookAttachmentRepository.findById(existing.getId()).orElseThrow().getFolder().getId(),
        equalTo(library.getId()));
    assertThat(ObjectId.fromString(binding(source).getAcceptedGitObjectId()), equalTo(sourceHead));
    assertThat(
        ObjectId.fromString(binding(destination).getAcceptedGitObjectId()),
        equalTo(destinationHead));
  }

  @Test
  void folderContainingAFileCannotMergeIntoAnotherNotebook() throws Exception {
    Notebook source = createGitBackedNotebook("Source");
    Folder refs = makeMe.aFolder().notebook(source).name("refs").please();
    NotebookAttachment paper =
        storeFolderAttachmentAndSnapshot(source, refs, "paper.pdf", new byte[] {1, 2, 3});
    ObjectId sourceHead = ObjectId.fromString(binding(source).getAcceptedGitObjectId());

    Notebook destination = createGitBackedNotebook("Destination");
    Folder destinationRefs = makeMe.aFolder().notebook(destination).name("refs").please();
    storeFolderAttachmentAndSnapshot(
        destination, destinationRefs, "existing.pdf", new byte[] {4, 5, 6});
    ObjectId destinationHead = ObjectId.fromString(binding(destination).getAcceptedGitObjectId());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> folderController.moveFolder(source, refs, folderMoveTo(destination, true)));

    assertThat(
        exception.getReason(),
        equalTo("Folders containing files cannot be moved to another notebook yet."));
    assertThat(folderRepository.findById(refs.getId()).isPresent(), equalTo(true));
    assertThat(
        notebookAttachmentRepository.findById(paper.getId()).orElseThrow().getFolder().getId(),
        equalTo(refs.getId()));
    assertThat(ObjectId.fromString(binding(source).getAcceptedGitObjectId()), equalTo(sourceHead));
    assertThat(
        ObjectId.fromString(binding(destination).getAcceptedGitObjectId()),
        equalTo(destinationHead));
  }

  @Test
  void movingAFolderToAnotherNotebookCommitsOnceInEach() throws Exception {
    Notebook science = createGitBackedNotebook("Science");
    Folder physics = seedPhysicsWithSoundAndSnapshot(science);
    Notebook engineering = createGitBackedNotebook("Engineering");
    snapshotCurrentPortableTree(engineering);
    AcceptedHistory scienceBefore = acceptedHistory(science);
    AcceptedHistory engineeringBefore = acceptedHistory(engineering);

    folderController.moveFolder(science, physics, folderMoveTo(engineering, false));

    AcceptedHistory scienceAfter = acceptedHistory(science);
    AcceptedHistory engineeringAfter = acceptedHistory(engineering);
    assertThat(scienceAfter.parents(), equalTo(scienceBefore.commits()));
    assertThat(engineeringAfter.parents(), equalTo(engineeringBefore.commits()));
    assertThat(scienceAfter.tipPaths(), containsInAnyOrder("Energy.md"));
    assertThat(engineeringAfter.tipPaths(), containsInAnyOrder("physics/waves/Sound.md"));
    assertAcceptedTreeMatchesTheFullAssembly(science);
    assertAcceptedTreeMatchesTheFullAssembly(engineering);
  }

  @Test
  void mergingAFolderIntoAnotherNotebookCommitsOnceInEach() throws Exception {
    Notebook science = createGitBackedNotebook("Science");
    Folder physics = seedPhysicsWithSoundAndSnapshot(science);
    Notebook engineering = createGitBackedNotebook("Engineering");
    Folder engineeringPhysics = makeMe.aFolder().notebook(engineering).name("physics").please();
    makeMe.aNote("Force").folder(engineeringPhysics).please();
    snapshotCurrentPortableTree(engineering);

    folderController.moveFolder(science, physics, folderMoveTo(engineering, true));

    assertThat(acceptedHistory(science).tipPaths(), containsInAnyOrder("Energy.md"));
    assertThat(
        acceptedHistory(engineering).tipPaths(),
        containsInAnyOrder("physics/Force.md", "physics/waves/Sound.md"));
    assertAcceptedTreeMatchesTheFullAssembly(science);
    assertAcceptedTreeMatchesTheFullAssembly(engineering);
  }

  @Test
  void movingAFolderBackAsUndoRestoresBothNotebooks() throws Exception {
    Notebook science = createGitBackedNotebook("Science");
    Folder physics = seedPhysicsWithSoundAndSnapshot(science);
    Notebook engineering = createGitBackedNotebook("Engineering");
    snapshotCurrentPortableTree(engineering);
    AcceptedHistory engineeringBefore = acceptedHistory(engineering);
    folderController.moveFolder(science, physics, folderMoveTo(engineering, false));

    folderController.moveFolder(
        engineering,
        folderRepository.findById(physics.getId()).orElseThrow(),
        folderMoveTo(science, false));

    assertThat(
        acceptedHistory(engineering).commits().size(),
        equalTo(engineeringBefore.commits().size() + 2));
    assertThat(
        acceptedHistory(science).tipPaths(),
        containsInAnyOrder("Energy.md", "physics/waves/Sound.md"));
    assertThat(acceptedHistory(engineering).tipPaths(), containsInAnyOrder());
    assertAcceptedTreeMatchesTheFullAssembly(science);
    assertAcceptedTreeMatchesTheFullAssembly(engineering);
  }

  private Folder seedPhysicsWithSoundAndSnapshot(Notebook science) throws Exception {
    Folder physics = makeMe.aFolder().notebook(science).name("physics").please();
    Folder waves = makeMe.aFolder().parentFolder(physics).name("waves").please();
    makeMe.aNote("Sound").folder(waves).please();
    makeMe.aNote("Energy").notebook(science).please();
    snapshotCurrentPortableTree(science);
    return physics;
  }
}

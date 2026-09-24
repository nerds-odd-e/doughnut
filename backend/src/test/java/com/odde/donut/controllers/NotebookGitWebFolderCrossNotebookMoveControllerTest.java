package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.FolderMoveRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.FolderRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitWebFolderCrossNotebookMoveControllerTest
    extends NotebookGitWebContentControllerTestBase {

  @Autowired FolderRepository folderRepository;

  @Test
  void folderContainingAFileCannotMoveToAnotherNotebook() throws Exception {
    Notebook source = createProductLfsNotebook("Source");
    Folder refs = makeMe.aFolder().notebook(source).name("refs").please();
    Folder papers = makeMe.aFolder().parentFolder(refs).name("papers").please();
    NotebookAttachment paper =
        storeFolderAttachmentAndSnapshot(source, papers, "paper.pdf", new byte[] {1, 2, 3});
    ObjectId sourceHead = ObjectId.fromString(binding(source).getAcceptedGitObjectId());

    Notebook destination = createProductLfsNotebook("Destination");
    Folder library = makeMe.aFolder().notebook(destination).name("library").please();
    NotebookAttachment existing =
        storeFolderAttachmentAndSnapshot(
            destination, library, "existing.pdf", new byte[] {4, 5, 6});
    ObjectId destinationHead = ObjectId.fromString(binding(destination).getAcceptedGitObjectId());
    FolderMoveRequest moveToDestinationRoot = new FolderMoveRequest();
    moveToDestinationRoot.setDestinationNotebookId(destination.getId());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> folderController.moveFolder(source, refs, moveToDestinationRoot));

    assertThat(
        exception.getReason(),
        equalTo(
            "Folders containing files cannot be dissolved, merged, or moved to another notebook"
                + " yet."));
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
}

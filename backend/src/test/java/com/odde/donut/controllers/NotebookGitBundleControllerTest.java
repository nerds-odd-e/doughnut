package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NotebookCreationRequest;
import com.odde.donut.controllers.dto.NotebookRealm;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.GitBundleTestReader;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class NotebookGitBundleControllerTest extends NotebookControllerTestBase {

  private Notebook createGitBackedNotebook() throws UnexpectedNoAccessRightException {
    NotebookCreationRequest request = new NotebookCreationRequest();
    request.setNewTitle("Git Backed Notebook For Bundle");
    NotebookRealm response = controller.createNotebook(request);
    return notebookRepository.findById(response.notebook().getId()).orElseThrow();
  }

  @Test
  void ownerDownloadsAcceptedBundleWithoutMutatingIt() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding before =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();

    ResponseEntity<byte[]> response = controller.downloadNotebookGitBundle(notebook);

    assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(
        response.getHeaders().getContentType(),
        equalTo(MediaType.valueOf("application/x-git-bundle")));
    assertThat(
        response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION),
        containsString("attachment;"));
    assertThat(response.getBody(), equalTo(before.getBundleBytes()));

    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId headObjectId = GitBundleTestReader.fetchHead(readBack, response.getBody());
      assertThat(headObjectId.getName(), equalTo(before.getAcceptedGitObjectId()));
    }

    NotebookGitBinding after =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    assertThat(after.getId(), equalTo(before.getId()));
    assertThat(after.getAcceptedGitObjectId(), equalTo(before.getAcceptedGitObjectId()));
    assertThat(after.getUpdatedAt(), equalTo(before.getUpdatedAt()));
  }

  @Test
  void deniesDownloadForNotebookOwnedByAnotherUser() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    currentUser.setUser(makeMe.aUser().please());

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.downloadNotebookGitBundle(notebook));
  }

  @Test
  void deniesDownloadForReadOnlySubscriber() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    User subscriber = makeMe.aUser().please();
    makeMe.aSubscription().forNotebook(notebook).forUser(subscriber).please();
    currentUser.setUser(subscriber);

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.downloadNotebookGitBundle(notebook));
  }
}

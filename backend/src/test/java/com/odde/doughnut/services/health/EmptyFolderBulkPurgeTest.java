package com.odde.doughnut.services.health;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.NotebookHealthFixRequest;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.services.NotebookHealthService;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmptyFolderBulkPurgeTest {
  @Autowired NotebookHealthService notebookHealthService;
  @Autowired FolderRepository folderRepository;
  @Autowired MakeMe makeMe;

  private User owner;
  private Notebook notebook;

  @BeforeEach
  void setup() {
    owner = makeMe.aUser().please();
    notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
  }

  @Test
  void nestedFullyEmptyTreePurgedDeepestFirst() {
    Folder parent = makeMe.aFolder().notebook(notebook).name("Parent").please();
    Folder child = makeMe.aFolder().parentFolder(parent).name("Child").please();

    notebookHealthService.fix(notebook, optInRequest());

    Set<Integer> remainingIds = folderIdsInNotebook();
    assertThat(remainingIds, not(hasItem(parent.getId())));
    assertThat(remainingIds, not(hasItem(child.getId())));
    assertThat(
        folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .map(Folder::getName)
            .toList(),
        not(hasItem("Parent")));
    assertThat(
        folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .map(Folder::getName)
            .toList(),
        not(hasItem("Child")));
  }

  @Test
  void readmeOnlyFolderNeverPurged() {
    Folder readmeOnly =
        makeMe.aFolder().notebook(notebook).name("Readme Only").readmeContent("keep me").please();
    Folder fullyEmpty = makeMe.aFolder().notebook(notebook).name("Fully Empty").please();

    notebookHealthService.fix(notebook, optInRequest());

    Set<Integer> remainingIds = folderIdsInNotebook();
    assertThat(remainingIds, hasItem(readmeOnly.getId()));
    assertThat(remainingIds, not(hasItem(fullyEmpty.getId())));
  }

  @Test
  void blankParentOverReadmeOnlyChildNotCascaded() {
    Folder blankParent = makeMe.aFolder().notebook(notebook).name("Blank Parent").please();
    Folder readmeChild =
        makeMe
            .aFolder()
            .parentFolder(blankParent)
            .name("Readme Child")
            .readmeContent("protected")
            .please();

    notebookHealthService.fix(notebook, optInRequest());

    Set<Integer> remainingIds = folderIdsInNotebook();
    assertThat(remainingIds, hasItem(blankParent.getId()));
    assertThat(remainingIds, hasItem(readmeChild.getId()));
    Folder survivingChild = folderRepository.findById(readmeChild.getId()).orElseThrow();
    assertThat(survivingChild.getParentFolder().getId(), equalTo(blankParent.getId()));
  }

  @Test
  void softDeletedOnlyEmptyFolderPurged() {
    Folder folder = makeMe.aFolder().notebook(notebook).name("OnlyDeleted").please();
    makeMe.aNote("gone").folder(folder).softDeleted().please();

    notebookHealthService.fix(notebook, optInRequest());

    assertThat(folderIdsInNotebook(), not(hasItem(folder.getId())));
  }

  @Test
  void fixRejectsWithoutOptIn() {
    makeMe.aFolder().notebook(notebook).name("Empty Shell").please();

    ResponseStatusException nullFlag =
        assertThrows(
            ResponseStatusException.class,
            () -> notebookHealthService.fix(notebook, requestWith(null)));
    assertThat(nullFlag.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));

    ResponseStatusException falseFlag =
        assertThrows(
            ResponseStatusException.class,
            () -> notebookHealthService.fix(notebook, requestWith(false)));
    assertThat(falseFlag.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));

    assertThat(folderIdsInNotebook(), hasSize(1));
  }

  private static NotebookHealthFixRequest optInRequest() {
    return requestWith(true);
  }

  private static NotebookHealthFixRequest requestWith(Boolean removeEmptyFolders) {
    NotebookHealthFixRequest request = new NotebookHealthFixRequest();
    request.setRemoveEmptyFolders(removeEmptyFolders);
    return request;
  }

  private Set<Integer> folderIdsInNotebook() {
    List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
    return folders.stream().map(Folder::getId).collect(Collectors.toSet());
  }
}

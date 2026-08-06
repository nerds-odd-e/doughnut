package com.odde.doughnut.services.health;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import com.odde.doughnut.controllers.dto.NotebookHealthFixRequest;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.services.NotebookHealthService;
import com.odde.doughnut.testability.MakeMe;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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

  private static NotebookHealthFixRequest optInRequest() {
    NotebookHealthFixRequest request = new NotebookHealthFixRequest();
    request.setRemoveEmptyFolders(true);
    return request;
  }

  private Set<Integer> folderIdsInNotebook() {
    return folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
        .map(Folder::getId)
        .collect(Collectors.toSet());
  }
}

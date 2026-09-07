package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.PlatformTransactionManager;

/** Folder id → parent id map for asserting hierarchy after a refused folder relocation. */
final class NotebookGitProposalFolderRelocationParentMap {

  private NotebookGitProposalFolderRelocationParentMap() {}

  static void assertUnchanged(
      PlatformTransactionManager transactionManager,
      FolderRepository folderRepository,
      Notebook notebook,
      Folder... folders) {
    Map<Integer, Integer> expectedParents = byId(List.of(folders));
    inCommittedTransaction(
        transactionManager,
        () ->
            assertThat(
                byId(folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId())),
                equalTo(expectedParents)));
  }

  static Map<Integer, Integer> byId(List<Folder> folders) {
    Map<Integer, Integer> parents = new HashMap<>();
    for (Folder folder : folders) {
      parents.put(folder.getId(), folder.getParentFolderId());
    }
    return parents;
  }
}

package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies an exact folder relocation is refused when the destination parent is missing or
 * unrepresented in accepted Portable content, without creating that parent. Represented parents are
 * accepted in {@link NotebookGitProposalFolderRelocationControllerTest}.
 */
class NotebookGitProposalFolderRelocationDestinationControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String README = "readme";
  private static final String NOTE = "note";

  @Autowired FolderRepository folderRepository;

  @Test
  void rejectsAnExactFolderRelocationIntoAMissingParentWithoutCreatingIt() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder topics =
        makeMe.aFolder().notebook(notebook).name("Topics").readmeContent(README).please();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, topicsAtRoot());

    ResponseStatusException exception =
        publishRejected(
            notebook,
            binding,
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("Missing/Topics/README.md", README),
                new PortableTreeEntry("Missing/Topics/A.md", NOTE)));

    assertThat(
        exception.getReason(),
        equalTo(
            "Parent folder for path \"Missing/Topics/README.md\" is not represented in accepted"
                + " Portable content; add this note at the notebook root or inside an existing"
                + " represented folder."));
    inCommittedTransaction(
        transactionManager,
        () ->
            assertThat(
                folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                    .map(Folder::getId)
                    .toList(),
                contains(topics.getId())));
  }

  @Test
  void rejectsAnExactFolderRelocationIntoAnExistingUnrepresentedParent() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder topics =
        makeMe.aFolder().notebook(notebook).name("Topics").readmeContent(README).please();
    Folder empty = makeMe.aFolder().notebook(notebook).name("Empty").please();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, topicsAtRoot());

    ResponseStatusException exception =
        publishRejected(
            notebook,
            binding,
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("Empty/Topics/README.md", README),
                new PortableTreeEntry("Empty/Topics/A.md", NOTE)));

    assertThat(exception.getReason(), containsString("Empty/Topics/README.md"));
    inCommittedTransaction(
        transactionManager,
        () ->
            assertThat(
                folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                    .map(Folder::getId)
                    .toList(),
                containsInAnyOrder(topics.getId(), empty.getId())));
  }

  private ResponseStatusException publishRejected(
      Notebook notebook, NotebookGitBinding binding, List<PortableTreeEntry> proposed)
      throws Exception {
    return assertProposalRejectedWithoutMutatingBinding(
        notebook,
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(binding, asProposal(proposed)),
        HttpStatus.BAD_REQUEST);
  }

  private static List<PortableTreeEntry> topicsAtRoot() {
    return List.of(
        new PortableTreeEntry("README.md", README),
        new PortableTreeEntry("Topics/README.md", README),
        new PortableTreeEntry("Topics/A.md", NOTE));
  }

  private static List<NotebookGitProposalFile> asProposal(List<PortableTreeEntry> entries) {
    return entries.stream()
        .map(entry -> new NotebookGitProposalFile(entry.path(), entry.content()))
        .toList();
  }
}

package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
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
 * Verifies an exact folder relocation is refused when the source has an active descendant folder
 * with no tracked accepted content. A descendant represented only through deeper content is
 * accepted in {@link NotebookGitProposalFolderRelocationControllerTest}.
 */
class NotebookGitProposalFolderRelocationEmptyDescendantControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String README = "readme";
  private static final String NOTE = "note";

  @Autowired FolderRepository folderRepository;

  @Test
  void rejectsAnExactFolderRelocationWhenASourceDescendantHasNoTrackedContent() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder archive =
        makeMe.aFolder().notebook(notebook).name("Archive").readmeContent(README).please();
    Folder topics =
        makeMe.aFolder().notebook(notebook).name("Topics").readmeContent(README).please();
    Folder empty = makeMe.aFolder().parentFolder(topics).name("Empty").please();
    NotebookGitBinding binding =
        seedAcceptedBinding(
            notebook,
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("Topics/README.md", README),
                new PortableTreeEntry("Topics/A.md", NOTE),
                new PortableTreeEntry("Archive/README.md", README)));

    ResponseStatusException exception =
        publishRejected(notebook, binding, moveTopicsUnderArchive());

    assertThat(
        exception.getReason(),
        equalTo(
            "Descendant folder \"Topics/Empty/\" is not represented in accepted Portable content;"
                + " every active descendant must have tracked content before the folder can be"
                + " moved."));
    NotebookGitProposalFolderRelocationParentMap.assertUnchanged(
        transactionManager, folderRepository, notebook, archive, topics, empty);
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

  private static List<PortableTreeEntry> moveTopicsUnderArchive() {
    return List.of(
        new PortableTreeEntry("README.md", README),
        new PortableTreeEntry("Archive/README.md", README),
        new PortableTreeEntry("Archive/Topics/README.md", README),
        new PortableTreeEntry("Archive/Topics/A.md", NOTE));
  }

  private static List<NotebookGitProposalFile> asProposal(List<PortableTreeEntry> entries) {
    return entries.stream()
        .map(entry -> new NotebookGitProposalFile(entry.path(), entry.content()))
        .toList();
  }
}

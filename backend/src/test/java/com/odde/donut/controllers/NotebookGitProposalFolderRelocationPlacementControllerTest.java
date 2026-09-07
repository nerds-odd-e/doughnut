package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.services.FolderSiblingNameValidation;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies an exact folder relocation is refused when the destination collides or would create a
 * cycle, without merging, overwriting, or reparenting.
 */
class NotebookGitProposalFolderRelocationPlacementControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String README = "readme";
  private static final String NOTE = "note";
  private static final String EXISTING = "existing";

  @Autowired FolderRepository folderRepository;

  @Test
  void rejectsAnExactFolderRelocationOntoAnExistingSameNameDestination() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder archive =
        makeMe.aFolder().notebook(notebook).name("Archive").readmeContent(README).please();
    Folder existingDest = makeMe.aFolder().parentFolder(archive).name("Topics").please();
    makeMe.aNote().folder(existingDest).title("Existing").content(EXISTING).please();
    Folder topics =
        makeMe.aFolder().notebook(notebook).name("Topics").readmeContent(README).please();
    NotebookGitBinding binding =
        seedAcceptedBinding(
            notebook,
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("Topics/README.md", README),
                new PortableTreeEntry("Topics/A.md", NOTE),
                new PortableTreeEntry("Archive/README.md", README),
                new PortableTreeEntry("Archive/Topics/Existing.md", EXISTING)));

    ApiException exception =
        publishRejectedAs(notebook, binding, moveTopicsUnderArchive(), ApiException.class);

    assertThat(
        exception.getErrorBody().getMessage(),
        equalTo(
            "Cannot move folder to path \"Archive/Topics/README.md\": "
                + FolderSiblingNameValidation.DUPLICATE_SIBLING_NAME_HERE));
    assertThat(
        exception.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
    NotebookGitProposalFolderRelocationParentMap.assertUnchanged(
        transactionManager, folderRepository, notebook, archive, existingDest, topics);
  }

  @Test
  void rejectsAnExactFolderRelocationOntoAnInvisibleEmptySameNameContainer() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder archive =
        makeMe.aFolder().notebook(notebook).name("Archive").readmeContent(README).please();
    Folder emptyDest = makeMe.aFolder().parentFolder(archive).name("Topics").please();
    Folder topics =
        makeMe.aFolder().notebook(notebook).name("Topics").readmeContent(README).please();
    NotebookGitBinding binding =
        seedAcceptedBinding(
            notebook,
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("Topics/README.md", README),
                new PortableTreeEntry("Topics/A.md", NOTE),
                new PortableTreeEntry("Archive/README.md", README)));

    ApiException exception =
        publishRejectedAs(notebook, binding, moveTopicsUnderArchive(), ApiException.class);

    assertThat(
        exception.getErrorBody().getMessage(),
        containsString(FolderSiblingNameValidation.DUPLICATE_SIBLING_NAME_HERE));
    NotebookGitProposalFolderRelocationParentMap.assertUnchanged(
        transactionManager, folderRepository, notebook, archive, emptyDest, topics);
  }

  @Test
  void rejectsAnExactFolderRelocationIntoTheSourceFolder() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder topics =
        makeMe.aFolder().notebook(notebook).name("Topics").readmeContent(README).please();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, topicsAtRoot());

    ResponseStatusException exception =
        publishRejectedAs(
            notebook,
            binding,
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("Topics/Topics/README.md", README),
                new PortableTreeEntry("Topics/Topics/A.md", NOTE)),
            ResponseStatusException.class);

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    assertThat(exception.getReason(), containsString("Topics/Topics/README.md"));
    assertThat(exception.getReason(), containsString("Cannot move folder into itself."));
    NotebookGitProposalFolderRelocationParentMap.assertUnchanged(
        transactionManager, folderRepository, notebook, topics);
  }

  @Test
  void rejectsAnExactFolderRelocationIntoADescendantFolder() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder topics =
        makeMe.aFolder().notebook(notebook).name("Topics").readmeContent(README).please();
    Folder sub = makeMe.aFolder().parentFolder(topics).name("Sub").readmeContent(README).please();
    NotebookGitBinding binding =
        seedAcceptedBinding(
            notebook,
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("Topics/README.md", README),
                new PortableTreeEntry("Topics/A.md", NOTE),
                new PortableTreeEntry("Topics/Sub/README.md", README)));

    ResponseStatusException exception =
        publishRejectedAs(
            notebook,
            binding,
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("Topics/Sub/Topics/README.md", README),
                new PortableTreeEntry("Topics/Sub/Topics/A.md", NOTE),
                new PortableTreeEntry("Topics/Sub/Topics/Sub/README.md", README)),
            ResponseStatusException.class);

    assertThat(exception.getReason(), containsString("Cannot move folder into its descendant."));
    NotebookGitProposalFolderRelocationParentMap.assertUnchanged(
        transactionManager, folderRepository, notebook, topics, sub);
  }

  private <T extends RuntimeException> T publishRejectedAs(
      Notebook notebook,
      NotebookGitBinding binding,
      List<PortableTreeEntry> proposed,
      Class<T> exceptionType)
      throws Exception {
    return assertProposalRejectedWithoutMutatingBinding(
        notebook,
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(binding, asProposal(proposed)),
        exceptionType);
  }

  private static List<PortableTreeEntry> moveTopicsUnderArchive() {
    return List.of(
        new PortableTreeEntry("README.md", README),
        new PortableTreeEntry("Archive/README.md", README),
        new PortableTreeEntry("Archive/Topics/README.md", README),
        new PortableTreeEntry("Archive/Topics/A.md", NOTE));
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

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
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies an exact folder relocation is refused when the destination parent is missing or
 * unrepresented in accepted Portable content, without creating that parent. Represented parents
 * still reach the interim reserved-README refusal.
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

  @ParameterizedTest(name = "{0}")
  @MethodSource("representedDestinationParents")
  void stillRefusesAnExactFolderRelocationWhenTheDestinationParentIsRepresented(
      String scenario, List<PortableTreeEntry> accepted, List<PortableTreeEntry> proposed)
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    seedRepresentedFolders(notebook, scenario);
    NotebookGitBinding binding = seedAcceptedBinding(notebook, accepted);

    ResponseStatusException exception = publishRejected(notebook, binding, proposed);

    assertThat(exception.getReason(), containsString("folder README, which is reserved"));
  }

  static Stream<Arguments> representedDestinationParents() {
    return Stream.of(
        Arguments.of(
            "root",
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("Archive/README.md", README),
                new PortableTreeEntry("Archive/Topics/README.md", README),
                new PortableTreeEntry("Archive/Topics/A.md", NOTE)),
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("Archive/README.md", README),
                new PortableTreeEntry("Topics/README.md", README),
                new PortableTreeEntry("Topics/A.md", NOTE))),
        Arguments.of(
            "readme-only",
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("Topics/README.md", README),
                new PortableTreeEntry("Topics/A.md", NOTE),
                new PortableTreeEntry("Archive/README.md", README)),
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("Archive/README.md", README),
                new PortableTreeEntry("Archive/Topics/README.md", README),
                new PortableTreeEntry("Archive/Topics/A.md", NOTE))),
        Arguments.of(
            "descendant-only",
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("Topics/README.md", README),
                new PortableTreeEntry("Topics/A.md", NOTE),
                new PortableTreeEntry("Courses/Physics/Motion.md", NOTE)),
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("Courses/Physics/Motion.md", NOTE),
                new PortableTreeEntry("Courses/Topics/README.md", README),
                new PortableTreeEntry("Courses/Topics/A.md", NOTE))));
  }

  private void seedRepresentedFolders(Notebook notebook, String scenario) {
    if ("root".equals(scenario)) {
      Folder archive =
          makeMe.aFolder().notebook(notebook).name("Archive").readmeContent(README).please();
      makeMe.aFolder().parentFolder(archive).name("Topics").readmeContent(README).please();
      return;
    }
    makeMe.aFolder().notebook(notebook).name("Topics").readmeContent(README).please();
    if ("readme-only".equals(scenario)) {
      makeMe.aFolder().notebook(notebook).name("Archive").readmeContent(README).please();
      return;
    }
    makeMe.aFolder().notebook(notebook).name("Courses").please();
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

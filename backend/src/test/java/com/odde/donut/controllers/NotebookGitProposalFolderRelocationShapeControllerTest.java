package com.odde.donut.controllers;

import static com.odde.donut.services.notebookTree.PortableTreeEntry.ofText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies {@code publishNotebookGitProposal} explains why a README-shaped directory proposal is
 * not one exact same-name subtree relocation. Exact candidates are accepted in {@link
 * NotebookGitProposalFolderRelocationControllerTest}.
 */
class NotebookGitProposalFolderRelocationShapeControllerTest extends NotebookGitControllerTestBase {

  private static final String OTHER = "---\ntype: Note\n---\nother";

  @Test
  void explainsAPartialFolderMoveIsNotAnExactSubtreeRelocation() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder topics = aFolderWithReadme(notebook, "Topics");
    makeMe.aNote().folder(topics).title("A").content(NOTE).please();
    aFolderWithReadme(notebook, "Archive");

    ResponseStatusException exception =
        publishRejected(
            notebook,
            List.of(
                ofText("Topics/A.md", NOTE),
                ofText("Archive/README.md", README),
                ofText("Archive/Topics/README.md", README)));

    assertThat(exception.getReason(), containsString("Unsupported tree shape"));
    assertThat(exception.getReason(), containsString("path \"Topics/A.md\""));
    assertThat(exception.getReason(), containsString("is not an exact folder relocation"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("inexactFolderProposals")
  void namesThePathThatMakesAFolderProposalInexact(
      String scenario, boolean withOtherFolder, List<PortableTreeEntry> proposed, String path)
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    topicsAndArchive(notebook);
    if (withOtherFolder) {
      Folder other = aFolderWithReadme(notebook, "Other");
      makeMe.aNote().folder(other).title("C").content(OTHER).please();
    }

    ResponseStatusException exception = publishRejected(notebook, proposed);

    assertThat(exception.getReason(), containsString("path \"" + path + "\""));
    assertThat(exception.getReason(), containsString("is not an exact folder relocation"));
  }

  static Stream<Arguments> inexactFolderProposals() {
    return Stream.of(
        Arguments.of(
            "multiple",
            true,
            List.of(
                ofText("note.md", NOTE),
                ofText("Copy.md", NOTE),
                ofText("Archive/README.md", README),
                ofText("Archive/Topics/README.md", README),
                ofText("Archive/Topics/A.md", NOTE),
                ofText("Archive/Topics/Sub/README.md", README),
                ofText("Archive/Topics/Sub/B.md", NOTE),
                ofText("Archive/Other/README.md", README),
                ofText("Archive/Other/C.md", OTHER)),
            "Topics/README.md"),
        Arguments.of(
            "renamed",
            false,
            List.of(
                ofText("note.md", NOTE),
                ofText("Copy.md", NOTE),
                ofText("Archive/README.md", README),
                ofText("Archive/Renamed/README.md", README),
                ofText("Archive/Renamed/A.md", NOTE),
                ofText("Archive/Renamed/Sub/README.md", README),
                ofText("Archive/Renamed/Sub/B.md", NOTE)),
            "Archive/Renamed/README.md"),
        Arguments.of(
            "edited",
            false,
            List.of(
                ofText("note.md", NOTE),
                ofText("Copy.md", NOTE),
                ofText("Archive/README.md", README),
                ofText("Archive/Topics/README.md", README),
                ofText("Archive/Topics/A.md", "---\ntype: Note\n---\nedited"),
                ofText("Archive/Topics/Sub/README.md", README),
                ofText("Archive/Topics/Sub/B.md", NOTE)),
            "Archive/Topics/A.md"));
  }

  private ResponseStatusException publishRejected(
      Notebook notebook, List<PortableTreeEntry> proposed) throws Exception {
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    return assertProposalRejectedWithoutMutatingBinding(
        notebook,
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(binding, NotebookGitProposalFile.asProposal(proposed)),
        HttpStatus.BAD_REQUEST);
  }

  private void topicsAndArchive(Notebook notebook) {
    makeMe.aNote().notebook(notebook).title("note").content(NOTE).please();
    makeMe.aNote().notebook(notebook).title("Copy").content(NOTE).please();
    Folder topics = aFolderWithReadme(notebook, "Topics");
    makeMe.aNote().folder(topics).title("A").content(NOTE).please();
    Folder sub =
        makeMe.aFolder().parentFolder(topics).name("Sub").readmeContent(README_BODY).please();
    makeMe.aNote().folder(sub).title("B").content(NOTE).please();
    aFolderWithReadme(notebook, "Archive");
  }

  private Folder aFolderWithReadme(Notebook notebook, String name) {
    return makeMe.aFolder().notebook(notebook).name(name).readmeContent(README_BODY).please();
  }
}

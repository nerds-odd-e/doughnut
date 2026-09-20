package com.odde.donut.controllers;

import static com.odde.donut.services.notebookExport.PortableTreeEntry.ofText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
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
class NotebookGitProposalFolderRelocationShapeControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String README = "readme";
  private static final String NOTE = "note";
  private static final String OTHER = "other";

  @Test
  void explainsAPartialFolderMoveIsNotAnExactSubtreeRelocation() throws Exception {
    ResponseStatusException exception =
        publishRejected(
            List.of(
                ofText("README.md", README),
                ofText("Topics/README.md", README),
                ofText("Topics/A.md", NOTE),
                ofText("Archive/README.md", README)),
            List.of(
                ofText("README.md", README),
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
      String scenario,
      List<PortableTreeEntry> accepted,
      List<PortableTreeEntry> proposed,
      String path)
      throws Exception {
    ResponseStatusException exception = publishRejected(accepted, proposed);

    assertThat(exception.getReason(), containsString("path \"" + path + "\""));
    assertThat(exception.getReason(), containsString("is not an exact folder relocation"));
  }

  static Stream<Arguments> inexactFolderProposals() {
    List<PortableTreeEntry> topicsAndArchive = topicsAndArchive();
    return Stream.of(
        Arguments.of(
            "multiple",
            withOtherFolder(topicsAndArchive),
            List.of(
                ofText("README.md", README),
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
            topicsAndArchive,
            List.of(
                ofText("README.md", README),
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
            topicsAndArchive,
            List.of(
                ofText("README.md", README),
                ofText("note.md", NOTE),
                ofText("Copy.md", NOTE),
                ofText("Archive/README.md", README),
                ofText("Archive/Topics/README.md", README),
                ofText("Archive/Topics/A.md", "edited"),
                ofText("Archive/Topics/Sub/README.md", README),
                ofText("Archive/Topics/Sub/B.md", NOTE)),
            "Archive/Topics/A.md"));
  }

  private ResponseStatusException publishRejected(
      List<PortableTreeEntry> accepted, List<PortableTreeEntry> proposed) throws Exception {
    return publishRejected(createGitBackedNotebook(), accepted, proposed);
  }

  private ResponseStatusException publishRejected(
      Notebook notebook, List<PortableTreeEntry> accepted, List<PortableTreeEntry> proposed)
      throws Exception {
    NotebookGitBinding binding = seedAcceptedBinding(notebook, accepted);
    return assertProposalRejectedWithoutMutatingBinding(
        notebook,
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(binding, NotebookGitProposalFile.asProposal(proposed)),
        HttpStatus.BAD_REQUEST);
  }

  private static List<PortableTreeEntry> topicsAndArchive() {
    return List.of(
        ofText("README.md", README),
        ofText("note.md", NOTE),
        ofText("Copy.md", NOTE),
        ofText("Topics/README.md", README),
        ofText("Topics/A.md", NOTE),
        ofText("Topics/Sub/README.md", README),
        ofText("Topics/Sub/B.md", NOTE),
        ofText("Archive/README.md", README));
  }

  private static List<PortableTreeEntry> withOtherFolder(List<PortableTreeEntry> accepted) {
    return Stream.concat(
            accepted.stream(),
            Stream.of(ofText("Other/README.md", README), ofText("Other/C.md", OTHER)))
        .toList();
  }
}

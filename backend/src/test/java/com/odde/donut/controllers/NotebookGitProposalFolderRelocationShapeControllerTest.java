package com.odde.donut.controllers;

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
 * not one exact same-name subtree relocation. An exact candidate still reaches the existing
 * reserved-README refusal; this class does not accept folder moves.
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
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("Topics/README.md", README),
                new PortableTreeEntry("Topics/A.md", NOTE),
                new PortableTreeEntry("Archive/README.md", README)),
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("Topics/A.md", NOTE),
                new PortableTreeEntry("Archive/README.md", README),
                new PortableTreeEntry("Archive/Topics/README.md", README)));

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
            "mixed",
            topicsAndArchive,
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("note.md", "edited"),
                new PortableTreeEntry("Copy.md", NOTE),
                new PortableTreeEntry("Archive/README.md", README),
                new PortableTreeEntry("Archive/Topics/README.md", README),
                new PortableTreeEntry("Archive/Topics/A.md", NOTE),
                new PortableTreeEntry("Archive/Topics/Sub/README.md", README),
                new PortableTreeEntry("Archive/Topics/Sub/B.md", NOTE)),
            "note.md"),
        Arguments.of(
            "multiple",
            withOtherFolder(topicsAndArchive),
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("note.md", NOTE),
                new PortableTreeEntry("Copy.md", NOTE),
                new PortableTreeEntry("Archive/README.md", README),
                new PortableTreeEntry("Archive/Topics/README.md", README),
                new PortableTreeEntry("Archive/Topics/A.md", NOTE),
                new PortableTreeEntry("Archive/Topics/Sub/README.md", README),
                new PortableTreeEntry("Archive/Topics/Sub/B.md", NOTE),
                new PortableTreeEntry("Archive/Other/README.md", README),
                new PortableTreeEntry("Archive/Other/C.md", OTHER)),
            "Topics/README.md"),
        Arguments.of(
            "renamed",
            topicsAndArchive,
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("note.md", NOTE),
                new PortableTreeEntry("Copy.md", NOTE),
                new PortableTreeEntry("Archive/README.md", README),
                new PortableTreeEntry("Archive/Renamed/README.md", README),
                new PortableTreeEntry("Archive/Renamed/A.md", NOTE),
                new PortableTreeEntry("Archive/Renamed/Sub/README.md", README),
                new PortableTreeEntry("Archive/Renamed/Sub/B.md", NOTE)),
            "Archive/Renamed/README.md"),
        Arguments.of(
            "edited",
            topicsAndArchive,
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("note.md", NOTE),
                new PortableTreeEntry("Copy.md", NOTE),
                new PortableTreeEntry("Archive/README.md", README),
                new PortableTreeEntry("Archive/Topics/README.md", README),
                new PortableTreeEntry("Archive/Topics/A.md", "edited"),
                new PortableTreeEntry("Archive/Topics/Sub/README.md", README),
                new PortableTreeEntry("Archive/Topics/Sub/B.md", NOTE)),
            "Archive/Topics/A.md"));
  }

  @Test
  void stillRefusesAnExactFolderRelocationAsAReservedReadme() throws Exception {
    ResponseStatusException exception =
        publishRejected(
            topicsAndArchive(),
            List.of(
                new PortableTreeEntry("README.md", README),
                new PortableTreeEntry("note.md", NOTE),
                new PortableTreeEntry("Copy.md", NOTE),
                new PortableTreeEntry("Archive/README.md", README),
                new PortableTreeEntry("Archive/Topics/README.md", README),
                new PortableTreeEntry("Archive/Topics/A.md", NOTE),
                new PortableTreeEntry("Archive/Topics/Sub/README.md", README),
                new PortableTreeEntry("Archive/Topics/Sub/B.md", NOTE)));

    assertThat(exception.getReason(), containsString("folder README, which is reserved"));
  }

  private ResponseStatusException publishRejected(
      List<PortableTreeEntry> accepted, List<PortableTreeEntry> proposed) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, accepted);
    return assertProposalRejectedWithoutMutatingBinding(
        notebook,
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(binding, asProposal(proposed)),
        HttpStatus.BAD_REQUEST);
  }

  private static List<PortableTreeEntry> topicsAndArchive() {
    return List.of(
        new PortableTreeEntry("README.md", README),
        new PortableTreeEntry("note.md", NOTE),
        new PortableTreeEntry("Copy.md", NOTE),
        new PortableTreeEntry("Topics/README.md", README),
        new PortableTreeEntry("Topics/A.md", NOTE),
        new PortableTreeEntry("Topics/Sub/README.md", README),
        new PortableTreeEntry("Topics/Sub/B.md", NOTE),
        new PortableTreeEntry("Archive/README.md", README));
  }

  private static List<PortableTreeEntry> withOtherFolder(List<PortableTreeEntry> accepted) {
    return Stream.concat(
            accepted.stream(),
            Stream.of(
                new PortableTreeEntry("Other/README.md", README),
                new PortableTreeEntry("Other/C.md", OTHER)))
        .toList();
  }

  private static List<NotebookGitProposalFile> asProposal(List<PortableTreeEntry> entries) {
    return entries.stream()
        .map(entry -> new NotebookGitProposalFile(entry.path(), entry.content()))
        .toList();
  }
}

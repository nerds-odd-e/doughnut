package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** A `.keep` that no longer marks an empty folder is refused by name. */
class NotebookGitProposalLeftoverFolderMarkerControllerTest extends NotebookGitControllerTestBase {

  private static final NotebookGitProposalFile INTRO =
      new NotebookGitProposalFile("Intro.md", "---\ntype: Note\n---\nIntro body.\n");

  @Test
  void refusesAMarkerLeftBesideANewNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding initial = snapshotCurrentPortableTree(notebook);
    NotebookGitProposalFile marker = new NotebookGitProposalFile("Chemistry/.keep", "");
    controller.publishNotebookGitProposal(
        notebook.getId(),
        initial.getAcceptedGitObjectId(),
        proposalBundleBytes(initial, List.of(INTRO, marker)));
    NotebookGitBinding accepted = reloadCommittedBinding(notebook.getId());

    assertRefusedNaming(
        notebook,
        accepted,
        List.of(
            INTRO,
            marker,
            new NotebookGitProposalFile("Chemistry/Atoms.md", "---\ntype: Note\n---\nAtoms.\n")),
        "Chemistry/.keep");
  }

  @Test
  void refusesAMarkerWithContent() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding initial = snapshotCurrentPortableTree(notebook);

    assertRefusedNaming(
        notebook,
        initial,
        List.of(INTRO, new NotebookGitProposalFile("Empty/.keep", "not empty")),
        "Empty/.keep");
  }

  private void assertRefusedNaming(
      Notebook notebook,
      NotebookGitBinding binding,
      List<NotebookGitProposalFile> files,
      String markerPath)
      throws Exception {
    ResponseStatusException refusal =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            binding.getAcceptedGitObjectId(),
            proposalBundleBytes(binding, files),
            HttpStatus.BAD_REQUEST);
    assertThat(refusal.getReason(), allOf(containsString(markerPath), containsString("delete")));
  }
}

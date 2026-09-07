package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies {@code publishNotebookGitProposal} relocates and renames an unchanged note using only
 * the final folder and title. Filename-preserving relocation is covered in {@link
 * NotebookGitProposalRelocationControllerTest}; same-parent filename changes in {@link
 * NotebookGitProposalRenameControllerTest}.
 */
class NotebookGitProposalRelocateAndRenameControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String TYPED_NOTE_CONTENT = "---\ntype: Note\n---\noriginal content";
  private static final String RESERVED_DELETED_CONTENT =
      "---\ntype: Note\n---\nreserved deleted content";

  @Autowired NoteController noteController;

  @Test
  void relocatesAndRenamesANoteDespiteADeletedTitleAtTheSourceAndNewFilename() throws Exception {
    RelocateAndRenameNotebook setup = notebookWithDeletedIntermediateTitle("Source", "renamed");

    controller.publishNotebookGitProposal(
        setup.notebook().getId(),
        setup.binding().getAcceptedGitObjectId(),
        relocateAndRenameProposal(setup.binding()));

    Note relocated = noteRepository.findById(setup.note().getId()).orElseThrow();
    assertThat(relocated.getId(), equalTo(setup.note().getId()));
    NoteRealm shown = noteController.showNote(relocated);
    assertThat(
        shown.getAncestorFolders().stream().map(Folder::getId).toList(),
        contains(setup.destination().getId()));
    assertThat(shown.getNote().getTitle(), equalTo("renamed"));
    assertThat(shown.getNote().getContent(), equalTo(TYPED_NOTE_CONTENT));
  }

  @Test
  void relocatesAndRenamesANoteDespiteADeletedTitleAtTheDestinationAndOldFilename()
      throws Exception {
    RelocateAndRenameNotebook setup = notebookWithDeletedIntermediateTitle("Dest", "note");

    controller.publishNotebookGitProposal(
        setup.notebook().getId(),
        setup.binding().getAcceptedGitObjectId(),
        relocateAndRenameProposal(setup.binding()));

    NoteRealm shown =
        noteController.showNote(noteRepository.findById(setup.note().getId()).orElseThrow());
    assertThat(
        shown.getAncestorFolders().stream().map(Folder::getId).toList(),
        contains(setup.destination().getId()));
    assertThat(shown.getNote().getTitle(), equalTo("renamed"));
  }

  /**
   * Live {@code Source/note.md} relocating to {@code Dest/renamed.md}, with siblings so both
   * folders stay represented. A deleted note occupies {@code reservedFolderName}/{@code
   * reservedTitle}.md, a hypothetical intermediate of rename-then-move or move-then-rename.
   */
  private RelocateAndRenameNotebook notebookWithDeletedIntermediateTitle(
      String reservedFolderName, String reservedTitle) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder source = makeMe.aFolder().notebook(notebook).name("Source").please();
    Folder destination = makeMe.aFolder().notebook(notebook).name("Dest").please();
    makeMe.aNote().folder(source).title("keep").content(TYPED_NOTE_CONTENT).please();
    makeMe.aNote().folder(destination).title("other").content(TYPED_NOTE_CONTENT).please();
    Note note = makeMe.aNote().folder(source).title("note").content(TYPED_NOTE_CONTENT).please();
    Folder reservedFolder = "Source".equals(reservedFolderName) ? source : destination;
    makeMe
        .aNote()
        .folder(reservedFolder)
        .title(reservedTitle)
        .content(RESERVED_DELETED_CONTENT)
        .please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Source/keep.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("Source/note.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("Dest/other.md", TYPED_NOTE_CONTENT))));
    return new RelocateAndRenameNotebook(
        notebook,
        destination,
        note,
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());
  }

  private byte[] relocateAndRenameProposal(NotebookGitBinding binding) throws Exception {
    return proposalBundleBytes(
        binding,
        List.of(
            new NotebookGitProposalFile("Source/keep.md", TYPED_NOTE_CONTENT),
            new NotebookGitProposalFile("Dest/other.md", TYPED_NOTE_CONTENT),
            new NotebookGitProposalFile("Dest/renamed.md", TYPED_NOTE_CONTENT)));
  }

  private record RelocateAndRenameNotebook(
      Notebook notebook, Folder destination, Note note, NotebookGitBinding binding) {}
}

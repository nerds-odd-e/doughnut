package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.MemoryTrackerType;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

abstract class NotebookGitProposalFolderControllerTestBase
    extends NotebookGitBundleControllerTestBase {

  static final String FOLDER_README =
      "---\ntype: Readme\nsource: local\n---\nPrecisely preserved folder readme.\n";
  static final String EXISTING_CONTENT = "---\ntype: Note\n---\nExisting learned content.\n";
  static final String NOTE_A = "---\ntype: Note\nauthor: local\n---\nPrecisely preserved A.\n";
  static final String NOTE_B = "---\ntype: Note\nauthor: local\n---\nPrecisely preserved B.\n";
  static final String FOLDER_README_PATH = "例文/README.md";
  static final String NOTE_A_PATH = "例文/A.md";
  static final String NOTE_B_PATH = "例文/B.md";

  @Autowired FolderRepository folderRepository;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;
  @Autowired TextContentController textContentController;
  @Autowired NoteController noteController;

  LearnedNotebook boundNotebookWithLearnedNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note existing =
        makeMe.aNote().notebook(notebook).title("Existing").content(EXISTING_CONTENT).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(existing.getId()).orElseThrow())
                    .spelling()
                    .please());
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    return new LearnedNotebook(notebook, existing, tracker, binding);
  }

  List<NotebookGitProposalFile> withExisting(List<NotebookGitProposalFile> additions) {
    List<NotebookGitProposalFile> files = new ArrayList<>();
    files.add(new NotebookGitProposalFile("Existing.md", EXISTING_CONTENT));
    files.addAll(additions);
    return files;
  }

  static Note noteByTitle(List<Note> notes, String title) {
    return notes.stream().filter(note -> title.equals(note.getTitle())).findFirst().orElseThrow();
  }

  PublicationFootprint committedFootprint(Notebook notebook) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          List<Note> notes = noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId());
          List<Integer> noteIds = notes.stream().map(Note::getId).toList();
          MemoryTracker tracker =
              memoryTrackerRepository.findByNote_IdIn(noteIds).stream().findFirst().orElse(null);
          return new PublicationFootprint(
              notebookGitBindingRepository
                  .findByNotebook_Id(notebook.getId())
                  .orElseThrow()
                  .getAcceptedGitObjectId(),
              folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                  .map(Folder::getId)
                  .toList(),
              noteIds,
              notes.stream().map(Note::getContent).toList(),
              tracker == null ? null : tracker.getId(),
              tracker == null ? null : tracker.getType(),
              tracker == null || tracker.isActive());
        });
  }

  record LearnedNotebook(
      Notebook notebook, Note existing, MemoryTracker tracker, NotebookGitBinding binding) {}

  record PublicationFootprint(
      String acceptedHead,
      List<Integer> folderIds,
      List<Integer> noteIds,
      List<String> noteContents,
      Integer trackerId,
      MemoryTrackerType trackerType,
      boolean trackerActive) {}
}

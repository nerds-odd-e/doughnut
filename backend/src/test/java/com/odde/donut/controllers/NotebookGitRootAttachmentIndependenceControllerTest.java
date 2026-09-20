package com.odde.donut.controllers;

import static com.odde.donut.services.notebookExport.PortableTreeEntry.ofText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.controllers.dto.FolderMoveRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Root files belong to the notebook, not to any note or folder. A local note relocation, a web
 * removal of the very note that refers to a file, and the Markdown-only folder placement, dissolve
 * and permanent-deletion operations each leave the same root files, byte for byte, at the notebook
 * root: none of them collects a file into a folder, and none takes a file away. Where the note
 * survives the operation, its identity and learning data survive with it.
 */
class NotebookGitRootAttachmentIndependenceControllerTest
    extends NotebookGitWebContentControllerTestBase {

  private static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";

  /** This note's own text refers to a root file, so its removal is the sharpest case. */
  private static final String READING_BODY =
      "---\ntype: Note\n---\nSee reference.json for the schema.";

  private static final String REFERENCE_JSON = "{\"schema\": \"donut\"}\n";
  // Deliberately not valid UTF-8, so nothing may decode these bytes on the way in or out.
  private static final byte[] DIAGRAM_BYTES = {(byte) 0x89, (byte) 0xFF, (byte) 0xFE, 0x00};

  private static final PortableTreeEntry DIAGRAM =
      new PortableTreeEntry("Diagram.png", DIAGRAM_BYTES);
  private static final PortableTreeEntry REFERENCE = ofText("reference.json", REFERENCE_JSON);
  private static final List<PortableTreeEntry> ROOT_FILES = List.of(DIAGRAM, REFERENCE);

  @Autowired NotebookAttachmentRepository notebookAttachmentRepository;
  @Autowired FolderRepository folderRepository;

  @Test
  void aLocalNoteRelocationLeavesTheRootFilesUntouched() throws Exception {
    Fixture fixture = publishRootFilesOverNotesAndFolders();
    NotebookGitBinding accepted = reloadCommittedBinding(fixture.notebook().getId());
    // Read the current accepted tree through the live download endpoint, not the JPA
    // bundle_bytes column directly: once a binding's saves move onto native object storage,
    // that column is no longer kept in sync with the accepted head.
    List<PortableTreeEntry> relocated =
        movePath(
            GitBundleTestReader.fetchTipTreeEntries(downloadedBundleBytes(fixture.notebook())),
            "Biology/Cells.md",
            "Study/Cells.md");

    controller.publishNotebookGitProposal(
        fixture.notebook().getId(),
        accepted.getAcceptedGitObjectId(),
        proposalBundleBytes(accepted, NotebookGitProposalFile.asProposal(relocated)));

    assertThat(reloadNote(fixture.cells()).getFolder().getId(), equalTo(fixture.study().getId()));
    assertShownContentAndRetainedLearning(fixture.cells(), fixture.tracker(), CELLS_BODY);
    assertRootFilesSurvived(
        fixture.notebook(), ObjectId.fromString(accepted.getAcceptedGitObjectId()));
  }

  @Test
  void removingTheReferringNoteOnTheWebLeavesItsRootFileIntact() throws Exception {
    Fixture fixture = publishRootFilesOverNotesAndFolders();
    noteController.trashNote(reloadNote(fixture.reading()), leaveDeadLinks());
    ObjectId afterTrash = acceptedHead(fixture.notebook());

    noteController.permanentlyDeleteNote(reloadNote(fixture.reading()));

    assertThat(committedNoteTitles(fixture.notebook()), not(hasItem("Reading")));
    assertRootFilesSurvived(fixture.notebook(), afterTrash);
  }

  @Test
  void webFolderPlacementCannotCollectTheRootFiles() throws Exception {
    Fixture fixture = publishRootFilesOverNotesAndFolders();
    ObjectId beforePlacement = acceptedHead(fixture.notebook());
    FolderMoveRequest request = new FolderMoveRequest();
    request.setNewParentFolderId(fixture.study().getId());

    folderController.moveFolder(fixture.notebook(), fixture.biology(), request);

    assertThat(
        reloadFolder(fixture.biology()).getParentFolderId(), equalTo(fixture.study().getId()));
    assertShownContentAndRetainedLearning(fixture.cells(), fixture.tracker(), CELLS_BODY);
    assertRootFilesSurvived(fixture.notebook(), beforePlacement);
  }

  @Test
  void webFolderDissolveCannotCollectTheRootFiles() throws Exception {
    Fixture fixture = publishRootFilesOverNotesAndFolders();
    ObjectId beforeDissolve = acceptedHead(fixture.notebook());

    folderController.dissolveFolder(fixture.notebook(), fixture.biology(), false);

    assertThat(reloadNote(fixture.cells()).getFolder(), nullValue());
    assertShownContentAndRetainedLearning(fixture.cells(), fixture.tracker(), CELLS_BODY);
    assertRootFilesSurvived(fixture.notebook(), beforeDissolve);
  }

  @Test
  void webFolderDeletionTakesOnlyItsOwnSubtree() throws Exception {
    Fixture fixture = publishRootFilesOverNotesAndFolders();
    folderController.trashFolder(fixture.notebook(), fixture.biology());
    ObjectId afterTrash = acceptedHead(fixture.notebook());

    folderController.permanentlyDeleteFolder(fixture.notebook(), reloadFolder(fixture.biology()));

    assertThat(committedNoteTitles(fixture.notebook()), not(hasItem("Cells")));
    assertRootFilesSurvived(fixture.notebook(), afterTrash);
  }

  /**
   * The accepted starting point: a learned note inside a folder, an empty destination folder, a
   * root note referring to a root file, and two accepted root files published through the ordinary
   * publication boundary.
   */
  private Fixture publishRootFilesOverNotesAndFolders() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder biology =
        makeMe.aFolder().notebook(notebook).name("Biology").readmeContent("readme").please();
    Folder study =
        makeMe.aFolder().notebook(notebook).name("Study").readmeContent("readme").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    Note reading = makeMe.aNote("Reading").notebook(notebook).content(READING_BODY).please();
    MemoryTracker tracker = learnedTracker(cells, 0.5f, 1);
    NotebookGitBinding markdownOnly = snapshotCurrentPortableTree(notebook);

    List<PortableTreeEntry> withRootFiles =
        new ArrayList<>(GitBundleTestReader.fetchTipTreeEntries(markdownOnly.getBundleBytes()));
    withRootFiles.addAll(ROOT_FILES);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        markdownOnly.getAcceptedGitObjectId(),
        proposalBundleBytes(markdownOnly, NotebookGitProposalFile.asProposal(withRootFiles)));
    assertThat(committedRootAttachments(notebook), equalTo(ROOT_FILES));

    return new Fixture(notebook, biology, study, cells, reading, tracker);
  }

  /**
   * The operation appended exactly one accepted commit, and every non-Markdown file the notebook
   * holds - anywhere in the accepted tree and in the live projection - is still the same root file
   * with the same bytes.
   */
  private void assertRootFilesSurvived(Notebook notebook, ObjectId parentHead) throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      RevCommit head =
          revWalk.parseCommit(
              GitBundleTestReader.fetchHead(repository, downloadedBundleBytes(notebook)));
      assertThat(head.getParent(0).getId(), equalTo(parentHead));
      assertThat(
          filesAmong(GitBundleTestReader.readTreeEntries(repository, head)), equalTo(ROOT_FILES));
    }
    assertThat(committedRootAttachments(notebook), equalTo(ROOT_FILES));
  }

  /** Every entry that is not Markdown and not an empty-folder marker, at whatever depth. */
  private static List<PortableTreeEntry> filesAmong(List<PortableTreeEntry> entries) {
    return entries.stream()
        .filter(entry -> !entry.path().endsWith(".md") && !entry.path().endsWith("/.keep"))
        .toList();
  }

  private static List<PortableTreeEntry> movePath(
      List<PortableTreeEntry> entries, String from, String to) {
    return entries.stream()
        .map(
            entry -> entry.path().equals(from) ? new PortableTreeEntry(to, entry.content()) : entry)
        .toList();
  }

  private ObjectId acceptedHead(Notebook notebook) {
    return ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());
  }

  private byte[] downloadedBundleBytes(Notebook notebook) throws UnexpectedNoAccessRightException {
    return controller
        .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
        .getBody();
  }

  private List<PortableTreeEntry> committedRootAttachments(Notebook notebook) {
    return NotebookLiveProjectionTestReader.rootAttachments(
        transactionManager, notebookAttachmentRepository, notebook.getId());
  }

  private List<String> committedNoteTitles(Notebook notebook) {
    return NotebookLiveProjectionTestReader.noteTitles(
        transactionManager, noteRepository, notebook.getId());
  }

  private Note reloadNote(Note note) {
    return noteRepository.findById(note.getId()).orElseThrow();
  }

  private Folder reloadFolder(Folder folder) {
    return folderRepository.findById(folder.getId()).orElseThrow();
  }

  private record Fixture(
      Notebook notebook,
      Folder biology,
      Folder study,
      Note cells,
      Note reading,
      MemoryTracker tracker) {}
}

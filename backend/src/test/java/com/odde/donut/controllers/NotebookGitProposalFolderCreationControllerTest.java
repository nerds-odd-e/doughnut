package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.algorithms.FrontmatterNoteLevel;
import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.MemoryTrackerType;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.FolderSiblingNameValidation;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/** Verifies publication of locally authored folder README documents and contained notes. */
class NotebookGitProposalFolderCreationControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String FOLDER_README =
      "---\ntype: Readme\nsource: local\n---\nPrecisely preserved folder readme.\n";
  private static final String EXISTING_CONTENT =
      "---\ntype: Note\n---\nExisting learned content.\n";
  private static final String EDITED_EXISTING_WITH_LINK =
      "---\ntype: Note\n---\nExisting learned content linking [[Added]].\n";
  private static final String ADDED_INITIAL = "---\ntype: Note\n---\nInitial added body.\n";
  private static final String ADDED_FINAL = "---\ntype: Note\n---\nFinal added body.\n";
  private static final String NOTE_A =
      "---\ntype: Note\nauthor: local\n---\nPrecisely preserved A.\n";
  private static final String NOTE_B =
      "---\ntype: Note\nauthor: local\n---\nPrecisely preserved B.\n";
  private static final String NOTE_C = "---\ntype: Note\n---\nPrecisely preserved C.\n";
  private static final String FOLDER_README_PATH = "例文/README.md";
  private static final String NOTE_A_PATH = "例文/A.md";
  private static final String NOTE_B_PATH = "例文/B.md";

  @Autowired FolderRepository folderRepository;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;
  @Autowired TextContentController textContentController;
  @Autowired NoteController noteController;

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void publishesMixedAddAndEditRangeWithFolderReadmeAndFinalAddedContent(boolean nestedDestination)
      throws Exception {
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
    Folder nestedParent = null;
    if (nestedDestination) {
      nestedParent = makeMe.aFolder().notebook(notebook).name("Parent").please();
      makeMe
          .aNote()
          .folder(nestedParent)
          .title("Keeper")
          .content("---\ntype: Note\n---\nKeeper body.\n")
          .please();
    }
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String folderReadmePath =
        nestedDestination ? "Parent/Child/README.md" : "Field Notes/README.md";
    String addedNotePath = nestedDestination ? "Parent/Child/Added.md" : "Field Notes/Added.md";
    String keeperPath = "Parent/Keeper.md";
    String keeperContent = "---\ntype: Note\n---\nKeeper body.\n";
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    ObjectId tip;
    byte[] proposalBytes;
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, binding.getBundleBytes());
      List<NotebookGitProposalFile> afterB = new ArrayList<>();
      afterB.add(new NotebookGitProposalFile("Existing.md", EDITED_EXISTING_WITH_LINK));
      if (nestedDestination) {
        afterB.add(new NotebookGitProposalFile(keeperPath, keeperContent));
      }
      afterB.add(new NotebookGitProposalFile(folderReadmePath, FOLDER_README));
      afterB.add(new NotebookGitProposalFile(addedNotePath, ADDED_INITIAL));
      ObjectId afterAddAndEdit =
          commitOnTopOf(repository, List.of(acceptedHead), afterB, "Add folder and edit existing");
      List<NotebookGitProposalFile> afterC = new ArrayList<>();
      afterC.add(new NotebookGitProposalFile("Existing.md", EDITED_EXISTING_WITH_LINK));
      if (nestedDestination) {
        afterC.add(new NotebookGitProposalFile(keeperPath, keeperContent));
      }
      afterC.add(new NotebookGitProposalFile(folderReadmePath, FOLDER_README));
      afterC.add(new NotebookGitProposalFile(addedNotePath, ADDED_FINAL));
      tip = commitOnTopOf(repository, List.of(afterAddAndEdit), afterC, "Edit newly added note");
      proposalBytes = bundleBytesForHead(repository, tip);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    assertThat(publishedHead, equalTo(tip.getName()));
    Note reloadedExisting = noteRepository.findById(existing.getId()).orElseThrow();
    assertThat(reloadedExisting.getContent(), equalTo(EDITED_EXISTING_WITH_LINK));
    MemoryTracker retained = memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
    assertThat(retained.getNote().getId(), equalTo(existing.getId()));
    Folder created =
        folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .filter(folder -> folder.getName().equals(nestedDestination ? "Child" : "Field Notes"))
            .findFirst()
            .orElseThrow();
    assertThat(created.getReadmeContent(), equalTo(FOLDER_README));
    if (nestedParent == null) {
      assertThat(created.getParentFolderId(), nullValue());
    } else {
      assertThat(created.getParentFolderId(), equalTo(nestedParent.getId()));
    }
    Note added =
        noteByTitle(
            noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), "Added");
    assertThat(added.getContent(), equalTo(ADDED_FINAL));
    assertThat(added.getFolder().getId(), equalTo(created.getId()));
    NoteRealm existingView =
        inCommittedTransaction(
            transactionManager,
            () -> {
              try {
                return noteController.showNote(
                    noteRepository.findById(existing.getId()).orElseThrow());
              } catch (UnexpectedNoAccessRightException exception) {
                throw new IllegalStateException(exception);
              }
            });
    assertThat(existingView.getWikiLinks(), hasSize(1));
    assertThat(existingView.getWikiLinks().getFirst().getAuthoredLink(), equalTo("Added"));
    assertThat(
        existingView.getWikiLinks().getFirst().getResolution(),
        equalTo(WikiLink.Resolution.RESOLVED));
    assertThat(
        existingView.getWikiLinks().getFirst().getDestinationNoteId(), equalTo(added.getId()));
  }

  @Test
  void publishesANewRootFolderAsTheExactAuthoredCommitAndMakesItDownloadable() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Field Notes/README.md", FOLDER_README)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(folders, hasSize(1));
    Folder created = folders.getFirst();
    assertThat(created.getName(), equalTo("Field Notes"));
    assertThat(created.getParentFolderId(), nullValue());
    assertThat(created.getReadmeContent(), equalTo(FOLDER_README));
    assertThat(noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(0));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
    }
  }

  @Test
  void preservesExistingRepresentedFolderIdentityWhenPublishingANewRootFolderReadme()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("Physics").please();
    Note motion =
        makeMe
            .aNote()
            .folder(physics)
            .title("Motion")
            .content("---\ntype: Note\n---\nExisting content.\n")
            .please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile(
                    "Physics/Motion.md", "---\ntype: Note\n---\nExisting content.\n"),
                new NotebookGitProposalFile("Field Notes/README.md", FOLDER_README)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(folders, hasSize(2));
    assertThat(
        noteRepository.findById(motion.getId()).orElseThrow().getFolder().getId(),
        equalTo(physics.getId()));
    Folder created =
        folders.stream()
            .filter(folder -> folder.getName().equals("Field Notes"))
            .findFirst()
            .orElseThrow();
    assertThat(created.getReadmeContent(), equalTo(FOLDER_README));
  }

  @Test
  void refusesToAdoptAnUnrepresentedLiveFolderAsANewlyCreatedFolder() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    Folder live = makeMe.aFolder().notebook(notebook).name("Field Notes").please();
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Field Notes/README.md", FOLDER_README)));

    ApiException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposalBytes, ApiException.class);

    assertThat(
        exception.getErrorBody().getMessage(),
        equalTo(FolderSiblingNameValidation.DUPLICATE_SIBLING_NAME_HERE));
    assertThat(
        exception.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
    assertThat(
        folderRepository.findById(live.getId()).orElseThrow().getReadmeContent(), nullValue());
    assertThat(folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(1));
  }

  @Test
  void publishesAFolderReadmeAndNotesTogetherPreservingExistingIdentityAndLearning()
      throws Exception {
    LearnedNotebook fixture = boundNotebookWithLearnedNote();
    byte[] proposalBytes =
        proposalBundleBytes(
            fixture.binding(),
            withExisting(
                List.of(
                    new NotebookGitProposalFile(FOLDER_README_PATH, FOLDER_README),
                    new NotebookGitProposalFile(NOTE_A_PATH, NOTE_A),
                    new NotebookGitProposalFile(NOTE_B_PATH, NOTE_B))));
    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            fixture.notebook().getId(), fixture.binding().getAcceptedGitObjectId(), proposalBytes);

    List<Folder> folders =
        folderRepository.findByNotebookIdOrderByIdAsc(fixture.notebook().getId());
    assertThat(folders, hasSize(1));
    Folder created = folders.getFirst();
    assertThat(created.getName(), equalTo("例文"));
    assertThat(created.getParentFolderId(), nullValue());
    assertThat(created.getReadmeContent(), equalTo(FOLDER_README));
    List<Note> notes =
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(fixture.notebook().getId());
    assertThat(notes, hasSize(3));
    Note reloadedExisting = noteRepository.findById(fixture.existing().getId()).orElseThrow();
    assertThat(reloadedExisting.getContent(), equalTo(EXISTING_CONTENT));
    assertThat(reloadedExisting.getTitle(), equalTo("Existing"));
    MemoryTracker retained =
        memoryTrackerRepository.findById(fixture.tracker().getId()).orElseThrow();
    assertThat(retained.getNote().getId(), equalTo(fixture.existing().getId()));
    assertThat(retained.getType(), equalTo(MemoryTrackerType.SPELLING));
    assertThat(retained.isActive(), is(true));
    Note noteA = noteByTitle(notes, "A");
    Note noteB = noteByTitle(notes, "B");
    assertThat(noteA.getId(), not(equalTo(fixture.existing().getId())));
    assertThat(noteB.getId(), not(equalTo(fixture.existing().getId())));
    assertThat(noteA.getFolder().getId(), equalTo(created.getId()));
    assertThat(noteB.getFolder().getId(), equalTo(created.getId()));
    assertThat(noteA.getContent(), equalTo(NOTE_A));
    assertThat(noteB.getContent(), equalTo(NOTE_B));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));

    Notebook acceptedNotebook =
        notebookRepository.findById(fixture.notebook().getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(
              readBack, downloadedCommit.head(), FOLDER_README_PATH),
          equalTo(FOLDER_README));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(readBack, downloadedCommit.head(), NOTE_A_PATH),
          equalTo(NOTE_A));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(readBack, downloadedCommit.head(), NOTE_B_PATH),
          equalTo(NOTE_B));
    }
  }

  @Test
  void publishesConceptsBeneathAnImpliedFolderWithoutAFolderReadme() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder examples = makeMe.aFolder().notebook(notebook).name("例文").please();
    makeMe.aNote().folder(examples).title("Existing").content(EXISTING_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String ordinary = "---\ntype: Note\n---\nA body.\n";
    String relationship =
        "---\ntype: Relationship\nrelation: related-to\nsource: \"[[例文/111/A]]\"\ntarget: \"[[例文/111/B]]\"\n---\nRelation body.\n";
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("例文/Existing.md", EXISTING_CONTENT),
                new NotebookGitProposalFile("例文/111/A.md", ordinary),
                new NotebookGitProposalFile("例文/111/A-related-to-B.md", relationship)));
    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(folders, hasSize(2));
    Folder created =
        folders.stream().filter(folder -> folder.getName().equals("111")).findFirst().orElseThrow();
    assertThat(created.getParentFolderId(), equalTo(examples.getId()));
    assertThat(created.getReadmeContent(), nullValue());
    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    Note noteA = noteByTitle(notes, "A");
    Note related = noteByTitle(notes, "A-related-to-B");
    assertThat(noteA.getFolder().getId(), equalTo(created.getId()));
    assertThat(related.getFolder().getId(), equalTo(created.getId()));
    assertThat(noteA.getContent(), equalTo(ordinary));
    assertThat(related.getContent(), equalTo(relationship));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
    }
  }

  @Test
  void publishesAFolderReadmeWithOneOrdinaryNote() throws Exception {
    LearnedNotebook fixture = boundNotebookWithLearnedNote();
    byte[] proposalBytes =
        proposalBundleBytes(
            fixture.binding(),
            withExisting(
                List.of(
                    new NotebookGitProposalFile(FOLDER_README_PATH, FOLDER_README),
                    new NotebookGitProposalFile(NOTE_A_PATH, NOTE_A))));

    controller.publishNotebookGitProposal(
        fixture.notebook().getId(), fixture.binding().getAcceptedGitObjectId(), proposalBytes);

    List<Note> notes =
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(fixture.notebook().getId());
    assertThat(notes, hasSize(2));
    assertThat(notes.stream().map(Note::getTitle).toList(), containsInAnyOrder("Existing", "A"));
  }

  @Test
  void publishesSeveralOrdinaryNotesRegardlessOfProposalEntryOrder() throws Exception {
    LearnedNotebook fixture = boundNotebookWithLearnedNote();
    byte[] proposalBytes =
        proposalBundleBytes(
            fixture.binding(),
            withExisting(
                List.of(
                    new NotebookGitProposalFile("例文/C.md", NOTE_C),
                    new NotebookGitProposalFile(NOTE_B_PATH, NOTE_B),
                    new NotebookGitProposalFile(FOLDER_README_PATH, FOLDER_README),
                    new NotebookGitProposalFile(NOTE_A_PATH, NOTE_A))));

    controller.publishNotebookGitProposal(
        fixture.notebook().getId(), fixture.binding().getAcceptedGitObjectId(), proposalBytes);

    List<Note> notes =
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(fixture.notebook().getId());
    assertThat(notes, hasSize(4));
    assertThat(
        notes.stream().map(Note::getTitle).toList(), containsInAnyOrder("Existing", "A", "B", "C"));
  }

  @Test
  void retriesAnAcceptedFolderAndNotesCommitWithoutDuplicatingIdentities() throws Exception {
    LearnedNotebook fixture = boundNotebookWithLearnedNote();
    String initialHead = fixture.binding().getAcceptedGitObjectId();
    byte[] proposalBytes =
        proposalBundleBytes(
            fixture.binding(),
            withExisting(
                List.of(
                    new NotebookGitProposalFile(FOLDER_README_PATH, FOLDER_README),
                    new NotebookGitProposalFile(NOTE_A_PATH, NOTE_A),
                    new NotebookGitProposalFile(NOTE_B_PATH, NOTE_B))));

    String publishedHead =
        controller.publishNotebookGitProposal(
            fixture.notebook().getId(), initialHead, proposalBytes);
    PublicationFootprint published = committedFootprint(fixture.notebook());
    assertThat(published.folderIds(), hasSize(1));
    assertThat(published.noteIds(), hasSize(3));

    String retriedHead =
        controller.publishNotebookGitProposal(
            fixture.notebook().getId(), initialHead, proposalBytes);

    assertThat(retriedHead, equalTo(publishedHead));
    PublicationFootprint retried = committedFootprint(fixture.notebook());
    assertThat(retried.acceptedHead(), equalTo(published.acceptedHead()));
    assertThat(retried.folderIds(), equalTo(published.folderIds()));
    assertThat(retried.noteIds(), equalTo(published.noteIds()));
    assertThat(retried.trackerId(), equalTo(published.trackerId()));
  }

  @Test
  void rejectsInvalidMemberAfterEligibleFolderAndNotesWithoutPartialPublication() throws Exception {
    LearnedNotebook fixture = boundNotebookWithLearnedNote();
    PublicationFootprint before = committedFootprint(fixture.notebook());
    byte[] proposalBytes =
        proposalBundleBytes(
            fixture.binding(),
            withExisting(
                List.of(
                    new NotebookGitProposalFile(FOLDER_README_PATH, FOLDER_README),
                    new NotebookGitProposalFile(NOTE_A_PATH, NOTE_A),
                    new NotebookGitProposalFile(
                        NOTE_B_PATH, "---\ntype: Note\nnote_level: 7\n---\ninvalid content"))));

    ApiException exception =
        assertProposalRejectedWithoutMutatingBinding(
            fixture.notebook(),
            fixture.binding().getAcceptedGitObjectId(),
            proposalBytes,
            ApiException.class);

    assertThat(exception.getErrorBody().getMessage(), containsString(NOTE_B_PATH));
    assertThat(
        exception.getErrorBody().getMessage(),
        containsString(FrontmatterNoteLevel.AUTHORED_NOTE_LEVEL_MESSAGE));
    assertThat(committedFootprint(fixture.notebook()), equalTo(before));
  }

  @Test
  void refusesAComposedProposalThatWouldAdoptAnUnrepresentedLiveFolder() throws Exception {
    LearnedNotebook fixture = boundNotebookWithLearnedNote();
    Folder live = makeMe.aFolder().notebook(fixture.notebook()).name("例文").please();
    byte[] proposalBytes =
        proposalBundleBytes(
            fixture.binding(),
            withExisting(
                List.of(
                    new NotebookGitProposalFile(FOLDER_README_PATH, FOLDER_README),
                    new NotebookGitProposalFile(NOTE_A_PATH, NOTE_A),
                    new NotebookGitProposalFile(NOTE_B_PATH, NOTE_B))));

    ApiException exception =
        assertProposalRejectedWithoutMutatingBinding(
            fixture.notebook(),
            fixture.binding().getAcceptedGitObjectId(),
            proposalBytes,
            ApiException.class);

    assertThat(
        exception.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
    assertThat(committedFootprint(fixture.notebook()).folderIds(), equalTo(List.of(live.getId())));
    assertThat(
        committedFootprint(fixture.notebook()).noteIds(),
        equalTo(List.of(fixture.existing().getId())));
  }

  @Test
  void refusesAComposedProposalBasedOnAStaleHeadWithoutWritingFolderOrNotes() throws Exception {
    LearnedNotebook fixture = boundNotebookWithLearnedNote();
    byte[] proposalBytes =
        proposalBundleBytes(
            fixture.binding(),
            withExisting(
                List.of(
                    new NotebookGitProposalFile(FOLDER_README_PATH, FOLDER_README),
                    new NotebookGitProposalFile(NOTE_A_PATH, NOTE_A),
                    new NotebookGitProposalFile(NOTE_B_PATH, NOTE_B))));
    NoteUpdateContentDTO update = new NoteUpdateContentDTO();
    update.setContent("---\ntype: Note\n---\nWeb advanced content.\n");
    textContentController.updateNoteContent(fixture.existing(), update);
    PublicationFootprint afterWeb = committedFootprint(fixture.notebook());

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            fixture.notebook(),
            fixture.binding().getAcceptedGitObjectId(),
            proposalBytes,
            HttpStatus.CONFLICT);

    assertThat(exception.getReason(), containsString("expectedHead no longer matches"));
    assertThat(committedFootprint(fixture.notebook()), equalTo(afterWeb));
  }

  @Test
  void refusesAComposedProposalWhenLiveProjectionHasDrifted() throws Exception {
    LearnedNotebook fixture = boundNotebookWithLearnedNote();
    NoteCreationDTO webCreation = new NoteCreationDTO();
    webCreation.setNewTitle("addition");
    webCreation.setContent(
        "---\ntype: Relationship\nsource: \"[[A]]\"\ntarget: \"[[B]]\"\n---\nweb content");
    Note occupied =
        noteRepository
            .findById(controller.createNoteAtNotebookRoot(fixture.notebook(), webCreation).getId())
            .orElseThrow();
    byte[] proposalBytes =
        proposalBundleBytes(
            fixture.binding(),
            withExisting(
                List.of(
                    new NotebookGitProposalFile(FOLDER_README_PATH, FOLDER_README),
                    new NotebookGitProposalFile(NOTE_A_PATH, NOTE_A),
                    new NotebookGitProposalFile(NOTE_B_PATH, NOTE_B))));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            fixture.notebook(),
            fixture.binding().getAcceptedGitObjectId(),
            proposalBytes,
            HttpStatus.CONFLICT);

    assertThat(exception.getReason(), containsString("refresh the checkout before publishing"));
    assertThat(
        noteRepository.findById(fixture.existing().getId()).orElseThrow().getContent(),
        equalTo(EXISTING_CONTENT));
    assertThat(
        folderRepository.findByNotebookIdOrderByIdAsc(fixture.notebook().getId()), hasSize(0));
    assertThat(
        noteRepository.findById(occupied.getId()).orElseThrow().getTitle(), equalTo("addition"));
    assertThat(
        inCommittedTransaction(
            transactionManager,
            () ->
                memoryTrackerRepository
                    .findById(fixture.tracker().getId())
                    .orElseThrow()
                    .isActive()),
        equalTo(true));
  }

  private LearnedNotebook boundNotebookWithLearnedNote() throws Exception {
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

  private List<NotebookGitProposalFile> withExisting(List<NotebookGitProposalFile> additions) {
    List<NotebookGitProposalFile> files = new ArrayList<>();
    files.add(new NotebookGitProposalFile("Existing.md", EXISTING_CONTENT));
    files.addAll(additions);
    return files;
  }

  private static Note noteByTitle(List<Note> notes, String title) {
    return notes.stream().filter(note -> title.equals(note.getTitle())).findFirst().orElseThrow();
  }

  private PublicationFootprint committedFootprint(Notebook notebook) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
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

  private record LearnedNotebook(
      Notebook notebook, Note existing, MemoryTracker tracker, NotebookGitBinding binding) {}

  private record PublicationFootprint(
      String acceptedHead,
      List<Integer> folderIds,
      List<Integer> noteIds,
      List<String> noteContents,
      Integer trackerId,
      MemoryTrackerType trackerType,
      boolean trackerActive) {}
}

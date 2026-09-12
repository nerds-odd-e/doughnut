package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteRecallInfo;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.MemoryTrackerType;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.NoteReferenceService;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/** Verifies the accepted publication round trip across Note projection and Git history. */
class NotebookGitPublicationControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\nOriginal authored bytes.\n";
  private static final String TARGET_CONTENT = "---\ntype: Note\n---\nReference target.\n";
  private static final String PUBLISHED_CONTENT =
      "---\n"
          + "type: FieldObservation\n"
          + "confidence: 7\n"
          + "reviewed: false\n"
          + "---\n"
          + "Precisely preserved authored bytes linking [[Reference Target|the target]].\n";
  private static final String TARGET_NOTE_CONTENT = "---\ntype: Note\n---\nTarget body.\n";
  private static final String CARRIER_WITH_PROPERTY_AND_ALIAS =
      """
      ---
      type: Note
      example of: "[[Target]]"
      aliases:
        - color
      ---
      Carrier body.
      """;
  private static final String CARRIER_WITH_STALE_PROPERTY_AND_ALIAS =
      """
      ---
      type: Note
      example of: "[[Old Target]]"
      aliases:
        - old-color
      ---
      Carrier body.
      """;
  private static final String ALIAS_VIEWER_CONTENT = "---\ntype: Note\n---\nSee [[color]]\n";
  private static final String STALE_ALIAS_VIEWER_CONTENT =
      "---\ntype: Note\n---\nSee [[old-color]]\n";

  @Autowired NoteController noteController;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;
  @Autowired NoteReferenceService noteReferenceService;

  @Test
  void publishesAnAdditionBetweenTwoEditsOnTheSameLearnedNotes() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note first =
        makeMe.aNote().notebook(notebook).title("First").content(ORIGINAL_CONTENT).please();
    Note last = makeMe.aNote().notebook(notebook).title("Last").content(ORIGINAL_CONTENT).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(first.getId()).orElseThrow())
                    .difficulty(7f)
                    .please());
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("First.md", PUBLISHED_CONTENT),
                new NotebookGitProposalFile("Intermediate.md", TARGET_CONTENT),
                new NotebookGitProposalFile("Last.md", PUBLISHED_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposal);

    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(notes, hasSize(3));
    for (Note edited : List.of(first, last)) {
      NoteRealm view =
          noteController.showNote(noteRepository.findById(edited.getId()).orElseThrow());
      assertThat(view.getId(), equalTo(edited.getId()));
      assertThat(view.getNote().getContent(), equalTo(PUBLISHED_CONTENT));
    }
    Note added =
        notes.stream()
            .filter(note -> note.getTitle().equals("Intermediate"))
            .findFirst()
            .orElseThrow();
    assertThat(noteController.showNote(added).getNote().getContent(), equalTo(TARGET_CONTENT));
    NoteRecallInfo recallInfo =
        noteController.getNoteInfo(noteRepository.findById(first.getId()).orElseThrow());
    assertThat(recallInfo.getMemoryTrackers(), hasSize(1));
    assertThat(recallInfo.getMemoryTrackers().getFirst().getId(), equalTo(tracker.getId()));
    MemoryTracker retained = memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
    assertThat(retained.getDifficulty(), equalTo(tracker.getDifficulty()));
    assertThat(retained.getStability(), equalTo(tracker.getStability()));
    assertThat(retained.getLastRecalledAt(), equalTo(tracker.getLastRecalledAt()));
    assertThat(retained.getNextRecallAt(), equalTo(tracker.getNextRecallAt()));
    assertThat(retained.getAssimilatedAt(), equalTo(tracker.getAssimilatedAt()));
    assertThat(retained.getRemovedFromTracking(), equalTo(tracker.getRemovedFromTracking()));
    assertThat(retained.getType(), equalTo(tracker.getType()));
    assertThat(retained.getPropertyKey(), equalTo(tracker.getPropertyKey()));
  }

  @Test
  void publishesExactCommitOnTheSameLearnedNoteAndMakesItDownloadable() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Research").please();
    Note target =
        makeMe
            .aNote()
            .notebook(notebook)
            .title("Reference Target")
            .content(TARGET_CONTENT)
            .please();
    Note refinedNote =
        makeMe.aNote().folder(folder).title("Refined Note").content(ORIGINAL_CONTENT).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(refinedNote.getId()).orElseThrow())
                    .difficulty(7f)
                    .please());
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Reference Target.md", TARGET_CONTENT),
                new NotebookGitProposalFile("Research/Refined Note.md", PUBLISHED_CONTENT)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
      assertThat(proposedCommit.parent(), equalTo(acceptedHead));
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Note reloadedNote = noteRepository.findById(refinedNote.getId()).orElseThrow();
    NoteRealm noteRealm = noteController.showNote(reloadedNote);
    NoteRecallInfo recallInfo = noteController.getNoteInfo(reloadedNote);
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));
    assertThat(noteRealm.getId(), equalTo(refinedNote.getId()));
    assertThat(noteRealm.getNote().getContent(), equalTo(PUBLISHED_CONTENT));
    assertThat(noteRealm.getWikiLinks(), hasSize(1));
    assertThat(
        noteRealm.getWikiLinks().getFirst().getAuthoredLink(),
        equalTo("Reference Target|the target"));
    assertThat(
        noteRealm.getWikiLinks().getFirst().getResolution(), equalTo(WikiLink.Resolution.RESOLVED));
    assertThat(noteRealm.getWikiLinks().getFirst().getDestinationNoteId(), equalTo(target.getId()));
    assertThat(recallInfo.getMemoryTrackers(), hasSize(1));
    assertThat(recallInfo.getMemoryTrackers().getFirst().getId(), equalTo(tracker.getId()));
    assertThat(recallInfo.getMemoryTrackers().getFirst().getDifficulty(), equalTo(7f));

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(
              readBack, downloadedCommit.head(), "Research/Refined Note.md"),
          equalTo(PUBLISHED_CONTENT));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(downloadedCommit.parent(), equalTo(acceptedHead));
    }
  }

  @Test
  void reportsAnAlreadyAcceptedCommitWithoutMutatingPublicationOrLearningState() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("Refined Note").content(ORIGINAL_CONTENT).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
                    .difficulty(7f)
                    .please());
    NotebookGitBinding initialBinding = snapshotCurrentPortableTree(notebook);
    String initialHead = initialBinding.getAcceptedGitObjectId();
    byte[] proposalBytes =
        proposalBundleBytes(
            initialBinding,
            List.of(new NotebookGitProposalFile("Refined Note.md", PUBLISHED_CONTENT)));

    String publishedHead =
        controller.publishNotebookGitProposal(notebook.getId(), initialHead, proposalBytes);
    PublicationState stateAfterPublication = publicationState(notebook, note, tracker);

    String retriedHead =
        controller.publishNotebookGitProposal(notebook.getId(), initialHead, proposalBytes);

    assertThat(retriedHead, equalTo(publishedHead));
    assertThat(publicationState(notebook, note, tracker), equalTo(stateAfterPublication));
  }

  @Test
  void publishesPropertyWikiReferencesAndAliasesVisibleAfterCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note target =
        makeMe.aNote().notebook(notebook).title("Target").content(TARGET_NOTE_CONTENT).please();
    Note viewer = makeMe.aNote().notebook(notebook).title("Viewer").please();
    inCommittedTransaction(
        transactionManager,
        () ->
            makeMe.authorReferencingContent(
                noteRepository.findById(viewer.getId()).orElseThrow(), ALIAS_VIEWER_CONTENT));
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Target.md", TARGET_NOTE_CONTENT),
                new NotebookGitProposalFile("Viewer.md", ALIAS_VIEWER_CONTENT),
                new NotebookGitProposalFile("Carrier.md", CARRIER_WITH_PROPERTY_AND_ALIAS))));

    Note carrier =
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .filter(note -> note.getTitle().equals("Carrier"))
            .findFirst()
            .orElseThrow();
    NoteRealm published = shownAfterCommit(carrier);
    assertThat(
        wikiLink(published, "Target").getResolution(), equalTo(WikiLink.Resolution.RESOLVED));
    assertThat(wikiLink(published, "Target").getDestinationNoteId(), equalTo(target.getId()));
    NoteRealm inbound = shownAfterCommit(viewer);
    assertThat(wikiLink(inbound, "color").getResolution(), equalTo(WikiLink.Resolution.RESOLVED));
    assertThat(wikiLink(inbound, "color").getDestinationNoteId(), equalTo(carrier.getId()));
  }

  @Test
  void replacingPropertyWikiAndAliasRemovesStaleDerivedEntries() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note target =
        makeMe.aNote().notebook(notebook).title("Target").content(TARGET_NOTE_CONTENT).please();
    makeMe.aNote().notebook(notebook).title("Old Target").content(TARGET_NOTE_CONTENT).please();
    Note carrier =
        makeMe
            .aNote()
            .notebook(notebook)
            .title("Carrier")
            .content(CARRIER_WITH_STALE_PROPERTY_AND_ALIAS)
            .please();
    inCommittedTransaction(
        transactionManager,
        () -> {
          Note reloaded = noteRepository.findById(carrier.getId()).orElseThrow();
          makeMe.authorReferencingContent(reloaded, CARRIER_WITH_STALE_PROPERTY_AND_ALIAS);
          noteReferenceService.refreshDerivedIndexesForNote(reloaded);
        });
    Note staleViewer = makeMe.aNote().notebook(notebook).title("Stale Viewer").please();
    inCommittedTransaction(
        transactionManager,
        () ->
            makeMe.authorReferencingContent(
                noteRepository.findById(staleViewer.getId()).orElseThrow(),
                STALE_ALIAS_VIEWER_CONTENT));
    Note viewer = makeMe.aNote().notebook(notebook).title("Viewer").please();
    inCommittedTransaction(
        transactionManager,
        () ->
            makeMe.authorReferencingContent(
                noteRepository.findById(viewer.getId()).orElseThrow(), ALIAS_VIEWER_CONTENT));
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Target.md", TARGET_NOTE_CONTENT),
                new NotebookGitProposalFile("Old Target.md", TARGET_NOTE_CONTENT),
                new NotebookGitProposalFile("Stale Viewer.md", STALE_ALIAS_VIEWER_CONTENT),
                new NotebookGitProposalFile("Viewer.md", ALIAS_VIEWER_CONTENT),
                new NotebookGitProposalFile("Carrier.md", CARRIER_WITH_PROPERTY_AND_ALIAS))));

    NoteRealm published = shownAfterCommit(carrier);
    assertThat(wikiLink(published, "Target").getDestinationNoteId(), equalTo(target.getId()));
    assertThat(
        published.getWikiLinks().stream()
            .filter(link -> "Old Target".equals(link.getAuthoredLink()))
            .toList(),
        empty());
    assertThat(shownAfterCommit(staleViewer).getWikiLinks(), empty());
    NoteRealm inbound = shownAfterCommit(viewer);
    assertThat(wikiLink(inbound, "color").getDestinationNoteId(), equalTo(carrier.getId()));
  }

  private NoteRealm shownAfterCommit(Note note) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          try {
            return noteController.showNote(noteRepository.findById(note.getId()).orElseThrow());
          } catch (UnexpectedNoAccessRightException exception) {
            throw new IllegalStateException(exception);
          }
        });
  }

  private static WikiLink wikiLink(NoteRealm realm, String authoredLink) {
    return realm.getWikiLinks().stream()
        .filter(link -> authoredLink.equals(link.getAuthoredLink()))
        .findFirst()
        .orElseThrow();
  }

  private PublicationState publicationState(Notebook notebook, Note note, MemoryTracker tracker) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          NotebookGitBinding binding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          Note reloadedNote = noteRepository.findById(note.getId()).orElseThrow();
          MemoryTracker reloadedTracker =
              memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
          return new PublicationState(
              binding.getAcceptedGitObjectId(),
              binding.getUpdatedAt(),
              reloadedNote.getContent(),
              reloadedNote.getUpdatedAt(),
              reloadedTracker.getLastRecalledAt(),
              reloadedTracker.getNextRecallAt(),
              reloadedTracker.getAssimilatedAt(),
              reloadedTracker.getStability(),
              reloadedTracker.getDifficulty(),
              reloadedTracker.getRemovedFromTracking(),
              reloadedTracker.getType(),
              reloadedTracker.getPropertyKey());
        });
  }

  private record PublicationState(
      String acceptedHead,
      Timestamp bindingUpdatedAt,
      String noteContent,
      Timestamp noteUpdatedAt,
      Timestamp lastRecalledAt,
      Timestamp nextRecallAt,
      Timestamp assimilatedAt,
      Float stability,
      Float difficulty,
      Boolean removedFromTracking,
      MemoryTrackerType trackerType,
      String propertyKey) {}
}

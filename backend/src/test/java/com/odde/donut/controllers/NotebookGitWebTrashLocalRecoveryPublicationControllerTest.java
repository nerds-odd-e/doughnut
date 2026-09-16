package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotebookGitWebTrashLocalRecoveryPublicationControllerTest
    extends NotebookGitWebContentControllerTestBase {
  static final Instant TRASH_AT = Instant.parse("2026-09-08T10:00:00Z");
  static final Instant OCCUPY_AT = Instant.parse("2026-09-08T10:03:00Z");
  static final Instant RECOVER_AT = Instant.parse("2026-09-08T10:05:00Z");
  static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";
  static final String OCCUPIER_BODY = "---\ntype: Note\n---\noccupier body";
  static final String REFERRER_BODY =
      """
      ---
      type: Note
      related: "[[Biology/Cells]]"
      ---
      See [[Biology/Cells]]
      """;
  static final String PROPERTY_ONLY_REFERRER_BODY = "---\ntarget: \"[[Biology/Cells]]\"\n---\nBody";
  static final String PROPERTY_REMOVED_REFERRER_BODY = "---\ntype: Note\n---\nBody";

  @Test
  void localPublicationAfterActualTrashRestoresSameNoteAvailabilityAndRetainedDependencies()
      throws Exception {
    LearnedRecoveryFixture f = seedLearnedCellsWithRetainedReferrer();
    testabilitySettings.timeTravelTo(Timestamp.from(TRASH_AT));
    noteController.trashNote(f.cells(), leaveDeadLinks());
    testabilitySettings.timeTravelTo(Timestamp.from(RECOVER_AT));
    NotebookGitBinding afterTrash = binding(f.notebook());

    controller.publishNotebookGitProposal(
        f.notebook().getId(),
        afterTrash.getAcceptedGitObjectId(),
        recoveryProposal(afterTrash, REFERRER_BODY));

    inCommittedTransaction(
        transactionManager,
        () -> {
          Note recovered = noteRepository.findById(f.cells().getId()).orElseThrow();
          assertThat(recovered.isTrashed(), is(false));
          assertThat(recovered.isAvailable(), is(true));
          assertThat(recovered.getFolder().getId(), equalTo(f.biology().getId()));
          assertThat(recovered.getContent(), equalTo(CELLS_BODY));
          MemoryTracker learned =
              memoryTrackerRepository.findById(f.tracker().getId()).orElseThrow();
          assertThat(learned.getNote().getId(), equalTo(f.cells().getId()));
          assertThat(learned.isActive(), is(true));
          assertThat(learned.getDifficulty(), equalTo(f.tracker().getDifficulty()));
          assertThat(learned.getStability(), equalTo(f.tracker().getStability()));
          assertThat(learned.getLastRecalledAt(), equalTo(f.tracker().getLastRecalledAt()));
          assertThat(learned.getNextRecallAt(), equalTo(f.tracker().getNextRecallAt()));
          assertThat(learned.getAssimilatedAt(), equalTo(f.tracker().getAssimilatedAt()));
          MemoryTracker removed =
              memoryTrackerRepository.findById(f.removed().getId()).orElseThrow();
          assertThat(removed.getNote().getId(), equalTo(f.cells().getId()));
          assertThat(removed.getRemovedFromTracking(), is(true));
          assertThat(removed.isActive(), is(false));
          assertThat(
              countRecallLogsByTrackerId(f.tracker().getId()), equalTo(f.recallCountBefore()));
        });
    NoteRealm shown =
        noteController.showNote(noteRepository.findById(f.referrer().getId()).orElseThrow());
    assertThat(shown.getNote().getContent(), equalTo(REFERRER_BODY));
    List<WikiLink> cellsLinks = biologyCellsLinks(shown);
    assertThat(cellsLinks, not(empty()));
    assertThat(
        cellsLinks.stream().map(WikiLink::getResolution).toList(),
        everyItem(equalTo(WikiLink.Resolution.RESOLVED)));
    assertThat(
        cellsLinks.stream().map(WikiLink::getDestinationNoteId).distinct().toList(),
        equalTo(List.of(f.cells().getId())));
  }

  @Test
  void localPublicationAfterRemoveFromPropertiesTrashLeavesTheRemovedPropertyAbsent()
      throws Exception {
    PropertyRemovedFixture f = seedCellsWithPropertyReferrer();
    testabilitySettings.timeTravelTo(Timestamp.from(TRASH_AT));
    noteController.trashNote(f.cells(), removeFromProperties());
    String strippedReferrer =
        inCommittedTransaction(
            transactionManager,
            () -> noteRepository.findById(f.referrer().getId()).orElseThrow().getContent());
    assertThat(strippedReferrer, equalTo(PROPERTY_REMOVED_REFERRER_BODY));
    testabilitySettings.timeTravelTo(Timestamp.from(RECOVER_AT));
    NotebookGitBinding afterTrash = binding(f.notebook());

    controller.publishNotebookGitProposal(
        f.notebook().getId(),
        afterTrash.getAcceptedGitObjectId(),
        recoveryProposal(afterTrash, strippedReferrer));

    NoteRealm shown =
        noteController.showNote(noteRepository.findById(f.referrer().getId()).orElseThrow());
    assertThat(shown.getNote().getContent(), equalTo(PROPERTY_REMOVED_REFERRER_BODY));
    assertThat(shown.getWikiLinks(), empty());
  }

  @Test
  void localPublicationRecoversToAFreePathWhenTheOriginalPathIsOccupied() throws Exception {
    LearnedRecoveryFixture f = seedLearnedCellsWithRetainedReferrer();
    testabilitySettings.timeTravelTo(Timestamp.from(TRASH_AT));
    noteController.trashNote(f.cells(), leaveDeadLinks());
    testabilitySettings.timeTravelTo(Timestamp.from(OCCUPY_AT));
    NoteCreationDTO occupierCreation = new NoteCreationDTO();
    occupierCreation.setNewTitle("Cells");
    occupierCreation.setFolderId(f.biology().getId());
    Integer occupierId =
        controller.createNoteAtNotebookRoot(f.notebook(), occupierCreation).getId();
    textContentController.updateNoteContent(
        noteRepository.findById(occupierId).orElseThrow(), contentDto(OCCUPIER_BODY));
    testabilitySettings.timeTravelTo(Timestamp.from(RECOVER_AT));
    NotebookGitBinding afterOccupier = binding(f.notebook());
    byte[] proposalBytes =
        proposalBundleBytes(
            afterOccupier,
            List.of(
                new NotebookGitProposalFile("Biology/Cells.md", OCCUPIER_BODY),
                new NotebookGitProposalFile("Biology/Nucleus.md", CELLS_BODY),
                new NotebookGitProposalFile("Reading.md", REFERRER_BODY),
                new NotebookGitProposalFile("_trash/Biology/.keep", "")));

    controller.publishNotebookGitProposal(
        f.notebook().getId(), afterOccupier.getAcceptedGitObjectId(), proposalBytes);

    Note occupier = noteRepository.findById(occupierId).orElseThrow();
    assertThat(occupier.getTitle(), equalTo("Cells"));
    assertThat(occupier.getFolder().getId(), equalTo(f.biology().getId()));
    assertThat(occupier.getContent(), equalTo(OCCUPIER_BODY));
    Note recovered = noteRepository.findById(f.cells().getId()).orElseThrow();
    assertThat(recovered.getTitle(), equalTo("Nucleus"));
    assertThat(recovered.isTrashed(), is(false));
    assertThat(recovered.getFolder().getId(), equalTo(f.biology().getId()));
    NoteRealm shown =
        noteController.showNote(noteRepository.findById(f.referrer().getId()).orElseThrow());
    assertThat(shown.getNote().getContent(), equalTo(REFERRER_BODY));
    assertThat(
        biologyCellsLinks(shown).stream().map(WikiLink::getDestinationNoteId).distinct().toList(),
        equalTo(List.of(occupierId)));
  }

  byte[] recoveryProposal(NotebookGitBinding afterTrash, String referrerBody) throws Exception {
    return proposalBundleBytes(
        afterTrash,
        List.of(
            new NotebookGitProposalFile("Biology/Cells.md", CELLS_BODY),
            new NotebookGitProposalFile("Reading.md", referrerBody),
            new NotebookGitProposalFile("_trash/Biology/.keep", "")));
  }

  LearnedRecoveryFixture seedLearnedCellsWithRetainedReferrer()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    Note referrer = makeMe.aNote("Reading").notebook(notebook).please();
    inCommittedTransaction(
        transactionManager,
        () ->
            authorReferencingContent(
                noteRepository.findById(referrer.getId()).orElseThrow(), REFERRER_BODY));
    MemoryTracker tracker = learnedTracker(cells, 0.5f, 1);
    MemoryTracker removed =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(cells.getId()).orElseThrow())
                    .spelling()
                    .removedFromTracking()
                    .please());
    long recallCountBefore =
        inCommittedTransaction(
            transactionManager, () -> countRecallLogsByTrackerId(tracker.getId()));
    snapshotCurrentPortableTree(notebook);
    return new LearnedRecoveryFixture(
        notebook, biology, cells, referrer, tracker, removed, recallCountBefore);
  }

  PropertyRemovedFixture seedCellsWithPropertyReferrer() throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    Note referrer = makeMe.aNote("Reading").notebook(notebook).please();
    inCommittedTransaction(
        transactionManager,
        () ->
            authorReferencingContent(
                noteRepository.findById(referrer.getId()).orElseThrow(),
                PROPERTY_ONLY_REFERRER_BODY));
    snapshotCurrentPortableTree(notebook);
    return new PropertyRemovedFixture(notebook, cells, referrer);
  }

  static List<WikiLink> biologyCellsLinks(NoteRealm shown) {
    return shown.getWikiLinks().stream()
        .filter(link -> "Biology/Cells".equals(link.getTarget()))
        .toList();
  }

  record LearnedRecoveryFixture(
      Notebook notebook,
      Folder biology,
      Note cells,
      Note referrer,
      MemoryTracker tracker,
      MemoryTracker removed,
      long recallCountBefore) {}

  record PropertyRemovedFixture(Notebook notebook, Note cells, Note referrer) {}
}

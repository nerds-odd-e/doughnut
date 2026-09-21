package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookGitProposalFolderRelocationAttachmentControllerTest
    extends NotebookGitControllerTestBase {

  private static final String NOTE = "---\ntype: Note\n---\nnote";
  private static final byte[] FORCE_DIAGRAM = NOTE.getBytes(StandardCharsets.UTF_8);

  @Autowired FolderRepository folderRepository;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;
  @Autowired NotebookAttachmentRepository notebookAttachmentRepository;

  @Test
  void localFolderRenameRetainsLearnedNoteIdentityAndCarriesItsAttachment() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    List<NotebookGitProposalFile> physicsTree =
        List.of(
            new NotebookGitProposalFile("physics/Particle.md", NOTE),
            new NotebookGitProposalFile("physics/diagrams/force.png", FORCE_DIAGRAM));
    controller.publishNotebookGitProposal(
        notebook.getId(), empty.getAcceptedGitObjectId(), proposalBundleBytes(empty, physicsTree));
    Note particle = noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()).getFirst();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(particle.getId()).orElseThrow())
                    .difficulty(7f)
                    .recallCount(2)
                    .please());

    NotebookGitBinding accepted = reloadCommittedBinding(notebook.getId());
    List<NotebookGitProposalFile> mechanicsTree =
        List.of(
            new NotebookGitProposalFile("mechanics/Particle.md", NOTE),
            new NotebookGitProposalFile("mechanics/diagrams/force.png", FORCE_DIAGRAM));
    controller.publishNotebookGitProposal(
        notebook.getId(),
        accepted.getAcceptedGitObjectId(),
        proposalBundleBytes(accepted, mechanicsTree));

    Note retainedParticle = noteRepository.findById(particle.getId()).orElseThrow();
    assertThat(retainedParticle.getFolder().getName(), equalTo("mechanics"));
    MemoryTracker retainedTracker = memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
    assertThat(retainedTracker.getNote().getId(), equalTo(particle.getId()));
    assertThat(retainedTracker.getDifficulty(), equalTo(7f));
    assertThat(retainedTracker.getRecallCount(), equalTo(2));
    List<PortableTreeEntry> expectedTree =
        List.of(
            PortableTreeEntry.ofText("mechanics/Particle.md", NOTE),
            new PortableTreeEntry("mechanics/diagrams/force.png", FORCE_DIAGRAM));
    assertThat(
        GitBundleTestReader.fetchTipTreeEntries(acceptedBundleBytes(notebook)),
        equalTo(expectedTree));
    assertThat(
        NotebookLiveProjectionTestReader.attachmentTree(
            transactionManager, notebookAttachmentRepository, folderRepository, notebook.getId()),
        equalTo(List.of(expectedTree.getLast())));
  }
}

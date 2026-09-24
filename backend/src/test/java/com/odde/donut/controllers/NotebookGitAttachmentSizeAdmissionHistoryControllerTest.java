package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Raw Git attachment-size admission across unpublished first-parent history. Oversized intermediate
 * blobs are refused even when the tip is valid; within-limit multi-version history is retained.
 */
class NotebookGitAttachmentSizeAdmissionHistoryControllerTest
    extends NotebookGitAttachmentSizeAdmissionTestSupport {

  private static final byte[] FIRST_SMALL = filledBytes(64, (byte) 0x51);
  private static final byte[] SECOND_SMALL = filledBytes(128, (byte) 0x52);
  private static final byte[] HALF_PLUS = filledBytes(LIMIT / 2 + 1, (byte) 0x53);

  private record AttachmentRange(ObjectId tip, byte[] proposalBytes, ObjectId overBlobId) {}

  private record ValidVersionsRange(ObjectId afterFirst, ObjectId tip, byte[] proposalBytes) {}

  @Test
  void refusesOversizedIntermediateAttachmentDeletedBeforeTipMatchingAcceptedTree()
      throws Exception {
    Notebook notebook = createLegacyRawNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty, List.of(new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN))));
    NotebookGitBinding accepted = reloadCommittedBinding(notebook.getId());
    ObjectId acceptedHead = ObjectId.fromString(accepted.getAcceptedGitObjectId());
    byte[] over = filledBytes(LIMIT + 1, (byte) 0x61);
    List<NotebookGitProposalFile> tipFiles =
        List.of(new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN));
    AttachmentRange range =
        twoStepRange(
            acceptedBundleBytes(notebook),
            acceptedHead,
            Stream.concat(
                    tipFiles.stream(), Stream.of(new NotebookGitProposalFile("huge.bin", over)))
                .toList(),
            tipFiles,
            over);

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            accepted.getAcceptedGitObjectId(),
            range.proposalBytes(),
            HttpStatus.BAD_REQUEST);

    assertOversizedRefusal(exception, "huge.bin", LIMIT + 1);
    assertThat(nativeObjectPresent(accepted.getId(), range.overBlobId()), is(false));
  }

  @Test
  void refusesOversizedIntermediateAttachmentReplacedBySmallerTipVersion() throws Exception {
    Notebook notebook = createLegacyRawNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] over = filledBytes(LIMIT + 1, (byte) 0x62);
    byte[] small = filledBytes(32, (byte) 0x63);
    AttachmentRange range =
        twoStepRange(
            acceptedBundleBytes(notebook),
            ObjectId.fromString(empty.getAcceptedGitObjectId()),
            List.of(new NotebookGitProposalFile("payload.bin", over)),
            List.of(new NotebookGitProposalFile("payload.bin", small)),
            over);

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            empty.getAcceptedGitObjectId(),
            range.proposalBytes(),
            HttpStatus.BAD_REQUEST);

    assertOversizedRefusal(exception, "payload.bin", LIMIT + 1);
    assertThat(nativeObjectPresent(empty.getId(), range.overBlobId()), is(false));
  }

  @Test
  void refusesOversizedIntermediateAttachmentLaterRenamedToMarkdown() throws Exception {
    Notebook notebook = createLegacyRawNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] over = filledBytes(LIMIT + 1, (byte) 0x64);
    AttachmentRange range =
        twoStepRange(
            acceptedBundleBytes(notebook),
            ObjectId.fromString(empty.getAcceptedGitObjectId()),
            List.of(new NotebookGitProposalFile("hidden.bin", over)),
            List.of(new NotebookGitProposalFile("hidden.md", NOTE_MARKDOWN)),
            over);

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            empty.getAcceptedGitObjectId(),
            range.proposalBytes(),
            HttpStatus.BAD_REQUEST);

    assertOversizedRefusal(exception, "hidden.bin", LIMIT + 1);
    assertThat(nativeObjectPresent(empty.getId(), range.overBlobId()), is(false));
  }

  @Test
  void acceptsWithinLimitMultiVersionHistoryPreservingEarlierAttachmentBytes() throws Exception {
    Notebook notebook = createLegacyRawNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty, List.of(new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN))));
    NotebookGitBinding accepted = reloadCommittedBinding(notebook.getId());
    ObjectId acceptedHead = ObjectId.fromString(accepted.getAcceptedGitObjectId());
    ValidVersionsRange range =
        validReplaceThenDeleteRange(acceptedBundleBytes(notebook), acceptedHead);

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), accepted.getAcceptedGitObjectId(), range.proposalBytes());

    assertThat(publishedHead, equalTo(range.tip().getName()));
    assertThat(
        acceptedHistory(notebook).exactTree(),
        contains(PortableTreeEntry.ofText("Root Note.md", NOTE_MARKDOWN)));
    byte[] downloaded = controller.downloadNotebookGitBundle(notebook).getBody();
    try (InMemoryRepository acceptedRepo = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk walk = new RevWalk(acceptedRepo)) {
      RevCommit tip = walk.parseCommit(GitBundleTestReader.fetchHead(acceptedRepo, downloaded));
      assertThat(tip.getId(), equalTo(range.tip()));
      assertThat(tip.getParentCount(), equalTo(1));
      RevCommit afterSecond = walk.parseCommit(tip.getParent(0));
      assertThat(afterSecond.getParent(0), equalTo(range.afterFirst()));
      assertThat(
          GitBundleTestReader.blobIdAt(acceptedRepo, range.afterFirst(), "version.bin"),
          equalTo(blobIdOf(FIRST_SMALL)));
      assertThat(
          GitBundleTestReader.blobIdAt(acceptedRepo, afterSecond.getId(), "version.bin"),
          equalTo(blobIdOf(SECOND_SMALL)));
      RevCommit afterFirst = walk.parseCommit(range.afterFirst());
      assertThat(afterFirst.getParent(0), equalTo(acceptedHead));
    }
  }

  @Test
  void acceptsTwoWithinLimitAttachmentsWhoseCombinedSizeExceedsLimit() throws Exception {
    Notebook notebook = createLegacyRawNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] other = filledBytes(LIMIT / 2 + 1, (byte) 0x54);

    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty,
            List.of(
                new NotebookGitProposalFile("a.bin", HALF_PLUS),
                new NotebookGitProposalFile("b.bin", other))));

    assertThat(
        acceptedHistory(notebook).exactTree(),
        contains(new PortableTreeEntry("a.bin", HALF_PLUS), new PortableTreeEntry("b.bin", other)));
  }

  private AttachmentRange twoStepRange(
      byte[] baseBundleBytes,
      ObjectId baseHead,
      List<NotebookGitProposalFile> middleFiles,
      List<NotebookGitProposalFile> tipFiles,
      byte[] over)
      throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, baseBundleBytes);
      ObjectId middle =
          localCommitOnTopOf(repository, baseHead, middleFiles, "Add oversized attachment");
      ObjectId tip = localCommitOnTopOf(repository, middle, tipFiles, "Tip after intermediate");
      return new AttachmentRange(tip, bundleBytesForHead(repository, tip), blobIdOf(over));
    }
  }

  private ValidVersionsRange validReplaceThenDeleteRange(byte[] baseBundleBytes, ObjectId baseHead)
      throws Exception {
    NotebookGitProposalFile note = new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN);
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, baseBundleBytes);
      ObjectId afterFirst =
          localCommitOnTopOf(
              repository,
              baseHead,
              List.of(note, new NotebookGitProposalFile("version.bin", FIRST_SMALL)),
              "First version");
      ObjectId afterSecond =
          localCommitOnTopOf(
              repository,
              afterFirst,
              List.of(note, new NotebookGitProposalFile("version.bin", SECOND_SMALL)),
              "Second version");
      ObjectId tip =
          localCommitOnTopOf(repository, afterSecond, List.of(note), "Delete attachment");
      return new ValidVersionsRange(afterFirst, tip, bundleBytesForHead(repository, tip));
    }
  }
}

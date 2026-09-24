package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Notebook;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * Multi-commit LFS proposal fixtures and first-parent preservation checks for attachment-size
 * history admission.
 */
abstract class NotebookGitAttachmentSizeAdmissionLfsHistoryTestSupport
    extends NotebookGitAttachmentLfsPublicationTestSupport {

  record LfsHistoryRange(ObjectId afterFirst, ObjectId tip, byte[] proposalBytes) {}

  LfsHistoryRange lfsReplaceThenKeepTipRange(
      byte[] baseBundleBytes, ObjectId baseHead, byte[] firstPointer, byte[] tipPointer)
      throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, baseBundleBytes);
      ObjectId afterFirst =
          commitOnTopOf(
              repository,
              List.of(baseHead),
              withAcceptedMetadata(
                  repository,
                  baseHead,
                  List.of(new NotebookGitProposalFile("version.bin", firstPointer))),
              "First LFS version");
      ObjectId tip =
          commitOnTopOf(
              repository,
              List.of(afterFirst),
              withAcceptedMetadata(
                  repository,
                  afterFirst,
                  List.of(new NotebookGitProposalFile("version.bin", tipPointer))),
              "Corrected LFS tip");
      return new LfsHistoryRange(afterFirst, tip, bundleBytesForHead(repository, tip));
    }
  }

  LfsHistoryRange lfsIntroduceThenRemoveKeepingNote(
      byte[] baseBundleBytes, ObjectId baseHead, byte[] temporaryPointer) throws Exception {
    NotebookGitProposalFile note = new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN);
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, baseBundleBytes);
      ObjectId afterFirst =
          commitOnTopOf(
              repository,
              List.of(baseHead),
              withAcceptedMetadata(
                  repository,
                  baseHead,
                  List.of(note, new NotebookGitProposalFile("temporary.bin", temporaryPointer))),
              "Temporary LFS attachment");
      ObjectId tip =
          commitOnTopOf(
              repository,
              List.of(afterFirst),
              withAcceptedMetadata(repository, afterFirst, List.of(note)),
              "Remove temporary attachment");
      return new LfsHistoryRange(afterFirst, tip, bundleBytesForHead(repository, tip));
    }
  }

  void assertPreservedFirstParentChain(
      Notebook notebook, ObjectId acceptedHead, ObjectId afterFirst, ObjectId tip)
      throws Exception {
    byte[] downloaded = controller.downloadNotebookGitBundle(notebook).getBody();
    try (InMemoryRepository acceptedRepo = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk walk = new RevWalk(acceptedRepo)) {
      RevCommit publishedTip =
          walk.parseCommit(GitBundleTestReader.fetchHead(acceptedRepo, downloaded));
      assertThat(publishedTip.getId(), equalTo(tip));
      assertThat(publishedTip.getParentCount(), equalTo(1));
      assertThat(publishedTip.getParent(0), equalTo(afterFirst));
      RevCommit middle = walk.parseCommit(afterFirst);
      assertThat(middle.getParent(0), equalTo(acceptedHead));
    }
  }
}

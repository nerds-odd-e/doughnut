package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitAttachmentSizeAdmissionControllerTest
    extends NotebookGitAttachmentSizeAdmissionTestSupport {

  private static Stream<String> attachmentPaths() {
    return Stream.of("payload.bin", "tools/cache/payload.bin");
  }

  @ParameterizedTest
  @MethodSource("attachmentPaths")
  void exactLimitTipAttachmentIsAcceptedAtRootOrNestedPath(String path) throws Exception {
    Notebook notebook = createLegacyRawNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] exact = filledBytes(LIMIT, (byte) 0x41);

    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(empty, List.of(new NotebookGitProposalFile(path, exact))));

    assertThat(acceptedHistory(notebook).exactTree(), contains(new PortableTreeEntry(path, exact)));
  }

  @ParameterizedTest
  @MethodSource("attachmentPaths")
  void overLimitTipAttachmentIsRefusedWithPathSizeAndLimit(String path) throws Exception {
    Notebook notebook = createLegacyRawNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] over = filledBytes(LIMIT + 1, (byte) 0x42);

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            empty.getAcceptedGitObjectId(),
            proposalBundleBytes(empty, List.of(new NotebookGitProposalFile(path, over))),
            HttpStatus.BAD_REQUEST);

    assertOversizedRefusal(exception, path, LIMIT + 1);
  }

  @Test
  void mixedOversizedRefusalLeavesAcceptedStateLearningAndObjectsUnchanged() throws Exception {
    Notebook notebook = createLegacyRawNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty,
            List.of(
                new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN),
                new NotebookGitProposalFile("reference.json", SMALL_JSON))));
    NotebookGitBinding accepted = reloadCommittedBinding(notebook.getId());
    Note note =
        noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .filter(n -> n.getTitle().equals("Root Note"))
            .findFirst()
            .orElseThrow();
    MemoryTracker tracker = learnedTracker(note, 4.5f, 2);
    List<PortableTreeEntry> attachmentsBefore = committedRootAttachments(notebook);
    long objectRowsBefore = countNativeObjectStoreRows(accepted.getId());
    byte[] over = filledBytes(LIMIT + 1, (byte) 0x43);
    ObjectId overBlobId = blobIdOf(over);

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            accepted.getAcceptedGitObjectId(),
            proposalBundleBytes(
                accepted,
                List.of(
                    new NotebookGitProposalFile("Root Note.md", EDITED_CONTENT),
                    new NotebookGitProposalFile("reference.json", SMALL_JSON),
                    new NotebookGitProposalFile("huge.bin", over))),
            HttpStatus.BAD_REQUEST);

    assertOversizedRefusal(exception, "huge.bin", LIMIT + 1);
    assertShownContentAndRetainedLearning(note, tracker, NOTE_MARKDOWN);
    assertThat(committedRootAttachments(notebook), equalTo(attachmentsBefore));
    assertThat(countNativeObjectStoreRows(accepted.getId()), is(objectRowsBefore));
    assertThat(nativeObjectPresent(accepted.getId(), overBlobId), is(false));
  }

  @Test
  void grandfatheredOversizedBytesRemainReusableAcrossKeepRenameAndHistoricalRestoration()
      throws Exception {
    byte[] oversized = filledBytes(LIMIT + 1, (byte) 0x44);
    Notebook notebook = createLegacyRawNotebook();
    makeMe.aNote("Root Note").notebook(notebook).content(NOTE_MARKDOWN).please();
    storeAcceptedAttachmentAndSnapshot(notebook, null, "legacy.bin", oversized);
    NotebookGitBinding withLegacy = reloadCommittedBinding(notebook.getId());

    controller.publishNotebookGitProposal(
        notebook.getId(),
        withLegacy.getAcceptedGitObjectId(),
        proposalBundleBytes(
            withLegacy,
            List.of(
                new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN),
                new NotebookGitProposalFile("legacy.bin", oversized))));
    NotebookGitBinding kept = reloadCommittedBinding(notebook.getId());
    assertThat(
        acceptedHistory(notebook).exactTree(),
        contains(
            PortableTreeEntry.ofText("Root Note.md", NOTE_MARKDOWN),
            new PortableTreeEntry("legacy.bin", oversized)));

    controller.publishNotebookGitProposal(
        notebook.getId(),
        kept.getAcceptedGitObjectId(),
        proposalBundleBytes(
            kept,
            List.of(
                new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN),
                new NotebookGitProposalFile("renamed.bin", oversized))));
    NotebookGitBinding renamed = reloadCommittedBinding(notebook.getId());
    assertThat(
        acceptedHistory(notebook).exactTree(),
        contains(
            PortableTreeEntry.ofText("Root Note.md", NOTE_MARKDOWN),
            new PortableTreeEntry("renamed.bin", oversized)));

    controller.publishNotebookGitProposal(
        notebook.getId(),
        renamed.getAcceptedGitObjectId(),
        proposalBundleBytes(
            renamed, List.of(new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN))));
    NotebookGitBinding withoutAttachment = reloadCommittedBinding(notebook.getId());

    controller.publishNotebookGitProposal(
        notebook.getId(),
        withoutAttachment.getAcceptedGitObjectId(),
        proposalBundleBytes(
            withoutAttachment,
            List.of(
                new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN),
                new NotebookGitProposalFile("restored.bin", oversized))));

    assertThat(
        acceptedHistory(notebook).exactTree(),
        contains(
            PortableTreeEntry.ofText("Root Note.md", NOTE_MARKDOWN),
            new PortableTreeEntry("restored.bin", oversized)));
  }

  @Test
  void webNoteSavePreservesGrandfatheredOversizedAttachmentBytes() throws Exception {
    byte[] oversized = filledBytes(LIMIT + 1, (byte) 0x45);
    Notebook notebook = createLegacyRawNotebook();
    Note note = makeMe.aNote("Root Note").notebook(notebook).content(NOTE_MARKDOWN).please();
    storeAcceptedAttachmentAndSnapshot(notebook, null, "legacy.bin", oversized);

    textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT));

    assertThat(
        acceptedHistory(notebook).exactTree(),
        contains(
            PortableTreeEntry.ofText("Root Note.md", EDITED_CONTENT),
            new PortableTreeEntry("legacy.bin", oversized)));
  }

  @Test
  void oversizedBytesAcceptedInAnotherNotebookAreNotExemptHere() throws Exception {
    byte[] oversized = filledBytes(LIMIT + 1, (byte) 0x46);
    Notebook other = createLegacyRawNotebook("Other Notebook");
    storeAcceptedAttachmentAndSnapshot(other, null, "foreign.bin", oversized);

    Notebook notebook = createLegacyRawNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            empty.getAcceptedGitObjectId(),
            proposalBundleBytes(
                empty, List.of(new NotebookGitProposalFile("foreign.bin", oversized))),
            HttpStatus.BAD_REQUEST);

    assertOversizedRefusal(exception, "foreign.bin", LIMIT + 1);
  }

  @Test
  void differentOversizedBytesAreRefusedEvenWhenGrandfatheredBytesExist() throws Exception {
    byte[] grandfathered = filledBytes(LIMIT + 1, (byte) 0x47);
    byte[] different = filledBytes(LIMIT + 1, (byte) 0x48);
    Notebook notebook = createLegacyRawNotebook();
    makeMe.aNote("Root Note").notebook(notebook).content(NOTE_MARKDOWN).please();
    storeAcceptedAttachmentAndSnapshot(notebook, null, "legacy.bin", grandfathered);
    NotebookGitBinding accepted = reloadCommittedBinding(notebook.getId());

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            accepted.getAcceptedGitObjectId(),
            proposalBundleBytes(
                accepted,
                List.of(
                    new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN),
                    new NotebookGitProposalFile("legacy.bin", grandfathered),
                    new NotebookGitProposalFile("other.bin", different))),
            HttpStatus.BAD_REQUEST);

    assertOversizedRefusal(exception, "other.bin", LIMIT + 1);
    assertThat(
        acceptedHistory(notebook).exactTree(),
        contains(
            PortableTreeEntry.ofText("Root Note.md", NOTE_MARKDOWN),
            new PortableTreeEntry("legacy.bin", grandfathered)));
  }
}

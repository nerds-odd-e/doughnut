package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookAttachment.InMemoryNotebookAttachmentContent;
import com.odde.donut.services.notebookGit.SqlStatementCallLog;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.util.HashSet;
import java.util.List;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** A web save's server work depends on what changed, not on the notebook's size. */
class NotebookGitWebContentSaveCostControllerTest extends NotebookGitWebContentSaveCostTestSupport {

  @Test
  void savingContentInALargeNotebookWithAttachmentsDoesNotQueryAttachmentsOrPortableTreeRows()
      throws Throwable {
    Notebook large = createGitBackedNotebook("Large");
    Note note = makeMe.aNote().notebook(large).content(ACCEPTED_CONTENT).please();
    for (int i = 0; i < 30; i++) {
      makeMe.aNote().notebook(large).title("Unrelated " + i).please();
    }
    for (int i = 0; i < 3; i++) {
      storeFolderAttachmentAndSnapshot(large, null, "attachment-" + i + ".bin", new byte[4096]);
    }
    var acceptedBefore = acceptedHistory(large);

    Statistics contentSave =
        hibernateStatisticsOf(
            () -> textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT)));

    var queries = List.of(contentSave.getQueries());
    assertThat(queries, not(hasItem(containsString("NotebookAttachment"))));
    assertThat(queries, not(hasItem(containsString("PortableTreeNoteRow"))));
    AcceptedHistory after = acceptedHistory(large);
    assertThat(after.parents(), equalTo(acceptedBefore.commits()));
  }

  /**
   * Observes JDBC executions for one content save on a nested accepted tree. Counts are JDBC
   * execute* calls, not wire round trips; tree fetches stay within one whole-tree walk for this
   * fixture.
   */
  @Test
  void noteOnlySaveOnLfsNotebookDoesNotReadPayloadsOrRewriteObjects() throws Throwable {
    Notebook notebook = createGitBackedNotebook("Lfs Save Cost");
    NotebookGitBinding binding = reloadCommittedBinding(notebook.getId());
    binding.setAttachmentRepresentation(NotebookGitAttachmentRepresentation.LFS);
    notebookGitBindingRepository.save(binding);
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] payload = new byte[4096];
    for (int i = 0; i < payload.length; i++) {
      payload[i] = (byte) i;
    }
    byte[] pointer = pointerFor(notebook, payload);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty,
            List.of(
                new NotebookGitProposalFile("Root Note.md", ACCEPTED_CONTENT),
                new NotebookGitProposalFile("diagram.png", pointer))));
    Note note =
        noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .filter(n -> n.getTitle().equals("Root Note"))
            .findFirst()
            .orElseThrow();
    InMemoryNotebookAttachmentContent memory =
        (InMemoryNotebookAttachmentContent) notebookAttachmentContent;
    memory.resetAccessCounts();
    AcceptedHistory acceptedBefore = acceptedHistory(notebook);

    Statistics contentSave =
        hibernateStatisticsOf(
            () -> textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT)));

    var queries = List.of(contentSave.getQueries());
    assertThat(queries, not(hasItem(containsString("NotebookAttachment"))));
    assertThat(memory.getCalls(), equalTo(0L));
    assertThat(memory.storeCalls(), equalTo(0L));
    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(acceptedBefore.commits()));
    assertThat(
        after.exactTree().stream()
            .filter(entry -> entry.path().equals("diagram.png"))
            .map(entry -> entry.content())
            .findFirst()
            .orElseThrow(),
        equalTo(pointer));
  }

  @Test
  void contentSaveJdbcObjectFetchesAreScopedToTheControllerCall() throws Throwable {
    Notebook notebook = createGitBackedNotebook("Jdbc Cost");
    Folder parent = buildDepthPath(notebook, 6);
    Note note = makeMe.aNote().folder(parent).content(ACCEPTED_CONTENT).please();
    for (int i = 0; i < 4; i++) {
      makeMe.aFolder().notebook(notebook).name("Sibling" + i).please();
    }
    snapshotCurrentPortableTree(notebook);
    AcceptedHistory acceptedBefore = acceptedHistory(notebook);

    SqlStatementCallLog callLog = new SqlStatementCallLog();
    long started = System.nanoTime();
    try (AutoCloseable ignored = callLog.activate()) {
      textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT));
    }
    long elapsedMs = (System.nanoTime() - started) / 1_000_000L;

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(acceptedBefore.commits()));
    SaveCostObservation observation = observationFrom(callLog, notebook);
    System.out.printf(
        "content-save JDBC observation revision-local fixture=depth-6-plus-4-sibling-folders"
            + " elapsedMs=%d objectFetches=%d treeFetches=%d commitFetches=%d"
            + " objectInsertExecutions=%d bindingUpdateExecutions=%d jdbcExecutions=%d"
            + " attemptedObjectIds=%d objectInsertRows=%d fetchedObjectBytes=%d%n",
        elapsedMs,
        observation.objectFetches(),
        observation.treeFetches(),
        observation.commitFetches(),
        observation.objectInsertExecutions(),
        observation.bindingUpdateExecutions(),
        observation.jdbcExecutions(),
        observation.attemptedObjectIds(),
        observation.objectInsertRows(),
        observation.fetchedObjectBytes());
    assertThat(observation.objectFetches(), greaterThan(0L));
    assertThat(observation.treeFetches(), greaterThan(0L));
    assertThat(observation.objectInsertExecutions(), greaterThan(0L));
    assertThat(observation.treeFetches(), lessThanOrEqualTo(12L));
  }

  @Test
  void contentSaveTreeFetchesStayBoundedWhenUnrelatedFoldersGrowFromDozensToThousands()
      throws Exception {
    SaveCostObservation shallow = measureContentSave(12, 40, 0, 0, "cost-depth12-unrelated-40");
    SaveCostObservation deep = measureContentSave(12, 3000, 0, 0, "cost-depth12-unrelated-3000");

    assertThat(shallow.treeFetches(), equalTo(deep.treeFetches()));
    assertThat(shallow.treeFetches(), lessThanOrEqualTo(13L));
    assertThat(deep.existenceCheckExecutions(), equalTo(1L));
    assertThat(deep.objectInsertExecutions(), equalTo(1L));
    assertThat(deep.objectInsertRows(), lessThanOrEqualTo(15));
    assertThat(deep.fetchedTreeIds(), hasSize((int) deep.treeFetches()));
    assertThat(
        "editor opens each ancestor tree once",
        deep.fetchedTreeIds().size(),
        equalTo(new HashSet<>(deep.fetchedTreeIds()).size()));
    assertAcceptedTreeMatchesTheFullAssembly(deep.notebook());
  }

  @Test
  void contentSaveTreeFetchesScaleWithEditedPathDepthNotUnrelatedFolderCount() throws Exception {
    SaveCostObservation depth1 = measureContentSave(1, 40, 0, 0, "cost-depth1-unrelated-40");
    SaveCostObservation depth12 = measureContentSave(12, 40, 0, 0, "cost-depth12-vs-depth1");

    assertThat(depth1.treeFetches(), lessThanOrEqualTo(2L));
    assertThat(depth12.treeFetches(), lessThanOrEqualTo(13L));
    assertThat(depth12.treeFetches(), greaterThan(depth1.treeFetches()));
    assertThat(depth1.existenceCheckExecutions(), equalTo(1L));
    assertThat(depth12.existenceCheckExecutions(), equalTo(1L));
  }

  @Test
  void contentSaveTreeFetchCountIsStableWhenAncestorDirectoryWidthGrows() throws Exception {
    SaveCostObservation narrow = measureContentSave(12, 40, 0, 0, "cost-depth12-width-0");
    SaveCostObservation wide = measureContentSave(12, 40, 20, 0, "cost-depth12-width-20");

    assertThat(narrow.treeFetches(), equalTo(wide.treeFetches()));
    assertThat(wide.treeFetches(), lessThanOrEqualTo(13L));
    assertThat(wide.fetchedObjectBytes(), greaterThan(narrow.fetchedObjectBytes()));
    assertThat(wide.objectInsertRows(), lessThanOrEqualTo(15));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 12})
  void contentSaveDoesNotRediscoverTheAppendedHead(int depth) throws Exception {
    SaveCostObservation saved = measureContentSave(depth, 40, 0, 0, "accepted-head-depth" + depth);

    assertThat(saved.postAppendRefReadExecutions(), equalTo(0L));
    assertThat(saved.refReadExecutions(), equalTo(1L));
  }

  @Test
  void unchangedContentSaveDoesNotInsertGitObjectsOrAdvanceTheRef() throws Exception {
    Notebook notebook = createGitBackedNotebook("Unchanged Save");
    Note note = makeMe.aNote().notebook(notebook).content(EDITED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);
    AcceptedHistory before = acceptedHistory(notebook);
    Integer noteId = note.getId();

    Note reloaded =
        inCommittedTransaction(
            transactionManager,
            () -> {
              entityManager.clear();
              return entityManager.find(Note.class, noteId);
            });

    SqlStatementCallLog callLog = new SqlStatementCallLog();
    try (AutoCloseable ignored = callLog.activate()) {
      textContentController.updateNoteContent(reloaded, contentDto(EDITED_CONTENT));
    }

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.commits(), equalTo(before.commits()));
    assertThat(
        callLog.countExecutionsMatching("INSERT INTO notebook_git_accepted_object"), equalTo(0L));
    assertThat(
        callLog.countExecutionsMatching("UPDATE notebook_git_binding", "accepted_git_object_id"),
        equalTo(0L));
  }
}

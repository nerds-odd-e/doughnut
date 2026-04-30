package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDataMigrationService {

  private enum BatchPhase {
    TOPOLOGY,
    SLUG_PREP,
    SLUG_NOTEBOOKS
  }

  private static final class MigrationSession {
    BatchPhase phase = BatchPhase.TOPOLOGY;

    /** Each row: [notebook_id, index_note_id]. */
    List<int[]> topologyPairs = List.of();

    List<Integer> slugNotebookIds = List.of();

    int topoIndex;
    int slugIndex;

    int detachedChildFoldersFromIndexFolder;
    int updatedNormalNotesDetachedFromIndex;
    int updatedRelationNotesClearedFolder;
    int deletedObsoleteNotebookNameRootFolders;

    int batchesExecuted;

    int batchTotalPlanned;

    MigrationSession shallowCopy() {
      MigrationSession copy = new MigrationSession();
      copy.phase = phase;
      copy.topologyPairs = topologyPairs;
      copy.slugNotebookIds = slugNotebookIds;
      copy.topoIndex = topoIndex;
      copy.slugIndex = slugIndex;
      copy.detachedChildFoldersFromIndexFolder = detachedChildFoldersFromIndexFolder;
      copy.updatedNormalNotesDetachedFromIndex = updatedNormalNotesDetachedFromIndex;
      copy.updatedRelationNotesClearedFolder = updatedRelationNotesClearedFolder;
      copy.deletedObsoleteNotebookNameRootFolders = deletedObsoleteNotebookNameRootFolders;
      copy.batchesExecuted = batchesExecuted;
      copy.batchTotalPlanned = batchTotalPlanned;
      return copy;
    }
  }

  private final JdbcTemplate jdbcTemplate;
  private final AdminDataMigrationBatchExecutor batchExecutor;

  private volatile AdminDataMigrationStatusDTO lastSuccessfulStatus = freshIdleStatusDto();

  private MigrationSession activeSession;

  public AdminDataMigrationService(
      JdbcTemplate jdbcTemplate, AdminDataMigrationBatchExecutor batchExecutor) {
    this.jdbcTemplate = jdbcTemplate;
    this.batchExecutor = batchExecutor;
  }

  public synchronized AdminDataMigrationStatusDTO getStatus() {
    if (activeSession == null) {
      return duplicate(lastSuccessfulStatus);
    }
    return runningDtoFromSession(
        activeSession.shallowCopy(), lastSuccessfulStatus.isCompletedOnce());
  }

  @Transactional
  public synchronized AdminDataMigrationStatusDTO runBatch() {
    if (activeSession == null) {
      activeSession = startNewMigrationSession();
    }

    MigrationSession session = activeSession;
    executeExactlyOneTransactionalBatch(session);

    if (!migrationFullyDone(session)) {
      return runningDtoFromSession(session.shallowCopy(), lastSuccessfulStatus.isCompletedOnce());
    }

    AdminDataMigrationStatusDTO done = finalDtoFromFinishedSession(session);
    lastSuccessfulStatus = duplicate(done);
    activeSession = null;
    return duplicate(done);
  }

  private MigrationSession startNewMigrationSession() {
    List<int[]> topologyPairs =
        jdbcTemplate.query(
            """
            SELECT nh.notebook_id, nh.head_note_id
            FROM notebook_head_note nh
            INNER JOIN notebook n ON n.id = nh.notebook_id
            ORDER BY nh.notebook_id ASC
            """,
            (rs, rowNum) -> new int[] {rs.getInt("notebook_id"), rs.getInt("head_note_id")});

    List<Integer> slugNotebookIds =
        new ArrayList<>(
            jdbcTemplate.queryForList("SELECT id FROM notebook ORDER BY id ASC", Integer.class));

    MigrationSession s = new MigrationSession();
    s.topologyPairs = topologyPairs;
    s.slugNotebookIds = slugNotebookIds;
    s.phase = BatchPhase.TOPOLOGY;
    s.topoIndex = 0;
    s.slugIndex = 0;
    s.batchTotalPlanned = topologyPairs.size() + 1 + slugNotebookIds.size();
    s.batchesExecuted = 0;
    return s;
  }

  private void executeExactlyOneTransactionalBatch(MigrationSession s) {
    foldEmptyTopologyIntoSlugPrepWhenNeeded(s);

    if (s.phase == BatchPhase.TOPOLOGY) {
      int[] pair = s.topologyPairs.get(s.topoIndex);
      AdminDataMigrationBatchExecutor.TopologyBatchTotals totals =
          batchExecutor.detachIndexTopologyForNotebook(pair[0], pair[1]);
      s.detachedChildFoldersFromIndexFolder += totals.detachedChildFolders();
      s.updatedNormalNotesDetachedFromIndex += totals.normalNotes();
      s.updatedRelationNotesClearedFolder += totals.relationNotes();
      s.deletedObsoleteNotebookNameRootFolders += totals.deletedRoots();
      s.topoIndex++;
      s.batchesExecuted++;
      return;
    }

    if (s.phase == BatchPhase.SLUG_PREP) {
      batchExecutor.installSlugPrepPlaceholdersGlobally();
      s.phase = BatchPhase.SLUG_NOTEBOOKS;
      s.slugIndex = 0;
      s.batchesExecuted++;
      return;
    }

    if (s.slugIndex >= s.slugNotebookIds.size()) {
      return;
    }

    batchExecutor.regenerateSlugPathsForNotebook(s.slugNotebookIds.get(s.slugIndex));
    s.slugIndex++;
    s.batchesExecuted++;
  }

  private static void foldEmptyTopologyIntoSlugPrepWhenNeeded(MigrationSession s) {
    if (s.phase == BatchPhase.TOPOLOGY && s.topoIndex >= s.topologyPairs.size()) {
      s.phase = BatchPhase.SLUG_PREP;
    }
  }

  private static boolean migrationFullyDone(MigrationSession s) {
    return s.phase == BatchPhase.SLUG_NOTEBOOKS && s.slugIndex >= s.slugNotebookIds.size();
  }

  private AdminDataMigrationStatusDTO runningDtoFromSession(
      MigrationSession s, boolean hadCompletedPreviously) {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    copyAggregatesFromSessionInto(s, dto);
    dto.setNotebookCountSlugScan(0);

    dto.setMigrationInProgress(true);
    dto.setMoreBatchesRemain(true);

    dto.setCompletedOnce(hadCompletedPreviously);
    dto.setLastCompletedAt(null);

    dto.setCompletedBatchOrdinal(s.batchesExecuted);
    dto.setBatchTotalPlanned(s.batchTotalPlanned);
    dto.setBatchPhaseSummary(phaseSummaryLabel(s));

    dto.setMessage(runningCountersLine(dto));
    return dto;
  }

  private AdminDataMigrationStatusDTO finalDtoFromFinishedSession(MigrationSession s) {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    copyAggregatesFromSessionInto(s, dto);

    long notebookDistinct =
        jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT id) FROM notebook", Long.class);
    dto.setNotebookCountSlugScan(notebookDistinct);

    dto.setMigrationInProgress(false);
    dto.setMoreBatchesRemain(false);

    dto.setCompletedOnce(true);
    dto.setLastCompletedAt(Instant.now());

    dto.setCompletedBatchOrdinal(s.batchesExecuted);
    dto.setBatchTotalPlanned(s.batchTotalPlanned);
    dto.setBatchPhaseSummary("Done");

    dto.setMessage(summaryMessage(dto));
    return dto;
  }

  private static void copyAggregatesFromSessionInto(
      MigrationSession s, AdminDataMigrationStatusDTO dto) {
    dto.setDetachedChildFoldersFromIndexFolder(s.detachedChildFoldersFromIndexFolder);
    dto.setUpdatedNormalNotesDetachedFromIndex(s.updatedNormalNotesDetachedFromIndex);
    dto.setUpdatedRelationNotesClearedFolder(s.updatedRelationNotesClearedFolder);
    dto.setDeletedObsoleteNotebookNameRootFolders(s.deletedObsoleteNotebookNameRootFolders);
  }

  private static String phaseSummaryLabel(MigrationSession s) {
    if (s.phase == BatchPhase.TOPOLOGY) {
      int n = Math.max(1, s.topologyPairs.size());
      int at = Math.min(s.topoIndex, s.topologyPairs.size());
      return "Topology batch " + at + "/" + n;
    }
    if (s.phase == BatchPhase.SLUG_PREP) {
      return "Slug preparation (globally assigns placeholders)";
    }
    int totalNb = Math.max(1, s.slugNotebookIds.size());
    int atNb = Math.min(s.slugIndex, s.slugNotebookIds.size());
    return "Slug notebooks " + atNb + "/" + totalNb;
  }

  private static String runningCountersLine(AdminDataMigrationStatusDTO dto) {
    return summaryMessageBody(dto)
        + "; ("
        + dto.getCompletedBatchOrdinal()
        + "/"
        + dto.getBatchTotalPlanned()
        + " batches completed).";
  }

  private static String summaryMessage(AdminDataMigrationStatusDTO dto) {
    return summaryMessageBody(dto)
        + "; slug regen scanned "
        + dto.getNotebookCountSlugScan()
        + " notebooks.";
  }

  private static String summaryMessageBody(AdminDataMigrationStatusDTO dto) {
    return "Detached child folders "
        + dto.getDetachedChildFoldersFromIndexFolder()
        + "; moved normal notes "
        + dto.getUpdatedNormalNotesDetachedFromIndex()
        + "; cleared relation note folders "
        + dto.getUpdatedRelationNotesClearedFolder()
        + "; deleted obsolete notebook-name root folders "
        + dto.getDeletedObsoleteNotebookNameRootFolders();
  }

  private static AdminDataMigrationStatusDTO duplicate(AdminDataMigrationStatusDTO src) {
    AdminDataMigrationStatusDTO d = new AdminDataMigrationStatusDTO();
    d.setCompletedOnce(src.isCompletedOnce());
    d.setLastCompletedAt(src.getLastCompletedAt());
    d.setMessage(src.getMessage());
    d.setDetachedChildFoldersFromIndexFolder(src.getDetachedChildFoldersFromIndexFolder());
    d.setUpdatedNormalNotesDetachedFromIndex(src.getUpdatedNormalNotesDetachedFromIndex());
    d.setUpdatedRelationNotesClearedFolder(src.getUpdatedRelationNotesClearedFolder());
    d.setDeletedObsoleteNotebookNameRootFolders(src.getDeletedObsoleteNotebookNameRootFolders());
    d.setNotebookCountSlugScan(src.getNotebookCountSlugScan());
    d.setMigrationInProgress(src.isMigrationInProgress());
    d.setMoreBatchesRemain(src.isMoreBatchesRemain());
    d.setCompletedBatchOrdinal(src.getCompletedBatchOrdinal());
    d.setBatchTotalPlanned(src.getBatchTotalPlanned());
    d.setBatchPhaseSummary(src.getBatchPhaseSummary());
    return d;
  }

  private static AdminDataMigrationStatusDTO freshIdleStatusDto() {
    AdminDataMigrationStatusDTO d = new AdminDataMigrationStatusDTO();
    d.setMessage("No migration has completed in this server process yet.");
    d.setCompletedOnce(false);
    d.setLastCompletedAt(null);
    d.setMigrationInProgress(false);
    d.setMoreBatchesRemain(false);
    d.setCompletedBatchOrdinal(0);
    d.setBatchTotalPlanned(0);
    d.setBatchPhaseSummary("");
    return d;
  }
}

package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDataMigrationService {

  private static final byte CHECKPOINT_ID = 1;

  private enum BatchPhase {
    TOPOLOGY,
    SLUG_PREP,
    SLUG_NOTEBOOKS
  }

  private static final RowMapper<Checkpoint> CHECKPOINT_MAPPER =
      new RowMapper<>() {
        @Override
        public Checkpoint mapRow(ResultSet rs, int rowNum) throws SQLException {
          Checkpoint c = new Checkpoint();
          c.phase = BatchPhase.valueOf(rs.getString("phase"));
          c.topoPairsDone = rs.getInt("topo_pairs_done");
          c.topologyPairTotal = rs.getInt("topology_pair_total");
          c.slugPrepDone = rs.getBoolean("slug_prep_done");
          c.detachedChildFoldersFromIndexFolder = rs.getInt("detached_child_folders");
          c.updatedNormalNotesDetachedFromIndex = rs.getInt("updated_normal_notes");
          c.updatedRelationNotesClearedFolder = rs.getInt("updated_relation_notes");
          c.deletedObsoleteNotebookNameRootFolders = rs.getInt("deleted_obsolete_roots");
          c.batchesCompleted = rs.getInt("batches_completed");
          c.batchTotalPlanned = rs.getInt("batch_total_planned");
          return c;
        }
      };

  private final JdbcTemplate jdbcTemplate;
  private final AdminDataMigrationBatchExecutor batchExecutor;

  private volatile AdminDataMigrationStatusDTO lastSuccessfulStatus = freshIdleStatusDto();

  public AdminDataMigrationService(
      JdbcTemplate jdbcTemplate, AdminDataMigrationBatchExecutor batchExecutor) {
    this.jdbcTemplate = jdbcTemplate;
    this.batchExecutor = batchExecutor;
  }

  public synchronized AdminDataMigrationStatusDTO getStatus() {
    Optional<Checkpoint> ck = fetchCheckpointSkipLock();
    if (ck.isEmpty()) {
      return duplicate(lastSuccessfulStatus);
    }
    return runningDtoFromCheckpoint(ck.get(), lastSuccessfulStatus.isCompletedOnce());
  }

  @Transactional
  public synchronized AdminDataMigrationStatusDTO runBatch() {
    List<Checkpoint> locked =
        jdbcTemplate.query(
            """
            SELECT id, phase, topo_pairs_done, topology_pair_total, slug_prep_done,
                   detached_child_folders, updated_normal_notes, updated_relation_notes,
                   deleted_obsolete_roots, batches_completed, batch_total_planned
            FROM wiki_data_migration_checkpoint
            WHERE id = ?
            FOR UPDATE
            """,
            CHECKPOINT_MAPPER,
            CHECKPOINT_ID);

    Checkpoint checkpoint;
    if (locked.isEmpty()) {
      clearSlugNotebookDoneTable();
      checkpoint = insertFreshCheckpointLocked();
    } else {
      checkpoint = locked.getFirst();
    }

    executeExactlyOneTransactionalBatch(checkpoint);

    boolean done = migrationFullyDoneAfterMutation(checkpoint);
    AdminDataMigrationStatusDTO dto;

    if (done) {
      clearPersistedProgress();
      dto = finalDtoFromCheckpoint(checkpoint);
      lastSuccessfulStatus = duplicate(dto);
    } else {
      persistCheckpoint(checkpoint);
      dto = runningDtoFromCheckpoint(checkpoint, lastSuccessfulStatus.isCompletedOnce());
    }

    return duplicate(dto);
  }

  private static final class Checkpoint {
    BatchPhase phase;
    int topoPairsDone;
    int topologyPairTotal;
    boolean slugPrepDone;

    int detachedChildFoldersFromIndexFolder;
    int updatedNormalNotesDetachedFromIndex;
    int updatedRelationNotesClearedFolder;
    int deletedObsoleteNotebookNameRootFolders;

    int batchesCompleted;
    int batchTotalPlanned;
  }

  private Optional<Checkpoint> fetchCheckpointSkipLock() {
    List<Checkpoint> rows =
        jdbcTemplate.query(
            """
            SELECT id, phase, topo_pairs_done, topology_pair_total, slug_prep_done,
                   detached_child_folders, updated_normal_notes, updated_relation_notes,
                   deleted_obsolete_roots, batches_completed, batch_total_planned
            FROM wiki_data_migration_checkpoint
            WHERE id = ?
            """,
            CHECKPOINT_MAPPER,
            CHECKPOINT_ID);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
  }

  private Checkpoint insertFreshCheckpointLocked() {
    int topoTotal = countTopologyPairs();
    int notebookLive = countNotebooksLive();

    Checkpoint c = new Checkpoint();
    c.phase = BatchPhase.TOPOLOGY;
    c.topoPairsDone = 0;
    c.topologyPairTotal = topoTotal;
    c.slugPrepDone = false;
    c.detachedChildFoldersFromIndexFolder = 0;
    c.updatedNormalNotesDetachedFromIndex = 0;
    c.updatedRelationNotesClearedFolder = 0;
    c.deletedObsoleteNotebookNameRootFolders = 0;
    c.batchesCompleted = 0;
    c.batchTotalPlanned = Math.addExact(topoTotal, Math.addExact(1, notebookLive));

    jdbcTemplate.update(
        """
        INSERT INTO wiki_data_migration_checkpoint
            (id, phase, topo_pairs_done, topology_pair_total, slug_prep_done,
             detached_child_folders, updated_normal_notes, updated_relation_notes,
             deleted_obsolete_roots, batches_completed, batch_total_planned)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        CHECKPOINT_ID,
        c.phase.name(),
        c.topoPairsDone,
        c.topologyPairTotal,
        c.slugPrepDone ? 1 : 0,
        c.detachedChildFoldersFromIndexFolder,
        c.updatedNormalNotesDetachedFromIndex,
        c.updatedRelationNotesClearedFolder,
        c.deletedObsoleteNotebookNameRootFolders,
        c.batchesCompleted,
        c.batchTotalPlanned);
    return c;
  }

  private void persistCheckpoint(Checkpoint c) {
    jdbcTemplate.update(
        """
        UPDATE wiki_data_migration_checkpoint
           SET phase = ?,
               topo_pairs_done = ?,
               topology_pair_total = ?,
               slug_prep_done = ?,
               detached_child_folders = ?,
               updated_normal_notes = ?,
               updated_relation_notes = ?,
               deleted_obsolete_roots = ?,
               batches_completed = ?,
               batch_total_planned = ?
         WHERE id = ?
        """,
        c.phase.name(),
        c.topoPairsDone,
        c.topologyPairTotal,
        c.slugPrepDone ? 1 : 0,
        c.detachedChildFoldersFromIndexFolder,
        c.updatedNormalNotesDetachedFromIndex,
        c.updatedRelationNotesClearedFolder,
        c.deletedObsoleteNotebookNameRootFolders,
        c.batchesCompleted,
        c.batchTotalPlanned,
        CHECKPOINT_ID);
  }

  private void clearSlugNotebookDoneTable() {
    jdbcTemplate.update("DELETE FROM wiki_data_migration_slug_notebook_done");
  }

  private void clearPersistedProgress() {
    jdbcTemplate.update("DELETE FROM wiki_data_migration_slug_notebook_done");
    jdbcTemplate.update("DELETE FROM wiki_data_migration_checkpoint WHERE id = ?", CHECKPOINT_ID);
  }

  private int countTopologyPairs() {
    Integer n =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM notebook_head_note nh
            INNER JOIN notebook n ON n.id = nh.notebook_id
            """,
            Integer.class);
    return n == null ? 0 : n;
  }

  private int countNotebooksLive() {
    Integer n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notebook", Integer.class);
    return n == null ? 0 : n;
  }

  private int countSlugNotebookDoneRows() {
    Integer n =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM wiki_data_migration_slug_notebook_done", Integer.class);
    return n == null ? 0 : n;
  }

  private int[] topologyPairAtOffset(int zeroBasedOffset) {
    List<int[]> pairs =
        jdbcTemplate.query(
            """
            SELECT nh.notebook_id, nh.head_note_id
            FROM notebook_head_note nh
            INNER JOIN notebook n ON n.id = nh.notebook_id
            ORDER BY nh.notebook_id ASC
            LIMIT 1 OFFSET ?
            """,
            (rs, rowNum) -> new int[] {rs.getInt("notebook_id"), rs.getInt("head_note_id")},
            zeroBasedOffset);
    if (pairs.isEmpty()) {
      throw new IllegalStateException(
          "topology pair offset "
              + zeroBasedOffset
              + " out of bounds (migrate checkpoint vs live data mismatch)");
    }
    return pairs.getFirst();
  }

  private Optional<Integer> nextSlugNotebookWithoutDoneRow() {
    List<Integer> ids =
        jdbcTemplate.queryForList(
            """
            SELECT n.id FROM notebook n
             WHERE NOT EXISTS (
               SELECT 1 FROM wiki_data_migration_slug_notebook_done d WHERE d.notebook_id = n.id
             )
            ORDER BY n.id ASC
            LIMIT 1
            """,
            Integer.class);
    return ids.isEmpty() ? Optional.empty() : Optional.of(ids.getFirst());
  }

  private void executeExactlyOneTransactionalBatch(Checkpoint s) {
    foldEmptyTopologyIntoSlugPrepWhenNeeded(s);

    if (s.phase == BatchPhase.TOPOLOGY) {
      int[] pair = topologyPairAtOffset(s.topoPairsDone);
      AdminDataMigrationBatchExecutor.TopologyBatchTotals totals =
          batchExecutor.detachIndexTopologyForNotebook(pair[0], pair[1]);
      s.detachedChildFoldersFromIndexFolder += totals.detachedChildFolders();
      s.updatedNormalNotesDetachedFromIndex += totals.normalNotes();
      s.updatedRelationNotesClearedFolder += totals.relationNotes();
      s.deletedObsoleteNotebookNameRootFolders += totals.deletedRoots();
      s.topoPairsDone++;
      s.batchesCompleted++;
      return;
    }

    if (s.phase == BatchPhase.SLUG_PREP) {
      batchExecutor.installSlugPrepPlaceholdersGlobally();
      s.phase = BatchPhase.SLUG_NOTEBOOKS;
      s.slugPrepDone = true;
      s.batchesCompleted++;
      return;
    }

    Optional<Integer> nextSlugNotebook = nextSlugNotebookWithoutDoneRow();
    if (nextSlugNotebook.isEmpty()) {
      return;
    }
    Integer notebookId = nextSlugNotebook.get();
    batchExecutor.regenerateSlugPathsForNotebook(notebookId);
    jdbcTemplate.update(
        """
        INSERT INTO wiki_data_migration_slug_notebook_done (notebook_id)
        VALUES (?)
        """,
        notebookId);
    s.batchesCompleted++;
  }

  private static void foldEmptyTopologyIntoSlugPrepWhenNeeded(Checkpoint s) {
    if (s.phase == BatchPhase.TOPOLOGY && s.topoPairsDone >= s.topologyPairTotal) {
      s.phase = BatchPhase.SLUG_PREP;
    }
  }

  private boolean migrationFullyDoneAfterMutation(Checkpoint c) {
    foldEmptyTopologyIntoSlugPrepWhenNeeded(c);
    if (c.phase != BatchPhase.SLUG_NOTEBOOKS) {
      return false;
    }
    return nextSlugNotebookWithoutDoneRow().isEmpty();
  }

  private AdminDataMigrationStatusDTO runningDtoFromCheckpoint(
      Checkpoint c, boolean hadCompletedPreviously) {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    copyAggregatesFromCheckpointInto(c, dto);
    dto.setNotebookCountSlugScan(0);

    dto.setMigrationInProgress(true);
    dto.setMoreBatchesRemain(true);

    dto.setCompletedOnce(hadCompletedPreviously);
    dto.setLastCompletedAt(null);

    dto.setCompletedBatchOrdinal(c.batchesCompleted);
    dto.setBatchTotalPlanned(c.batchTotalPlanned);
    dto.setBatchPhaseSummary(phaseSummaryLabel(c));

    dto.setMessage(runningCountersLine(dto));
    return dto;
  }

  private AdminDataMigrationStatusDTO finalDtoFromCheckpoint(Checkpoint c) {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    copyAggregatesFromCheckpointInto(c, dto);

    Long notebookDistinct =
        jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT id) FROM notebook", Long.class);
    dto.setNotebookCountSlugScan(notebookDistinct == null ? 0L : notebookDistinct);

    dto.setMigrationInProgress(false);
    dto.setMoreBatchesRemain(false);

    dto.setCompletedOnce(true);
    dto.setLastCompletedAt(Instant.now());

    dto.setCompletedBatchOrdinal(c.batchesCompleted);
    dto.setBatchTotalPlanned(c.batchTotalPlanned);
    dto.setBatchPhaseSummary("Done");

    dto.setMessage(summaryMessage(dto));
    return dto;
  }

  private static void copyAggregatesFromCheckpointInto(
      Checkpoint s, AdminDataMigrationStatusDTO dto) {
    dto.setDetachedChildFoldersFromIndexFolder(s.detachedChildFoldersFromIndexFolder);
    dto.setUpdatedNormalNotesDetachedFromIndex(s.updatedNormalNotesDetachedFromIndex);
    dto.setUpdatedRelationNotesClearedFolder(s.updatedRelationNotesClearedFolder);
    dto.setDeletedObsoleteNotebookNameRootFolders(s.deletedObsoleteNotebookNameRootFolders);
  }

  private String phaseSummaryLabel(Checkpoint c) {
    if (c.phase == BatchPhase.TOPOLOGY) {
      int n = Math.max(1, c.topologyPairTotal);
      int at = Math.min(c.topoPairsDone, c.topologyPairTotal);
      return "Topology batch " + at + "/" + n;
    }
    if (c.phase == BatchPhase.SLUG_PREP) {
      return "Slug preparation (globally assigns placeholders)";
    }
    int done = countSlugNotebookDoneRows();
    int totalNb = Math.max(1, countNotebooksLive());
    return "Slug notebooks " + done + "/" + totalNb + " marked done";
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

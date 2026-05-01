package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.WikiReferenceMigrationProgress;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.WikiReferenceMigrationProgressRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WikiReferenceMigrationProgressService {

  @PersistenceContext private EntityManager entityManager;

  private final WikiReferenceMigrationProgressRepository progressRepository;
  private final NoteRepository noteRepository;
  private final JdbcTemplate jdbcTemplate;

  public WikiReferenceMigrationProgressService(
      WikiReferenceMigrationProgressRepository progressRepository,
      NoteRepository noteRepository,
      JdbcTemplate jdbcTemplate) {
    this.progressRepository = progressRepository;
    this.noteRepository = noteRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  /** Joins the caller transaction so nested lookups are not wrapped in {@code readOnly=true}. */
  public Optional<WikiReferenceMigrationProgress> find(String stepName) {
    return progressRepository.findByStepName(stepName);
  }

  /**
   * Creates a new progress row or returns an existing one. Does not reset {@code processedCount} or
   * the last-processed cursor. Completed steps are returned unchanged.
   */
  @Transactional
  public WikiReferenceMigrationProgress startOrResume(String stepName, int totalCount) {
    Optional<WikiReferenceMigrationProgress> existing = progressRepository.findByStepName(stepName);
    if (existing.isPresent()) {
      WikiReferenceMigrationProgress p = existing.get();
      if (p.getStatus() == WikiReferenceMigrationStepStatus.COMPLETED) {
        return p;
      }
      if (totalCount > 0 && p.getTotalCount() == 0) {
        p.setTotalCount(totalCount);
      }
      return progressRepository.save(p);
    }
    WikiReferenceMigrationProgress created = new WikiReferenceMigrationProgress();
    created.setStepName(stepName);
    created.setStatus(WikiReferenceMigrationStepStatus.RUNNING);
    created.setTotalCount(Math.max(0, totalCount));
    created.setProcessedCount(0);
    return progressRepository.save(created);
  }

  /**
   * Note ids still to process for this step, in stable order, skipping ids {@code <=}
   * last-processed. Returns an empty list when the step is completed.
   */
  public List<Integer> pendingNoteIdsOrdered(String stepName, List<Integer> orderedCandidateIds) {
    Optional<WikiReferenceMigrationProgress> opt = progressRepository.findByStepName(stepName);
    if (opt.isEmpty()) {
      return List.copyOf(orderedCandidateIds);
    }
    WikiReferenceMigrationProgress p = opt.get();
    if (p.getStatus() == WikiReferenceMigrationStepStatus.COMPLETED) {
      return List.of();
    }
    Integer lastId = p.getLastProcessedNote() != null ? p.getLastProcessedNote().getId() : null;
    if (lastId == null) {
      return List.copyOf(orderedCandidateIds);
    }
    return orderedCandidateIds.stream().filter(id -> id > lastId).toList();
  }

  @Transactional
  public void recordBatchSuccess(
      String stepName, int lastProcessedNoteId, int newlyProcessedCount) {
    WikiReferenceMigrationProgress p = progressRepository.findByStepName(stepName).orElseThrow();
    Note last =
        noteRepository
            .findById(lastProcessedNoteId)
            .orElseThrow(
                () -> new IllegalArgumentException("Unknown note id: " + lastProcessedNoteId));
    p.setLastProcessedNote(last);
    p.setProcessedCount(p.getProcessedCount() + newlyProcessedCount);
    p.setLastError(null);
    p.setStatus(WikiReferenceMigrationStepStatus.RUNNING);
    progressRepository.save(p);
  }

  @Transactional
  public void markCompleted(String stepName) {
    WikiReferenceMigrationProgress p = progressRepository.findByStepName(stepName).orElseThrow();
    p.setStatus(WikiReferenceMigrationStepStatus.COMPLETED);
    p.setCompletedAt(new Timestamp(System.currentTimeMillis()));
    progressRepository.save(p);
  }

  /**
   * Updates failure row via JDBC to avoid Hibernate auto-flush triggered by repository queries when
   * the session holds bad cache entities.
   */
  @Transactional
  public void markFailed(String stepName, String message) {
    Timestamp now = new Timestamp(System.currentTimeMillis());
    int updated =
        jdbcTemplate.update(
            "UPDATE wiki_reference_migration_progress SET status = ?, last_error = ?, updated_at = ?"
                + " WHERE step_name = ?",
            WikiReferenceMigrationStepStatus.FAILED.name(),
            message,
            now,
            stepName);
    if (updated != 1) {
      throw new IllegalStateException(
          "No wiki_reference_migration_progress row for step: " + stepName);
    }
    entityManager.clear();
  }
}

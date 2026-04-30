package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.WikiReferenceMigrationProgress;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.WikiReferenceMigrationProgressRepository;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WikiReferenceMigrationProgressService {

  private final WikiReferenceMigrationProgressRepository progressRepository;
  private final NoteRepository noteRepository;

  public WikiReferenceMigrationProgressService(
      WikiReferenceMigrationProgressRepository progressRepository, NoteRepository noteRepository) {
    this.progressRepository = progressRepository;
    this.noteRepository = noteRepository;
  }

  @Transactional(readOnly = true)
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
  @Transactional(readOnly = true)
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

  @Transactional
  public void markFailed(String stepName, String message) {
    WikiReferenceMigrationProgress p = progressRepository.findByStepName(stepName).orElseThrow();
    p.setStatus(WikiReferenceMigrationStepStatus.FAILED);
    p.setLastError(message);
    progressRepository.save(p);
  }
}

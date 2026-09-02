package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.entities.AuthoredNoteReferenceBackfillProgress;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.AuthoredNoteReferenceBackfillProgressRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time startup backfill of {@code authored_note_reference} rows for pre-existing notes. Each
 * batch is its own transaction ({@link Propagation#REQUIRES_NEW}), committed together with the
 * progress watermark, so a mid-run failure loses at most the in-flight batch and a restart resumes
 * from the last committed watermark.
 */
@Service
public class AuthoredNoteReferenceBackfillTx {

  private static final int SINGLETON_PROGRESS_ID =
      AuthoredNoteReferenceBackfillProgressRepository.SINGLETON_ID;

  private final NoteRepository noteRepository;
  private final AuthoredNoteReferenceBackfillProgressRepository progressRepository;
  private final CanonicalDonutOrigin canonicalOrigin;
  private final NotePropertyIndexService notePropertyIndexService;

  public AuthoredNoteReferenceBackfillTx(
      NoteRepository noteRepository,
      AuthoredNoteReferenceBackfillProgressRepository progressRepository,
      CanonicalDonutOrigin canonicalOrigin,
      NotePropertyIndexService notePropertyIndexService) {
    this.noteRepository = noteRepository;
    this.progressRepository = progressRepository;
    this.canonicalOrigin = canonicalOrigin;
    this.notePropertyIndexService = notePropertyIndexService;
  }

  /** True once the backfill has fully completed — a single-row read, no note scanning. */
  @Transactional(readOnly = true)
  public boolean isComplete() {
    return progress().getCompletedAt() != null;
  }

  /** Processes the next bounded batch of notes; returns whether more notes remain. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean processNextBatch(int batchSize) {
    AuthoredNoteReferenceBackfillProgress progress = progress();
    Integer after = progress.getLastProcessedNoteId();
    List<Note> batch =
        noteRepository.findByIdGreaterThanAndDeletedAtIsNullOrderByIdAsc(
            after == null ? 0 : after, PageRequest.of(0, batchSize));

    for (Note note : batch) {
      backfillNote(note);
    }

    boolean hasMore = batch.size() == batchSize;
    if (!batch.isEmpty()) {
      progress.setLastProcessedNoteId(batch.get(batch.size() - 1).getId());
    }
    if (!hasMore) {
      progress.setCompletedAt(new Timestamp(System.currentTimeMillis()));
    }
    progressRepository.save(progress);
    return hasMore;
  }

  private void backfillNote(Note note) {
    note.replaceContent(AuthoredNoteDocument.fromContent(note.getContent(), canonicalOrigin));
    notePropertyIndexService.refreshForNote(note);
  }

  private AuthoredNoteReferenceBackfillProgress progress() {
    return progressRepository
        .findById(SINGLETON_PROGRESS_ID)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "authored_note_reference_backfill_progress singleton row missing"));
  }
}

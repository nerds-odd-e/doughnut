package com.odde.donut.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

/**
 * Single-row (id = 1) progress marker for the one-time startup backfill of {@code
 * authored_note_reference} rows onto pre-existing notes. Recorded separately from the reference
 * rows so a note with zero authored references is never mistaken for "not yet backfilled" — see
 * {@code AuthoredNoteReferenceBackfillTx}.
 */
@Entity
@Table(name = "authored_note_reference_backfill_progress")
public class AuthoredNoteReferenceBackfillProgress {

  @Id @Getter @Setter private Integer id;

  @Column(name = "last_processed_note_id")
  @Getter
  @Setter
  private Integer lastProcessedNoteId;

  @Column(name = "completed_at")
  @Getter
  @Setter
  private Timestamp completedAt;
}

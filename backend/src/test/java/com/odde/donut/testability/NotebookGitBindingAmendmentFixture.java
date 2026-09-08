package com.odde.donut.testability;

import com.odde.donut.entities.NotebookGitBinding;
import java.sql.Timestamp;
import java.time.Instant;

/** Test-only helpers for amendment eligibility on {@link NotebookGitBinding}. */
public final class NotebookGitBindingAmendmentFixture {

  private NotebookGitBindingAmendmentFixture() {}

  public static void markEligible(
      NotebookGitBinding binding, Integer noteId, Instant lastChangedAt) {
    binding.setAmendmentHead(binding.getAcceptedGitObjectId());
    binding.setAmendmentNoteId(noteId);
    binding.setAmendmentLastChangedAt(Timestamp.from(lastChangedAt));
  }
}
